package com.example.camp_serve

import com.example.camp_serve.ui.entrant.campsites.matchesStay
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CampsiteStayFilterTest {
    // Every night must be available; departure day is excluded.
    @Test
    fun matchesEveryNightButNotDepartureDay() {
        val dates = listOf("2027-06-01", "2027-06-02")
        assertTrue(matchesStay(dates, "2027-06-01", "2027-06-03"))
        assertFalse(matchesStay(listOf("2027-06-01", "2027-06-03"), "2027-06-01", "2027-06-03"))
    }

    // Incomplete ranges are ignored, while invalid ranges do not match.
    @Test
    fun handlesIncompleteAndInvalidRanges() {
        assertTrue(matchesStay(emptyList(), "2027-06-01", ""))
        assertFalse(matchesStay(listOf("2027-06-01"), "2027-06-01", "2027-06-01"))
        assertFalse(matchesStay(listOf("2027-06-01"), "2027-06-02", "2027-06-01"))
    }

    // Availability matching crosses month and leap-day boundaries.
    @Test
    fun handlesMonthBoundary() {
        assertTrue(matchesStay(listOf("2028-02-29", "2028-03-01"), "2028-02-29", "2028-03-02"))
    }
}
