package com.example.camp_serve

import com.example.camp_serve.data.TempCampsiteRepo
import com.example.camp_serve.model.Campsite
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/*
 * TempCampsiteRepoTest.kt
 * Unit tests for the in-memory campsite repo used by the owner screens
 * (US 02.03.01 list, US 02.01.01 register, US 02.01.03 edit).
 */
class TempCampsiteRepoTest {

    @Test
    fun ownerSeesTheirTwoSampleCampsites() = runBlocking {
        val repo = TempCampsiteRepo()
        val result = repo.getCampsitesForOwner("fake-owner-01")
        assertEquals(2, result.size)
    }

    @Test
    fun unknownOwnerHasNoCampsites() = runBlocking {
        val repo = TempCampsiteRepo()
        val result = repo.getCampsitesForOwner("nobody")
        assertTrue(result.isEmpty())
    }

    @Test
    fun getCampsiteFindsTheRightOne() = runBlocking {
        val repo = TempCampsiteRepo()
        val campsite = repo.getCampsite("camp-02")
        assertNotNull(campsite)
        assertEquals("Maligne Ridge", campsite!!.name)
    }

    @Test
    fun getCampsiteWithWrongIdGivesNull() = runBlocking {
        val repo = TempCampsiteRepo()
        assertNull(repo.getCampsite("does-not-exist"))
    }

    @Test
    fun addedCampsiteShowsUpForTheOwner() = runBlocking {
        val repo = TempCampsiteRepo()
        repo.addCampsite(Campsite(ownerId = "owner-2", name = "Pine Lake"))
        val result = repo.getCampsitesForOwner("owner-2")
        assertEquals(1, result.size)
        assertEquals("Pine Lake", result[0].name)
    }

    @Test
    fun addedCampsiteGetsAnId() = runBlocking {
        val repo = TempCampsiteRepo()
        repo.addCampsite(Campsite(ownerId = "owner-2", name = "Pine Lake"))
        val added = repo.getCampsitesForOwner("owner-2")[0]
        assertTrue(added.id != "")
    }

    @Test
    fun updateChangesThePrice() = runBlocking {
        val repo = TempCampsiteRepo()
        val updated = Campsite(id = "camp-01", ownerId = "fake-owner-01", name = "Grande Cache Cookhouse Site", price = 80.0)
        repo.updateCampsite(updated)
        val saved = repo.getCampsite("camp-01")
        assertEquals(80.0, saved!!.price, 0.001)
    }

    @Test
    fun getAllCampsitesIncludesNewOnes() = runBlocking {
        val repo = TempCampsiteRepo()
        repo.addCampsite(Campsite(ownerId = "owner-2", name = "Pine Lake"))
        assertEquals(3, repo.getAllCampsites().size)
    }
}