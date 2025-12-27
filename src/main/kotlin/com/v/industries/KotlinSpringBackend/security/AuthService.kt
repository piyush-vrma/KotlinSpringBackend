package com.v.industries.KotlinSpringBackend.security

import com.v.industries.KotlinSpringBackend.database.model.RefreshToken
import com.v.industries.KotlinSpringBackend.database.model.User
import com.v.industries.KotlinSpringBackend.database.repository.RefreshTokenRepository
import com.v.industries.KotlinSpringBackend.database.repository.UserRepository
import com.v.industries.KotlinSpringBackend.security.JwtService.Companion.REFRESH_TOKEN_VALIDITY_MS
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.security.MessageDigest
import java.time.Instant
import java.util.*

@Service
class AuthService(
    private val jwtService: JwtService,
    private val userRepository: UserRepository,
    private val hashEncoder: HashEncoder,
    private val refreshTokenRepository: RefreshTokenRepository
) {
    data class TokenPair(
        val accessToken: String,
        val refreshToken: String
    )

    fun register(email: String, password: String): User {
        val user = userRepository.findByEmail(email.trim())
        user?.let {
            throw ResponseStatusException(HttpStatus.CONFLICT, "A user with that email already exists.")
        }
        return userRepository.save(
            User(
                email = email,
                hashedPassword = hashEncoder.encode(password)
            )
        )
    }

    fun login(email: String, password: String): TokenPair {
        val user = userRepository.findByEmail(email)
            ?: throw BadCredentialsException("Invalid credentials")

        if (!hashEncoder.matches(password, user.hashedPassword)) {
            throw BadCredentialsException("Invalid credentials")
        }

        val newAccessToken = jwtService.generateAccessToken(user.id.toHexString())
        val newRefreshToken = jwtService.generateRefreshToken(user.id.toHexString())

        storeRefreshToken(user.id, newRefreshToken)

        return TokenPair(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken
        )
    }

    @Transactional
    fun refresh(refreshToken: String): TokenPair {
        if (!jwtService.validateRefreshToken(refreshToken)) {
            throw ResponseStatusException(HttpStatusCode.valueOf(401), "Invalid refresh token!")
        }

        val userId = jwtService.getUserIdFromToken(refreshToken)

        // To check if the user exists with this refresh token
        val user = userRepository.findById(ObjectId(userId)).orElseThrow {
            ResponseStatusException(HttpStatusCode.valueOf(401), "Invalid refresh token!")
        }

        val hashedRefreshToken = hashToken(refreshToken)
        refreshTokenRepository.findByUserIdAndHashedToken(user.id, hashedRefreshToken)
            ?: throw ResponseStatusException(
                HttpStatusCode.valueOf(401),
                "Refresh token not recognized (maybe used or expired!)"
            )

        refreshTokenRepository.deleteByUserIdAndHashedToken(user.id, hashedRefreshToken)

        val newAccessToken = jwtService.generateAccessToken(userId)
        val newRefreshToken = jwtService.generateRefreshToken(userId)

        storeRefreshToken(user.id, newRefreshToken)

        return TokenPair(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken
        )
    }

    private fun storeRefreshToken(userId: ObjectId, rawRefreshToken: String) {
        val hashedRefreshToken = hashToken(rawRefreshToken)
        val expiryMs = REFRESH_TOKEN_VALIDITY_MS
        val expiresAt = Instant.now().plusMillis(expiryMs)

        refreshTokenRepository.save(
            RefreshToken(
                userId = userId,
                expiresAt = expiresAt,
                hashedToken = hashedRefreshToken
            )
        )
    }

    /**
     * Unlike passwords (Human chosen), Tokens are randomly (System) generated -> Hard to guess
     * Therefore, Less complex & Efficient hashing is sufficient
     */
    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(token.encodeToByteArray())
        return Base64.getEncoder().encodeToString(hashBytes)
    }
}