package com.example.camp_serve.data

import com.example.camp_serve.model.Reservation
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/*
 * FirestoreReservationRecordRepo.kt
 * The real ReservationRecordRepository. Saves and loads reservations from
 * the "reservations" collection in Firestore.
 * Outstanding issues: reservations with wrong field types are skipped, not shown.
 */

/**
 * Loads and saves reservations in Firestore.
 */
class FirestoreReservationRecordRepo : ReservationRecordRepository {

    private val reservations = FirebaseFirestore.getInstance().collection("reservations")

    override suspend fun getReservationsForEntrant(entrantId: String): List<Reservation> {
        val result = reservations.whereEqualTo("entrantId", entrantId).get().await()
        val list = mutableListOf<Reservation>()
        for (doc in result.documents) {
            val reservation = toReservationOrNull(doc)
            if (reservation != null) {
                list.add(reservation)
            }
        }
        return list
    }

    override suspend fun getReservationsForOwner(ownerId: String): List<Reservation> {
        val result = reservations.whereEqualTo("ownerId", ownerId).get().await()
        val list = mutableListOf<Reservation>()
        for (doc in result.documents) {
            val reservation = toReservationOrNull(doc)
            if (reservation != null) {
                list.add(reservation)
            }
        }
        return list
    }

    override suspend fun getReservationsForCampsite(campsiteId: String): List<Reservation> {
        val result = reservations.whereEqualTo("campsiteId", campsiteId).get().await()
        val list = mutableListOf<Reservation>()
        for (doc in result.documents) {
            val reservation = toReservationOrNull(doc)
            if (reservation != null) {
                list.add(reservation)
            }
        }
        return list
    }

    override suspend fun getReservation(id: String): Reservation? {
        val doc = reservations.document(id).get().await()
        return toReservationOrNull(doc)
    }

    override suspend fun addReservation(reservation: Reservation) {
        // if there's no id yet, let Firestore make one
        var toSave = reservation
        if (toSave.id == "") {
            toSave = toSave.copy(id = reservations.document().id)
        }
        reservations.document(toSave.id).set(toSave).await()
    }

    override suspend fun updateStatus(id: String, status: String) {
        // only change the status field, leave everything else the same
        reservations.document(id).update("status", status).await()
    }

    /**
     * Turns a Firestore doc into a Reservation. If a field has the wrong type
     * (e.g. "true" as text instead of a boolean), we skip it instead of crashing.
     *
     * @param doc the Firestore document
     * @return the reservation, or null if it couldn't be read
     */
    private fun toReservationOrNull(doc: DocumentSnapshot): Reservation? {
        try {
            return doc.toObject(Reservation::class.java)
        } catch (e: Exception) {
            return null
        }
    }
}