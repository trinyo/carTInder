package hu.zoltanegyhazi.cartinder

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
import hu.zoltanegyhazi.cartinder.data.api.MessagesResponse
import hu.zoltanegyhazi.cartinder.data.api.RegisterRequest
import hu.zoltanegyhazi.cartinder.data.api.SwipeResponse
import hu.zoltanegyhazi.cartinder.data.api.UndoResponse
import hu.zoltanegyhazi.cartinder.data.api.UpdateProfileRequest

/** Memóriabeli backend a ViewModel tesztekhez. */
class FakeBackend : CarTinderBackend {
    var meDto = testMe()
    val feedListings = mutableListOf<ListingDto>()
    val swiped = mutableListOf<Pair<ListingDto, SwipeDirection>>()
    var matchOnSwipe: (ListingDto, SwipeDirection) -> MatchDto? = { _, _ -> null }
    var matchList = mutableListOf<MatchDto>()
    val likeList = mutableListOf<IncomingLikeDto>()
    val serverMessages = mutableMapOf<Int, MutableList<MessageDto>>()
    val readMarks = mutableListOf<Pair<Int, Int>>()
    val feedRequests = mutableListOf<CarFilter>()
    /** Ha nem null, minden hívás ezt dobja. */
    var failWith: ApiException? = null

    private fun check() { failWith?.let { throw it } }

    override suspend fun register(req: RegisterRequest) = login(LoginRequest(req.email, req.password))
    override suspend fun login(req: LoginRequest): AuthResponse {
        check()
        if (req.password != "jelszo123") throw ApiException(401, "Hibás e-mail cím vagy jelszó.")
        return AuthResponse("token-${req.email}", meDto)
    }
    override suspend fun me(): MeDto { check(); return meDto }
    override suspend fun updateProfile(req: UpdateProfileRequest): MeDto {
        check(); meDto = meDto.copy(name = req.name, city = req.city); return meDto
    }

    override suspend fun feed(filter: CarFilter): List<ListingDto> {
        check()
        feedRequests += filter
        val seen = swiped.map { it.first.id }.toSet()
        return feedListings.filter { it.id !in seen && it.priceHuf <= filter.maxPrice }
    }
    override suspend fun swipe(listingId: Int, direction: SwipeDirection): SwipeResponse {
        check()
        val listing = feedListings.first { it.id == listingId }
        swiped += listing to direction
        return SwipeResponse(matchOnSwipe(listing, direction)?.also { matchList.add(0, it) })
    }
    override suspend fun undo(): UndoResponse {
        check()
        val (listing, _) = swiped.removeAt(swiped.lastIndex)
        matchList.removeAll { it.listing.id == listing.id }
        return UndoResponse(listing)
    }

    override suspend fun likes(): List<IncomingLikeDto> { check(); return likeList.toList() }
    override suspend fun acceptLike(likeId: Int): MatchDto {
        check()
        val like = likeList.first { it.id == likeId }
        likeList.remove(like)
        return match(500 + likeId, like.listing).also { matchList.add(0, it) }
    }
    override suspend fun declineLike(likeId: Int) { check(); likeList.removeAll { it.id == likeId } }

    override suspend fun matches(): List<MatchDto> { check(); return matchList.toList() }
    override suspend fun unmatch(matchId: Int) { check(); matchList.removeAll { it.id == matchId } }
    override suspend fun messages(matchId: Int, after: Int): MessagesResponse {
        check()
        if (matchList.none { it.id == matchId }) throw ApiException(404, "A match nem található.")
        return MessagesResponse(serverMessages[matchId].orEmpty().filter { it.id > after }, otherTyping = false)
    }
    override suspend fun sendMessage(matchId: Int, text: String): MessageDto {
        check()
        val list = serverMessages.getOrPut(matchId) { mutableListOf() }
        val msg = MessageDto((serverMessages.values.flatten().maxOfOrNull { it.id } ?: 0) + 1, matchId, true, text, 0)
        list += msg
        return msg
    }
    override suspend fun markRead(matchId: Int, lastMessageId: Int) { readMarks += matchId to lastMessageId }

    override suspend fun myListings(): List<ListingDto> { check(); return emptyList() }
    override suspend fun createListing(req: ListingRequest): ListingDto { check(); return listing(900, req.make, sellerId = meDto.id) }
    override suspend fun updateListing(listingId: Int, req: ListingRequest): ListingDto { check(); return listing(listingId, req.make) }
    override suspend fun deleteListing(listingId: Int) { check() }
    override suspend fun uploadPhoto(listingId: Int, bytes: ByteArray, mimeType: String): ListingDto {
        check(); return listing(listingId).copy(photoUrl = "/uploads/x.jpg")
    }
}

class FakeSession(override var token: String? = null, override var serverUrl: String = "http://test") : SessionStore

class FakeCache : LocalCache {
    var filter = CarFilter()
    val messages = mutableMapOf<Int, MutableList<MessageDto>>()
    var cleared = false

    override suspend fun loadFilter() = filter
    override suspend fun saveFilter(filter: CarFilter) { this.filter = filter }
    override suspend fun messages(matchId: Int) = messages[matchId].orEmpty().toList()
    override suspend fun saveMessages(messages: List<MessageDto>) {
        messages.forEach { m -> this.messages.getOrPut(m.matchId) { mutableListOf() }.apply { removeAll { it.id == m.id }; add(m) } }
    }
    override suspend fun deleteMessages(matchId: Int) { messages.remove(matchId) }
    override suspend fun clear() { messages.clear(); cleared = true }
}
