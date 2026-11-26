package com.example.camp_serve.model

/*
 * ReviewRules.kt
 * Small helpers for ratings and reviews: checking a review before it is saved
 * and working out a campsite's average rating (US 01.13.01, US 01.13.02).
 * Outstanding issues: none.
 */

const val MIN_RATING = 1
const val MAX_RATING = 5
const val MAX_REVIEW_LENGTH = 500

/**
 * Checks a rating and review text before they are saved.
 *
 * @param rating stars chosen, 0 means none chosen yet
 * @param text the written review (can be empty)
 * @return a message saying what is wrong, or null if it is fine
 */
fun validateReview(rating: Int, text: String): String? {
    if (rating < MIN_RATING || rating > MAX_RATING) {
        return "Please choose a rating from 1 to 5 stars."
    }
    if (text.length > MAX_REVIEW_LENGTH) {
        return "Reviews can be at most 500 characters."
    }
    return null
}

/**
 * Works out the average rating of some reviews.
 *
 * @param reviews the reviews of one campsite
 * @return the average stars, or 0.0 if there are no reviews
 */
fun averageRating(reviews: List<Review>): Double {
    if (reviews.isEmpty()) {
        return 0.0
    }
    var total = 0
    for (review in reviews) {
        total += review.rating
    }
    return total.toDouble() / reviews.size
}