package com.example.camp_serve.data

import com.example.camp_serve.model.Owner

/**
 * Where owner data comes from and goes to. Screens depend on this interface only.
 */
interface OwnerRepository {

    /**
     * Gets the owner for this device.
     *
     * @return the owner's profile
     */
    suspend fun getOwner(): Owner

    /**
     * Gets every owner (used by admin).
     *
     * @return list of all owners
     */
    suspend fun getAllOwners(): List<Owner>

    /**
     * Saves the changed profile fields.
     *
     * @param owner the owner with its new details
     */
    suspend fun updateOwner(owner: Owner)

    /**
     * Deletes an owner's profile.
     *
     * @param id the id of the owner to delete
     */
    suspend fun deleteOwner(id: String)
}