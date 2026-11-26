package com.example.camp_serve.data

import com.example.camp_serve.model.NotificationLog
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * Firestore-backed [NotificationLogRepository]. Each log is one document in the
 * "notificationLogs" collection, with the document id equal to the log's id.
 *
 * @param db the Firestore instance to use (replaceable in tests)
 */
class FirestoreNotificationLogRepo(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : NotificationLogRepository {

    private companion object {
        const val COLLECTION = "notificationLogs"
    }

    override suspend fun getAllNotificationLogs(): List<NotificationLog> {
        // Ordering by one field needs no extra Firestore index.
        val result = db.collection(COLLECTION)
            .orderBy("sentAt", Query.Direction.DESCENDING)
            .get().await()
        return result.documents.mapNotNull { doc ->
            doc.toObject(NotificationLog::class.java)?.copy(id = doc.id)
        }
    }

    override suspend fun addNotificationLog(log: NotificationLog) {
        val collection = db.collection(COLLECTION)
        val toSave = if (log.id.isBlank()) log.copy(id = collection.document().id) else log
        collection.document(toSave.id).set(toSave).await()
    }
}