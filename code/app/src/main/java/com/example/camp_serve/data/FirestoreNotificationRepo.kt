package com.example.camp_serve.data

import com.example.camp_serve.model.Notification
import com.example.camp_serve.model.newestFirst
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/*
 * FirestoreNotificationRepo.kt stores notifications in Cloud Firestore.
 * Outstanding issues: Firestore security rules should limit who can read these
 * documents (entrants their own, owners the ones they sent).
 */

/**
 * Firestore-backed [NotificationRepository]. Each notification is one document in
 * the "notifications" collection, with the document id equal to [Notification.id].
 *
 * Queries filter on one field and sort in the app, so no composite index is needed.
 *
 * @param db the Firestore instance to use (replaceable in tests)
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

    /**
     * Saves the notifications in batches. Ids are reused, so saving again overwrites
     * instead of duplicating.
     *
     * @throws Exception if a write fails
     */
    override suspend fun saveAll(notifications: List<Notification>) {
        notifications.chunked(MAX_WRITES_PER_BATCH).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { batch.set(collection().document(it.id), it) }
            batch.commit().await()
        }
    }

    override suspend fun getForRecipient(recipientId: String): List<Notification> =
        load(collection().whereEqualTo("recipientId", recipientId))

    override fun observeForRecipient(recipientId: String): Flow<List<Notification>> =
        callbackFlow {
            val listener = collection().whereEqualTo("recipientId", recipientId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    try {
                        trySend(
                            snapshot?.toObjects(Notification::class.java).orEmpty().newestFirst()
                        )
                    } catch (e: Exception) {
                        close(e)
                    }
                }
            awaitClose { listener.remove() }
        }.buffer(Channel.CONFLATED)

    override suspend fun getSentBy(senderId: String): List<Notification> =
        load(collection().whereEqualTo("senderId", senderId))

    private suspend fun load(query: Query): List<Notification> =
        query.get().await().toObjects(Notification::class.java).newestFirst()
}
