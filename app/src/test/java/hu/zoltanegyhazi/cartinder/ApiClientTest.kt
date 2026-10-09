package hu.zoltanegyhazi.cartinder

import hu.zoltanegyhazi.cartinder.data.CarFilter
import hu.zoltanegyhazi.cartinder.data.Fuel
import hu.zoltanegyhazi.cartinder.data.SwipeDirection
import hu.zoltanegyhazi.cartinder.data.api.ApiClient
import hu.zoltanegyhazi.cartinder.data.api.ApiException
import hu.zoltanegyhazi.cartinder.data.api.LoginRequest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ApiClientTest {
    private val requests = mutableListOf<HttpRequestData>()
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private fun client(token: String? = "abc", status: HttpStatusCode = HttpStatusCode.OK, body: String = "[]") =
        ApiClient(
            MockEngine { request ->
                requests += request
                respond(body, status, jsonHeaders)
            },
            baseUrl = { "http://server:8080/" },
            token = { token },
        )

    @Test
    fun sendsBearerTokenAndFilterQuery() = runBlocking {
        client().feed(CarFilter(maxPrice = 2_000_000, fuels = setOf(Fuel.DIZEL, Fuel.BENZIN)))
        val req = requests.single()
        assertEquals("http://server:8080/api/feed?maxPrice=2000000&fuels=BENZIN%2CDIZEL", req.url.toString())
        assertEquals("Bearer abc", req.headers[HttpHeaders.Authorization])
    }

    @Test
    fun defaultFilterSendsNoQuery() = runBlocking {
        client().feed(CarFilter())
        assertEquals("http://server:8080/api/feed", requests.single().url.toString())
    }

    @Test
    fun loginDoesNotSendToken() = runBlocking {
        val body = """{"token":"t","user":{"id":1,"email":"a@b.hu","name":"A","city":"","bio":"","currentCar":"","stats":{"swipes":0,"likes":0,"matches":0,"listings":0}}}"""
        val res = client(body = body).login(LoginRequest("a@b.hu", "jelszo123"))
        assertEquals("t", res.token)
        assertNull(requests.single().headers[HttpHeaders.Authorization])
    }

    @Test
    fun sendsSwipeAsJson() = runBlocking {
        client(body = """{"match":null}""").swipe(5, SwipeDirection.UP)
        val req = requests.single()
        assertEquals("http://server:8080/api/listings/5/swipe", req.url.toString())
        assertEquals("""{"direction":"UP"}""", String(req.body.toByteArray()))
    }

    @Test
    fun serverErrorMessageIsSurfaced() = runBlocking {
        try {
            client(status = HttpStatusCode.Conflict, body = """{"error":"Ezt az autót már láttad."}""").swipe(5, SwipeDirection.LEFT)
            fail("kivételt vártunk")
        } catch (e: ApiException) {
            assertEquals(409, e.status)
            assertEquals("Ezt az autót már láttad.", e.message)
        }
    }

    @Test
    fun unauthorizedIsFlagged() = runBlocking {
        try {
            client(status = HttpStatusCode.Unauthorized, body = """{"error":"Bejelentkezés szükséges."}""").me()
            fail("kivételt vártunk")
        } catch (e: ApiException) {
            assertTrue(e.isUnauthorized)
        }
    }

    @Test
    fun networkFailureBecomesFriendlyError() = runBlocking {
        val client = ApiClient(MockEngine { throw java.io.IOException("connection refused") }, { "http://server:8080" }, { null })
        try {
            client.matches()
            fail("kivételt vártunk")
        } catch (e: ApiException) {
            assertEquals(0, e.status)
            assertTrue(e.message, e.message.startsWith("Nem érem el a szervert"))
        }
    }
}
