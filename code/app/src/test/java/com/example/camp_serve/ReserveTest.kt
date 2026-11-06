package com.example.camp_serve

import com.example.camp_serve.data.TempAlertRepo
import com.example.camp_serve.data.TempReservationRepo
import com.example.camp_serve.model.AlertType
import com.example.camp_serve.ui.entrant.reserve.canReserve
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReserveTest {

    @Test
    fun reserveDisabled_whenNothingChosen() {
        assertFalse(canReserve(null, false))
    }

    @Test
    fun reserveDisabled_whenDateChosenButRulesNotAccepted() {
        assertFalse(canReserve("Jul 4 - Jul 6", false))
    }

    @Test
    fun reserveDisabled_whenRulesAcceptedButNoDate() {
        assertFalse(canReserve(null, true))
    }

    @Test
    fun reserveEnabled_whenDateChosenAndRulesAccepted() {
        assertTrue(canReserve("Jul 4 - Jul 6", true))
    }

    @Test
    fun tempReservationRepo_returnsOfferWithDatesAndRules() {
        runBlocking {
            val offer = TempReservationRepo().getOffer()
            assertEquals("Lakeview Campsite", offer.campsite)
            assertTrue(offer.availableDates.isNotEmpty())
            assertTrue(offer.rules.isNotBlank())
        }
    }

    @Test
    fun tempAlertRepo_returnsAlertsIncludingAWin() {
        runBlocking {
            val alerts = TempAlertRepo().getAlerts()
            assertEquals(4, alerts.size)
            assertTrue(alerts.any { it.type == AlertType.WON })
        }
    }
}