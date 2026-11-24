package com.example.camp_serve

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.camp_serve.ui.entrant.EntrantApp
import org.junit.Rule
import org.junit.Test

/** Navigation tests for [EntrantApp]. */
class EntrantAppTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun entrantApp_startsOnCampsitesScreen() {
        composeTestRule.setContent { EntrantApp(lotteryRepository = TestLotteryRepository()) }

        composeTestRule.onNodeWithText("Grande Cache Cookhouse Site").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Profile").assertIsDisplayed()
    }

    @Test
    fun bottomNavigation_switchesBetweenCampsitesAndProfile() {
        composeTestRule.setContent { EntrantApp(lotteryRepository = TestLotteryRepository()) }

        composeTestRule.onNodeWithContentDescription("Profile").performClick()
        composeTestRule.onNodeWithText("Cole Smith Sample").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Campsites").performClick()
        composeTestRule.onNodeWithText("Search Campsites").assertIsDisplayed()
    }

    @Test
    fun campsiteClick_navigatesToDetailScreen() {
        composeTestRule.setContent { EntrantApp(lotteryRepository = TestLotteryRepository()) }

        composeTestRule.onNodeWithText("Grande Cache Cookhouse Site").performClick()
        composeTestRule.onNodeWithText("Description & Amenities").assertIsDisplayed()
        composeTestRule.onNodeWithText("No active lottery").assertIsDisplayed()
    }

    @Test
    fun backButtonClick_navigatesFromDetailBackToCampsitesScreen() {
        composeTestRule.setContent { EntrantApp(lotteryRepository = TestLotteryRepository()) }

        composeTestRule.onNodeWithText("Grande Cache Cookhouse Site").performClick()
        composeTestRule.onNodeWithText("< Back").performClick()
        composeTestRule.onNodeWithText("Search Campsites").assertIsDisplayed()
    }
}
