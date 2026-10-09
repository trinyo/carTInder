package hu.zoltanegyhazi.cartinder.server

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.delay
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ApiTest {

    private class Api(val client: HttpClient) {
        suspend fun register(email: String, name: String = email.substringBefore('@')): AuthResponse {
            val res = client.post("/api/auth/register") {
                json(RegisterRequest(email, "jelszo123", name, "Budapest"))
            }
            assertEquals(HttpStatusCode.Created, res.status)
            return res.body()
        }

        suspend fun get(path: String, token: String) = client.get(path) { bearerAuth(token) }
        suspend fun delete(path: String, token: String) = client.delete(path) { bearerAuth(token) }
        suspend inline fun <reified T : Any> post(path: String, token: String, body: T) = client.post(path) {
            bearerAuth(token)
            json(body)
        }
        suspend fun post(path: String, token: String) = client.post(path) { bearerAuth(token) }
        suspend inline fun <reified T : Any> put(path: String, token: String, body: T) = client.put(path) {
            bearerAuth(token)
            json(body)
        }

        suspend fun feed(token: String, query: String = ""): List<ListingDto> = get("/api/feed$query", token).body()
        suspend fun matches(token: String): List<MatchDto> = get("/api/matches", token).body()
        suspend fun swipe(token: String, listingId: Int, direction: SwipeDirection): HttpResponse =
            post("/api/listings/$listingId/swipe", token, SwipeRequest(direction))
        suspend fun messages(token: String, matchId: Int): MessagesResponse =
            get("/api/matches/$matchId/messages", token).body()
    }

    private fun apiTest(block: suspend Api.(ApplicationTestBuilder) -> Unit) = testApplication {
        val dir = createTempDirectory("cartinder-test").toFile()
        val config = Config(
            databaseUrl = "jdbc:sqlite:${File(dir, "test.db").absolutePath}",
            jwtSecret = "test-secret",
            uploadDir = File(dir, "uploads"),
            botDelayMs = 0L..0L,
        )
        application { module(config, Random(42)) }
        val client = createClient { install(ContentNegotiation) { json() } }
        try {
            Api(client).block(this)
        } finally {
            dir.deleteRecursively()
        }
    }

    private fun sampleListing(make: String = "Opel", price: Int = 1_500_000) = ListingRequest(
        make = make, model = "Corsa C", year = 2004, priceHuf = price, km = 180_000, horsepower = 60,
        fuel = Fuel.BENZIN, body = BodyType.HATCHBACK, city = "Szeged", bio = "Megbízható.",
    )

    // --- Fiókok ---

    @Test
    fun registerLoginAndMe() = apiTest {
        val auth = register("anna@example.com", "Anna")
        assertEquals("Anna", auth.user.name)

        val me: MeDto = get("/api/me", auth.token).body()
        assertEquals("anna@example.com", me.email)

        val login = client.post("/api/auth/login") { json(LoginRequest("ANNA@example.com ", "jelszo123")) }
        assertEquals(HttpStatusCode.OK, login.status)

        val wrong = client.post("/api/auth/login") { json(LoginRequest("anna@example.com", "rossz-jelszo")) }
        assertEquals(HttpStatusCode.Unauthorized, wrong.status)
    }

    @Test
    fun registrationValidation() = apiTest {
        register("anna@example.com")
        val duplicate = client.post("/api/auth/register") { json(RegisterRequest("anna@example.com", "jelszo123", "Anna")) }
        assertEquals(HttpStatusCode.Conflict, duplicate.status)
        val shortPassword = client.post("/api/auth/register") { json(RegisterRequest("b@example.com", "rovid", "B")) }
        assertEquals(HttpStatusCode.BadRequest, shortPassword.status)
        val badEmail = client.post("/api/auth/register") { json(RegisterRequest("nem-email", "jelszo123", "C")) }
        assertEquals(HttpStatusCode.BadRequest, badEmail.status)
    }

    @Test
    fun demoBotAccountsCannotLogIn() = apiTest {
        val res = client.post("/api/auth/login") { json(LoginRequest("bot1@demo.cartinder.hu", "!")) }
        assertEquals(HttpStatusCode.Unauthorized, res.status)
    }

    @Test
    fun protectedRoutesNeedToken() = apiTest {
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/feed").status)
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/feed") { bearerAuth("hamis") }.status)
    }

    @Test
    fun updateProfile() = apiTest {
        val auth = register("anna@example.com")
        val me: MeDto = put("/api/me", auth.token, UpdateProfileRequest("Anna B", "Pécs", "Szeretem a kombikat", "Škoda Fabia")).body()
        assertEquals("Pécs", me.city)
        assertEquals("Škoda Fabia", me.currentCar)
    }

    // --- Feed és swipe ---

    @Test
    fun feedContainsDemoCarsAndFilters() = apiTest {
        val auth = register("anna@example.com")
        assertEquals(demoCars.size, feed(auth.token).size)

        val electric = feed(auth.token, "?fuels=ELEKTROMOS")
        assertTrue(electric.isNotEmpty())
        assertTrue(electric.all { it.fuel == Fuel.ELEKTROMOS })

        val cheap = feed(auth.token, "?maxPrice=1000000&maxKm=200000")
        assertTrue(cheap.all { it.priceHuf <= 1_000_000 && it.km <= 200_000 })

        assertEquals(HttpStatusCode.BadRequest, get("/api/feed?fuels=KEROZIN", auth.token).status)
    }

    @Test
    fun swipedCarsLeaveTheFeedAndCannotBeSwipedTwice() = apiTest {
        val auth = register("anna@example.com")
        val car = feed(auth.token).first()
        val res: SwipeResponse = swipe(auth.token, car.id, SwipeDirection.LEFT).body()
        assertNull(res.match)
        assertFalse(feed(auth.token).any { it.id == car.id })
        assertEquals(HttpStatusCode.Conflict, swipe(auth.token, car.id, SwipeDirection.RIGHT).status)
        assertEquals(HttpStatusCode.NotFound, swipe(auth.token, 99_999, SwipeDirection.RIGHT).status)
    }

    @Test
    fun superLikeOnDemoSellerMatchesWithOpener() = apiTest {
        val auth = register("anna@example.com")
        val porsche = feed(auth.token).first { it.make == "Porsche" }
        val res: SwipeResponse = swipe(auth.token, porsche.id, SwipeDirection.UP).body()
        val match = assertNotNull(res.match)
        assertTrue(match.superLike)
        assertEquals(Role.BUYER, match.role)
        assertEquals(demoCars.first { it.make == "Porsche" }.opener, match.lastMessage?.text)
        assertEquals(1, match.unread)

        post("/api/matches/${match.id}/read", auth.token, ReadRequest(match.lastMessage!!.id))
        assertEquals(0, matches(auth.token).single().unread)
    }

    @Test
    fun likeOnEasyDemoSellerMatches() = apiTest {
        val auth = register("anna@example.com")
        // A Multipla senkit sem utasít el (pickiness = 0).
        val multipla = feed(auth.token).first { it.make == "Fiat" }
        val res: SwipeResponse = swipe(auth.token, multipla.id, SwipeDirection.RIGHT).body()
        assertNotNull(res.match)
    }

    @Test
    fun demoSellerRepliesToMessages() = apiTest {
        val auth = register("anna@example.com")
        val car = feed(auth.token).first()
        val match = assertNotNull(swipe(auth.token, car.id, SwipeDirection.UP).body<SwipeResponse>().match)

        val sent = post("/api/matches/${match.id}/messages", auth.token, SendMessageRequest("  Mennyi az ára?  "))
        assertEquals(HttpStatusCode.Created, sent.status)
        assertEquals("Mennyi az ára?", sent.body<MessageDto>().text)

        var thread = messages(auth.token, match.id).messages
        repeat(50) {
            if (thread.size >= 3) return@repeat
            delay(100)
            thread = messages(auth.token, match.id).messages
        }
        assertEquals(listOf(false, true, false), thread.map { it.fromMe })
    }

    @Test
    fun emptyMessageIsRejected() = apiTest {
        val auth = register("anna@example.com")
        val car = feed(auth.token).first()
        val match = assertNotNull(swipe(auth.token, car.id, SwipeDirection.UP).body<SwipeResponse>().match)
        assertEquals(HttpStatusCode.BadRequest, post("/api/matches/${match.id}/messages", auth.token, SendMessageRequest("   ")).status)
    }

    // --- Valódi eladó: lájk, elfogadás, chat ---

    @Test
    fun realSellerAcceptsLikeAndBothCanChat() = apiTest {
        val seller = register("seller@example.com", "Eladó Ede")
        val buyer = register("buyer@example.com", "Vevő Vera")
        val listing: ListingDto = post("/api/listings", seller.token, sampleListing()).body()

        assertFalse(feed(seller.token).any { it.id == listing.id }, "a saját hirdetés nem jelenik meg")
        assertTrue(feed(buyer.token).any { it.id == listing.id })

        val res: SwipeResponse = swipe(buyer.token, listing.id, SwipeDirection.RIGHT).body()
        assertNull(res.match, "valódi eladónál előbb el kell fogadni")
        assertTrue(matches(buyer.token).isEmpty())

        val likes: List<IncomingLikeDto> = get("/api/likes", seller.token).body()
        assertEquals("Vevő Vera", likes.single().buyer.name)

        val match: MatchDto = post("/api/likes/${likes.single().id}/accept", seller.token).body()
        assertEquals(Role.SELLER, match.role)
        assertTrue((get("/api/likes", seller.token).body<List<IncomingLikeDto>>()).isEmpty())

        val buyerMatch = matches(buyer.token).single()
        assertEquals(Role.BUYER, buyerMatch.role)
        assertEquals("Eladó Ede", buyerMatch.other.name)

        post("/api/matches/${match.id}/messages", buyer.token, SendMessageRequest("Szia, megvan még?"))
        assertEquals(1, matches(seller.token).single().unread)
        post("/api/matches/${match.id}/messages", seller.token, SendMessageRequest("Meg, szombaton nézd meg!"))

        val sellerView = messages(seller.token, match.id).messages
        assertEquals(listOf(false, true), sellerView.map { it.fromMe })
        val buyerView = messages(buyer.token, match.id).messages
        assertEquals(listOf(true, false), buyerView.map { it.fromMe })
        assertEquals(1, matches(buyer.token).single().unread)

        assertEquals(HttpStatusCode.Conflict, post("/api/likes/${likes.single().id}/accept", seller.token).status)
    }

    @Test
    fun sellerCanDeclineLike() = apiTest {
        val seller = register("seller@example.com")
        val buyer = register("buyer@example.com")
        val listing: ListingDto = post("/api/listings", seller.token, sampleListing()).body()
        swipe(buyer.token, listing.id, SwipeDirection.UP)
        val like = get("/api/likes", seller.token).body<List<IncomingLikeDto>>().single()
        assertTrue(like.superLike)

        assertEquals(HttpStatusCode.NoContent, post("/api/likes/${like.id}/decline", seller.token).status)
        assertTrue(get("/api/likes", seller.token).body<List<IncomingLikeDto>>().isEmpty())
        assertTrue(matches(buyer.token).isEmpty())
        assertEquals(HttpStatusCode.NotFound, post("/api/likes/${like.id}/accept", buyer.token).status)
    }

    @Test
    fun cannotSwipeOwnListing() = apiTest {
        val seller = register("seller@example.com")
        val listing: ListingDto = post("/api/listings", seller.token, sampleListing()).body()
        assertEquals(HttpStatusCode.Forbidden, swipe(seller.token, listing.id, SwipeDirection.RIGHT).status)
    }

    @Test
    fun strangersCannotReadAMatch() = apiTest {
        val buyer = register("buyer@example.com")
        val stranger = register("stranger@example.com")
        val car = feed(buyer.token).first()
        val match = assertNotNull(swipe(buyer.token, car.id, SwipeDirection.UP).body<SwipeResponse>().match)
        assertEquals(HttpStatusCode.NotFound, get("/api/matches/${match.id}/messages", stranger.token).status)
        assertEquals(HttpStatusCode.NotFound, post("/api/matches/${match.id}/messages", stranger.token, SendMessageRequest("hé")).status)
    }

    // --- Visszavonás és unmatch ---

    @Test
    fun undoRestoresCarAndRemovesMatch() = apiTest {
        val auth = register("anna@example.com")
        val car = feed(auth.token).first()
        swipe(auth.token, car.id, SwipeDirection.UP)
        val undo: UndoResponse = post("/api/swipes/undo", auth.token).body()
        assertEquals(car.id, undo.listing.id)
        assertTrue(matches(auth.token).isEmpty())
        assertEquals(car.id, feed(auth.token).first().id)
        assertEquals(HttpStatusCode.NotFound, post("/api/swipes/undo", auth.token).status)
    }

    @Test
    fun cannotUndoAfterWriting() = apiTest {
        val auth = register("anna@example.com")
        val car = feed(auth.token).first()
        val match = assertNotNull(swipe(auth.token, car.id, SwipeDirection.UP).body<SwipeResponse>().match)
        post("/api/matches/${match.id}/messages", auth.token, SendMessageRequest("Szia"))
        assertEquals(HttpStatusCode.Conflict, post("/api/swipes/undo", auth.token).status)
    }

    @Test
    fun unmatchRemovesChatButCarStaysSeen() = apiTest {
        val auth = register("anna@example.com")
        val car = feed(auth.token).first()
        val match = assertNotNull(swipe(auth.token, car.id, SwipeDirection.UP).body<SwipeResponse>().match)
        assertEquals(HttpStatusCode.NoContent, delete("/api/matches/${match.id}", auth.token).status)
        assertTrue(matches(auth.token).isEmpty())
        assertEquals(HttpStatusCode.NotFound, get("/api/matches/${match.id}/messages", auth.token).status)
        assertFalse(feed(auth.token).any { it.id == car.id })
    }

    // --- Hirdetések ---

    @Test
    fun listingCrudAndOwnership() = apiTest {
        val seller = register("seller@example.com")
        val other = register("other@example.com")
        val created: ListingDto = post("/api/listings", seller.token, sampleListing()).body()
        assertEquals("Opel", created.make)

        val updated: ListingDto = put("/api/listings/${created.id}", seller.token, sampleListing(price = 1_200_000)).body()
        assertEquals(1_200_000, updated.priceHuf)
        assertEquals(HttpStatusCode.Forbidden, put("/api/listings/${created.id}", other.token, sampleListing()).status)

        val mine: List<ListingDto> = get("/api/listings/mine", seller.token).body()
        assertEquals(listOf(created.id), mine.map { it.id })

        val hidden = put("/api/listings/${created.id}", seller.token, sampleListing().copy(active = false))
        assertEquals(HttpStatusCode.OK, hidden.status)
        assertFalse(feed(other.token).any { it.id == created.id }, "inaktív hirdetés nem jelenik meg")

        assertEquals(HttpStatusCode.Forbidden, delete("/api/listings/${created.id}", other.token).status)
        assertEquals(HttpStatusCode.NoContent, delete("/api/listings/${created.id}", seller.token).status)
        assertTrue(get("/api/listings/mine", seller.token).body<List<ListingDto>>().isEmpty())
    }

    @Test
    fun editingKeepsOpenerWhenLeftBlank() = apiTest {
        val seller = register("seller@example.com")
        val buyer = register("buyer@example.com")
        val listing: ListingDto = post("/api/listings", seller.token, sampleListing().copy(opener = "Szia, nézd meg!")).body()
        put("/api/listings/${listing.id}", seller.token, sampleListing(price = 999_000))

        swipe(buyer.token, listing.id, SwipeDirection.RIGHT)
        val like = get("/api/likes", seller.token).body<List<IncomingLikeDto>>().single()
        val match: MatchDto = post("/api/likes/${like.id}/accept", seller.token).body()
        assertEquals("Szia, nézd meg!", match.lastMessage?.text)
    }

    @Test
    fun listingValidation() = apiTest {
        val seller = register("seller@example.com")
        assertEquals(HttpStatusCode.BadRequest, post("/api/listings", seller.token, sampleListing().copy(year = 1850)).status)
        assertEquals(HttpStatusCode.BadRequest, post("/api/listings", seller.token, sampleListing().copy(make = " ")).status)
        assertEquals(HttpStatusCode.BadRequest, post("/api/listings", seller.token, sampleListing().copy(km = -1)).status)
    }

    @Test
    fun photoUploadIsStoredAndServed() = apiTest {
        val seller = register("seller@example.com")
        val other = register("other@example.com")
        val listing: ListingDto = post("/api/listings", seller.token, sampleListing()).body()
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3)

        val res = uploadPhoto(listing.id, seller.token, png, "image/png")
        assertEquals(HttpStatusCode.OK, res.status)
        val url = assertNotNull(res.body<ListingDto>().photoUrl)
        assertTrue(url.startsWith("/uploads/") && url.endsWith(".png"))
        assertTrue(png.contentEquals(client.get(url).readRawBytes()))

        assertEquals(HttpStatusCode.BadRequest, uploadPhoto(listing.id, seller.token, "hello".toByteArray(), "text/plain").status)
        assertEquals(HttpStatusCode.Forbidden, uploadPhoto(listing.id, other.token, png, "image/png").status)
    }

    private suspend fun Api.uploadPhoto(listingId: Int, token: String, bytes: ByteArray, type: String) =
        client.submitFormWithBinaryData(
            url = "/api/listings/$listingId/photo",
            formData = formData {
                append("photo", bytes, Headers.build {
                    append(HttpHeaders.ContentType, type)
                    append(HttpHeaders.ContentDisposition, "filename=\"photo\"")
                })
            },
        ) { bearerAuth(token) }
}

private inline fun <reified T : Any> HttpRequestBuilder.json(body: T) {
    contentType(ContentType.Application.Json)
    setBody(body)
}
