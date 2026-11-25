package com.example.camp_serve.data

import com.example.camp_serve.model.Reservation

/*
 * ReservationRecordRepository.kt
 * Everything the app can do with saved reservations (lists, cancel, reject).
 * Separate from ReservationRepository, which handles the lottery winner's offer.
 * Outstanding issues: none.
 */

/**
 * Loads and saves campsite reservations.
 */
interface ReservationRecordRepository {

    /**
     * Gets every reservation made by one entrant (US 01.06.01, US 01.06.02).
     *
     * @param entrantId the entrant's device id
     * @return their reservations, any status
     */
    suspend fun getReservationsForEntrant(entrantId: String): List<Reservation>

    /**
     * Gets every reservation for any campsite this owner owns
     * (US 02.07.01, US 02.07.02, US 02.07.03).
     *
     * @param ownerId the owner's device id
     * @return reservations for their campsites, any status
     */
    suspend fun getReservationsForOwner(ownerId: String): List<Reservation>

    /**
     * Gets every reservation for one campsite (US 02.06.01 booked dates).
     *
     * @param campsiteId the campsite's id
     * @return reservations for that campsite, any status
     */
    suspend fun getReservationsForCampsite(campsiteId: String): List<Reservation>

    /**
     * Gets one reservation.
     *
     * @param id the reservation's id
     * @return the reservation, or null if it doesn't exist
     */
    suspend fun getReservation(id: String): Reservation?

    /**
     * Saves a new reservation. If its id is empty, a new id is made.
     *
     * @param reservation the reservation to save
     */
    suspend fun addReservation(reservation: Reservation)

    /**
     * Changes a reservation's status, e.g. to "cancelled" (US 01.07.01)
     * or "rejected" (US 02.08.01).
     *
     * @param id the reservation's id
     * @param status the new status
     */
    suspend fun updateStatus(id: String, status: String)
}