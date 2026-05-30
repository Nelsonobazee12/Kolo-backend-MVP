package com.kolo.kolo_backend.contributions.controller

import com.kolo.kolo_backend.auth.User
import com.kolo.kolo_backend.auth.dto.ApiResponse
import com.kolo.kolo_backend.contributions.dto.*
import com.kolo.kolo_backend.contributions.service.ContributionService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/contributions")
class ContributionController(
    private val contributionService: ContributionService
) {

    @PostMapping("/initiate")
    fun initiateContribution(
        @Valid @RequestBody request: InitiateContributionRequest,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<ApiResponse<PaymentInitResponse>> {
        val result = contributionService.initiateContribution(user, request)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Payment initialized", data = result)
        )
    }

    @GetMapping("/group/{groupId}/history")
    fun getGroupHistory(
        @PathVariable groupId: UUID,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<ApiResponse<ContributionHistoryResponse>> {
        val result = contributionService.getGroupHistory(user, groupId)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "History retrieved", data = result)
        )
    }

    @GetMapping("/my-history")
    fun getMyHistory(
        @AuthenticationPrincipal user: User
    ): ResponseEntity<ApiResponse<List<TransactionResponse>>> {
        val result = contributionService.getMyHistory(user)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "History retrieved", data = result)
        )
    }

    @GetMapping("/verify/{reference}")
    fun verifyPayment(
        @PathVariable reference: String,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<ApiResponse<TransactionResponse>> {
        val result = contributionService.verifyAndProcessPayment(reference, user)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Payment verified", data = result)
        )
    }
}