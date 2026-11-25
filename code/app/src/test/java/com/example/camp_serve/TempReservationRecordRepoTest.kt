package com.example.camp_serve

import com.example.camp_serve.data.TempReservationRecordRepo
import com.example.camp_serve.model.Reservation
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test

/*
 * TempReservationRecordRepoTest.kt
 * Unit tests for TempReservationRecordRepo (US 01.06.01, 02.07.01, 01.07.01, 02.08.01).
 * Uses the demo data inside the repo, so no Firestore is needed.
 */
class TempReservationRecordRepoTest {

    @Test
    fun getReservationsForEntrant_returnsOnlyTheirs() = runBlocking {
        val repo = TempReservationRecordRepo()
        val result = repo.getReservationsForEntrant("fake-id-01")
        Assert.assertEquals(2, result.size)
        for (reservation in result) {
            Assert.assertEquals("fake-id-01", reservation.entrantId)
        }
    }

    @Test
    fun getReservationsForOwner_returnsAllForTheirCampsites() = runBlocking {
        val repo = TempReservationRecordRepo()
        val result = repo.getReservationsForOwner("fake-owner-01")
        Assert.assertEquals(4, result.size)
    }

    @Test
    fun getReservationsForOwner_otherOwner_returnsEmpty() = runBlocking {
        val repo = TempReservationRecordRepo()
        val result = repo.getReservationsForOwner("someone-else")
        Assert.assertTrue(result.isEmpty())
    }

    @Test
    fun getReservationsForCampsite_returnsOnlyThatCampsite() = runBlocking {
        val repo = TempReservationRecordRepo()
        val result = repo.getReservationsForCampsite("camp-01")
        Assert.assertEquals(2, result.size)
        for (reservation in result) {
            Assert.assertEquals("camp-01", reservation.campsiteId)
        }
    }

    @Test
    fun getReservation_missingId_returnsNull() = runBlocking {
        val repo = TempReservationRecordRepo()
        Assert.assertNull(repo.getReservation("not-real"))
    }

    @Test
    fun addReservation_noId_givesItOne() = runBlocking {
        val repo = TempReservationRecordRepo()
        repo.addReservation(Reservation(entrantId = "new-entrant", campsiteId = "camp-01"))
        val result = repo.getReservationsForEntrant("new-entrant")
        Assert.assertEquals(1, result.size)
        Assert.assertTrue(result[0].id != "")
    }

    @Test
    fun updateStatus_changesOnlyThatReservation() = runBlocking {
        val repo = TempReservationRecordRepo()
        repo.updateStatus("res-01", "cancelled")
        Assert.assertEquals("cancelled", repo.getReservation("res-01")!!.status)
        // a different one should stay the same
        Assert.assertEquals("active", repo.getReservation("res-02")!!.status)
    }
}