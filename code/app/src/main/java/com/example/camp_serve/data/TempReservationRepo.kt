package com.example.camp_serve.data

import com.example.camp_serve.model.CampsiteOffer
import com.example.camp_serve.model.StayDates

/*
 * TempReservationRepo.kt
 * Sample offer data for the Reserve screen, until offers come from lotteries.
 * Outstanding issues: ids are made up, not real campsites.
 */

val sampleOffer = CampsiteOffer(
    id = "1",
    campsiteId = "campsite-1",
    campsite = "Lakeview Campsite",
    ownerId = "owner-1",
    availableDates = listOf(
        StayDates("2027-07-04", "2027-07-06"),
        StayDates("2027-07-11", "2027-07-13"),
        StayDates("2027-07-18", "2027-07-20")
    ),
    rules = "Quiet hours 10 PM to 7 AM. No open fires during a fire ban. " +
            "Cancellations must be made 48 hours before arrival."
)

/** Fake [ReservationRepository] that returns [sampleOffer] and saves nothing. */
class TempReservationRepo : ReservationRepository {
    override suspend fun getOffer(): CampsiteOffer = sampleOffer
    override suspend fun reserve(offerId: String, stay: StayDates) {}
    override suspend fun decline(offerId: String) {}
}