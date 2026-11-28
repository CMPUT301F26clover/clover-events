package com.example.camp_serve

import com.example.camp_serve.data.TempAlertRepo
import com.example.camp_serve.data.TempReservationRepo
import com.example.camp_serve.data.sampleOffer
import com.example.camp_serve.model.AlertType
import com.example.camp_serve.model.StayDates
import com.example.camp_serve.ui.entrant.reserve.buildReservation
import com.example.camp_serve.ui.entrant.reserve.canReserve
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReserveTest {

    private val stay = StayDates("2027-07-04", "2027-07-06")

    @Test
    fun reserveDisabled_whenNothingChosen() {
        assertFalse(canReserve(null, false))
    }

    @Test
    fun reserveDisabled_whenStayChosenButRulesNotAccepted() {
        assertFalse(canReserve(stay, false))
    }

    @Test
    fun reserveDisabled_whenRulesAcceptedButNoStay() {
        assertFalse(canReserve(null, true))
    }

    @Test
    fun reserveEnabled_whenStayChosenAndRulesAccepted() {
        assertTrue(canReserve(stay, true))
    }

    @Test
    fun stayLabel_showsBothDates() {
        assertEquals("2027-07-04 to 2027-07-06", stay.label)
    }

    @Test
    fun buildReservation_copiesOfferAndEntrantDetails() {
        val reservation = buildReservation(sampleOffer, stay, "device-1", "Cole")
        assertEquals(sampleOffer.campsiteId, reservation.campsiteId)
        assertEquals("Lakeview Campsite", reservation.campsiteName)
        assertEquals(sampleOffer.ownerId, reservation.ownerId)
        assertEquals("device-1", reservation.entrantId)
        assertEquals("Cole", reservation.entrantName)
        assertEquals("2027-07-04", reservation.startDate)
        assertEquals("2027-07-06", reservation.endDate)
    }

    @Test
    fun buildReservation_isActiveWithRulesAccepted() {
        val reservation = buildReservation(sampleOffer, stay, "device-1", "Cole")
        assertEquals("active", reservation.status)
        assertTrue(reservation.rulesAccepted)
    }

    @Test
    fun tempReservationRepo_returnsOfferWithStaysAndRules() {
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