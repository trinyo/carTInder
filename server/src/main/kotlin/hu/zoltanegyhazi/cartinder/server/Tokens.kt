package hu.zoltanegyhazi.cartinder.server

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

class Tokens(secret: String, private val clock: () -> Long = System::currentTimeMillis) {
    private val algorithm = Algorithm.HMAC256(secret)

    val verifier: JWTVerifier = JWT.require(algorithm).withIssuer(ISSUER).build()

    fun create(userId: Int): String = JWT.create()
        .withIssuer(ISSUER)
        .withClaim(USER_ID, userId)
        .withIssuedAt(Date(clock()))
        .withExpiresAt(Date(clock() + VALIDITY_MS))
        .sign(algorithm)

    companion object {
        const val ISSUER = "cartinder"
        const val USER_ID = "uid"
        const val VALIDITY_MS = 30L * 24 * 60 * 60 * 1000
    }
}
