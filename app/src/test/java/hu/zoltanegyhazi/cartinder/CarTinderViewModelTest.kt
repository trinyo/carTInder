package hu.zoltanegyhazi.cartinder

import hu.zoltanegyhazi.cartinder.data.CarFilter
import hu.zoltanegyhazi.cartinder.data.SwipeDirection
import hu.zoltanegyhazi.cartinder.data.api.ApiException
import hu.zoltanegyhazi.cartinder.data.api.IncomingLikeDto
import hu.zoltanegyhazi.cartinder.data.api.MessageDto
import hu.zoltanegyhazi.cartinder.data.api.UserPublicDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CarTinderViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val backend = FakeBackend().apply { feedListings += (1..5).map { listing(it) } }
    private val session = FakeSession()
    private val cache = FakeCache()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.loggedIn(): CarTinderViewModel {
        session.token = "token"
        return CarTinderViewModel(backend, session, cache).also { advanceUntilIdle() }
    }

    @Test
    fun startsLoggedOutWithoutToken() = runTest(dispatcher) {
        val vm = CarTinderViewModel(backend, session, cache)
        advanceUntilIdle()
        assertEquals(Session.LoggedOut, vm.state)
    }

    @Test
    fun restoresSessionAndLoadsFeed() = runTest(dispatcher) {
        val vm = loggedIn()
        assertTrue(vm.state is Session.LoggedIn)
        assertEquals(5, vm.feed.size)
    }

    @Test
    fun loginStoresTokenAndServerUrl() = runTest(dispatcher) {
        val vm = CarTinderViewModel(backend, session, cache)
        advanceUntilIdle()
        vm.login("anna@example.com", "jelszo123", " http://192.168.1.10:8080/ ")
        advanceUntilIdle()
        assertEquals("token-anna@example.com", session.token)
        assertEquals("http://192.168.1.10:8080", session.serverUrl)
        assertTrue(vm.state is Session.LoggedIn)
        assertNull(vm.authError)
    }

    @Test
    fun wrongPasswordShowsError() = runTest(dispatcher) {
        val vm = CarTinderViewModel(backend, session, cache)
        advanceUntilIdle()
        vm.login("anna@example.com", "rossz", "http://test")
        advanceUntilIdle()
        assertEquals(Session.LoggedOut, vm.state)
        assertEquals("Hibás e-mail cím vagy jelszó.", vm.authError)
        assertNull(session.token)
    }

    @Test
    fun swipeRemovesCardAndShowsMatch() = runTest(dispatcher) {
        backend.matchOnSwipe = { l, d -> if (d == SwipeDirection.UP) match(77, l) else null }
        val vm = loggedIn()
        val first = vm.feed.first()
        vm.swipe(first, SwipeDirection.UP)
        assertFalse("a kártya azonnal eltűnik", vm.feed.any { it.id == first.id })
        advanceUntilIdle()
        assertEquals(77, vm.pendingMatch?.id)
        assertEquals(77, vm.matches.first().id)
        assertEquals(1, vm.undoable)
    }

    @Test
    fun failedSwipeBringsCardBack() = runTest(dispatcher) {
        val vm = loggedIn()
        val first = vm.feed.first()
        backend.failWith = ApiException(0, "Nem érem el a szervert.")
        vm.swipe(first, SwipeDirection.RIGHT)
        advanceUntilIdle()
        assertEquals(first.id, vm.feed.first().id)
        assertEquals("Nem érem el a szervert.", vm.notice)
        assertEquals(0, vm.undoable)
    }

    @Test
    fun undoPutsCardBackOnTopAndRemovesMatch() = runTest(dispatcher) {
        backend.matchOnSwipe = { l, _ -> match(77, l) }
        val vm = loggedIn()
        val first = vm.feed.first()
        vm.swipe(first, SwipeDirection.RIGHT)
        advanceUntilIdle()
        vm.undo()
        advanceUntilIdle()
        assertEquals(first.id, vm.feed.first().id)
        assertTrue(vm.matches.isEmpty())
        assertEquals(0, vm.undoable)
    }

    @Test
    fun filterChangeIsSavedAndRefetchedAfterDebounce() = runTest(dispatcher) {
        val vm = loggedIn()
        val before = backend.feedRequests.size
        vm.updateFilter(CarFilter(maxPrice = 900_000))
        vm.updateFilter(CarFilter(maxPrice = 800_000))
        advanceTimeBy(200)
        assertEquals("a gyors egymásutáni változások nem kérnek külön", before, backend.feedRequests.size)
        advanceUntilIdle()
        assertEquals(before + 1, backend.feedRequests.size)
        assertEquals(800_000, cache.filter.maxPrice)
        assertTrue(vm.feed.isEmpty())
    }

    @Test
    fun syncThreadAppendsCachesAndMarksRead() = runTest(dispatcher) {
        val l = listing(1)
        backend.matchList += match(10, l, unread = 2)
        backend.serverMessages[10] = mutableListOf(
            MessageDto(1, 10, false, "Szia!", 1),
            MessageDto(2, 10, false, "Kávé?", 2),
        )
        val vm = loggedIn()
        vm.openThread(10)
        assertEquals(listOf("Szia!", "Kávé?"), vm.messages(10).map { it.text })
        assertEquals(listOf(10 to 2), backend.readMarks)
        assertEquals(0, vm.matches.single().unread)
        assertEquals(2, cache.messages[10]?.size)

        assertTrue(vm.send(10, "Mehet!"))
        assertEquals("Mehet!", vm.messages(10).last().text)
        assertEquals("Mehet!", vm.matches.single().lastMessage?.text)
    }

    @Test
    fun openThreadShowsCacheFirst() = runTest(dispatcher) {
        backend.matchList += match(10, listing(1))
        cache.messages[10] = mutableListOf(MessageDto(1, 10, false, "régi", 1))
        backend.serverMessages[10] = mutableListOf(MessageDto(1, 10, false, "régi", 1))
        val vm = loggedIn()
        vm.openThread(10)
        assertEquals(listOf("régi"), vm.messages(10).map { it.text })
        assertTrue("a már meglévőt nem kéri le újra", backend.readMarks.isEmpty())
    }

    @Test
    fun unmatchedThreadDisappears() = runTest(dispatcher) {
        backend.matchList += match(10, listing(1))
        val vm = loggedIn()
        backend.matchList.clear()
        vm.syncThread(10)
        assertTrue(vm.matches.isEmpty())
    }

    @Test
    fun acceptingLikeCreatesMatch() = runTest(dispatcher) {
        backend.likeList += IncomingLikeDto(3, listing(1, sellerId = 1), UserPublicDto(2, "Vera", "", "", ""), superLike = true, createdAt = 1)
        val vm = loggedIn()
        assertEquals(1, vm.likes.size)
        vm.acceptLike(vm.likes.single())
        advanceUntilIdle()
        assertTrue(vm.likes.isEmpty())
        assertEquals(503, vm.matches.first().id)
        assertEquals("Match Vera vevővel!", vm.notice)
    }

    @Test
    fun expiredTokenLogsOut() = runTest(dispatcher) {
        val vm = loggedIn()
        backend.failWith = ApiException(401, "Bejelentkezés szükséges.")
        vm.refreshFeed()
        advanceUntilIdle()
        assertEquals(Session.LoggedOut, vm.state)
        assertNull(session.token)
        assertNotNull(vm.authError)
        assertTrue(cache.cleared)
    }

    @Test
    fun offlineStartKeepsToken() = runTest(dispatcher) {
        session.token = "token"
        backend.failWith = ApiException(0, "Nem érem el a szervert.")
        val vm = CarTinderViewModel(backend, session, cache)
        advanceUntilIdle()
        assertEquals(Session.LoggedOut, vm.state)
        assertEquals("token", session.token)
        assertEquals("Nem érem el a szervert.", vm.authError)
    }

    @Test
    fun logoutClearsEverything() = runTest(dispatcher) {
        val vm = loggedIn()
        vm.logout()
        advanceUntilIdle()
        assertEquals(Session.LoggedOut, vm.state)
        assertTrue(vm.feed.isEmpty())
        assertNull(session.token)
        assertTrue(cache.cleared)
    }
}
