package com.example.camp_serve.data

import com.example.camp_serve.model.Notification

/*
 * NotificationRepository.kt
 * Where notifications are saved and read from. Screens and services depend on this
 * interface only; the app uses [FirestoreNotificationRepo].
 * Outstanding issues: none.
 */

/** Saves and loads [Notification]s. */
interface NotificationRepository {

    /**
     * Saves notifications. Saving one with an id that already exists replaces it,
     * so saving the same draw twice does not create duplicates.
     *
     * @param notifications the notifications to save
     */
    suspend fun saveAll(notifications: List<Notification>)

    /**
     * Gets every notification for one entrant, newest first.
     *
     * @param recipientId the entrant's id
     * @return that entrant's notifications
     */
    suspend fun getForRecipient(recipientId: String): List<Notification>
}
