package com.example.camp_serve.data

import com.example.camp_serve.model.Campsite
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/*
 * FirestoreCampsiteRepo.kt
 * Real database version of the campsite repo. Campsites get saved in the
 * "campsites" collection in Firestore, so they don't disappear when the app closes.
 * TODO: if Firestore fails, the screens don't show an error message yet.
 */

/**
 * Saves and loads campsites using Firestore.
 * Each campsite is saved as one document, and the document's id is the campsite's id.
 *
 * @param db the Firestore database
 */
class FirestoreCampsiteRepo(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : CampsiteRepository {

    // the "campsites" folder in Firestore
    private val campsites = db.collection("campsites")

    /**
     * Gets all the campsites in the database.
     *
     * @return every campsite
     */
    override suspend fun getAllCampsites(): List<Campsite> {
        val result = campsites.get().await()
        val list = mutableListOf<Campsite>()
        for (doc in result.documents) {
            val campsite = doc.toObject(Campsite::class.java)
            if (campsite != null) {
                list.add(campsite)
            }
        }
        return list
    }

    /**
     * Gets only the campsites that belong to one owner.
     *
     * @param ownerId the owner's id
     * @return that owner's campsites, or an empty list if they have none
     */
    override suspend fun getCampsitesForOwner(ownerId: String): List<Campsite> {
        val result = campsites.whereEqualTo("ownerId", ownerId).get().await()
        val list = mutableListOf<Campsite>()
        for (doc in result.documents) {
            val campsite = doc.toObject(Campsite::class.java)
            if (campsite != null) {
                list.add(campsite)
            }
        }
        return list
    }

    /**
     * Gets one campsite using its id.
     *
     * @param id the campsite's id
     * @return the campsite, or null if it isn't found
     */
    override suspend fun getCampsite(id: String): Campsite? {
        val doc = campsites.document(id).get().await()
        return doc.toObject(Campsite::class.java)
    }

    /**
     * Adds a new campsite. If it doesn't have an id yet, Firestore makes one for it.
     *
     * @param campsite the new campsite
     */
    override suspend fun addCampsite(campsite: Campsite) {
        var newCampsite = campsite
        if (campsite.id == "") {
            // get a new random id from Firestore
            val newId = campsites.document().id
            newCampsite = campsite.copy(id = newId)
        }
        campsites.document(newCampsite.id).set(newCampsite).await()
    }

    /**
     * Saves changes to a campsite that's already in the database.
     *
     * @param campsite the campsite with the updated info
     */
    override suspend fun updateCampsite(campsite: Campsite) {
        campsites.document(campsite.id).set(campsite).await()
    }

    /**
     * Deletes a campsite from the database.
     *
     * @param id the id of the campsite to delete
     */
    override suspend fun deleteCampsite(id: String) {
        campsites.document(id).delete().await()
    }
}