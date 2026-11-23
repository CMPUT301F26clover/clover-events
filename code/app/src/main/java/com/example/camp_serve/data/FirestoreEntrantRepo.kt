package com.example.camp_serve.data

import com.example.camp_serve.model.Entrant
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/*
 * FirestoreEntrantRepo.kt stores the entrant profile in Cloud Firestore
 * It replaces TempEntrantRepo so profile changes survive app restarts.
 * Screens only see the EntrantRepository interface.
 *
 * Outstanding issues: none
 */

/**
 * Firestore-backed [EntrantRepository]. Each entrant is one document in the
 * "entrants" collection, with the document id equal to the entrant's id.
 *
 * @param db the Firestore instance to use (replaceable in tests)
 */
class FirestoreEntrantRepo(
    private val entrantId: String,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : EntrantRepository {

    private companion object {
        const val COLLECTION = "entrants"

        // Firestore allows at most 30 values in one whereIn filter.
        const val MAX_IDS_PER_QUERY = 30

    }

    private fun entrantDoc() = db.collection(COLLECTION).document(entrantId)

    /**
     * Loads this device's entrant. If none is stored yet, a blank entrant is
     * created and saved so first-time users don't get an empty result.
     *
     * @return the stored (or newly created) entrant
     * @throws Exception if Firestore can't be reached
     */
    override suspend fun getEntrant(): Entrant {
        val snapshot = entrantDoc().get().await()
        val existing = snapshot.toObject(Entrant::class.java)
        if (existing != null) {
            return existing
        }
        val blank = Entrant(id = entrantId)
        entrantDoc().set(blank).await()
        return blank
    }

    /**
     * Saves the entrant, replacing the stored version.
     *
     * @param entrant the entrant with its new details
     * @throws Exception if the write fails
     */
    override suspend fun updateEntrant(entrant: Entrant) {
        entrantDoc().set(entrant).await()
    }

    /**
     * Deletes this device's entrant document.
     *
     * @throws Exception if the delete fails
     */
    override suspend fun deleteEntrant(id: String) {
        db.collection(COLLECTION).document(id).delete().await()
    }

    override suspend fun getAllEntrants(): List<Entrant> {
        return db.collection(COLLECTION).get().await().toObjects(Entrant::class.java)
    }

    /**
     * Loads entrants by document id, 30 at a time (Firestore's whereIn limit).
     * Does not depend on [entrantId], so an owner can use this to show waitlist names.
     */
    override suspend fun getEntrantsByIds(ids: Collection<String>): List<Entrant> {
        val wanted = ids.filter { it.isNotBlank() && '/' !in it }.distinct()
        return wanted.chunked(MAX_IDS_PER_QUERY).flatMap { chunk ->
            db.collection(COLLECTION)
                .whereIn(FieldPath.documentId(), chunk)
                .get().await()
                .documents.mapNotNull { doc ->
                    doc.toObject(Entrant::class.java)?.let { it.copy(id = it.id.ifBlank { doc.id }) }
                }
        }
    }
}
