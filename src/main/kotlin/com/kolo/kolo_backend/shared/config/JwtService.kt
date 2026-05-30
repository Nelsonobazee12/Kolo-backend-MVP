package com.kolo.kolo_backend.shared.config

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Service
class JwtService {

    @Value("\${jwt.secret}")
    private lateinit var secret: String

    @Value("\${jwt.access-token-expiry}")
    private var accessTokenExpiry: Long = 0

    @Value("\${jwt.refresh-token-expiry}")
    private var refreshTokenExpiry: Long = 0

    private val signingKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    fun generateAccessToken(userId: UUID, phoneNumber: String): String {
        return Jwts.builder()
            .subject(userId.toString())
            .claim("phoneNumber", phoneNumber)
            .claim("type", "access")
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + accessTokenExpiry))
            .signWith(signingKey)
            .compact()
    }

    fun generateRefreshToken(userId: UUID): String {
        return Jwts.builder()
            .subject(userId.toString())
            .claim("type", "refresh")
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + refreshTokenExpiry))
            .signWith(signingKey)
            .compact()
    }

    fun validateToken(token: String): Boolean {
        return try {
            getClaims(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getUserIdFromToken(token: String): UUID {
        return UUID.fromString(getClaims(token).subject)
    }

    fun getTokenType(token: String): String {
        return getClaims(token)["type"] as String
    }

    private fun getClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }

    fun generateTempToken(phoneNumber: String): String {
        return Jwts.builder()
            .subject(phoneNumber)
            .claim("type", "temp")
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + 300000)) // 5 minutes
            .signWith(signingKey)
            .compact()
    }

    fun getPhoneFromTempToken(token: String): String {
        return getClaims(token).subject
    }

}