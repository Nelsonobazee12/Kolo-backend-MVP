
package com.kolo.kolo_backend.auth.services

import com.kolo.kolo_backend.auth.User
import com.kolo.kolo_backend.auth.dto.*
import com.kolo.kolo_backend.auth.repository.UserRepository
import com.kolo.kolo_backend.notifications.service.NotificationService
import com.kolo.kolo_backend.shared.config.JwtService
import com.kolo.kolo_backend.shared.exception.AppException
import com.kolo.kolo_backend.shared.util.HashUtil
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val otpService: OtpService,
    private val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder,
    private val notificationService: NotificationService
    // SmsProvider injected later when we build notifications
) {

    // Step 1 — Register or resend OTP
    @Transactional
    fun registerOrResendOtp(request: RegisterRequest): OtpResponse {
        val normalizedPhone = normalizePhone(request.phoneNumber)

        // Create user if doesn't exist
        if (!userRepository.existsByPhoneNumber(normalizedPhone)) {
            userRepository.save(
                User(
                    phoneNumber = normalizedPhone,
                    fullName = request.fullName
                )
            )
        }

        // Generate OTP and store in Redis
        val otp = otpService.generateOtp(normalizedPhone)

        // TODO: Send via Termii SMS — plug in when notifications module is built

        // With this — sends SMS and falls back to console
        val smsSent = try {
            notificationService.sendOtpSms(normalizedPhone, otp)
            true
        } catch (e: Exception) {
            false
        }

        // Always log in development so we're never stuck
        if (!smsSent || System.getenv("SPRING_PROFILES_ACTIVE") != "prod") {
            println("📱 OTP for $normalizedPhone: $otp")
        }

        return OtpResponse(
            message = "OTP sent to $normalizedPhone",
            expiresInMinutes = 10
        )
    }

    // Step 2 — Verify OTP
    fun verifyOtp(request: VerifyOtpRequest): ApiResponse<OtpVerifiedResponse> {
        val normalizedPhone = normalizePhone(request.phoneNumber)

        val result = otpService.verifyOtp(normalizedPhone, request.otp)

        return when (result) {
            OtpVerificationResult.SUCCESS -> {
                val tempToken = jwtService.generateTempToken(normalizedPhone)
                ApiResponse(
                    success = true,
                    message = "OTP verified. Please set your PIN.",
                    data = OtpVerifiedResponse(
                        tempToken = tempToken,
                        message = "Use this token to set your PIN. Valid for 5 minutes."
                    )
                )
            }
            OtpVerificationResult.INVALID -> throw AppException(
                "Invalid OTP. Please try again.",
                HttpStatus.BAD_REQUEST
            )
            OtpVerificationResult.EXPIRED -> throw AppException(
                "OTP has expired. Please request a new one.",
                HttpStatus.BAD_REQUEST
            )
            OtpVerificationResult.TOO_MANY_ATTEMPTS -> throw AppException(
                "Too many failed attempts. Please request a new OTP.",
                HttpStatus.TOO_MANY_REQUESTS
            )
        }
    }

    // Step 3 — Set PIN (after OTP verified)
    @Transactional
    fun setPin(phoneNumber: String, request: SetPinRequest): AuthResponse {
        if (request.pin != request.confirmPin) {
            throw AppException("PINs do not match", HttpStatus.BAD_REQUEST)
        }

        val normalizedPhone = normalizePhone(phoneNumber)
        val user = userRepository.findByPhoneNumber(normalizedPhone)
            ?: throw AppException("User not found", HttpStatus.NOT_FOUND)

        user.pinHash = passwordEncoder.encode(request.pin)
        user.updatedAt = LocalDateTime.now()
        userRepository.save(user)

        return generateAuthResponse(user)
    }

    // Login with phone + PIN
    @Transactional
    fun login(request: LoginRequest): AuthResponse {
        val normalizedPhone = normalizePhone(request.phoneNumber)
        val user = userRepository.findByPhoneNumber(normalizedPhone)
            ?: throw AppException("Invalid credentials", HttpStatus.UNAUTHORIZED)

        if (user.isBlacklisted) {
            throw AppException(
                "Your account has been suspended. Contact support.",
                HttpStatus.FORBIDDEN
            )
        }

        val pinHash = user.pinHash
            ?: throw AppException(
                "Please complete registration by setting your PIN.",
                HttpStatus.BAD_REQUEST
            )

        if (!passwordEncoder.matches(request.pin, pinHash)) {
            throw AppException("Invalid credentials", HttpStatus.UNAUTHORIZED)
        }

        return generateAuthResponse(user)
    }

    // Refresh access token
    @Transactional
    fun refreshToken(request: RefreshTokenRequest): AuthResponse {
        if (!jwtService.validateToken(request.refreshToken)) {
            throw AppException("Invalid refresh token", HttpStatus.UNAUTHORIZED)
        }

        if (jwtService.getTokenType(request.refreshToken) != "refresh") {
            throw AppException("Invalid token type", HttpStatus.UNAUTHORIZED)
        }

        val userId = jwtService.getUserIdFromToken(request.refreshToken)
        val user = userRepository.findById(userId).orElseThrow {
            AppException("User not found", HttpStatus.NOT_FOUND)
        }

        // Verify refresh token matches what we stored
        if (HashUtil.sha256(request.refreshToken) != user.refreshTokenHash) {
            throw AppException("Refresh token has been revoked", HttpStatus.UNAUTHORIZED)
        }

        return generateAuthResponse(user)
    }

    // Internal — generates tokens and builds response
    private fun generateAuthResponse(user: User): AuthResponse {
        val accessToken = jwtService.generateAccessToken(user.id!!, user.phoneNumber)
        val refreshToken = jwtService.generateRefreshToken(user.id!!)

        // Store hashed refresh token
        user.refreshTokenHash = HashUtil.sha256(refreshToken)
        userRepository.save(user)

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            user = user.toResponse()
        )
    }

    // Normalize Nigerian phone numbers to +234 format
    private fun normalizePhone(phone: String): String {
        return when {
            phone.startsWith("+234") -> phone
            phone.startsWith("0") -> "+234${phone.substring(1)}"
            else -> "+234$phone"
        }
    }

    private fun User.toResponse() = UserResponse(
        id = id.toString(),
        phoneNumber = phoneNumber,
        fullName = fullName,
        kycTier = kycTier.name,
        trustScore = trustScore,
        bvnVerified = bvnVerified
    )
}