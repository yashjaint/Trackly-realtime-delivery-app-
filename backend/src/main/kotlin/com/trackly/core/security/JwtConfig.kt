package com.trackly.core.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date
import java.util.UUID

object JwtConfig {
    const val ISSUER = "https://trackly.com"
    const val AUDIENCE = "trackly-users"
    private const val SECRET = "trackly_secret_key_sde2_production_grade_super_secure_jwt_token"
    private const val VALIDITY_MS = 3600_000 * 24 * 30L // 30 Days

    val algorithm: Algorithm = Algorithm.HMAC256(SECRET)

    fun generateToken(userId: UUID, email: String, role: String): String {
        return JWT.create()
            .withIssuer(ISSUER)
            .withAudience(AUDIENCE)
            .withClaim("userId", userId.toString())
            .withClaim("email", email)
            .withClaim("role", role)
            .withExpiresAt(Date(System.currentTimeMillis() + VALIDITY_MS))
            .sign(algorithm)
    }

    fun makeVerifier() = JWT
        .require(algorithm)
        .withIssuer(ISSUER)
        .withAudience(AUDIENCE)
        .build()
}
