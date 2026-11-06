package com.example.camp_serve.data

import com.example.camp_serve.model.Owner
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Firestore-backed [OwnerRepository]. Each owner is one document in the
 * "owners" collection, with the document id equal to the owner's id.
 *
 * @param ownerId id of the owner on this device
 * @param db the Firestore instance to use (replaceable in tests)
 */
class FirestoreOwnerRepo(
    private val ownerId: String,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : OwnerRepository {

    private companion object {
        const val COLLECTION = "owners"
    }

    private fun ownerDoc() = db.collection(COLLECTION).document(ownerId)

    override suspend fun getOwner(): Owner {
        val existing = ownerDoc().get().await().toObject(Owner::class.java)
        if (existing != null) {
            return existing
        }
        val blank = Owner(id = ownerId)
        ownerDoc().set(blank).await()
        return blank
    }

    override suspend fun getAllOwners(): List<Owner> {
        return db.collection(COLLECTION).get().await().toObjects(Owner::class.java)
    }

    override suspend fun updateOwner(owner: Owner) {
        db.collection(COLLECTION).document(owner.id).set(owner).await()
    }

    override suspend fun deleteOwner(id: String) {
        db.collection(COLLECTION).document(id).delete().await()
    }
}