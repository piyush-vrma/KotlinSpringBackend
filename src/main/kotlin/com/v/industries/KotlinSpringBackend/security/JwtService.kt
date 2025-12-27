package com.v.industries.KotlinSpringBackend.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.*

@Service
class JwtService(
    @Value("\${jwt.secret}") private val jwtSecret: String
) {
    private val secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret))

    private fun generateToken(
        userId: String, type: String, expiry: Long
    ): String {
        val now = Date()
        val expiryDate = Date(now.time + expiry)

        return Jwts.builder().subject(userId).claim(TYPE, type).issuedAt(now).expiration(expiryDate)
            .signWith(secretKey, Jwts.SIG.HS256).compact()
    }

    fun generateAccessToken(userId: String): String {
        return generateToken(userId, TYPE_ACCESS, ACCESS_TOKEN_VALIDITY_MS)
    }

    fun generateRefreshToken(userId: String): String {
        return generateToken(userId, TYPE_REFRESH, REFRESH_TOKEN_VALIDITY_MS)
    }

    fun validateAccessToken(token: String): Boolean {
        val claims = parseAllClaims(token) ?: return false
        val tokenType = claims[TYPE] ?: return false
        return tokenType == TYPE_ACCESS
    }

    fun validateRefreshToken(token: String): Boolean {
        val claims = parseAllClaims(token) ?: return false
        val tokenType = claims[TYPE] ?: return false
        return tokenType == TYPE_REFRESH
    }

    fun getUserIdFromToken(token: String): String {
        val claims = parseAllClaims(token)
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(401), "Invalid Token!")
        return claims.subject
    }

    /**
     * Token may come as header value
     * e.g -> Authorization: Bearer <token>
     * We only want <token>, so this method will check and remove if token has any prefix
     */
    private fun parseAllClaims(token: String): Claims? {
        val rawToken = if (token.startsWith(BEARER_PREFIX)) {
            token.removePrefix(BEARER_PREFIX)
        } else token

        return try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(rawToken).payload
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val TYPE = "type"
        private const val TYPE_ACCESS = "access"
        private const val TYPE_REFRESH = "refresh"
        const val BEARER_PREFIX = "Bearer "

        private const val ACCESS_TOKEN_VALIDITY_MS = 15L * 60L * 1000L
        val REFRESH_TOKEN_VALIDITY_MS = 30L * 24L * 60L * 60L * 1000L
    }
}