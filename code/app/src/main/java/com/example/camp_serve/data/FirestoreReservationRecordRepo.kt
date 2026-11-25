package com.example.camp_serve.data

import com.example.camp_serve.model.Reservation
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/*
 * FirestoreReservationRecordRepo.kt
 * The real ReservationRecordRepository. Saves and loads reservations from
 * the "reservations" collection in Firestore.
 * Outstanding issues: none.
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
            val reservation = doc.toObject(Reservation::class.java)
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
            val reservation = doc.toObject(Reservation::class.java)
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
            val reservation = doc.toObject(Reservation::class.java)
            if (reservation != null) {
                list.add(reservation)
            }
        }
        return list
    }

    override suspend fun getReservation(id: String): Reservation? {
        val doc = reservations.document(id).get().await()
        return doc.toObject(Reservation::class.java)
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
}