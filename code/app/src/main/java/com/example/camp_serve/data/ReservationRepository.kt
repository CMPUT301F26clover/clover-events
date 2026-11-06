/*
 * ReservationRepository.kt lists what the owner can do with reservations (view, reject)
 * and holds a temporary in-memory version with sample data.
 * TODO: REPLACE TempReservationRepo WITH FIRESTORE VERSION LATER
 * Outstanding issues: reservations are not created by the entrant side yet.
 */

package com.example.camp_serve.data

import com.example.camp_serve.model.Reservation
import com.example.camp_serve.model.ReservationStatus
import com.example.camp_serve.util.addDays
import com.example.camp_serve.util.startOfDay
import java.util.Date

/**
 * Where reservations come from and get changed.
 */
interface ReservationRepository {

    /**
     * Gets all reservations for an owner's campsites.
     *
     * @param ownerId the owner's id
     */
    suspend fun getReservationsForOwner(ownerId: String): List<Reservation>

    /**
     * Gets one reservation.
     *
     * @param id reservation id
     */
    suspend fun getReservation(id: String): Reservation?

    /**
     * Owner rejects (cancels) a confirmed reservation.
     *
     * @param id reservation id
     * @return true if it was confirmed and is now cancelled by the owner
     */
    suspend fun reject(id: String): Boolean
}

/**
 * Stand-in for Firestore that keeps reservations in memory.
 *
 * @param withSampleData true to start with sample reservations
 */
class TempReservationRepo(withSampleData: Boolean = true) : ReservationRepository {
    private val reservations = mutableListOf<Reservation>()

    init {
        if (withSampleData) {
            val today = startOfDay(Date())
            reservations.add(Reservation("res-01", "fake-owner-01", "camp-01", "Grande Cache Cookhouse Site",
                "e1", "Cole Smith", addDays(today, 4), addDays(today, 6)))
            reservations.add(Reservation("res-02", "fake-owner-01", "camp-02", "Maligne Ridge",
                "e2", "Ava Tremblay", addDays(today, 9), addDays(today, 12)))
            reservations.add(Reservation("res-03", "fake-owner-01", "camp-01", "Grande Cache Cookhouse Site",
                "e3", "Noah Singh", addDays(today, -20), addDays(today, -18), ReservationStatus.COMPLETED))
            reservations.add(Reservation("res-04", "fake-owner-01", "camp-02", "Maligne Ridge",
                "e4", "Mia Chen", addDays(today, 15), addDays(today, 17), ReservationStatus.CANCELLED_BY_ENTRANT))
        }
    }

    override suspend fun getReservationsForOwner(ownerId: String): List<Reservation> =
        reservations.filter { it.ownerId == ownerId }

    override suspend fun getReservation(id: String): Reservation? =
        reservations.firstOrNull { it.id == id }

    override suspend fun reject(id: String): Boolean {
        for (i in reservations.indices) {
            if (reservations[i].id == id) {
                if (!reservations[i].isActive()) return false
                reservations[i] = reservations[i].copy(status = ReservationStatus.CANCELLED_BY_OWNER)
                return true
            }
        }
        return false
    }
}
