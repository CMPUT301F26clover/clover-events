package com.example.camp_serve.data

import com.example.camp_serve.model.MessageLog
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * Firestore-backed [MessageLogRepository]. Each log is one document in the
 * "messageLogs" collection, with the document id equal to the log's id.
 *
 * @param db the Firestore instance to use (replaceable in tests)
 */
class FirestoreMessageLogRepo(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : MessageLogRepository {

    private companion object {
        const val COLLECTION = "messageLogs"
    }

    override suspend fun getAllMessageLogs(): List<MessageLog> {
        // Ordering by one field needs no extra Firestore index.
        val result = db.collection(COLLECTION)
            .orderBy("sentAt", Query.Direction.DESCENDING)
            .get().await()
        return result.documents.mapNotNull { doc ->
            doc.toObject(MessageLog::class.java)?.copy(id = doc.id)
        }
    }

    override suspend fun addMessageLog(log: MessageLog) {
        val collection = db.collection(COLLECTION)
        val toSave = if (log.id.isBlank()) log.copy(id = collection.document().id) else log
        collection.document(toSave.id).set(toSave).await()
    }
}