package com.example.camp_serve

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.camp_serve.model.UserRole
import com.example.camp_serve.ui.PromptScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * UI and Navigation tests for [PromptScreen].
 */
class PromptScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun promptScreen_displaysOnlyAvailableRoles() {
        composeTestRule.setContent {
            PromptScreen(
                availableRoles = setOf(UserRole.ENTRANT, UserRole.CAMPSITEOWNER),
                onRoleSelected = {}
            )
        }

        composeTestRule.onNodeWithText("Choose Your Role").assertIsDisplayed()
        composeTestRule.onNodeWithText("Continue as Entrant").assertIsDisplayed()
        composeTestRule.onNodeWithText("Continue as Campsite Owner").assertIsDisplayed()
        composeTestRule.onNodeWithText("Continue as Admin").assertIsNotDisplayed()
    }

    @Test
    fun promptScreen_clickEntrant_triggersOnRoleSelected() {
        var selectedRole: UserRole? = null

        composeTestRule.setContent {
            PromptScreen(
                availableRoles = setOf(UserRole.ENTRANT, UserRole.ADMIN),
                onRoleSelected = { selectedRole = it }
            )
        }

        composeTestRule.onNodeWithText("Continue as Entrant").performClick()
        assertEquals(UserRole.ENTRANT, selectedRole)
    }

    @Test
    fun promptScreen_clickCampsiteOwner_triggersOnRoleSelected() {
        var selectedRole: UserRole? = null

        composeTestRule.setContent {
            PromptScreen(
                availableRoles = setOf(UserRole.ENTRANT, UserRole.CAMPSITEOWNER),
                onRoleSelected = { selectedRole = it }
            )
        }

        composeTestRule.onNodeWithText("Continue as Campsite Owner").performClick()
        assertEquals(UserRole.CAMPSITEOWNER, selectedRole)
    }

    @Test
    fun promptScreen_clickAdmin_triggersOnRoleSelected() {
        var selectedRole: UserRole? = null

        composeTestRule.setContent {
            PromptScreen(
                availableRoles = setOf(UserRole.ENTRANT, UserRole.ADMIN),
                onRoleSelected = { selectedRole = it }
            )
        }

        composeTestRule.onNodeWithText("Continue as Admin").performClick()
        assertEquals(UserRole.ADMIN, selectedRole)
    }
}
