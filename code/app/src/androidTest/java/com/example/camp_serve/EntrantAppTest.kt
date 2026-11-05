package com.example.camp_serve

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.camp_serve.ui.entrant.EntrantApp
import org.junit.Rule
import org.junit.Test

/**
 * Navigation tests for [EntrantApp].
 */
class EntrantAppTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun entrantApp_startsOnCampsitesScreen() {
        composeTestRule.setContent {
            EntrantApp()
        }

        composeTestRule.onNodeWithText("Campsites").assertIsDisplayed()
        composeTestRule.onNodeWithText("Grande Cache Cookhouse Site").assertIsDisplayed()
    }

    @Test
    fun campsiteClick_navigatesToDetailScreen() {
        composeTestRule.setContent {
            EntrantApp()
        }

        composeTestRule.onNodeWithText("Grande Cache Cookhouse Site").performClick()

        composeTestRule.onNodeWithText("Description & Amenities").assertIsDisplayed()
        composeTestRule.onNodeWithText("Enter Waitlist").assertIsDisplayed()
    }

    @Test
    fun backButtonClick_navigatesFromDetailBackToCampsitesScreen() {
        composeTestRule.setContent {
            EntrantApp()
        }

        composeTestRule.onNodeWithText("Grande Cache Cookhouse Site").performClick()
        composeTestRule.onNodeWithText("< Back").performClick()

        composeTestRule.onNodeWithText("Search Campsites").assertIsDisplayed()
    }
}
