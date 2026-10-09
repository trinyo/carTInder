package hu.zoltanegyhazi.cartinder.server

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticFiles
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import kotlin.random.Random

fun main() {
    val config = Config.fromEnv()
    embeddedServer(Netty, port = config.port, host = "0.0.0.0") { module(config) }.start(wait = true)
}

fun Application.module(config: Config = Config.fromEnv(), random: Random = Random.Default) {
    if (config.jwtSecret == Config.DEV_SECRET) {
        log.warn("JWT_SECRET nincs beállítva, a fejlesztői kulcs van használatban. Élesben állítsd be!")
    }
    val db = connectDatabase(config)
    if (config.seedDemoData) seedDemoData(db)
    config.uploadDir.mkdirs()

    val bots = BotRunner(this, config.botDelayMs, random)
    val service = CarTinderService(db, config, bots, random)
    val tokens = Tokens(config.jwtSecret)

    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(CallLogging)
    install(StatusPages) {
        exception<ApiException> { call, e -> call.respond(e.status, ErrorResponse(e.message)) }
        exception<BadRequestException> { call, _ -> call.respond(HttpStatusCode.BadRequest, ErrorResponse("Hibás kérés.")) }
        exception<Throwable> { call, e ->
            call.application.log.error("Váratlan hiba", e)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Szerverhiba."))
        }
    }
    install(Authentication) {
        jwt("jwt") {
            verifier(tokens.verifier)
            validate { credential ->
                credential.payload.getClaim(Tokens.USER_ID).asInt()?.let { JWTPrincipal(credential.payload) }
            }
            challenge { _, _ -> call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Bejelentkezés szükséges.")) }
        }
    }

    routing {
        get("/health") { call.respondText("ok") }
        staticFiles("/uploads", config.uploadDir)
        apiRoutes(service, tokens)
    }
}
