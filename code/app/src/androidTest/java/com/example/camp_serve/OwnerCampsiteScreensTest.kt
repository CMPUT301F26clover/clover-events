package com.example.camp_serve

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.camp_serve.data.TempCampsiteRepo
import com.example.camp_serve.ui.owner.campsites.EditCampsiteScreen
import com.example.camp_serve.ui.owner.campsites.MyCampsitesScreen
import com.example.camp_serve.ui.owner.campsites.OwnerCampsiteDetailScreen
import com.example.camp_serve.ui.owner.campsites.RegisterCampsiteScreen
import org.junit.Rule
import org.junit.Test

/*
 * OwnerCampsiteScreensTest.kt
 * Intent tests for the campsite owner screens
 * (US 02.03.01 list, US 02.01.01 register, US 02.01.04 details, US 02.01.03 edit).
 * Uses TempCampsiteRepo so the tests don't need Firestore.
 */
class OwnerCampsiteScreensTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // US 02.03.01: owner sees their campsites
    @Test
    fun myCampsites_showsOwnersCampsites() {
        composeTestRule.setContent {
            MyCampsitesScreen(repository = TempCampsiteRepo(), ownerId = "fake-owner-01")
        }
        composeTestRule.onNodeWithText("Grande Cache Cookhouse Site").assertIsDisplayed()
        composeTestRule.onNodeWithText("Maligne Ridge").assertIsDisplayed()
    }

    // US 02.03.01: owner with no campsites sees a message
    @Test
    fun myCampsites_noCampsites_showsMessage() {
        composeTestRule.setContent {
            MyCampsitesScreen(repository = TempCampsiteRepo(), ownerId = "new-owner")
        }
        composeTestRule.onNodeWithText("You haven't registered any campsites yet.").assertIsDisplayed()
    }

    // US 02.01.01: saving with an empty name shows an error
    @Test
    fun register_emptyName_showsError() {
        composeTestRule.setContent {
            RegisterCampsiteScreen(repository = TempCampsiteRepo(), ownerId = "fake-owner-01")
        }
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.onNodeWithText("Name is required").assertIsDisplayed()
    }

    // US 02.01.04: details screen shows the campsite's info
    @Test
    fun details_showsCampsiteInfo() {
        composeTestRule.setContent {
            OwnerCampsiteDetailScreen(repository = TempCampsiteRepo(), campsiteId = "camp-01")
        }
        composeTestRule.onNodeWithText("Grande Cache Cookhouse Site").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hinton").assertIsDisplayed()
        composeTestRule.onNodeWithText("Edit").assertIsDisplayed()
    }

    // US 02.01.03: edit form is filled in with the current details
    @Test
    fun edit_formIsFilledIn() {
        composeTestRule.setContent {
            EditCampsiteScreen(repository = TempCampsiteRepo(), campsiteId = "camp-01")
        }
        composeTestRule.onNodeWithText("Grande Cache Cookhouse Site").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hinton").assertIsDisplayed()
    }
}