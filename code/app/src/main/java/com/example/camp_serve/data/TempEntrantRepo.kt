package com.example.camp_serve.data

import com.example.camp_serve.model.Entrant

/**
 * Sample stand in for Firestore so entrant screens can
 * be built. Data is lost if app closes
 */

class TempEntrantRepo : EntrantRepository {
    private var entrant : Entrant? = Entrant (
        id = "fake-id-01",
        name = "Cole Smith Sample",
        email = "test@example.ca",
        phone = "780-123-4567"
    )

    override suspend fun getEntrant(): Entrant {
        return entrant ?: Entrant(id = "fake-id-01")
    }

    override suspend fun updateEntrant(entrant: Entrant) {
        this.entrant = entrant
    }

    override suspend fun deleteEntrant(id: String) {
        if (entrant?.id == id) entrant = null
    }

    override suspend fun getAllEntrants(): List<Entrant> {
        return listOfNotNull(entrant)
    }
}