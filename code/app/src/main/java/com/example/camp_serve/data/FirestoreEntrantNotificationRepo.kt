package com.example.camp_serve.data

import com.example.camp_serve.model.EntrantNotification
import com.example.camp_serve.model.EntrantNotificationType
import com.example.camp_serve.model.Notification
import com.example.camp_serve.model.NotificationType
import com.example.camp_serve.model.isDelivered

/**
 * [EntrantNotificationRepository] for the entrant's Notifications screen, built from the notifications
 * saved for this entrant (US 01.03.01, 01.03.02). Notifications skipped because the
 * entrant opted out (US 01.10.01) are left out.
 *
 * @param entrantId this device's entrant id
 * @param notifications where notifications are stored; Firestore by default
 */
class FirestoreEntrantNotificationRepo(
    private val entrantId: String,
    notifications: NotificationRepository? = null
) : EntrantNotificationRepository {
    private val storage by lazy { notifications ?: FirestoreNotificationRepo() }

    override suspend fun getNotifications(): List<EntrantNotification> =
        storage.getForRecipient(entrantId).filter { it.isDelivered() }.map { it.toEntrantNotification() }
}

/** Shows a notification on the entrant's Notifications screen. */
fun Notification.toEntrantNotification(): EntrantNotification = EntrantNotification(
    id = id,
    type = when (type) {
        NotificationType.SYSTEM_WIN -> EntrantNotificationType.WON
        NotificationType.SYSTEM_LOSE -> EntrantNotificationType.LOST
        NotificationType.OWNER -> EntrantNotificationType.MESSAGE
    },
    campsite = campsiteName,
    message = message
)
