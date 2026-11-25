package com.example.camp_serve

import com.example.camp_serve.model.Reservation
import com.example.camp_serve.model.activeReservations
import com.example.camp_serve.model.cancelledReservations
import com.example.camp_serve.model.pastReservations
import org.junit.Assert
import org.junit.Test

/*
 * ReservationFiltersTest.kt
 * Unit tests for the active / past / cancelled helpers (US 01.06.01, 01.06.02, 02.07.03).
 * Uses a fixed "today" so the tests always give the same result.
 */
class ReservationFiltersTest {

    private val today = "2026-11-25"

    private val list = listOf(
        Reservation(id = "future", endDate = "2027-01-10", status = "active"),
        Reservation(id = "endsToday", endDate = "2026-11-25", status = "active"),
        Reservation(id = "over", endDate = "2026-08-12", status = "active"),
        Reservation(id = "cancelled", endDate = "2027-01-10", status = "cancelled"),
        Reservation(id = "rejected", endDate = "2027-02-10", status = "rejected")
    )

    @Test
    fun active_keepsUpcomingAndToday() {
        val result = activeReservations(list, today)
        Assert.assertEquals(2, result.size)
        Assert.assertEquals("future", result[0].id)
        Assert.assertEquals("endsToday", result[1].id)
    }

    @Test
    fun past_keepsOnlyFinished() {
        val result = pastReservations(list, today)
        Assert.assertEquals(1, result.size)
        Assert.assertEquals("over", result[0].id)
    }

    @Test
    fun cancelled_keepsCancelledAndRejected() {
        val result = cancelledReservations(list)
        Assert.assertEquals(2, result.size)
        Assert.assertEquals("cancelled", result[0].id)
        Assert.assertEquals("rejected", result[1].id)
    }

    @Test
    fun emptyList_givesEmptyResults() {
        Assert.assertTrue(activeReservations(emptyList(), today).isEmpty())
        Assert.assertTrue(pastReservations(emptyList(), today).isEmpty())
        Assert.assertTrue(cancelledReservations(emptyList()).isEmpty())
    }
}