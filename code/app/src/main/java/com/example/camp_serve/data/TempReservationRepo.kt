package com.example.camp_serve.data

import com.example.camp_serve.model.CampsiteOffer

val sampleOffer = CampsiteOffer(
    id = "1",
    campsite = "Lakeview Campsite",
    availableDates = listOf("Jul 4 - Jul 6", "Jul 11 - Jul 13", "Jul 18 - Jul 20"),
    rules = "Quiet hours 10 PM to 7 AM. No open fires during a fire ban. " +
            "Cancellations must be made 48 hours before arrival."
)

class TempReservationRepo : ReservationRepository {
    override suspend fun getOffer(): CampsiteOffer = sampleOffer
    override suspend fun reserve(offerId: String, date: String) {}
    override suspend fun decline(offerId: String) {}
}