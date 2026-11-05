package com.example.camp_serve

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.camp_serve.data.TempEntrantRepo
import com.example.camp_serve.ui.entrant.profile.ProfileScreen
import org.junit.Rule
import org.junit.Test

/**
 * Intent tests for the entrant profile screen
 * (US 01.07.03 edit profile, US 01.08.01 delete profile).
 */
class ProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun openProfile() {
        composeTestRule.setContent {
            ProfileScreen(repository = TempEntrantRepo())
        }
    }



    @Test
    fun saveChanges_showsConfirmationPrompt() {
        openProfile()
        composeTestRule.onNodeWithText("Edit").performClick()
        composeTestRule.onNodeWithText("Save changes").performClick()
        composeTestRule.onNodeWithText("Save changes?").assertIsDisplayed()
    }




    @Test
    fun deleteProfile_showsPermanentWarning() {
        openProfile()
        composeTestRule.onNodeWithText("Delete Profile").performClick()
        composeTestRule.onNodeWithText("Delete profile?").assertIsDisplayed()
    }

    @Test
    fun cancelDelete_keepsProfile() {
        openProfile()
        composeTestRule.onNodeWithText("Delete Profile").performClick()
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.onNodeWithText("Edit").assertIsDisplayed()
    }


}