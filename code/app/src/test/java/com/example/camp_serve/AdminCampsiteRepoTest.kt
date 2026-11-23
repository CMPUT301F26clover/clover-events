package com.example.camp_serve

import com.example.camp_serve.data.TempCampsiteRepo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AdminCampsiteRepoTest {

    @Test
    fun getAllCampsites_returnsAllSampleCampsites() = runBlocking {
        val repo = TempCampsiteRepo()
        assertEquals(2, repo.getAllCampsites().size)
    }

    @Test
    fun deleteCampsite_removesThatCampsite() = runBlocking {
        val repo = TempCampsiteRepo()
        repo.deleteCampsite("camp-01")
        assertNull(repo.getCampsite("camp-01"))
        assertEquals(1, repo.getAllCampsites().size)
    }

    @Test
    fun deleteCampsite_leavesOthersAlone() = runBlocking {
        val repo = TempCampsiteRepo()
        repo.deleteCampsite("camp-01")
        assertEquals("camp-02", repo.getAllCampsites().first().id)
    }

    @Test
    fun deleteCampsite_unknownId_changesNothing() = runBlocking {
        val repo = TempCampsiteRepo()
        repo.deleteCampsite("does-not-exist")
        assertEquals(2, repo.getAllCampsites().size)
    }
}