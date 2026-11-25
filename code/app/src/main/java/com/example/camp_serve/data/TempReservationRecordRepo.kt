package com.example.camp_serve.data

import com.example.camp_serve.model.Reservation

/*
 * TempReservationRecordRepo.kt
 * A fake ReservationRecordRepository that keeps reservations in a list.
 * Used in tests and previews so they don't need Firestore.
 * Not the same as TempReservationRepo, which is for the lottery winner's offer.
 * Outstanding issues: data is lost when the app closes (on purpose).
 */

/**
 * Keeps reservations in memory, starting with some demo data.
 */
class TempReservationRecordRepo : ReservationRecordRepository {

    // demo reservations: one of each kind so every screen has something to show
    private val reservations = mutableListOf(
        Reservation(
            id = "res-01",
            campsiteId = "camp-01",
            campsiteName = "Grande Cache Cookhouse Site",
            ownerId = "fake-owner-01",
            entrantId = "fake-id-01",
            entrantName = "Alex Kim",
            startDate = "2027-05-01",
            endDate = "2027-05-02",
            status = "active"
        ),
        Reservation(
            id = "res-02",
            campsiteId = "camp-02",
            campsiteName = "Maligne Ridge",
            ownerId = "fake-owner-01",
            entrantId = "fake-id-01",
            entrantName = "Alex Kim",
            startDate = "2026-08-10",
            endDate = "2026-08-12",
            status = "active"
        ),
        Reservation(
            id = "res-03",
            campsiteId = "camp-01",
            campsiteName = "Grande Cache Cookhouse Site",
            ownerId = "fake-owner-01",
            entrantId = "fake-id-03",
            entrantName = "Jordan Lee",
            startDate = "2027-05-08",
            endDate = "2027-05-09",
            status = "cancelled"
        ),
        Reservation(
            id = "res-04",
            campsiteId = "camp-02",
            campsiteName = "Maligne Ridge",
            ownerId = "fake-owner-01",
            entrantId = "fake-id-03",
            entrantName = "Jordan Lee",
            startDate = "2027-06-05",
            endDate = "2027-06-06",
            status = "rejected"
        )
    )

    override suspend fun getReservationsForEntrant(entrantId: String): List<Reservation> {
        val result = mutableListOf<Reservation>()
        for (reservation in reservations) {
            if (reservation.entrantId == entrantId) {
                result.add(reservation)
            }
        }
        return result
    }

    override suspend fun getReservationsForOwner(ownerId: String): List<Reservation> {
        val result = mutableListOf<Reservation>()
        for (reservation in reservations) {
            if (reservation.ownerId == ownerId) {
                result.add(reservation)
            }
        }
        return result
    }

    override suspend fun getReservationsForCampsite(campsiteId: String): List<Reservation> {
        val result = mutableListOf<Reservation>()
        for (reservation in reservations) {
            if (reservation.campsiteId == campsiteId) {
                result.add(reservation)
            }
        }
        return result
    }

    override suspend fun getReservation(id: String): Reservation? {
        for (reservation in reservations) {
            if (reservation.id == id) {
                return reservation
            }
        }
        return null
    }

    override suspend fun addReservation(reservation: Reservation) {
        // give it an id if it doesn't have one yet
        var toSave = reservation
        if (toSave.id == "") {
            toSave = toSave.copy(id = "res-" + (reservations.size + 1))
        }
        reservations.add(toSave)
    }

    override suspend fun updateStatus(id: String, status: String) {
        // find the reservation and swap it for a copy with the new status
        for (i in reservations.indices) {
            if (reservations[i].id == id) {
                reservations[i] = reservations[i].copy(status = status)
            }
        }
    }
}