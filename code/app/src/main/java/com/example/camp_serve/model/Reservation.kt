/*
 * Reservation.kt
 *
 * Role: Model class for one reservation an entrant holds at an owner's campsite, as shown on
 * the owner's Reservations tab (US 02.07.01, 02.07.02, 02.08.01).
 * Outstanding issues: the entrant side does not create these yet; Temp repo has sample data.
 */

package com.example.camp_serve.model

import java.util.Date

/** Status of a reservation. [label] is the text shown on screen. */
enum class ReservationStatus(val label: String) {
    CONFIRMED("Confirmed"),
    COMPLETED("Completed"),
    CANCELLED_BY_OWNER("Cancelled by you"),
    CANCELLED_BY_ENTRANT("Cancelled by entrant")
}

/**
 * A reservation.
 *
 * @property id unique id
 * @property ownerId owner of the campsite
 * @property campsiteId campsite reserved
 * @property campsiteName campsite name copied so the list needs no extra lookup
 * @property entrantId entrant who reserved
 * @property entrantName name of that entrant
 * @property startDate first night
 * @property endDate departure day
 * @property status current status
 */
data class Reservation(
    val id: String = "",
    val ownerId: String = "",
    val campsiteId: String = "",
    val campsiteName: String = "",
    val entrantId: String = "",
    val entrantName: String = "",
    val startDate: Date = Date(0),
    val endDate: Date = Date(0),
    val status: ReservationStatus = ReservationStatus.CONFIRMED
) {
    /** True if the owner can still message or reject it. */
    fun isActive(): Boolean = status == ReservationStatus.CONFIRMED

    /** True if it was cancelled by either side. */
    fun isCancelled(): Boolean =
        status == ReservationStatus.CANCELLED_BY_OWNER ||
            status == ReservationStatus.CANCELLED_BY_ENTRANT
}
