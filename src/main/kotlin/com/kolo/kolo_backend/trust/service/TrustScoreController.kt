
package com.kolo.kolo_backend.trust.service

import com.kolo.kolo_backend.auth.User
import com.kolo.kolo_backend.auth.dto.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/trust")
class TrustScoreController(
    private val trustScoreService: TrustScoreService
) {

    @GetMapping("/my-score")
    fun getMyScore(
        @AuthenticationPrincipal user: User
    ): ResponseEntity<ApiResponse<TrustScoreBreakdown>> {
        val result = trustScoreService.getScoreBreakdown(user)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Trust score retrieved", data = result)
        )
    }

    @PostMapping("/recalculate")
    fun recalculate(
        @AuthenticationPrincipal user: User
    ): ResponseEntity<ApiResponse<Int>> {
        val result = trustScoreService.recalculateScore(user)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Trust score recalculated", data = result)
        )
    }
}