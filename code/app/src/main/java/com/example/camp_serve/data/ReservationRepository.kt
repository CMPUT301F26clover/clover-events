package com.example.camp_serve.data

import com.example.camp_serve.model.CampsiteOffer
import com.example.camp_serve.model.StayDates

/*
 * ReservationRepository.kt
 * The lottery winner's offer: loading it and answering it. Saved reservations
 * live in ReservationRecordRepository.
 * Outstanding issues: only a temporary implementation exists, no Firestore one.
 */

/**
 * Loads a winner's offer and records their answer.
 */
interface ReservationRepository {

    /** Gets the offer shown to the winner (US 01.04.01). */
    suspend fun getOffer(): CampsiteOffer

    /**
     * Records that the winner accepted the offer (US 01.04.02).
     *
     * @param offerId the offer's id
     * @param stay the stay the winner chose
     */
    suspend fun reserve(offerId: String, stay: StayDates)

    /**
     * Records that the winner declined the offer (US 01.04.03).
     *
     * @param offerId the offer's id
     */
    suspend fun decline(offerId: String)
}