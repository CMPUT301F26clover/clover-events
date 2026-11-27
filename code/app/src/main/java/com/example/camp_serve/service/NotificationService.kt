package com.example.camp_serve.service

import com.example.camp_serve.data.EntrantRepository
import com.example.camp_serve.data.FirestoreEntrantRepo
import com.example.camp_serve.data.FirestoreNotificationLogRepo
import com.example.camp_serve.data.FirestoreNotificationRepo
import com.example.camp_serve.data.NotificationLogRepository
import com.example.camp_serve.data.NotificationRepository
import com.example.camp_serve.model.Entrant
import com.example.camp_serve.model.Lottery
import com.example.camp_serve.model.LotteryResult
import com.example.camp_serve.model.Notification
import com.example.camp_serve.model.NotificationLog
import com.example.camp_serve.model.NotificationStatus
import com.example.camp_serve.model.NotificationType
import kotlinx.coroutines.CancellationException

/*
 * NotificationService.kt builds and saves notifications for entrants.
 * - Win notices after a draw
 * Every notification is also written to the admin notification log.
 *
 * Outstanding issues: notifications are written after the draw is saved. If the
 * device goes offline in between, those notices are not resent.
 */

/**
 * Sends notifications by saving them to a [NotificationRepository].
 *
 * The repositories are only created when first used, so local tests that never send
 * anything can call `NotificationService()` without Firebase.
 *
 * @param repository where notifications are saved; Firestore by default
 * @param entrantRepository used to look up entrant names; Firestore by default
 * @param logRepository the admin notification log (US 03.08.01); Firestore by default
 * @param clock current time in Unix epoch milliseconds
 */
class NotificationService(
    repository: NotificationRepository? = null,
    entrantRepository: EntrantRepository? = null,
    logRepository: NotificationLogRepository? = null,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val storage by lazy { repository ?: FirestoreNotificationRepo() }

    // Only getEntrantsByIds is used, which does not depend on the repo's own entrant id.
    private val entrants by lazy { entrantRepository ?: FirestoreEntrantRepo("notification-service") }

    private val logs by lazy { logRepository ?: FirestoreNotificationLogRepo() }

    /**
     * Notifies the winners of a saved draw (US 01.03.01). Each winner gets exactly one
     * notice per draw; calling this again for the same draw overwrites the same
     * documents instead of adding more.
     *
     * @param lottery the lottery that was drawn
     * @param result the saved draw result
     * @return the notifications that were saved (empty if there were no winners)
     */
    suspend fun sendDrawResult(lottery: Lottery, result: LotteryResult): List<Notification> {
        val notifications = buildDrawNotifications(lottery, result, clock())
        if (notifications.isEmpty()) return emptyList()
        storage.saveAll(notifications)
        log(notifications, profiles(notifications.map { it.recipientId }))
        return notifications
    }

    /** Entrant profiles by id. Entrants with no profile are simply missing. */
    private suspend fun profiles(ids: List<String>): Map<String, Entrant> =
        try {
            entrants.getEntrantsByIds(ids).associateBy { it.id }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyMap() // names are only for the log
        }

    /**
     * Writes one admin log entry per notification. A log that fails to save does not
     * undo the notification; the entrant has already been notified.
     */
    private suspend fun log(notifications: List<Notification>, profiles: Map<String, Entrant>) {
        notifications.forEach { notification ->
            try {
                logs.addNotificationLog(
                    toLog(notification, profiles[notification.recipientId]?.name.orEmpty())
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // best effort: the notification itself is saved
            }
        }
    }

    companion object {
        /** Log status for a delivered notification. */
        const val LOG_SENT = "sent"

        /** The campsite name(s) used in notifications. */
        fun campsiteLabel(lottery: Lottery): String =
            lottery.campsites.joinToString(", ") { it.name.ifBlank { "Campsite" } }
                .ifBlank { "Campsite" }

        fun winMessage(campsite: String): String =
            "You won the lottery for $campsite! Reserve your dates during the booking period."

        /** Fixed id for one entrant's notice from one draw, so it is never saved twice. */
        fun drawNotificationId(lotteryId: String, drawNumber: Long, entrantId: String): String =
            "draw-$lotteryId-$drawNumber-$entrantId"

        /**
         * Builds one win notice per winner, sent by the system on behalf of the
         * lottery's owner.
         */
        fun buildDrawNotifications(
            lottery: Lottery,
            result: LotteryResult,
            now: Long
        ): List<Notification> {
            val campsite = campsiteLabel(lottery)
            return result.winnerIds.distinct().map { id ->
                Notification(
                    id = drawNotificationId(lottery.id, result.drawNumber, id),
                    batchId = "draw-${lottery.id}-${result.drawNumber}-win",
                    recipientId = id,
                    senderId = lottery.ownerId,
                    lotteryId = lottery.id,
                    campsiteName = campsite,
                    type = NotificationType.SYSTEM_WIN,
                    message = winMessage(campsite),
                    sentAt = now,
                    status = NotificationStatus.SENT
                )
            }
        }

        /**
         * The admin log entry for a notification. It reuses the notification's id, so
         * logging the same notification twice does not create a second entry.
         */
        fun toLog(notification: Notification, recipientName: String): NotificationLog =
            NotificationLog(
                id = notification.id,
                recipientId = notification.recipientId,
                recipientName = recipientName,
                campsite = notification.campsiteName,
                message = notification.message,
                sentAt = notification.sentAt,
                status = LOG_SENT
            )
    }
}
