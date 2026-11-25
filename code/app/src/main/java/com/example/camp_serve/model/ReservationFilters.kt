package com.example.camp_serve.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/*
 * ReservationFilters.kt
 * Small helpers that split reservations into active, past and cancelled lists.
 * Used by the entrant screens (US 01.06.01, 01.06.02) and the owner screens
 * (US 02.07.01, 02.07.02, 02.07.03).
 * Outstanding issues: none.
 */

/**
 * Gets today's date as "YYYY-MM-DD", the same format reservations use.
 *
 * @return today's date
 */
fun todayString(): String {
    // "yyyy-MM-dd" gives the same format as reservation dates, e.g. "2026-11-25"
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return format.format(Date())
}

/**
 * Keeps reservations that are still coming up (not finished, not cancelled).
 *
 * @param reservations all the reservations
 * @param today today's date as "YYYY-MM-DD"
 * @return only the active ones
 */
fun activeReservations(reservations: List<Reservation>, today: String): List<Reservation> {
    val result = mutableListOf<Reservation>()
    for (reservation in reservations) {
        if (reservation.status == "active" && reservation.endDate >= today) {
            result.add(reservation)
        }
    }
    return result
}

/**
 * Keeps reservations whose stay is already over.
 *
 * @param reservations all the reservations
 * @param today today's date as "YYYY-MM-DD"
 * @return only the past ones
 */
fun pastReservations(reservations: List<Reservation>, today: String): List<Reservation> {
    val result = mutableListOf<Reservation>()
    for (reservation in reservations) {
        if (reservation.status == "active" && reservation.endDate < today) {
            result.add(reservation)
        }
    }
    return result
}

/**
 * Keeps reservations that were cancelled by the entrant or rejected by the owner.
 *
 * @param reservations all the reservations
 * @return only the cancelled or rejected ones
 */
fun cancelledReservations(reservations: List<Reservation>): List<Reservation> {
    val result = mutableListOf<Reservation>()
    for (reservation in reservations) {
        if (reservation.status == "cancelled" || reservation.status == "rejected") {
            result.add(reservation)
        }
    }
    return result
}