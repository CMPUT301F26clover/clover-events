package com.example.camp_serve.data

import com.example.camp_serve.model.Notification
import kotlinx.coroutines.flow.Flow

/**
 * Where notifications are saved and read from. Screens and services depend on
 * this interface only; the app uses [FirestoreNotificationRepo].
 */
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
     */
    suspend fun getForRecipient(recipientId: String): List<Notification>

    /**
     * Watches one entrant's notifications, emitting the full list (newest first)
     * whenever one is added. Used to show phone notifications (#101).
     *
     * @param recipientId the entrant's id
     */
    fun observeForRecipient(recipientId: String): Flow<List<Notification>>

    /**
     * Gets every notification an owner sent, including draw results sent on their
     * behalf (US 02.10.02), newest first.
     *
     * @param senderId the owner's id
     */
    suspend fun getSentBy(senderId: String): List<Notification>
}
