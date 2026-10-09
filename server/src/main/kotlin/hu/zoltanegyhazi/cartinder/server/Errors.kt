package hu.zoltanegyhazi.cartinder.server

import io.ktor.http.HttpStatusCode

class ApiException(val status: HttpStatusCode, override val message: String) : RuntimeException(message)

fun badRequest(message: String): Nothing = throw ApiException(HttpStatusCode.BadRequest, message)
fun notFound(message: String = "Nem található."): Nothing = throw ApiException(HttpStatusCode.NotFound, message)
fun forbidden(message: String = "Ehhez nincs jogosultságod."): Nothing = throw ApiException(HttpStatusCode.Forbidden, message)
fun conflict(message: String): Nothing = throw ApiException(HttpStatusCode.Conflict, message)
fun unauthorized(message: String): Nothing = throw ApiException(HttpStatusCode.Unauthorized, message)
