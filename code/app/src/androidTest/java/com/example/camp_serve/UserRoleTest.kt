package com.example.camp_serve

import com.example.camp_serve.model.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit and integration tests for [UserRole].
 */
class UserRoleTest {

    @Test
    fun isFlagTrue_parsesBooleansIntegersAndStrings() {
        assertTrue(UserRole.isFlagTrue(true))
        assertTrue(UserRole.isFlagTrue(1))
        assertTrue(UserRole.isFlagTrue(1L))
        assertTrue(UserRole.isFlagTrue("1"))
        assertTrue(UserRole.isFlagTrue("true"))
        assertTrue(UserRole.isFlagTrue("TRUE"))

        assertFalse(UserRole.isFlagTrue(false))
        assertFalse(UserRole.isFlagTrue(0))
        assertFalse(UserRole.isFlagTrue("0"))
        assertFalse(UserRole.isFlagTrue("false"))
        assertFalse(UserRole.isFlagTrue(null))
    }

    @Test
    fun fromRoleString_admin_returnsAdmin() {
        assertEquals(UserRole.ADMIN, UserRole.fromRoleString("admin"))
        assertEquals(UserRole.ADMIN, UserRole.fromRoleString("ADMIN"))
        assertEquals(UserRole.ADMIN, UserRole.fromRoleString("  admin  "))
    }

    @Test
    fun fromRoleString_campsiteOwner_returnsCampsiteOwner() {
        assertEquals(UserRole.CAMPSITEOWNER, UserRole.fromRoleString("campsiteowner"))
        assertEquals(UserRole.CAMPSITEOWNER, UserRole.fromRoleString("campsite_owner"))
        assertEquals(UserRole.CAMPSITEOWNER, UserRole.fromRoleString("owner"))
    }

    @Test
    fun fromRoleString_entrant_returnsEntrant() {
        assertEquals(UserRole.ENTRANT, UserRole.fromRoleString("entrant"))
        assertEquals(UserRole.ENTRANT, UserRole.fromRoleString("ENTRANT"))
    }

    @Test
    fun fromRoleString_unknownOrNull_defaultsToEntrant() {
        assertEquals(UserRole.ENTRANT, UserRole.fromRoleString(null))
        assertEquals(UserRole.ENTRANT, UserRole.fromRoleString(""))
        assertEquals(UserRole.ENTRANT, UserRole.fromRoleString("unknown_role"))
    }

    @Test
    fun determineUserRoles_nonExistentDevice_defaultsToEntrantSet() = runBlocking {
        val result = UserRole.determineUserRoles("non_existent_device_id_99999")
        assertEquals(setOf(UserRole.ENTRANT), result)
        assertTrue(result.isNotEmpty())
    }
}
