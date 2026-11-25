package com.example.camp_serve.model

/*
 * Reservation.kt
 * One booking of a campsite by an entrant. Used by the entrant's reservation
 * screens (US 01.06.01, 01.06.02, 01.07.01) and the owner's reservation
 * screens (US 02.06.01, 02.07.01, 02.07.02, 02.07.03, 02.08.01).
 * Outstanding issues: none.
 */

/**
 * A reservation of a campsite for some dates.
 *
 * @property id the reservation's id in Firestore
 * @property campsiteId id of the campsite that was booked
 * @property campsiteName name of the campsite, saved so lists don't need another lookup
 * @property ownerId id of the campsite's owner
 * @property entrantId id of the entrant who booked it
 * @property entrantName name of the entrant, saved for the owner's list
 * @property startDate first night, as "YYYY-MM-DD"
 * @property endDate last night, as "YYYY-MM-DD"
 * @property status "active", "cancelled" (by the entrant) or "rejected" (by the owner)
 * @property rulesAccepted true if the entrant agreed to the campsite rules
 */
data class Reservation(
    val id: String = "",
    val campsiteId: String = "",
    val campsiteName: String = "",
    val ownerId: String = "",
    val entrantId: String = "",
    val entrantName: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val status: String = "active",
    val rulesAccepted: Boolean = false
)