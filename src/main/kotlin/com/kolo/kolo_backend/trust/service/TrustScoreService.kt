
package com.kolo.kolo_backend.trust.service

import com.kolo.kolo_backend.auth.User
import com.kolo.kolo_backend.auth.repository.UserRepository
import com.kolo.kolo_backend.contributions.repository.TransactionRepository
import com.kolo.kolo_backend.fines.repository.FineRepository
import com.kolo.kolo_backend.loans.repository.LoanRepository
import com.kolo.kolo_backend.groups.repository.MembershipRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TrustScoreService(
    private val userRepository: UserRepository,
    private val transactionRepository: TransactionRepository,
    private val fineRepository: FineRepository,
    private val loanRepository: LoanRepository,
    private val membershipRepository: MembershipRepository
) {

    companion object {
        // Score changes
        const val ON_TIME_CONTRIBUTION = 2
        const val EARLY_CONTRIBUTION = 3
        const val FINE_ISSUED = -5
        const val FINE_PAID = 2
        const val LOAN_REPAID = 10
        const val LOAN_DEFAULTED = -20
        const val CYCLE_COMPLETED = 5

        // Thresholds
        const val MIN_SCORE = 0
        const val MAX_SCORE = 100
        const val MIN_BORROW_SCORE = 40
    }

    // Recalculate trust score for a user
    @Transactional
    fun recalculateScore(user: User): Int {
        var score = 50 // Base score

        // Contribution behavior
        val contributions = transactionRepository
            .findAllByUserIdOrderByCreatedAtDesc(user.id!!)
            .filter { it.type == "CONTRIBUTION" && it.status == "SUCCESS" }

        // On time contributions
        score += contributions.size * ON_TIME_CONTRIBUTION

        // Fine history
        val totalFines = fineRepository
            .findAllByUserIdAndStatus(user.id!!, "PENDING").size +
                fineRepository.findAllByUserIdAndStatus(user.id!!, "PAID").size

        val paidFines = fineRepository
            .findAllByUserIdAndStatus(user.id!!, "PAID").size

        score += totalFines * FINE_ISSUED
        score += paidFines * FINE_PAID

        // Loan repayment history
        val repaidLoans = loanRepository
            .findAllByBorrowerIdAndStatus(user.id!!, "REPAID").size

        val defaultedLoans = loanRepository
            .findAllByBorrowerIdAndStatus(user.id!!, "DEFAULTED").size

        score += repaidLoans * LOAN_REPAID
        score += defaultedLoans * LOAN_DEFAULTED

        // Completed cycles
        score += user.totalCyclesCompleted * CYCLE_COMPLETED

        // Clamp between 0 and 100
        score = score.coerceIn(MIN_SCORE, MAX_SCORE)

        // Save updated score
        user.trustScore = score
        userRepository.save(user)

        return score
    }

    // Award points for specific action
    @Transactional
    fun awardPoints(user: User, points: Int, reason: String) {
        val newScore = (user.trustScore + points).coerceIn(MIN_SCORE, MAX_SCORE)
        println("📊 Trust score update — ${user.phoneNumber}: " +
                "${user.trustScore} → $newScore ($reason)")
        user.trustScore = newScore
        userRepository.save(user)
    }

    // Deduct points for specific action
    @Transactional
    fun deductPoints(user: User, points: Int, reason: String) {
        awardPoints(user, -points, reason)
    }

    // Get trust score breakdown for a user
    @Transactional(readOnly = true)
    fun getScoreBreakdown(user: User): TrustScoreBreakdown {
        val contributions = transactionRepository
            .findAllByUserIdOrderByCreatedAtDesc(user.id!!)
            .filter { it.type == "CONTRIBUTION" && it.status == "SUCCESS" }

        val pendingFines = fineRepository
            .findAllByUserIdAndStatus(user.id!!, "PENDING")

        val paidFines = fineRepository
            .findAllByUserIdAndStatus(user.id!!, "PAID")

        val activeLoans = loanRepository
            .findAllByBorrowerIdAndStatus(user.id!!, "ACTIVE")

        val repaidLoans = loanRepository
            .findAllByBorrowerIdAndStatus(user.id!!, "REPAID")

        val defaultedLoans = loanRepository
            .findAllByBorrowerIdAndStatus(user.id!!, "DEFAULTED")

        // Determine tier
        val tier = when (user.trustScore) {
            in 80..100 -> "PLATINUM"
            in 60..79 -> "GOLD"
            in 40..59 -> "SILVER"
            else -> "BRONZE"
        }

        // Max loan multiplier based on tier
        val maxLoanMultiplier = when (tier) {
            "PLATINUM" -> 5
            "GOLD" -> 3
            "SILVER" -> 2
            else -> 1
        }

        return TrustScoreBreakdown(
            score = user.trustScore,
            tier = tier,
            totalContributions = contributions.size,
            pendingFines = pendingFines.size,
            paidFines = paidFines.size,
            activeLoans = activeLoans.size,
            repaidLoans = repaidLoans.size,
            defaultedLoans = defaultedLoans.size,
            cyclesCompleted = user.totalCyclesCompleted,
            canBorrow = user.trustScore >= MIN_BORROW_SCORE,
            maxLoanMultiplier = maxLoanMultiplier,
            nextMilestone = getNextMilestone(user.trustScore)
        )
    }

    // Runs every Sunday at midnight — recalculates all scores
    @Scheduled(cron = "0 0 0 * * SUN")
    @Transactional
    fun weeklyScoreRecalculation() {
        println("⏰ Weekly trust score recalculation starting...")
        val allUsers = userRepository.findAll()
        allUsers.forEach { user ->
            recalculateScore(user)
        }
        println("✅ Trust score recalculation complete — ${allUsers.size} users updated")
    }

    private fun getNextMilestone(score: Int): TrustMilestone {
        return when {
            score < 40 -> TrustMilestone(
                target = 40,
                pointsNeeded = 40 - score,
                reward = "Unlock borrowing"
            )
            score < 60 -> TrustMilestone(
                target = 60,
                pointsNeeded = 60 - score,
                reward = "GOLD tier — 3x loan multiplier"
            )
            score < 80 -> TrustMilestone(
                target = 80,
                pointsNeeded = 80 - score,
                reward = "PLATINUM tier — 5x loan multiplier"
            )
            else -> TrustMilestone(
                target = 100,
                pointsNeeded = 100 - score,
                reward = "Maximum trust score achieved"
            )
        }
    }
}

data class TrustScoreBreakdown(
    val score: Int,
    val tier: String,
    val totalContributions: Int,
    val pendingFines: Int,
    val paidFines: Int,
    val activeLoans: Int,
    val repaidLoans: Int,
    val defaultedLoans: Int,
    val cyclesCompleted: Int,
    val canBorrow: Boolean,
    val maxLoanMultiplier: Int,
    val nextMilestone: TrustMilestone
)

data class TrustMilestone(
    val target: Int,
    val pointsNeeded: Int,
    val reward: String
)