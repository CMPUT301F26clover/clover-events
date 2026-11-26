package com.example.camp_serve

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.camp_serve.data.TempEntrantNotificationRepo
import com.example.camp_serve.data.TempEntrantRepo
import com.example.camp_serve.ui.entrant.EntrantApp
import androidx.compose.runtime.Composable
import org.junit.Rule
import org.junit.Test

/** Navigation tests for [EntrantApp]. */
class EntrantAppTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun entrantApp_startsOnCampsitesScreen() {
        composeTestRule.setContent { testEntrantApp() }

        composeTestRule.onNodeWithText("Grande Cache Cookhouse Site").assertIsDisplayed()
        tab("Profile").assertIsDisplayed()
    }

    @Test
    fun bottomNavigation_switchesBetweenCampsitesAndProfile() {
        composeTestRule.setContent { testEntrantApp() }

        tab("Profile").performClick()
        // the profile shows the name twice (heading and "Name:" row), so check the first
        composeTestRule.onAllNodesWithText("Cole Smith Sample").onFirst().assertIsDisplayed()
        tab("Campsites").performClick()
        composeTestRule.onNodeWithText("Search Campsites").assertIsDisplayed()
    }

    @Test
    fun bottomNavigation_opensNotifications() {
        composeTestRule.setContent { testEntrantApp() }

        tab("Notifications").performClick()
        // sample notifications from TempEntrantNotificationRepo (US 01.03.01, 01.03.02)
        composeTestRule.onNodeWithText("Lakeview Campsite").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pine Ridge Campsite").assertIsDisplayed()
    }

    // opening the app from a tapped device notification shows Notifications (#101)
    @Test
    fun openNotifications_opensNotificationsScreen() {
        composeTestRule.setContent {
            EntrantApp(
                lotteryRepository = TestLotteryRepository(),
                entrantRepository = TempEntrantRepo(),
                entrantNotificationRepository = TempEntrantNotificationRepo(),
                openNotifications = true
            )
        }

        composeTestRule.onNodeWithText("Lakeview Campsite").assertIsDisplayed()
    }

    @Test
    fun campsiteClick_navigatesToDetailScreen() {
        composeTestRule.setContent { testEntrantApp() }

        composeTestRule.onNodeWithText("Grande Cache Cookhouse Site").performClick()
        composeTestRule.onNodeWithText("Description & Amenities").assertIsDisplayed()
        composeTestRule.onNodeWithText("No active lottery").assertIsDisplayed()
    }

    @Test
    fun backButtonClick_navigatesFromDetailBackToCampsitesScreen() {
        composeTestRule.setContent { testEntrantApp() }

        composeTestRule.onNodeWithText("Grande Cache Cookhouse Site").performClick()
        composeTestRule.onNodeWithText("< Back").performClick()
        composeTestRule.onNodeWithText("Search Campsites").assertIsDisplayed()
    }

    // A bottom bar tab is a selectable node showing the tab name. A tab with a label
    // hides its icon's content description, so find it by text; isSelectable() keeps
    // it from matching the "Campsites" heading on the campsites screen.
    private fun tab(label: String) =
        composeTestRule.onNode(hasText(label) and isSelectable())

    /** The entrant app with sample data, so tests do not depend on Firestore. */
    @Composable
    private fun testEntrantApp() = EntrantApp(
        lotteryRepository = TestLotteryRepository(),
        entrantRepository = TempEntrantRepo(),
        entrantNotificationRepository = TempEntrantNotificationRepo()
    )
}
