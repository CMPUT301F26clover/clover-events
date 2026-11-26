package com.example.camp_serve.data

import com.example.camp_serve.model.Review

/*
 * TempReviewRepo.kt
 * A fake ReviewRepository that keeps reviews in a list.
 * Used in tests and previews so they don't need Firestore.
 * Outstanding issues: data is lost when the app closes (on purpose).
 */

/**
 * Keeps reviews in memory, starting with one demo review.
 */
class TempReviewRepo : ReviewRepository {

    // demo review for reservation res-02 so editing a review can be tested
    private val reviews = mutableListOf(
        Review(
            id = "res-02",
            campsiteId = "camp-02",
            campsiteName = "Maligne Ridge",
            entrantId = "fake-id-01",
            entrantName = "Alex Kim",
            rating = 4,
            text = "Great views and quiet at night.",
            createdAt = 1L
        )
    )

    override suspend fun getReviewsForCampsite(campsiteId: String): List<Review> {
        val result = mutableListOf<Review>()
        for (review in reviews) {
            if (review.campsiteId == campsiteId) {
                result.add(review)
            }
        }
        return result
    }

    override suspend fun getReviewForReservation(reservationId: String): Review? {
        for (review in reviews) {
            if (review.id == reservationId) {
                return review
            }
        }
        return null
    }

    override suspend fun saveReview(review: Review) {
        // replace the old review with the same id, or add a new one
        for (i in reviews.indices) {
            if (reviews[i].id == review.id) {
                reviews[i] = review
                return
            }
        }
        reviews.add(review)
    }

    override suspend fun deleteReview(id: String) {
        reviews.removeAll { it.id == id }
    }
}