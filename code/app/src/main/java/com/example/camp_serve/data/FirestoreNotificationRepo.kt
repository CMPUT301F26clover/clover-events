package com.example.camp_serve.data

import com.example.camp_serve.model.Notification
import com.example.camp_serve.model.newestFirst
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/*
 * FirestoreNotificationRepo.kt stores notifications in Cloud Firestore, in the
 * "notifications" collection, with the document id equal to [Notification.id].
 * Queries filter on one field and sort in the app, so no composite index is needed.
 * Outstanding issues: Firestore security rules should limit who can read these.
 */

/**
 * Firestore-backed [NotificationRepository].
 *
 * @param db the Firestore instance to use
 */
class FirestoreNotificationRepo(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : NotificationRepository {

    private companion object {
        const val COLLECTION = "notifications"

        // Firestore allows at most 500 writes in one batch.
        const val MAX_WRITES_PER_BATCH = 500
    }

    private fun collection() = db.collection(COLLECTION)

    override suspend fun saveAll(notifications: List<Notification>) {
        notifications.chunked(MAX_WRITES_PER_BATCH).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { batch.set(collection().document(it.id), it) }
            batch.commit().await()
        }
    }

    override suspend fun getForRecipient(recipientId: String): List<Notification> =
        load(collection().whereEqualTo("recipientId", recipientId))

    private suspend fun load(query: Query): List<Notification> =
        query.get().await().toObjects(Notification::class.java).newestFirst()
}
