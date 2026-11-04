package com.example.camp_serve.data
import com.example.camp_serve.model.Campsite

/*
 * Temporary campsite repo with sample data so we can build the owner screens before Firestore is integrated.
 * Data resets when the app closes.
 * TODO: REPLACE WITH FIRESTORE VERSION LATER
 */

/**
 * Stand-in for Firestore that keeps campsites in a list in memory.
 */
class TempCampsiteRepo : CampsiteRepository {
    private val campsites = mutableListOf(
        Campsite(
            id = "camp-01",
            ownerId = "fake-owner-01",
            name = "Grande Cache Cookhouse Site",
            location = "Hinton",
            description = "Outdoor cookhouse and tennis court",
            maxOccupancy = 8,
            price = 65.0,
            rules = "Quiet hours after 10 pm"
        ),
        Campsite(
            id = "camp-02",
            ownerId = "fake-owner-01",
            name = "Maligne Ridge",
            location = "Jasper National Park",
            description = "Fire pit and lake view",
            maxOccupancy = 6,
            price = 50.0
        )
    )

    override suspend fun getAllCampsites(): List<Campsite> {
        return campsites.toList()
    }

    override suspend fun getCampsitesForOwner(ownerId: String): List<Campsite> {
        val result = mutableListOf<Campsite>()
        for (campsite in campsites) {
            if (campsite.ownerId == ownerId) {
                result.add(campsite)
            }
        }
        return result
    }

    override suspend fun getCampsite(id: String): Campsite? {
        for (campsite in campsites) {
            if (campsite.id == id) {
                return campsite
            }
        }
        return null // no campsite with that id
    }

    override suspend fun addCampsite(campsite: Campsite) {
        var newCampsite = campsite
        // if it has no id yet, make a simple one like "camp-03"
        if (campsite.id == "") {
            val newId = "camp-0" + (campsites.size + 1)
            newCampsite = campsite.copy(id = newId)
        }
        campsites.add(newCampsite)
    }

    override suspend fun updateCampsite(campsite: Campsite) {
        for (i in campsites.indices) {
            if (campsites[i].id == campsite.id) {
                campsites[i] = campsite // replace the old one
                return
            }
        }
    }
}