package com.kolo.kolo_backend.notifications.service

import com.kolo.kolo_backend.notifications.providers.SmsProvider
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class NotificationService(
    private val smsProvider: SmsProvider
) {

    // Auth notifications
    @Async
    fun sendOtpSms(phoneNumber: String, otp: String) {
        smsProvider.sendSms(
            phoneNumber,
            "Your Kolo verification code is $otp. " +
                    "Valid for 10 minutes. Do not share this."
        )
    }

    // Contribution notifications
    @Async
    fun sendContributionReminder(
        phoneNumber: String,
        amountFormatted: String,
        groupName: String,
        daysUntilDue: Int
    ) {
        val message = when (daysUntilDue) {
            3 -> "Reminder: Your $amountFormatted contribution to " +
                    "'$groupName' is due in 3 days. Stay on track!"
            1 -> "Tomorrow is contribution day! $amountFormatted due " +
                    "to '$groupName'. Tap to pay early."
            0 -> "Today is contribution day! Pay $amountFormatted " +
                    "to '$groupName' to keep your trust score healthy."
            else -> "Your $amountFormatted contribution to '$groupName' is due soon."
        }
        smsProvider.sendSms(phoneNumber, message)
    }

    @Async
    fun sendContributionConfirmation(
        phoneNumber: String,
        amountFormatted: String,
        groupName: String,
        newBalanceFormatted: String
    ) {
        smsProvider.sendSms(
            phoneNumber,
            "✓ Kolo: $amountFormatted received for '$groupName'. " +
                    "Your savings balance: $newBalanceFormatted. Keep it up!"
        )
    }

    // Fine notifications
    @Async
    fun sendFineNotification(
        phoneNumber: String,
        amountFormatted: String,
        groupName: String,
        daysLate: Int
    ) {
        smsProvider.sendSms(
            phoneNumber,
            "⚠ Kolo: A fine of $amountFormatted has been added to your " +
                    "account for being $daysLate days late on '$groupName'. " +
                    "Pay now to avoid suspension."
        )
    }

    @Async
    fun sendFinePaymentConfirmation(
        phoneNumber: String,
        amountFormatted: String
    ) {
        smsProvider.sendSms(
            phoneNumber,
            "✓ Kolo: Fine payment of $amountFormatted confirmed. " +
                    "Your account is now in good standing."
        )
    }

    @Async
    fun sendSuspensionNotice(
        phoneNumber: String,
        groupName: String
    ) {
        smsProvider.sendSms(
            phoneNumber,
            "⛔ Kolo: Your membership in '$groupName' has been suspended " +
                    "due to 30+ days of missed contributions. " +
                    "Pay all outstanding fines to reactivate."
        )
    }

    // Loan notifications
    @Async
    fun sendLoanApprovalNotification(
        phoneNumber: String,
        amountFormatted: String,
        groupName: String,
        dueDate: String
    ) {
        smsProvider.sendSms(
            phoneNumber,
            "✓ Kolo: Your loan of $amountFormatted from '$groupName' " +
                    "has been approved and disbursed. " +
                    "Repayment due: $dueDate."
        )
    }

    @Async
    fun sendLoanVoteRequest(
        phoneNumber: String,
        borrowerName: String?,
        amountFormatted: String,
        groupName: String
    ) {
        smsProvider.sendSms(
            phoneNumber,
            "🗳 Kolo: ${borrowerName ?: "A member"} is requesting a loan " +
                    "of $amountFormatted from '$groupName'. " +
                    "Open the app to vote."
        )
    }

    @Async
    fun sendLoanRejectionNotification(
        phoneNumber: String,
        amountFormatted: String,
        groupName: String
    ) {
        smsProvider.sendSms(
            phoneNumber,
            "Kolo: Your loan request of $amountFormatted from " +
                    "'$groupName' was declined by the group. " +
                    "You can try again after 30 days."
        )
    }

    @Async
    fun sendLoanRepaymentConfirmation(
        phoneNumber: String,
        amountFormatted: String,
        remainingFormatted: String,
        fullyRepaid: Boolean
    ) {
        val message = if (fullyRepaid) {
            "✓ Kolo: Loan fully repaid! $amountFormatted received. " +
                    "Your trust score has been boosted. Well done!"
        } else {
            "✓ Kolo: Loan repayment of $amountFormatted received. " +
                    "Remaining balance: $remainingFormatted."
        }
        smsProvider.sendSms(phoneNumber, message)
    }

    @Async
    fun sendLoanDueReminder(
        phoneNumber: String,
        amountFormatted: String,
        groupName: String,
        daysUntilDue: Int
    ) {
        smsProvider.sendSms(
            phoneNumber,
            "⚠ Kolo: Your loan repayment of $amountFormatted to " +
                    "'$groupName' is due in $daysUntilDue days. " +
                    "Open the app to repay."
        )
    }

    // Group notifications
    @Async
    fun sendGroupInvitation(
        phoneNumber: String,
        inviterName: String?,
        groupName: String,
        inviteLink: String
    ) {
        smsProvider.sendSms(
            phoneNumber,
            "${inviterName ?: "Someone"} has invited you to join " +
                    "'$groupName' on Kolo — Nigeria's trusted community savings app. " +
                    "Join here: $inviteLink"
        )
    }

    @Async
    fun sendMemberJoinedNotification(
        phoneNumber: String,
        memberName: String?,
        groupName: String
    ) {
        smsProvider.sendSms(
            phoneNumber,
            "🎉 Kolo: ${memberName ?: "A new member"} just joined " +
                    "'$groupName'. Your group is growing!"
        )
    }

    // Trust score notifications
    @Async
    fun sendTrustScoreUpdate(
        phoneNumber: String,
        oldScore: Int,
        newScore: Int
    ) {
        val direction = if (newScore > oldScore) "increased" else "decreased"
        smsProvider.sendSms(
            phoneNumber,
            "Kolo: Your trust score has $direction: $oldScore → $newScore. " +
                    if (newScore > oldScore)
                        "Keep it up!"
                    else
                        "Make contributions on time to improve it."
        )
    }
}