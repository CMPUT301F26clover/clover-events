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
     * Deletes an entrant's profile.
     *
     * @param id the id of the entrant to delete
     */
    suspend fun deleteEntrant(id: String)

    /**
     * Gets every entrant (used by admin).
     *
     * @return list of all entrants
     */
    suspend fun getAllEntrants(): List<Entrant>

    /**
     * Gets the entrants with these ids, for example to show names on a lottery waitlist.
     * Ids with no stored profile are left out of the result.
     *
     * @param ids entrant ids to look up
     * @return the entrants that were found, in no particular order
     */
    suspend fun getEntrantsByIds(ids: Collection<String>): List<Entrant>
}
