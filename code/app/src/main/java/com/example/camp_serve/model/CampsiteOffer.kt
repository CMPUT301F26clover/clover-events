package com.example.camp_serve.model

/*
 * CampsiteOffer.kt
 * What a lottery winner is offered to reserve: a campsite, the stays they can
 * pick from and the rules they must accept (US 01.04.01, 01.04.02, 01.04.03).
 * Outstanding issues: the offer is still sample data; it should be built from
 * the winner's lottery result.
 */

/**
 * A stay, from first night to last night.
 *
 * @property startDate first night, as "YYYY-MM-DD" (the format Reservation uses)
 * @property endDate last night, as "YYYY-MM-DD"
 */
data class StayDates(
    val startDate: String = "",
    val endDate: String = ""
) {
    /** Text shown to the entrant, e.g. "2027-07-04 to 2027-07-06". */
    val label: String get() = "$startDate to $endDate"
}

/**
 * What a lottery winner is offered.
 *
 * @property id unique id of the offer
 * @property campsiteId id of the campsite being offered
 * @property campsite name of the campsite
 * @property ownerId id of the campsite's owner
 * @property availableDates stays the entrant can choose from
 * @property rules rules and policies the entrant must accept
 */
data class CampsiteOffer(
    val id: String = "",
    val campsiteId: String = "",
    val campsite: String = "",
    val ownerId: String = "",
    val availableDates: List<StayDates> = emptyList(),
    val rules: String = ""
)