package hu.zoltanegyhazi.cartinder.data.api

import hu.zoltanegyhazi.cartinder.data.BodyType
import hu.zoltanegyhazi.cartinder.data.Fuel
import hu.zoltanegyhazi.cartinder.data.SwipeDirection
import kotlinx.serialization.Serializable

// A szerver Dto.kt-jének tükre; ha ott változik valami, itt is módosítani kell.

@Serializable
data class RegisterRequest(val email: String, val password: String, val name: String, val city: String = "")

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class AuthResponse(val token: String, val user: MeDto)

@Serializable
data class MeDto(
    val id: Int,
    val email: String,
    val name: String,
    val city: String,
    val bio: String,
    val currentCar: String,
    val stats: StatsDto,
)

@Serializable
data class StatsDto(val swipes: Int, val likes: Int, val matches: Int, val listings: Int)

@Serializable
data class UpdateProfileRequest(val name: String, val city: String, val bio: String, val currentCar: String)

@Serializable
data class UserPublicDto(val id: Int, val name: String, val city: String, val bio: String, val currentCar: String)

@Serializable
data class ListingDto(
    val id: Int,
    val sellerId: Int,
    val sellerName: String,
    val make: String,
    val model: String,
    val year: Int,
    val priceHuf: Int,
    val km: Int,
    val horsepower: Int,
    val fuel: Fuel,
    val body: BodyType,
    val city: String,
    val distanceKm: Int,
    val bio: String,
    val photoUrl: String?,
    val photoCredit: String?,
    val active: Boolean,
)

@Serializable
data class ListingRequest(
    val make: String,
    val model: String,
    val year: Int,
    val priceHuf: Int,
    val km: Int,
    val horsepower: Int,
    val fuel: Fuel,
    val body: BodyType,
    val city: String,
    val bio: String = "",
    val opener: String = "",
    val active: Boolean = true,
)

@Serializable
data class SwipeRequest(val direction: SwipeDirection)

@Serializable
data class SwipeResponse(
    /** Ha azonnal match lett (pl. egy demó eladó elfogadta). */
    val match: MatchDto? = null,
)

@Serializable
data class UndoResponse(val listing: ListingDto)

@Serializable
data class IncomingLikeDto(val id: Int, val listing: ListingDto, val buyer: UserPublicDto, val superLike: Boolean, val createdAt: Long)

@Serializable
enum class Role { BUYER, SELLER }

@Serializable
data class MatchDto(
    val id: Int,
    val listing: ListingDto,
    val other: UserPublicDto,
    val role: Role,
    val superLike: Boolean,
    val lastMessage: MessageDto?,
    val unread: Int,
    val otherTyping: Boolean,
    val createdAt: Long,
)

@Serializable
data class MessageDto(val id: Int, val matchId: Int, val fromMe: Boolean, val text: String, val createdAt: Long)

@Serializable
data class SendMessageRequest(val text: String)

@Serializable
data class MessagesResponse(val messages: List<MessageDto>, val otherTyping: Boolean)

@Serializable
data class ReadRequest(val lastMessageId: Int)

@Serializable
data class ErrorResponse(val error: String)
