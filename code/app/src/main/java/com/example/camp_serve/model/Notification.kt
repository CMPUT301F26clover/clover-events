package com.example.camp_serve.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/*
 * Notification.kt
 * One notification sent to one entrant, and the summaries built from it.
 * Used by US 01.03.01, 01.03.02, 01.10.01, 02.05.01, 02.10.01 and 02.10.02.
 * Outstanding issues: none.
 */

/**
 * Who a notification came from.
 * - [OWNER]: a message the campsite owner wrote to their waitlist (US 02.10.01)
 * - [SYSTEM_WIN]: sent automatically by the draw to a winner (US 01.03.01)
 * - [SYSTEM_LOSE]: sent automatically by the draw to an entrant who was not selected (US 01.03.02)
 */
enum class NotificationType { OWNER, SYSTEM_WIN, SYSTEM_LOSE }

/**
 * What happened to a notification.
 * - [SENT]: delivered to the entrant's Notifications screen and phone
 * - [OPTED_OUT]: not delivered because the entrant turned notifications off (US 01.10.01);
 *   kept so the owner can see how many were skipped (US 02.10.02)
 */
enum class NotificationStatus { SENT, OPTED_OUT }

/**
 * One notification for one entrant. Every field has a default so Firestore can load it.
 *
 * @property id unique id; draw notifications use a fixed id so they are never saved twice
 * @property batchId shared by every notification from one send, so the owner sees one row per send
 * @property recipientId id of the entrant (their device id)
 * @property senderId id of the owner it was sent by, or on behalf of for draw results
 * @property lotteryId id of the lottery it is about
 * @property campsiteName name(s) of the lottery's campsites
 * @property type who sent it: the owner or the draw (win/lose)
 * @property message text shown to the entrant
 * @property sentAt when it was sent, in Unix epoch milliseconds
 * @property status whether it was delivered or skipped because of the opt-out
 */
data class Notification(
    val id: String = "",
    val batchId: String = "",
    val recipientId: String = "",
    val senderId: String = "",
    val lotteryId: String = "",
    val campsiteName: String = "",
    val type: NotificationType = NotificationType.OWNER,
    val message: String = "",
    val sentAt: Long = 0,
    val status: NotificationStatus = NotificationStatus.SENT
)

/** Whether this notification should appear in the entrant's Notifications screen and as a phone notification. */
fun Notification.isDelivered(): Boolean = status == NotificationStatus.SENT

/**
 * One row on the owner's "Sent notifications" screen (US 02.10.02): everything
 * sent in one go, e.g. one waitlist message or the win notices from one draw.
 *
 * @property recipients number of entrants it was delivered to
 * @property optedOut number of entrants skipped because they turned notifications off
 */
data class SentNotification(
    val batchId: String,
    val type: NotificationType,
    val lotteryId: String,
    val campsiteName: String,
    val message: String,
    val sentAt: Long,
    val recipients: Int,
    val optedOut: Int
)

/** Groups notifications into one [SentNotification] per send, newest first. */
fun List<Notification>.toSentNotifications(): List<SentNotification> =
    groupBy { it.batchId.ifBlank { it.id } }
        .map { (batchId, group) ->
            val first = group.first()
            SentNotification(
                batchId = batchId,
                type = first.type,
                lotteryId = first.lotteryId,
                campsiteName = first.campsiteName,
                message = first.message,
                sentAt = group.maxOf { it.sentAt },
                recipients = group.count { it.isDelivered() },
                optedOut = group.count { it.status == NotificationStatus.OPTED_OUT }
            )
        }
        .sortedByDescending { it.sentAt }

/** Sorts notifications from most recent to oldest. */
fun List<Notification>.newestFirst(): List<Notification> = sortedByDescending { it.sentAt }

/**
 * Short relative time like the storyboard: "Just now", "5m ago", "2h ago", "3d ago",
 * then the date for anything older than a week.
 */
fun timeAgo(then: Long, now: Long = System.currentTimeMillis()): String {
    val minutes = (now - then).coerceAtLeast(0) / 60_000
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        minutes < 24 * 60 -> "${minutes / 60}h ago"
        minutes < 7 * 24 * 60 -> "${minutes / (24 * 60)}d ago"
        else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(then))
    }
}

/**
 * Which of an entrant's notifications should pop up on their phone now (#101):
 * delivered ones (not opted out) that arrived since [since] and were not shown before.
 * Oldest first, so they appear in the order they were sent.
 *
 * @param notifications the entrant's notifications
 * @param alreadyShown ids that were already shown or skipped on this device
 * @param since only notifications sent at or after this time are shown, so a new
 *   install does not pop up every old notification
 */
fun pendingPushes(
    notifications: List<Notification>,
    alreadyShown: Set<String>,
    since: Long
): List<Notification> =
    notifications
        .filter { it.isDelivered() && it.id !in alreadyShown && it.sentAt >= since }
        .sortedBy { it.sentAt }
