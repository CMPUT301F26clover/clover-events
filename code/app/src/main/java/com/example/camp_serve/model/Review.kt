package com.example.camp_serve.model

/*
 * Review.kt
 * One entrant's rating and optional written review of a campsite they stayed at
 * (US 01.13.01, US 01.13.02).
 * Outstanding issues: none.
 */

/**
 * A rating (1 to 5 stars) with an optional written review.
 * The review's id is the id of the reservation it is for, so an entrant can only
 * leave one review per stay, and rating again replaces the old one.
 * Every field has a default value so Firestore can turn it back into a review.
 *
 * @property id the id of the reservation this review is for
 * @property campsiteId id of the campsite that was reviewed
 * @property campsiteName name of the campsite, saved so lists don't need another lookup
 * @property entrantId id of the entrant who wrote it
 * @property entrantName name of the entrant, saved for lists
 * @property rating stars from 1 to 5
 * @property text the written review, empty if they only gave a rating
 * @property createdAt when it was saved, in epoch milliseconds
 */
data class Review(
    val id: String = "",
    val campsiteId: String = "",
    val campsiteName: String = "",
    val entrantId: String = "",
    val entrantName: String = "",
    val rating: Int = 0,
    val text: String = "",
    val createdAt: Long = 0L
)