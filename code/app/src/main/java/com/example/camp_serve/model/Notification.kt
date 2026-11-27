package com.example.camp_serve.model

/*
 * Notification.kt
 * One notification sent to one entrant, saved in Firestore. The entrant's Alerts
 * screen shows it as an [Alert] (see NotificationAlertRepo).
 * Outstanding issues: none.
 */

/**
 * Who a notification came from.
 * - [SYSTEM_WIN]: sent automatically by the draw to a winner
 * - [SYSTEM_LOSE]: sent automatically by the draw to an entrant who was not selected
 */
enum class NotificationType { SYSTEM_WIN, SYSTEM_LOSE }

/**
 * What happened to a notification.
 * - [SENT]: delivered to the entrant's Alerts
 * - [OPTED_OUT]: not delivered because the entrant turned notifications off
 */
enum class NotificationStatus { SENT, OPTED_OUT }

/**
 * One notification for one entrant. Every field has a default so Firestore can load it.
 *
 * @property id unique id; draw notifications use a fixed id so they are never saved twice
 * @property batchId shared by every notification from one send
 * @property recipientId id of the entrant (their device id)
 * @property senderId id of the campsite owner it was sent by or on behalf of
 * @property lotteryId id of the lottery it is about
 * @property campsiteName name(s) of the lottery's campsites
 * @property type why it was sent
 * @property message text shown to the entrant
 * @property sentAt when it was sent, in Unix epoch milliseconds
 * @property status whether it was delivered
 */
data class Notification(
    val id: String = "",
    val batchId: String = "",
    val recipientId: String = "",
    val senderId: String = "",
    val lotteryId: String = "",
    val campsiteName: String = "",
    val type: NotificationType = NotificationType.SYSTEM_WIN,
    val message: String = "",
    val sentAt: Long = 0,
    val status: NotificationStatus = NotificationStatus.SENT
)

/** Whether this notification should appear in the entrant's Alerts. */
fun Notification.isDelivered(): Boolean = status == NotificationStatus.SENT

/** Sorts notifications from most recent to oldest. */
fun List<Notification>.newestFirst(): List<Notification> = sortedByDescending { it.sentAt }
