package com.example.camp_serve.data
import com.example.camp_serve.model.Entrant

/**
 * Where entrant data comes from and goes to. Screens depend on this interface only
 */

interface EntrantRepository {

    /**
     * Gets the entrant for this device.
     *
     * @return the entrant's profile
     */
    suspend fun getEntrant(): Entrant

    /**
     * Saves the changed profile fields.
     *
     * @param entrant the entrant with its new details
     */
    suspend fun updateEntrant(entrant: Entrant)

    /**
     * Deletes the entrant's profile on this device.
     */
    suspend fun deleteEntrant()
}