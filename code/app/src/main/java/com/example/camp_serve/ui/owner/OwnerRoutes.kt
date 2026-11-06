/*
 * OwnerRoutes.kt holds the route names for the owner's navigation graph, so the strings are
 * written once. Routes with an id in them use the helper functions.
 * Outstanding issues: none.
 */

package com.example.camp_serve.ui.owner

object OwnerRoutes {
    const val CAMPSITES = "owner_campsites"
    const val REGISTER_CAMPSITE = "owner_register_campsite"
    const val CAMPSITE = "owner_campsite/{id}"
    const val EDIT_CAMPSITE = "owner_edit_campsite/{id}"
    const val LOTTERIES = "owner_lotteries"
    const val CREATE_LOTTERY = "owner_create_lottery/{campsiteId}"
    const val LOTTERY = "owner_lottery/{id}"
    const val EDIT_LOTTERY = "owner_edit_lottery/{id}"
    const val RESERVATIONS = "owner_reservations"
    const val CHAT = "owner_chat/{id}"
    const val ALERTS = "owner_alerts"

    /** Used as the campsite id when a lottery is created from the Lotteries tab. */
    const val NO_CAMPSITE = "none"

    fun campsite(id: String) = "owner_campsite/$id"
    fun editCampsite(id: String) = "owner_edit_campsite/$id"
    fun createLottery(campsiteId: String = NO_CAMPSITE) = "owner_create_lottery/$campsiteId"
    fun lottery(id: String) = "owner_lottery/$id"
    fun editLottery(id: String) = "owner_edit_lottery/$id"
    fun chat(id: String) = "owner_chat/$id"

    /** Which bottom bar tab a screen belongs to, so the right tab stays highlighted. */
    fun tabFor(route: String?): String = when {
        route == null -> CAMPSITES
        route.contains("lotter") -> LOTTERIES
        route.startsWith("owner_reservations") || route.startsWith("owner_chat") -> RESERVATIONS
        route.startsWith("owner_alerts") -> ALERTS
        else -> CAMPSITES
    }
}
