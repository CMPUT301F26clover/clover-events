package com.example.camp_serve.data

import com.example.camp_serve.model.Alert
import com.example.camp_serve.model.AlertType
import com.example.camp_serve.model.Notification
import com.example.camp_serve.model.NotificationType
import com.example.camp_serve.model.isDelivered
import kotlinx.coroutines.CancellationException

/*
 * NotificationAlertRepo.kt
 * Feeds the entrant's existing Alerts screen from the notifications saved in
 * Firestore, so the Alert classes stay as they are.
 * Outstanding issues: if loading fails (e.g. offline with no cache), the screen
 * shows no alerts instead of an error.
 */

/**
 * [AlertRepository] built from the notifications saved for this entrant.
 *
 * @param entrantId this device's entrant id
 * @param notifications where notifications are stored; Firestore by default
 */
class NotificationAlertRepo(
    private val entrantId: String,
    notifications: NotificationRepository? = null
) : AlertRepository {
    private val storage by lazy { notifications ?: FirestoreNotificationRepo() }

    override suspend fun getAlerts(): List<Alert> =
        try {
            storage.getForRecipient(entrantId).filter { it.isDelivered() }.map { it.toAlert() }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
}

/** Shows a notification on the entrant's Alerts screen. */
fun Notification.toAlert(): Alert = Alert(
    id = id,
    type = when (type) {
        NotificationType.SYSTEM_WIN -> AlertType.WON
    },
    campsite = campsiteName,
    message = message
)
