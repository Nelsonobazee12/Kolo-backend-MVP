
package com.kolo.kolo_backend.notifications.service

import com.kolo.kolo_backend.groups.repository.MembershipRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Component
class ReminderJob(
    private val membershipRepository: MembershipRepository,
    private val notificationService: NotificationService
) {

    // Runs every day at 9 AM
    @Scheduled(cron = "0 0 9 * * ?")
    fun sendDailyReminders() {
        println("⏰ Reminder job running — ${LocalDate.now()}")

        val activeMemberships = membershipRepository
            .findAllByStatus("ACTIVE")
            .filter { it.nextDueDate != null }

        activeMemberships.forEach { membership ->
            val daysUntilDue = ChronoUnit.DAYS
                .between(LocalDate.now(), membership.nextDueDate)
                .toInt()

            // Send reminders at 3 days, 1 day, and day of
            if (daysUntilDue in listOf(3, 1, 0)) {
                notificationService.sendContributionReminder(
                    phoneNumber = membership.user.phoneNumber,
                    amountFormatted = formatKobo(membership.contributionAmount),
                    groupName = membership.group.name,
                    daysUntilDue = daysUntilDue
                )
                println("📨 Reminder sent — ${membership.user.phoneNumber}, due in $daysUntilDue days")
            }
        }
    }

    private fun formatKobo(kobo: Long): String {
        val naira = kobo / 100.0
        return "₦${String.format("%,.2f", naira)}"
    }
}