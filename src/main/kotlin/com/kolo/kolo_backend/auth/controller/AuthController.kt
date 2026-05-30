
package com.kolo.kolo_backend.auth.controller

import com.kolo.kolo_backend.auth.services.AuthService
import com.kolo.kolo_backend.auth.User
import com.kolo.kolo_backend.auth.dto.*
import com.kolo.kolo_backend.shared.config.JwtService
import com.kolo.kolo_backend.shared.exception.AppException
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
    private val jwtService: JwtService
) {

    @PostMapping("/register")
    fun register(
        @Valid @RequestBody request: RegisterRequest
    ): ResponseEntity<ApiResponse<OtpResponse>> {
        val result = authService.registerOrResendOtp(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse(success = true, message = result.message, data = result)
        )
    }

    @PostMapping("/verify-otp")
    fun verifyOtp(
        @Valid @RequestBody request: VerifyOtpRequest
    ): ResponseEntity<ApiResponse<OtpVerifiedResponse>> {
        val result = authService.verifyOtp(request)
        return ResponseEntity.ok(result)
    }

    @PostMapping("/set-pin")
    fun setPin(
        @Valid @RequestBody request: SetPinRequest,
        @RequestHeader("Authorization") authHeader: String
    ): ResponseEntity<ApiResponse<AuthResponse>> {

        // Extract and validate temp token
        val token = authHeader.removePrefix("Bearer ").trim()

        if (!jwtService.validateToken(token)) {
            throw AppException("Invalid or expired token", HttpStatus.UNAUTHORIZED)
        }

        if (jwtService.getTokenType(token) != "temp") {
            throw AppException("Invalid token type", HttpStatus.UNAUTHORIZED)
        }

        val phoneNumber = jwtService.getPhoneFromTempToken(token)
        val result = authService.setPin(phoneNumber, request)

        return ResponseEntity.ok(
            ApiResponse(success = true, message = "PIN set successfully. Welcome to Kolo!", data = result)
        )
    }

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest
    ): ResponseEntity<ApiResponse<AuthResponse>> {
        val result = authService.login(request)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Login successful", data = result)
        )
    }

    @PostMapping("/refresh")
    fun refresh(
        @Valid @RequestBody request: RefreshTokenRequest
    ): ResponseEntity<ApiResponse<AuthResponse>> {
        val result = authService.refreshToken(request)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Token refreshed", data = result)
        )
    }

    @GetMapping("/me")
    fun getCurrentUser(
        @AuthenticationPrincipal user: User
    ): ResponseEntity<ApiResponse<UserResponse>> {
        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "User retrieved",
                data = UserResponse(
                    id = user.id.toString(),
                    phoneNumber = user.phoneNumber,
                    fullName = user.fullName,
                    kycTier = user.kycTier.name,
                    trustScore = user.trustScore,
                    bvnVerified = user.bvnVerified
                )
            )
        )
    }
}