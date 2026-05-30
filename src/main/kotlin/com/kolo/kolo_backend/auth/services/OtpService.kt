
package com.kolo.kolo_backend.auth.services

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class OtpService(
    private val redisTemplate: StringRedisTemplate
) {

    companion object {
        private const val OTP_PREFIX = "otp:"
        private const val OTP_EXPIRY_MINUTES = 10L
        private const val MAX_ATTEMPTS = 3
        private const val ATTEMPTS_PREFIX = "otp_attempts:"
    }

    // Generate and store OTP in Redis
    fun generateOtp(phoneNumber: String): String {
        val otp = (100000..999999).random().toString()
        val key = "$OTP_PREFIX$phoneNumber"

        redisTemplate.opsForValue().set(
            key,
            otp,
            OTP_EXPIRY_MINUTES,
            TimeUnit.MINUTES
        )

        // Reset attempt counter
        redisTemplate.delete("$ATTEMPTS_PREFIX$phoneNumber")

        return otp
    }

    // Verify OTP — returns true if valid
    fun verifyOtp(phoneNumber: String, otp: String): OtpVerificationResult {
        val attemptsKey = "$ATTEMPTS_PREFIX$phoneNumber"
        val attempts = redisTemplate.opsForValue().get(attemptsKey)?.toInt() ?: 0

        // Too many wrong attempts
        if (attempts >= MAX_ATTEMPTS) {
            return OtpVerificationResult.TOO_MANY_ATTEMPTS
        }

        val storedOtp = redisTemplate.opsForValue().get("$OTP_PREFIX$phoneNumber")
            ?: return OtpVerificationResult.EXPIRED

        if (storedOtp != otp) {
            // Increment attempt counter
            redisTemplate.opsForValue().increment(attemptsKey)
            redisTemplate.expire(attemptsKey, OTP_EXPIRY_MINUTES, TimeUnit.MINUTES)
            return OtpVerificationResult.INVALID
        }

        // Valid — delete OTP so it can't be reused
        redisTemplate.delete("$OTP_PREFIX$phoneNumber")
        redisTemplate.delete(attemptsKey)

        return OtpVerificationResult.SUCCESS
    }
}

enum class OtpVerificationResult {
    SUCCESS, INVALID, EXPIRED, TOO_MANY_ATTEMPTS
}