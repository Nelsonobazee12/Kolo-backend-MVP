
package com.kolo.kolo_backend.loans.controller

import com.kolo.kolo_backend.auth.User
import com.kolo.kolo_backend.auth.dto.ApiResponse
import com.kolo.kolo_backend.contributions.dto.PaymentInitResponse
import com.kolo.kolo_backend.loans.dto.*
import com.kolo.kolo_backend.loans.service.LoanService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/loans")
class LoanController(
    private val loanService: LoanService
) {

    @GetMapping("/eligibility/{groupId}")
    fun checkEligibility(
        @PathVariable groupId: UUID,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<ApiResponse<LoanEligibilityResponse>> {
        val result = loanService.checkEligibility(user, groupId)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Eligibility retrieved", data = result)
        )
    }

    @PostMapping("/request")
    fun requestLoan(
        @Valid @RequestBody request: LoanRequest,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<ApiResponse<LoanResponse>> {
        val result = loanService.requestLoan(user, request)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Loan request submitted", data = result)
        )
    }

    @PostMapping("/{loanId}/vote")
    fun voteOnLoan(
        @PathVariable loanId: UUID,
        @Valid @RequestBody request: LoanVoteRequest,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<ApiResponse<VoteResponse>> {
        val result = loanService.voteOnLoan(user, loanId, request)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Vote recorded", data = result)
        )
    }

    @PostMapping("/repay")
    fun initiateRepayment(
        @Valid @RequestBody request: LoanRepaymentRequest,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<ApiResponse<PaymentInitResponse>> {
        val result = loanService.initiateRepayment(user, request)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Repayment initialized", data = result)
        )
    }

    @GetMapping("/repay/verify/{reference}")
    fun verifyRepayment(
        @PathVariable reference: String,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<ApiResponse<LoanResponse>> {
        val result = loanService.verifyRepayment(user, reference)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Repayment verified", data = result)
        )
    }

    @GetMapping("/my-loans")
    fun getMyLoans(
        @AuthenticationPrincipal user: User
    ): ResponseEntity<ApiResponse<List<LoanResponse>>> {
        val result = loanService.getMyLoans(user)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Loans retrieved", data = result)
        )
    }

    @GetMapping("/group/{groupId}/book")
    fun getGroupLoanBook(
        @PathVariable groupId: UUID,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<ApiResponse<List<LoanResponse>>> {
        val result = loanService.getGroupLoanBook(user, groupId)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Loan book retrieved", data = result)
        )
    }
}