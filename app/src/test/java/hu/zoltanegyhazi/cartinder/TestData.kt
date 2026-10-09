package hu.zoltanegyhazi.cartinder

import hu.zoltanegyhazi.cartinder.data.BodyType
import hu.zoltanegyhazi.cartinder.data.Fuel
import hu.zoltanegyhazi.cartinder.data.api.ListingDto
import hu.zoltanegyhazi.cartinder.data.api.MatchDto
import hu.zoltanegyhazi.cartinder.data.api.MeDto
import hu.zoltanegyhazi.cartinder.data.api.MessageDto
import hu.zoltanegyhazi.cartinder.data.api.Role
import hu.zoltanegyhazi.cartinder.data.api.StatsDto
import hu.zoltanegyhazi.cartinder.data.api.UserPublicDto

fun listing(id: Int, make: String = "Opel", sellerId: Int = 100 + id) = ListingDto(
    id = id, sellerId = sellerId, sellerName = "Eladó $id", make = make, model = "Astra", year = 2005,
    priceHuf = 1_000_000, km = 200_000, horsepower = 90, fuel = Fuel.BENZIN, body = BodyType.HATCHBACK,
    city = "Szeged", distanceKm = 10, bio = "", photoUrl = null, photoCredit = null, active = true,
)

fun testMe(id: Int = 1, name: String = "Anna") =
    MeDto(id, "anna@example.com", name, "Budapest", "", "", StatsDto(0, 0, 0, 0))

fun match(id: Int, listing: ListingDto, lastMessage: MessageDto? = null, unread: Int = 0) = MatchDto(
    id = id, listing = listing, other = UserPublicDto(listing.sellerId, listing.sellerName, "Szeged", "", ""),
    role = Role.BUYER, superLike = false, lastMessage = lastMessage, unread = unread, otherTyping = false, createdAt = 1,
)
