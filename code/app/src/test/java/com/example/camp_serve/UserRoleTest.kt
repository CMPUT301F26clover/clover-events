package com.example.camp_serve

import com.example.camp_serve.model.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test

/**
 * Unit and integration tests for [com.example.camp_serve.model.UserRole].
 */
class UserRoleTest {

    @Test
    fun isFlagTrue_parsesBooleansIntegersAndStrings() {
        Assert.assertTrue(UserRole.isFlagTrue(true))
        Assert.assertTrue(UserRole.isFlagTrue(1))
        Assert.assertTrue(UserRole.isFlagTrue(1L))
        Assert.assertTrue(UserRole.isFlagTrue("1"))
        Assert.assertTrue(UserRole.isFlagTrue("true"))
        Assert.assertTrue(UserRole.isFlagTrue("TRUE"))

        Assert.assertFalse(UserRole.isFlagTrue(false))
        Assert.assertFalse(UserRole.isFlagTrue(0))
        Assert.assertFalse(UserRole.isFlagTrue("0"))
        Assert.assertFalse(UserRole.isFlagTrue("false"))
        Assert.assertFalse(UserRole.isFlagTrue(null))
    }

    @Test
    fun fromRoleString_admin_returnsAdmin() {
        Assert.assertEquals(UserRole.ADMIN, UserRole.fromRoleString("admin"))
        Assert.assertEquals(UserRole.ADMIN, UserRole.fromRoleString("ADMIN"))
        Assert.assertEquals(UserRole.ADMIN, UserRole.fromRoleString("  admin  "))
    }

    @Test
    fun fromRoleString_campsiteOwner_returnsCampsiteOwner() {
        Assert.assertEquals(UserRole.CAMPSITEOWNER, UserRole.fromRoleString("campsiteowner"))
        Assert.assertEquals(UserRole.CAMPSITEOWNER, UserRole.fromRoleString("campsite_owner"))
        Assert.assertEquals(UserRole.CAMPSITEOWNER, UserRole.fromRoleString("owner"))
    }

    @Test
    fun fromRoleString_entrant_returnsEntrant() {
        Assert.assertEquals(UserRole.ENTRANT, UserRole.fromRoleString("entrant"))
        Assert.assertEquals(UserRole.ENTRANT, UserRole.fromRoleString("ENTRANT"))
    }

    @Test
    fun fromRoleString_unknownOrNull_defaultsToEntrant() {
        Assert.assertEquals(UserRole.ENTRANT, UserRole.fromRoleString(null))
        Assert.assertEquals(UserRole.ENTRANT, UserRole.fromRoleString(""))
        Assert.assertEquals(UserRole.ENTRANT, UserRole.fromRoleString("unknown_role"))
    }

    @Test
    fun determineUserRoles_nonExistentDevice_defaultsToEntrantSet() = runBlocking {
        val result = UserRole.determineUserRoles("non_existent_device_id_99999")
        Assert.assertEquals(setOf(UserRole.ENTRANT), result)
        Assert.assertTrue(result.isNotEmpty())
    }
}