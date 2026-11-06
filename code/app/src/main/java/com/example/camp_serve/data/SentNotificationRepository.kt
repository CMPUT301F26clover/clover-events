/*
 * SentNotificationRepository.kt lists how sent owner notifications are saved and read, and
 * holds a temporary in-memory version with sample data (US 02.07.01, 02.07.02, 02.10.02).
 * TODO: REPLACE TempSentNotificationRepo WITH FIRESTORE VERSION LATER
 * Outstanding issues: sending does not reach entrants' AlertRepository yet.
 */

package com.example.camp_serve.data

import com.example.camp_serve.model.SentNotification
import java.util.Date

/**
 * Where the owner's sent notifications are kept.
 */
interface SentNotificationRepository {

    /**
     * Gets the owner's sent notifications, newest first.
     *
     * @param ownerId the owner's id
     */
    suspend fun getForOwner(ownerId: String): List<SentNotification>

    /**
     * Records a sent notification.
     *
     * @param notification what was sent
     */
    suspend fun add(notification: SentNotification)
}

/**
 * Stand-in for Firestore that keeps sent notifications in memory.
 *
 * @param withSampleData true to start with sample notifications
 */
class TempSentNotificationRepo(withSampleData: Boolean = true) : SentNotificationRepository {
    private val items = mutableListOf<SentNotification>()

    init {
        if (withSampleData) {
            val now = System.currentTimeMillis()
            items.add(SentNotification("n1", "fake-owner-01", "lottery-01", "Grande Cache Cookhouse Site",
                "Registration closes tomorrow. Good luck!", 12, Date(now - 2 * 3600_000L)))
            items.add(SentNotification("n2", "fake-owner-01", "lottery-02", "Maligne Ridge",
                "Draw results are posted", 30, Date(now - 26 * 3600_000L)))
        }
    }

    override suspend fun getForOwner(ownerId: String): List<SentNotification> =
        items.filter { it.ownerId == ownerId }.sortedByDescending { it.sentAt }

    override suspend fun add(notification: SentNotification) {
        val id = if (notification.id.isBlank()) "n" + (items.size + 1) else notification.id
        items.add(notification.copy(id = id))
    }
}
