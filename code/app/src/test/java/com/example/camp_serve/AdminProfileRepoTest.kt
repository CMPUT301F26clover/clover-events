package com.example.camp_serve

import com.example.camp_serve.data.TempEntrantRepo
import com.example.camp_serve.data.TempOwnerRepo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminProfileRepoTest {

    @Test
    fun getAllEntrants_returnsTheSampleEntrant() = runBlocking {
        val repo = TempEntrantRepo()
        val all = repo.getAllEntrants()
        assertEquals(1, all.size)
        assertEquals("fake-id-01", all.first().id)
    }

    @Test
    fun deleteEntrant_removesIt() = runBlocking {
        val repo = TempEntrantRepo()
        repo.deleteEntrant("fake-id-01")
        assertTrue(repo.getAllEntrants().isEmpty())
    }

    @Test
    fun deleteEntrant_wrongId_keepsIt() = runBlocking {
        val repo = TempEntrantRepo()
        repo.deleteEntrant("someone-else")
        assertEquals(1, repo.getAllEntrants().size)
    }

    @Test
    fun getAllOwners_returnsAllSampleOwners() = runBlocking {
        val repo = TempOwnerRepo()
        assertEquals(2, repo.getAllOwners().size)
    }

    @Test
    fun deleteOwner_removesOnlyThatOwner() = runBlocking {
        val repo = TempOwnerRepo()
        repo.deleteOwner("fake-owner-01")
        val left = repo.getAllOwners()
        assertEquals(1, left.size)
        assertEquals("fake-owner-02", left.first().id)
    }
}