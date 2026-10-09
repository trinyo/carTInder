package hu.zoltanegyhazi.cartinder

import hu.zoltanegyhazi.cartinder.data.AppSnapshot
import hu.zoltanegyhazi.cartinder.data.AppState
import hu.zoltanegyhazi.cartinder.data.AppStore
import hu.zoltanegyhazi.cartinder.data.ChatBot
import hu.zoltanegyhazi.cartinder.data.Message
import hu.zoltanegyhazi.cartinder.data.Swipe
import hu.zoltanegyhazi.cartinder.data.CarFilter
import hu.zoltanegyhazi.cartinder.data.Fuel
import hu.zoltanegyhazi.cartinder.data.SwipeDirection
import hu.zoltanegyhazi.cartinder.data.formatHuf
import hu.zoltanegyhazi.cartinder.data.sampleCars
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppStateTest {
    @Test
    fun leftSwipeRemovesCarWithoutMatch() {
        val state = AppState(roll = { 1f })
        val car = state.deck.first()
        state.swipe(car, SwipeDirection.LEFT)
        assertFalse(car in state.deck)
        assertTrue(state.matches.isEmpty())
        assertNull(state.pendingMatch)
    }

    @Test
    fun rightSwipeMatchesWhenRollBeatsPickiness() {
        val state = AppState(roll = { 0.99f })
        val car = state.deck.first()
        state.swipe(car, SwipeDirection.RIGHT)
        assertEquals(car, state.matches.single().car)
        assertNotNull(state.pendingMatch)
    }

    @Test
    fun rightSwipeFailsForPickyCar() {
        val picky = sampleCars.maxBy { it.pickiness }
        val state = AppState(roll = { picky.pickiness - 0.01f })
        state.swipe(picky, SwipeDirection.RIGHT)
        assertTrue(state.matches.isEmpty())
    }

    @Test
    fun superLikeAlwaysMatches() {
        val state = AppState(roll = { 0f })
        val car = sampleCars.maxBy { it.pickiness }
        state.swipe(car, SwipeDirection.UP)
        assertTrue(state.matches.single().superLike)
    }

    @Test
    fun undoRestoresCarAndRemovesMatch() {
        val state = AppState(roll = { 1f })
        val car = state.deck.first()
        state.swipe(car, SwipeDirection.RIGHT)
        state.undo()
        assertEquals(car, state.deck.first())
        assertTrue(state.matches.isEmpty())
        assertFalse(state.canUndo)
    }

    @Test
    fun filterLimitsDeck() {
        val state = AppState()
        state.filter = CarFilter(fuels = setOf(Fuel.ELEKTROMOS))
        assertTrue(state.deck.isNotEmpty())
        assertTrue(state.deck.all { it.fuel == Fuel.ELEKTROMOS })
        state.filter = CarFilter(maxPrice = 1_000_000)
        assertTrue(state.deck.all { it.priceHuf <= 1_000_000 })
    }

    @Test
    fun formatsHungarianPrice() {
        assertEquals("1 290 000 Ft", formatHuf(1_290_000))
    }

    @Test
    fun matchStartsChatWithOpener() {
        val state = AppState(roll = { 1f })
        val car = state.deck.first()
        state.swipe(car, SwipeDirection.UP)
        val thread = state.messages(car.id)
        assertEquals(1, thread.size)
        assertEquals(car.opener, thread.single().text)
        assertFalse(thread.single().fromMe)
    }

    @Test
    fun sendAndReplyAppendToThread() {
        val state = AppState(roll = { 1f })
        val car = state.deck.first()
        state.swipe(car, SwipeDirection.UP)
        state.sendMessage(car, "  Mennyi az ár?  ")
        state.receiveReply(car)
        val thread = state.messages(car.id)
        assertEquals(listOf(false, true, false), thread.map { it.fromMe })
        assertEquals("Mennyi az ár?", thread[1].text)
    }

    @Test
    fun cannotMessageUnmatchedCar() {
        val state = AppState(roll = { 1f })
        val car = state.deck.first()
        state.sendMessage(car, "Szia")
        assertTrue(state.messages(car.id).isEmpty())
    }

    @Test
    fun unmatchRemovesMatchAndChatButKeepsCarOutOfDeck() {
        val state = AppState(roll = { 1f })
        val car = state.deck.first()
        state.swipe(car, SwipeDirection.UP)
        state.unmatch(car)
        assertTrue(state.matches.isEmpty())
        assertTrue(state.messages(car.id).isEmpty())
        assertFalse(car in state.deck)
    }

    @Test
    fun restoresFromSnapshot() {
        val car = sampleCars[0]
        val state = AppState(
            snapshot = AppSnapshot(
                swipes = listOf(Swipe(car.id, SwipeDirection.RIGHT, matched = true), Swipe(sampleCars[1].id, SwipeDirection.LEFT, matched = false)),
                messages = listOf(Message(car.id, fromMe = false, text = "hi", sentAt = 1)),
                filter = CarFilter(maxKm = 100_000),
            ),
        )
        assertEquals(car, state.matches.single().car)
        assertEquals("hi", state.messages(car.id).single().text)
        assertEquals(2, state.swipeCount)
        assertTrue(state.deck.all { it.km <= 100_000 })
    }

    @Test
    fun notifiesStoreOfChanges() {
        val events = mutableListOf<String>()
        val store = object : AppStore {
            override fun swipeAdded(swipe: Swipe) { events += "swipe:${swipe.carId}:${swipe.matched}" }
            override fun swipeRemoved(carId: Int) { events += "remove:$carId" }
            override fun messageAdded(message: Message) { events += "msg:${message.fromMe}" }
            override fun filterChanged(filter: CarFilter) { events += "filter" }
        }
        val state = AppState(store = store, roll = { 1f })
        val car = state.deck.first()
        state.swipe(car, SwipeDirection.RIGHT)
        state.sendMessage(car, "Szia")
        state.undo()
        state.filter = CarFilter(maxKm = 200_000)
        state.filter = CarFilter(maxKm = 200_000)
        assertEquals(listOf("swipe:${car.id}:true", "msg:false", "msg:true", "remove:${car.id}", "filter"), events)
    }

    @Test
    fun chatBotMatchesWholeWordPrefixes() {
        val car = sampleCars[0]
        val price = ChatBot.reply(car, "Mennyi az ára?", 2, kotlin.random.Random(1))
        assertTrue(price, "Ft" in price || "forint" in price || "tankolás" in price)
        // A "már" ne számítson ár-kérdésnek.
        val other = ChatBot.reply(car, "már nagyon vártam", 2, kotlin.random.Random(1))
        assertFalse(other, "Ft" in other)
    }
}
