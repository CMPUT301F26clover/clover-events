package com.example.camp_serve

import com.example.camp_serve.data.TempEntrantRepo
import com.example.camp_serve.model.Entrant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the in-memory entrant repository used by the entrant
 * profile screen (US 01.07.03, US 01.08.01).
 */
class TempEntrantRepoTest {

    @Test
    fun getEntrant_returnsSampleProfile() = runBlocking {
        val repo = TempEntrantRepo()
        assertEquals("test@example.ca", repo.getEntrant().email)
    }

    @Test
    fun updateEntrant_thenGet_returnsNewInfo() = runBlocking {
        val repo = TempEntrantRepo()
        val updated = Entrant(id = "fake-id-01", name = "New Name", email = "new@example.ca", phone = "")
        repo.updateEntrant(updated)
        assertEquals("New Name", repo.getEntrant().name)
        assertEquals("new@example.ca", repo.getEntrant().email)
    }

    @Test
    fun deleteEntrant_removesOldInfo() = runBlocking {
        val repo = TempEntrantRepo()
        repo.deleteEntrant("fake-id-01")
        assertEquals("", repo.getEntrant().name)
    }
}