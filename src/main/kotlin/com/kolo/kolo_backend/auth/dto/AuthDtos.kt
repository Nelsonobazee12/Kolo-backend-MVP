// src/main/kotlin/com/kolo/auth/dto/AuthDtos.kt
package com.kolo.kolo_backend.auth.dto

import jakarta.validation.constraints.*

// REQUEST DTOs

data class RegisterRequest(
    @field:NotBlank(message = "Phone number is required")
    @field:Pattern(
        regexp = "^(\\+234|0)[789][01]\\d{8}$",
        message = "Enter a valid Nigerian phone number"
    )
    val phoneNumber: String,

    @field:NotBlank(message = "Full name is required")
    @field:Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    val fullName: String
)

data class VerifyOtpRequest(
    @field:NotBlank(message = "Phone number is required")
    val phoneNumber: String,

    @field:NotBlank(message = "OTP is required")
    @field:Size(min = 6, max = 6, message = "OTP must be 6 digits")
    val otp: String
)

data class SetPinRequest(
    @field:NotBlank(message = "PIN is required")
    @field:Pattern(
        regexp = "^\\d{4}$",
        message = "PIN must be exactly 4 digits"
    )
    val pin: String,

    @field:NotBlank(message = "Confirm PIN is required")
    val confirmPin: String
)

data class LoginRequest(
    @field:NotBlank(message = "Phone number is required")
    val phoneNumber: String,

    @field:NotBlank(message = "PIN is required")
    val pin: String
)

data class RefreshTokenRequest(
    @field:NotBlank(message = "Refresh token is required")
    val refreshToken: String
)

// RESPONSE DTOs

data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null
)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserResponse
)

data class UserResponse(
    val id: String,
    val phoneNumber: String,
    val fullName: String?,
    val kycTier: String,
    val trustScore: Int,
    val bvnVerified: Boolean
)

data class OtpResponse(
    val message: String,
    val expiresInMinutes: Int = 10
)

data class OtpVerifiedResponse(
    val tempToken: String,
    val message: String
)