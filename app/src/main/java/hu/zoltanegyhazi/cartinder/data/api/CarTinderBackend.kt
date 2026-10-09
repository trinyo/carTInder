package hu.zoltanegyhazi.cartinder.data.api

import hu.zoltanegyhazi.cartinder.data.CarFilter
import hu.zoltanegyhazi.cartinder.data.SwipeDirection

/** A szerver API-ja; a tesztekben hamis megvalósítással cserélhető. */
interface CarTinderBackend {
    suspend fun register(req: RegisterRequest): AuthResponse
    suspend fun login(req: LoginRequest): AuthResponse
    suspend fun me(): MeDto
    suspend fun updateProfile(req: UpdateProfileRequest): MeDto

    suspend fun feed(filter: CarFilter): List<ListingDto>
    suspend fun swipe(listingId: Int, direction: SwipeDirection): SwipeResponse
    suspend fun undo(): UndoResponse

    suspend fun likes(): List<IncomingLikeDto>
    suspend fun acceptLike(likeId: Int): MatchDto
    suspend fun declineLike(likeId: Int)

    suspend fun matches(): List<MatchDto>
    suspend fun unmatch(matchId: Int)
    suspend fun messages(matchId: Int, after: Int): MessagesResponse
    suspend fun sendMessage(matchId: Int, text: String): MessageDto
    suspend fun markRead(matchId: Int, lastMessageId: Int)

    suspend fun myListings(): List<ListingDto>
    suspend fun createListing(req: ListingRequest): ListingDto
    suspend fun updateListing(listingId: Int, req: ListingRequest): ListingDto
    suspend fun deleteListing(listingId: Int)
    suspend fun uploadPhoto(listingId: Int, bytes: ByteArray, mimeType: String): ListingDto
}

/** Szerverhiba vagy hálózati hiba, felhasználónak mutatható üzenettel. */
class ApiException(val status: Int, override val message: String, cause: Throwable? = null) : Exception(message, cause) {
    val isUnauthorized get() = status == 401
}
