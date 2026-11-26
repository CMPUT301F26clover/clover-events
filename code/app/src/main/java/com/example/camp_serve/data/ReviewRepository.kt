package com.example.camp_serve.data

import com.example.camp_serve.model.Review

/*
 * ReviewRepository.kt
 * Everything the app can do with saved ratings and reviews.
 * Screens only use this, so we can swap the fake repo for Firestore.
 * Outstanding issues: none.
 */

/**
 * Loads and saves campsite ratings and reviews.
 */
interface ReviewRepository {

    /**
     * Gets every review of one campsite.
     *
     * @param campsiteId the campsite's id
     * @return its reviews, empty if there are none
     */
    suspend fun getReviewsForCampsite(campsiteId: String): List<Review>

    /**
     * Gets the review an entrant left for one stay.
     *
     * @param reservationId the reservation's id
     * @return the review, or null if they haven't reviewed it yet
     */
    suspend fun getReviewForReservation(reservationId: String): Review?

    /**
     * Saves a review. If one with the same id exists it is replaced.
     *
     * @param review the review to save (its id is the reservation id)
     */
    suspend fun saveReview(review: Review)

    /**
     * Deletes a review, e.g. when an admin removes one that breaks the app policy
     * (US 03.09.01, US 03.10.01).
     *
     * @param id the review's id
     */
    suspend fun deleteReview(id: String)
}