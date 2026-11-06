package com.example.camp_serve.data

import com.example.camp_serve.model.Owner

/**
 * Sample stand in for Firestore so owner screens can
 * be built. Data is lost if app closes.
 */
class TempOwnerRepo : OwnerRepository {
    private val owners = mutableListOf(
        Owner(id = "fake-owner-01", name = "Sam Sample", email = "sam@example.ca", phone = "780-111-2222"),
        Owner(id = "fake-owner-02", name = "Alex Sample", email = "alex@example.ca", phone = "780-333-4444")
    )

    override suspend fun getOwner(): Owner {
        return owners.first()
    }

    override suspend fun getAllOwners(): List<Owner> {
        return owners.toList()
    }

    override suspend fun updateOwner(owner: Owner) {
        for (i in owners.indices) {
            if (owners[i].id == owner.id) {
                owners[i] = owner
                return
            }
        }
    }

    override suspend fun deleteOwner(id: String) {
        owners.removeAll { it.id == id }
    }
}