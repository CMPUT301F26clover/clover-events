package com.example.camp_serve.data

import com.example.camp_serve.model.Review
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/*
 * FirestoreReviewRepo.kt
 * The real ReviewRepository. Saves and loads reviews from the "reviews"
 * collection in Firestore. Each review's document id is its reservation id.
 * Outstanding issues: reviews with wrong field types are skipped, not shown.
 */

/**
 * Loads and saves reviews in Firestore.
 */
class FirestoreReviewRepo : ReviewRepository {

    private val reviews = FirebaseFirestore.getInstance().collection("reviews")

    override suspend fun getReviewsForCampsite(campsiteId: String): List<Review> {
        val result = reviews.whereEqualTo("campsiteId", campsiteId).get().await()
        val list = mutableListOf<Review>()
        for (doc in result.documents) {
            val review = toReviewOrNull(doc)
            if (review != null) {
                list.add(review)
            }
        }
        return list
    }

    override suspend fun getReviewForReservation(reservationId: String): Review? {
        val doc = reviews.document(reservationId).get().await()
        return toReviewOrNull(doc)
    }

    override suspend fun saveReview(review: Review) {
        // if there's no id yet, let Firestore make one
        var toSave = review
        if (toSave.id == "") {
            toSave = toSave.copy(id = reviews.document().id)
        }
        reviews.document(toSave.id).set(toSave).await()
    }

    override suspend fun deleteReview(id: String) {
        reviews.document(id).delete().await()
    }

    /**
     * Turns a Firestore doc into a Review. If a field has the wrong type
     * we skip it instead of crashing.
     *
     * @param doc the Firestore document
     * @return the review, or null if it couldn't be read
     */
    private fun toReviewOrNull(doc: DocumentSnapshot): Review? {
        try {
            return doc.toObject(Review::class.java)
        } catch (e: Exception) {
            return null
        }
    }
}