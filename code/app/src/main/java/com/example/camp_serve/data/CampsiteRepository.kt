package com.example.camp_serve.data
import com.example.camp_serve.model.Campsite

/*
 * CampsiteRepository.kt lists what we can do with campsites (load, add, update).
 * Screens only use this, so we can swap the fake repo for Firestore later.
 */

/**
 * Where campsite data comes from and gets saved to.
 */
interface CampsiteRepository {

    /**
     * Gets all campsites.
     *
     * @return list of all campsites
     */
    suspend fun getAllCampsites(): List<Campsite>

    /**
     * Gets all campsites that belong to one owner.
     *
     * @param ownerId id of the owner
     * @return the owner's campsites, empty if they have none
     */
    suspend fun getCampsitesForOwner(ownerId: String): List<Campsite>

    /**
     * Gets one campsite by its id.
     *
     * @param id the campsite's id
     * @return the campsite, or null if it doesn't exist
     */
    suspend fun getCampsite(id: String): Campsite?

    /**
     * Saves a new campsite.
     *
     * @param campsite the campsite to add
     */
    suspend fun addCampsite(campsite: Campsite)

    /**
     * Saves changes to a campsite that already exists.
     *
     * @param campsite the campsite with its new details
     */
    suspend fun updateCampsite(campsite: Campsite)
}