package com.example.camp_serve.model

/**
 * What a lottery winner is offered: a campsite, the dates they can pick from
 * and the rules they must accept (US 01.04.01, 01.04.02, 01.04.03).
 *
 * @property id unique id of the offer
 * @property campsite name of the campsite
 * @property availableDates dates the entrant can choose from
 * @property rules rules and policies the entrant must accept
 */
data class CampsiteOffer(
    val id: String = "",
    val campsite: String = "",
    val availableDates: List<String> = emptyList(),
    val rules: String = ""
)