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
import java.util.UUID

/*
 * NotificationService.kt builds and saves notifications for entrants.
 * - Draw results: one win or lose notice per entrant (US 01.03.01, 01.03.02, 02.05.01)
 * - Owner messages to a lottery's waitlist (US 02.10.01)
 * Entrants who turned notifications off (US 01.10.01) are saved as OPTED_OUT, so the
 * owner's sent list can count them, but they get no in-app or phone notification.
 * The draw result itself is always saved on the lottery, whatever the opt-out.
 * Every notification is also written to the admin notification log (US 03.08.01,
 * "notificationLogs" collection) with status "sent" or "not sent (opted out)".
 *
 * Outstanding issues: notifications are written after the draw commits. If the
 * device goes offline in between, those notices are not resent. A Cloud Function
 * that runs the draw and writes notices together would remove that gap.
 */

/**
 * Sends notifications by saving them to a [NotificationRepository]. Phones pick up
 * new ones from there (see PushNotifier).
 *
 * The repositories are only created when first used, so local tests that never send
 * anything can call `NotificationService()` without Firebase.
 *
 * @param repository where notifications are saved; Firestore by default
 * @param entrantRepository used to check each entrant's opt-out; Firestore by default
 * @param logRepository the admin notification log (US 03.08.01); Firestore by default
 * @param clock current time in Unix epoch milliseconds
 * @param newId makes a unique id for each owner message
 */
class NotificationService(
    repository: NotificationRepository? = null,
    entrantRepository: EntrantRepository? = null,
    logRepository: NotificationLogRepository? = null,
    private val clock: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() }
) {
    private val storage by lazy { repository ?: FirestoreNotificationRepo() }

    // Only getEntrantsByIds is used, which does not depend on the repo's own entrant id.
    private val entrants by lazy { entrantRepository ?: FirestoreEntrantRepo("notification-service") }

    private val logs by lazy { logRepository ?: FirestoreNotificationLogRepo() }

    /**
     * Notifies every winner and every entrant who was not selected in a committed draw.
     * Each entrant gets exactly one notice per draw; calling this again for the same
     * draw overwrites the same documents instead of adding more.
     *
     * @param lottery the lottery that was drawn
     * @param result the saved draw result
     * @return the notifications that were saved (empty if no one joined)
     */
    suspend fun sendDrawResult(lottery: Lottery, result: LotteryResult): List<Notification> {
        val everyone = (result.winnerIds + result.loserIds).distinct()
        if (everyone.isEmpty()) return emptyList()
        val profiles = profiles(everyone)
        val notifications = buildDrawNotifications(lottery, result, optedOut(profiles), clock())
        storage.saveAll(notifications)
        log(notifications, profiles)
        return notifications
    }

    /**
     * Sends the owner's message to every entrant on a lottery's waitlist (US 02.10.01).
     *
     * @param lottery the lottery, loaded with its waitlist
     * @param ownerId the owner sending it; must own the lottery
     * @param message what to send
     * @return the notifications that were saved, one per entrant on the waitlist
     * @throws IllegalArgumentException if the message is empty or the sender is not the owner
     */
    suspend fun sendToWaitlist(
        lottery: Lottery,
        ownerId: String,
        message: String
    ): List<Notification> {
        require(message.isNotBlank()) { EMPTY_MESSAGE }
        require(ownerId == lottery.ownerId) { "Only the lottery's owner can notify its waitlist." }
        val recipients = lottery.getWaitingList(ownerId).map { it.id }.distinct()
        if (recipients.isEmpty()) return emptyList()
        val profiles = profiles(recipients)
        val notifications = buildWaitlistNotifications(
            lottery = lottery,
            message = message,
            recipients = recipients,
            optedOut = optedOut(profiles),
            now = clock(),
            batchId = newId()
        )
        storage.saveAll(notifications)
        log(notifications, profiles)
        return notifications
    }

    /** Entrant profiles by id. Entrants with no profile are simply missing. */
    private suspend fun profiles(ids: List<String>): Map<String, Entrant> =
        entrants.getEntrantsByIds(ids).associateBy { it.id }

    /** Ids of entrants who turned notifications off. Entrants with no profile get notified. */
    private fun optedOut(profiles: Map<String, Entrant>): Set<String> =
        profiles.values.filter { !it.notificationEnabled }.map { it.id }.toSet()

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
        /** Log status for a delivered notification (matches NotificationLog's "sent"). */
        const val LOG_SENT = "sent"

        /** Log status for a notification skipped because of the opt-out (US 01.10.01). */
        const val LOG_OPTED_OUT = "not sent (opted out)"

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
                status = if (notification.status == NotificationStatus.SENT) LOG_SENT else LOG_OPTED_OUT
            )

        /** Error shown when the owner tries to send an empty message. */
        const val EMPTY_MESSAGE = "Write a message before sending."

        /** The campsite name(s) used in notifications. */
        fun campsiteLabel(lottery: Lottery): String =
            lottery.campsites.joinToString(", ") { it.name.ifBlank { "Campsite" } }
                .ifBlank { "Campsite" }

        fun winMessage(campsite: String): String =
            "You won the lottery for $campsite! Reserve your dates during the booking period."

        fun loseMessage(campsite: String): String =
            "You were not selected in the lottery for $campsite this time."

        /** Fixed id for one entrant's notice from one draw, so it is never saved twice. */
        fun drawNotificationId(lotteryId: String, drawNumber: Long, entrantId: String): String =
            "draw-$lotteryId-$drawNumber-$entrantId"

        /**
         * Builds one win notice per winner and one lose notice per other entrant.
         * They are sent by the system on behalf of the lottery's owner.
         */
        fun buildDrawNotifications(
            lottery: Lottery,
            result: LotteryResult,
            optedOut: Set<String>,
            now: Long
        ): List<Notification> {
            val campsite = campsiteLabel(lottery)
            val winners = result.winnerIds.distinct()
            val losers = result.loserIds.distinct().filter { it !in winners }
            fun build(id: String, type: NotificationType, message: String, batch: String) =
                Notification(
                    id = drawNotificationId(lottery.id, result.drawNumber, id),
                    batchId = "draw-${lottery.id}-${result.drawNumber}-$batch",
                    recipientId = id,
                    senderId = lottery.ownerId,
                    lotteryId = lottery.id,
                    campsiteName = campsite,
                    type = type,
                    message = message,
                    sentAt = now,
                    status = statusFor(id, optedOut)
                )
            return winners.map { build(it, NotificationType.SYSTEM_WIN, winMessage(campsite), "win") } +
                losers.map { build(it, NotificationType.SYSTEM_LOSE, loseMessage(campsite), "lose") }
        }

        /** Builds one owner message per recipient, all sharing [batchId]. */
        fun buildWaitlistNotifications(
            lottery: Lottery,
            message: String,
            recipients: List<String>,
            optedOut: Set<String>,
            now: Long,
            batchId: String
        ): List<Notification> {
            val campsite = campsiteLabel(lottery)
            return recipients.distinct().map { id ->
                Notification(
                    id = "$batchId-$id",
                    batchId = batchId,
                    recipientId = id,
                    senderId = lottery.ownerId,
                    lotteryId = lottery.id,
                    campsiteName = campsite,
                    type = NotificationType.OWNER,
                    message = message.trim(),
                    sentAt = now,
                    status = statusFor(id, optedOut)
                )
            }
        }

        private fun statusFor(id: String, optedOut: Set<String>) =
            if (id in optedOut) NotificationStatus.OPTED_OUT else NotificationStatus.SENT
    }
}
