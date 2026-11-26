package com.example.camp_serve.model

/**
 * One entry in the admin notification log (US 03.08.01): a notification the app
 * sent, or tried to send, to a user.
 * Every field has a default value so Firestore can turn it back into a log.
 *
 * @property id unique id of the log entry
 * @property recipientId id of the user the notification was for
 * @property recipientName name of the recipient, saved so the admin list needs no lookup
 * @property campsite name of the campsite the notification is about
 * @property message text of the notification
 * @property sentAt when it was sent, in epoch milliseconds
 * @property status "sent" or "failed"
 */
data class NotificationLog(
    val id: String = "",
    val recipientId: String = "",
    val recipientName: String = "",
    val campsite: String = "",
    val message: String = "",
    val sentAt: Long = 0L,
    val status: String = "sent"
)