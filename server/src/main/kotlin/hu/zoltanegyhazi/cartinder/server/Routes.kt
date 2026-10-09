package hu.zoltanegyhazi.cartinder.server

import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray

fun Route.apiRoutes(service: CarTinderService, tokens: Tokens) = route("/api") {
    post("/auth/register") {
        val userId = service.register(call.receive())
        call.respond(HttpStatusCode.Created, AuthResponse(tokens.create(userId), service.me(userId)))
    }
    post("/auth/login") {
        val userId = service.login(call.receive())
        call.respond(AuthResponse(tokens.create(userId), service.me(userId)))
    }

    authenticate("jwt") {
        get("/me") { call.respond(service.me(call.userId)) }
        put("/me") { call.respond(service.updateProfile(call.userId, call.receive())) }

        get("/feed") {
            val p = call.request.queryParameters
            val filter = FeedFilter(
                maxPrice = p["maxPrice"]?.toIntOrNull(),
                maxKm = p["maxKm"]?.toIntOrNull(),
                fuels = p["fuels"]?.split(',')?.filter { it.isNotBlank() }?.map {
                    runCatching { Fuel.valueOf(it) }.getOrElse { badRequest("Ismeretlen üzemanyag: $it") }
                }?.toSet(),
            )
            call.respond(service.feed(call.userId, filter))
        }
        post("/listings/{id}/swipe") {
            val req = call.receive<SwipeRequest>()
            call.respond(service.swipe(call.userId, call.intParam("id"), req.direction))
        }
        post("/swipes/undo") { call.respond(service.undoLastSwipe(call.userId)) }

        get("/likes") { call.respond(service.incomingLikes(call.userId)) }
        post("/likes/{id}/accept") { call.respond(service.acceptLike(call.userId, call.intParam("id"))) }
        post("/likes/{id}/decline") {
            service.declineLike(call.userId, call.intParam("id"))
            call.respond(HttpStatusCode.NoContent)
        }

        get("/matches") { call.respond(service.matches(call.userId)) }
        get("/matches/{id}") { call.respond(service.match(call.userId, call.intParam("id"))) }
        delete("/matches/{id}") {
            service.unmatch(call.userId, call.intParam("id"))
            call.respond(HttpStatusCode.NoContent)
        }
        get("/matches/{id}/messages") {
            val after = call.request.queryParameters["after"]?.toIntOrNull() ?: 0
            call.respond(service.messages(call.userId, call.intParam("id"), after))
        }
        post("/matches/{id}/messages") {
            val req = call.receive<SendMessageRequest>()
            call.respond(HttpStatusCode.Created, service.sendMessage(call.userId, call.intParam("id"), req.text))
        }
        post("/matches/{id}/read") {
            service.markRead(call.userId, call.intParam("id"), call.receive<ReadRequest>().lastMessageId)
            call.respond(HttpStatusCode.NoContent)
        }

        get("/listings/mine") { call.respond(service.myListings(call.userId)) }
        post("/listings") { call.respond(HttpStatusCode.Created, service.createListing(call.userId, call.receive())) }
        put("/listings/{id}") { call.respond(service.updateListing(call.userId, call.intParam("id"), call.receive())) }
        delete("/listings/{id}") {
            service.deleteListing(call.userId, call.intParam("id"))
            call.respond(HttpStatusCode.NoContent)
        }
        post("/listings/{id}/photo") {
            var upload: Pair<ByteArray, String>? = null
            call.receiveMultipart(formFieldLimit = CarTinderService.MAX_PHOTO_BYTES + 1L).forEachPart { part ->
                if (part is PartData.FileItem && upload == null) {
                    val ext = when (part.contentType?.toString()?.substringBefore(';')) {
                        "image/jpeg" -> "jpg"
                        "image/png" -> "png"
                        "image/webp" -> "webp"
                        else -> badRequest("Csak JPEG, PNG vagy WebP képet tölthetsz fel.")
                    }
                    upload = part.provider().readRemaining().readByteArray() to ext
                }
                part.dispose()
            }
            val (bytes, ext) = upload ?: badRequest("Hiányzik a fotó.")
            call.respond(service.setListingPhoto(call.userId, call.intParam("id"), bytes, ext))
        }
    }
}

private val ApplicationCall.userId: Int
    get() = principal<JWTPrincipal>()!!.payload.getClaim(Tokens.USER_ID).asInt()

private fun ApplicationCall.intParam(name: String): Int =
    parameters[name]?.toIntOrNull() ?: badRequest("Érvénytelen azonosító.")
