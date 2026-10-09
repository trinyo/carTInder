package hu.zoltanegyhazi.cartinder.data.api

import hu.zoltanegyhazi.cartinder.data.CarFilter
import hu.zoltanegyhazi.cartinder.data.SwipeDirection
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

class ApiClient(
    engine: HttpClientEngine,
    private val baseUrl: () -> String,
    private val token: () -> String?,
    private val onNetworkError: (Throwable) -> Unit = {},
) : CarTinderBackend {

    private val json = Json { ignoreUnknownKeys = true }

    private val http = HttpClient(engine) {
        expectSuccess = false
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            connectTimeoutMillis = 8_000
            requestTimeoutMillis = 30_000
        }
    }

    override suspend fun register(req: RegisterRequest): AuthResponse = call(HttpMethod.Post, "auth/register", auth = false) { json(req) }
    override suspend fun login(req: LoginRequest): AuthResponse = call(HttpMethod.Post, "auth/login", auth = false) { json(req) }
    override suspend fun me(): MeDto = call(HttpMethod.Get, "me")
    override suspend fun updateProfile(req: UpdateProfileRequest): MeDto = call(HttpMethod.Put, "me") { json(req) }

    override suspend fun feed(filter: CarFilter): List<ListingDto> = call(HttpMethod.Get, "feed") {
        filter.toQuery().forEach { (k, v) -> parameter(k, v) }
    }
    override suspend fun swipe(listingId: Int, direction: SwipeDirection): SwipeResponse =
        call(HttpMethod.Post, "listings/$listingId/swipe") { json(SwipeRequest(direction)) }
    override suspend fun undo(): UndoResponse = call(HttpMethod.Post, "swipes/undo")

    override suspend fun likes(): List<IncomingLikeDto> = call(HttpMethod.Get, "likes")
    override suspend fun acceptLike(likeId: Int): MatchDto = call(HttpMethod.Post, "likes/$likeId/accept")
    override suspend fun declineLike(likeId: Int) = callUnit(HttpMethod.Post, "likes/$likeId/decline")

    override suspend fun matches(): List<MatchDto> = call(HttpMethod.Get, "matches")
    override suspend fun unmatch(matchId: Int) = callUnit(HttpMethod.Delete, "matches/$matchId")
    override suspend fun messages(matchId: Int, after: Int): MessagesResponse =
        call(HttpMethod.Get, "matches/$matchId/messages") { parameter("after", after) }
    override suspend fun sendMessage(matchId: Int, text: String): MessageDto =
        call(HttpMethod.Post, "matches/$matchId/messages") { json(SendMessageRequest(text)) }
    override suspend fun markRead(matchId: Int, lastMessageId: Int) =
        callUnit(HttpMethod.Post, "matches/$matchId/read") { json(ReadRequest(lastMessageId)) }

    override suspend fun myListings(): List<ListingDto> = call(HttpMethod.Get, "listings/mine")
    override suspend fun createListing(req: ListingRequest): ListingDto = call(HttpMethod.Post, "listings") { json(req) }
    override suspend fun updateListing(listingId: Int, req: ListingRequest): ListingDto =
        call(HttpMethod.Put, "listings/$listingId") { json(req) }
    override suspend fun deleteListing(listingId: Int) = callUnit(HttpMethod.Delete, "listings/$listingId")

    override suspend fun uploadPhoto(listingId: Int, bytes: ByteArray, mimeType: String): ListingDto = guard {
        val res = http.submitFormWithBinaryData(
            url = url("listings/$listingId/photo"),
            formData = formData {
                append("photo", bytes, Headers.build {
                    append(HttpHeaders.ContentType, mimeType)
                    append(HttpHeaders.ContentDisposition, "filename=\"photo\"")
                })
            },
        ) { token()?.let { bearerAuth(it) } }
        res.ensureSuccess().body()
    }

    private suspend inline fun <reified T> call(
        method: HttpMethod,
        path: String,
        auth: Boolean = true,
        crossinline block: HttpRequestBuilder.() -> Unit = {},
    ): T = guard {
        http.request(url(path)) {
            this.method = method
            if (auth) token()?.let { bearerAuth(it) }
            block()
        }.ensureSuccess().body()
    }

    private suspend inline fun callUnit(
        method: HttpMethod,
        path: String,
        crossinline block: HttpRequestBuilder.() -> Unit = {},
    ) {
        guard {
            http.request(url(path)) {
                this.method = method
                token()?.let { bearerAuth(it) }
                block()
            }.ensureSuccess()
        }
    }

    private fun url(path: String) = baseUrl().trimEnd('/') + "/api/" + path

    private suspend fun HttpResponse.ensureSuccess(): HttpResponse {
        if (status.isSuccess()) return this
        val message = runCatching { body<ErrorResponse>().error }.getOrNull()
            ?: "A szerver hibát jelzett (${status.value})."
        throw ApiException(status.value, message)
    }

    private inline fun <T> guard(block: () -> T): T = try {
        block()
    } catch (e: ApiException) {
        throw e
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        onNetworkError(e)
        throw ApiException(0, "Nem érem el a szervert (${baseUrl()}). Fut a szerver, és jó a cím?", e)
    }
}

private inline fun <reified T : Any> HttpRequestBuilder.json(body: T) {
    contentType(ContentType.Application.Json)
    setBody(body)
}
