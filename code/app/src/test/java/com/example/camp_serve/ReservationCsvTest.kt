package com.example.camp_serve

import com.example.camp_serve.model.Reservation
import com.example.camp_serve.model.csvFileName
import com.example.camp_serve.model.csvValue
import com.example.camp_serve.model.reservationToCsv
import org.junit.Assert
import org.junit.Test

/*
 * ReservationCsvTest.kt
 * Unit tests for the CSV confirmations (US 01.05.02, US 01.07.02).
 */
class ReservationCsvTest {

    private val active = Reservation(
        id = "res-01",
        campsiteName = "Grande Cache Cookhouse Site",
        entrantName = "Alex Kim",
        startDate = "2027-05-01",
        endDate = "2027-05-02",
        status = "active"
    )

    @Test
    fun csv_active_isReservationConfirmation() {
        val csv = reservationToCsv(active)
        val lines = csv.trim().split("\n")
        Assert.assertEquals(2, lines.size)
        Assert.assertEquals("Type,Confirmation number,Campsite,Check-in,Check-out,Booked by,Status", lines[0])
        Assert.assertEquals(
            "Reservation confirmation,res-01,Grande Cache Cookhouse Site,2027-05-01,2027-05-02,Alex Kim,active",
            lines[1]
        )
    }

    @Test
    fun csv_cancelled_isCancellationConfirmation() {
        val csv = reservationToCsv(active.copy(status = "cancelled"))
        Assert.assertTrue(csv.contains("Cancellation confirmation"))
    }

    @Test
    fun csv_rejected_isCancellationConfirmation() {
        val csv = reservationToCsv(active.copy(status = "rejected"))
        Assert.assertTrue(csv.contains("Cancellation confirmation"))
    }

    @Test
    fun csvValue_withComma_isWrappedInQuotes() {
        Assert.assertEquals("\"Banff, Lake\"", csvValue("Banff, Lake"))
        Assert.assertEquals("Banff", csvValue("Banff"))
    }

    @Test
    fun fileName_matchesType() {
        Assert.assertEquals("reservation_res-01.csv", csvFileName(active))
        Assert.assertEquals("cancellation_res-01.csv", csvFileName(active.copy(status = "cancelled")))
    }
}