package hu.zoltanegyhazi.cartinder

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hu.zoltanegyhazi.cartinder.data.CarFilter
import hu.zoltanegyhazi.cartinder.data.LocalCache
import hu.zoltanegyhazi.cartinder.data.SessionStore
import hu.zoltanegyhazi.cartinder.data.SwipeDirection
import hu.zoltanegyhazi.cartinder.data.api.ApiException
import hu.zoltanegyhazi.cartinder.data.api.AuthResponse
import hu.zoltanegyhazi.cartinder.data.api.CarTinderBackend
import hu.zoltanegyhazi.cartinder.data.api.IncomingLikeDto
import hu.zoltanegyhazi.cartinder.data.api.ListingDto
import hu.zoltanegyhazi.cartinder.data.api.ListingRequest
import hu.zoltanegyhazi.cartinder.data.api.LoginRequest
import hu.zoltanegyhazi.cartinder.data.api.MatchDto
import hu.zoltanegyhazi.cartinder.data.api.MeDto
import hu.zoltanegyhazi.cartinder.data.api.MessageDto
import hu.zoltanegyhazi.cartinder.data.api.RegisterRequest
import hu.zoltanegyhazi.cartinder.data.api.Role
import hu.zoltanegyhazi.cartinder.data.api.UpdateProfileRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface Session {
    data object Loading : Session
    data object LoggedOut : Session
    data class LoggedIn(val me: MeDto) : Session
}

class CarTinderViewModel(
    private val backend: CarTinderBackend,
    private val session: SessionStore,
    private val cache: LocalCache,
) : ViewModel() {

    var state by mutableStateOf<Session>(Session.Loading)
        private set
    var authBusy by mutableStateOf(false)
        private set
    var authError by mutableStateOf<String?>(null)
        private set

    var feed by mutableStateOf<List<ListingDto>>(emptyList())
        private set
    var feedLoading by mutableStateOf(false)
        private set
    var feedError by mutableStateOf<String?>(null)
        private set
    var filter by mutableStateOf(CarFilter())
        private set
    /** Ennyi swipe vonható vissza ebben a munkamenetben. */
    var undoable by mutableStateOf(0)
        private set

    var matches by mutableStateOf<List<MatchDto>>(emptyList())
        private set
    var likes by mutableStateOf<List<IncomingLikeDto>>(emptyList())
        private set
    var myListings by mutableStateOf<List<ListingDto>>(emptyList())
        private set
    var pendingMatch by mutableStateOf<MatchDto?>(null)

    private val threads = mutableStateMapOf<Int, List<MessageDto>>()
    private val typing = mutableStateMapOf<Int, Boolean>()

    /** Egyszer megjelenítendő üzenet (snackbar). */
    var notice by mutableStateOf<String?>(null)

    val serverUrl get() = session.serverUrl
    val me get() = (state as? Session.LoggedIn)?.me

    private var feedJob: Job? = null

    init {
        viewModelScope.launch {
            filter = cache.loadFilter()
            restoreSession()
        }
    }

    val hasSavedLogin get() = session.token != null

    /** Mentett tokennel újrapróbálja a belépést (pl. ha induláskor nem volt elérhető a szerver). */
    fun retrySavedLogin() {
        if (state == Session.LoggedOut && session.token != null) viewModelScope.launch { restoreSession() }
    }

    private suspend fun restoreSession() {
        if (session.token == null) {
            state = Session.LoggedOut
            return
        }
        try {
            state = Session.LoggedIn(backend.me())
            authError = null
            refreshAll()
        } catch (e: ApiException) {
            if (e.isUnauthorized) logout() else {
                // Nincs net: maradunk kijelentkezve, de a token megmarad a következő próbára.
                authError = e.message
                state = Session.LoggedOut
            }
        }
    }

    // --- Fiók ---

    fun login(email: String, password: String, serverUrl: String) = authenticate(serverUrl) {
        backend.login(LoginRequest(email, password))
    }

    fun register(email: String, password: String, name: String, city: String, serverUrl: String) = authenticate(serverUrl) {
        backend.register(RegisterRequest(email, password, name, city))
    }

    private fun authenticate(serverUrl: String, call: suspend () -> AuthResponse) {
        if (authBusy) return
        session.serverUrl = serverUrl.trim().trimEnd('/')
        authBusy = true
        authError = null
        viewModelScope.launch {
            try {
                val auth = call()
                session.token = auth.token
                state = Session.LoggedIn(auth.user)
                refreshAll()
            } catch (e: ApiException) {
                authError = e.message
            } finally {
                authBusy = false
            }
        }
    }

    fun logout() {
        session.token = null
        state = Session.LoggedOut
        feed = emptyList()
        matches = emptyList()
        likes = emptyList()
        myListings = emptyList()
        threads.clear()
        typing.clear()
        pendingMatch = null
        undoable = 0
        viewModelScope.launch { cache.clear() }
    }

    fun updateProfile(name: String, city: String, bio: String, currentCar: String) = launchSafe {
        state = Session.LoggedIn(backend.updateProfile(UpdateProfileRequest(name, city, bio, currentCar)))
        notice = "Profil mentve."
    }

    private suspend fun refreshMe() {
        state = Session.LoggedIn(backend.me())
    }

    fun refreshAll() {
        refreshFeed()
        refreshInbox()
        launchSafe { myListings = backend.myListings() }
    }

    // --- Felfedezés ---

    fun refreshFeed(debounceMs: Long = 0) {
        feedJob?.cancel()
        feedJob = viewModelScope.launch {
            if (debounceMs > 0) delay(debounceMs)
            feedLoading = true
            try {
                feed = backend.feed(filter)
                feedError = null
            } catch (e: ApiException) {
                if (handleAuth(e)) return@launch
                feedError = e.message
            } finally {
                feedLoading = false
            }
        }
    }

    fun updateFilter(newFilter: CarFilter) {
        if (newFilter == filter) return
        filter = newFilter
        viewModelScope.launch { cache.saveFilter(newFilter) }
        refreshFeed(debounceMs = 400)
    }

    /** A kártya animációja után hívandó; a kártya azonnal eltűnik, hiba esetén visszajön. */
    fun swipe(listing: ListingDto, direction: SwipeDirection) {
        feed = feed.filterNot { it.id == listing.id }
        viewModelScope.launch {
            try {
                val res = backend.swipe(listing.id, direction)
                undoable++
                res.match?.let { match ->
                    matches = listOf(match) + matches.filterNot { it.id == match.id }
                    pendingMatch = match
                }
                if (feed.size < 3) refreshFeed()
            } catch (e: ApiException) {
                if (handleAuth(e)) return@launch
                if (e.status != 409) {
                    feed = listOf(listing) + feed
                    notice = e.message
                }
            }
        }
    }

    fun undo() = launchSafe {
        val res = backend.undo()
        undoable = (undoable - 1).coerceAtLeast(0)
        feed = listOf(res.listing) + feed.filterNot { it.id == res.listing.id }
        matches.firstOrNull { it.listing.id == res.listing.id && it.role == Role.BUYER }?.let { removed ->
            matches = matches - removed
            threads.remove(removed.id)
            cache.deleteMessages(removed.id)
        }
    }

    // --- Matchek és kedvelések ---

    fun refreshInbox() = launchSafe(quiet = true) {
        matches = backend.matches()
        likes = backend.likes()
        matches.forEach { typing[it.id] = it.otherTyping }
    }

    fun acceptLike(like: IncomingLikeDto) = launchSafe {
        likes = likes.filterNot { it.id == like.id }
        val match = backend.acceptLike(like.id)
        matches = listOf(match) + matches
        notice = "Match ${like.buyer.name} vevővel!"
    }

    fun declineLike(like: IncomingLikeDto) = launchSafe {
        likes = likes.filterNot { it.id == like.id }
        backend.declineLike(like.id)
    }

    fun unmatch(match: MatchDto) = launchSafe {
        backend.unmatch(match.id)
        matches = matches.filterNot { it.id == match.id }
        threads.remove(match.id)
        cache.deleteMessages(match.id)
    }

    // --- Chat ---

    fun messages(matchId: Int): List<MessageDto> = threads[matchId].orEmpty()

    fun isTyping(matchId: Int) = typing[matchId] == true

    /** A gyorsítótárból azonnal betölti, majd a szerverről frissíti a beszélgetést. */
    suspend fun openThread(matchId: Int) {
        if (threads[matchId] == null) threads[matchId] = cache.messages(matchId)
        syncThread(matchId)
    }

    /** Lekéri az új üzeneteket, és olvasottnak jelöli őket. */
    suspend fun syncThread(matchId: Int) {
        try {
            val current = threads[matchId].orEmpty()
            val res = backend.messages(matchId, after = current.lastOrNull()?.id ?: 0)
            typing[matchId] = res.otherTyping
            if (res.messages.isEmpty()) return
            val merged = (current + res.messages).distinctBy { it.id }.sortedBy { it.id }
            threads[matchId] = merged
            cache.saveMessages(res.messages)
            val last = merged.last()
            matches = matches.map { if (it.id == matchId) it.copy(lastMessage = last, unread = 0) else it }
            if (res.messages.any { !it.fromMe }) backend.markRead(matchId, last.id)
        } catch (e: ApiException) {
            if (e.status == 404) {
                // Közben unmatch lett.
                matches = matches.filterNot { it.id == matchId }
                threads.remove(matchId)
                cache.deleteMessages(matchId)
            } else {
                handleAuth(e)
            }
        }
    }

    /** true, ha sikerült elküldeni. */
    suspend fun send(matchId: Int, text: String): Boolean = try {
        val sent = backend.sendMessage(matchId, text)
        threads[matchId] = (threads[matchId].orEmpty() + sent).distinctBy { it.id }
        cache.saveMessages(listOf(sent))
        matches = matches.map { if (it.id == matchId) it.copy(lastMessage = sent) else it }
        true
    } catch (e: ApiException) {
        if (!handleAuth(e)) notice = e.message
        false
    }

    // --- Saját hirdetések ---

    /** Létrehoz vagy frissít egy hirdetést, és ha van, feltölti a fotót. true, ha sikerült. */
    suspend fun saveListing(listingId: Int?, req: ListingRequest, photo: Pair<ByteArray, String>?): Boolean = try {
        var saved = if (listingId == null) backend.createListing(req) else backend.updateListing(listingId, req)
        if (photo != null) saved = backend.uploadPhoto(saved.id, photo.first, photo.second)
        myListings = listOf(saved) + myListings.filterNot { it.id == saved.id }
        notice = if (listingId == null) "Hirdetés feladva." else "Hirdetés mentve."
        refreshMe()
        true
    } catch (e: ApiException) {
        if (!handleAuth(e)) notice = e.message
        false
    }

    fun deleteListing(listing: ListingDto) = launchSafe {
        backend.deleteListing(listing.id)
        myListings = myListings.filterNot { it.id == listing.id }
        likes = likes.filterNot { it.listing.id == listing.id }
        matches = matches.filterNot { it.listing.id == listing.id }
        refreshMe()
        notice = "Hirdetés törölve."
    }

    // --- Segédek ---

    private fun launchSafe(quiet: Boolean = false, block: suspend () -> Unit) = viewModelScope.launch {
        try {
            block()
        } catch (e: ApiException) {
            if (!handleAuth(e) && !quiet) notice = e.message
        }
    }

    /** Lejárt token esetén kijelentkeztet. true, ha ez történt. */
    private fun handleAuth(e: ApiException): Boolean {
        if (!e.isUnauthorized || state !is Session.LoggedIn) return false
        logout()
        authError = "Lejárt a bejelentkezésed, lépj be újra."
        return true
    }
}
