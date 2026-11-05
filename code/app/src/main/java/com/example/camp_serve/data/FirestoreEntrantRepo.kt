package com.example.camp_serve.data

import com.example.camp_serve.model.Entrant
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
    override suspend fun deleteEntrant() {
        entrantDoc().delete().await()
    }
}