package com.example.camp_serve

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.camp_serve.data.TempAlertRepo
import com.example.camp_serve.ui.entrant.EntrantApp
import org.junit.Rule
import org.junit.Test

/*
 * EntrantNotificationsTest.kt
 * Intent tests for the entrant's notifications.
 * Uses Temp repos so the tests don't need Firestore.
 */
class EntrantNotificationsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    // the Notifications button on the campsites screen opens the entrant's alerts
    @Test
    fun notificationsButton_showsWinningNotification() {
        composeTestRule.setContent {
            EntrantApp(
                lotteryRepository = TestLotteryRepository(),
                alertRepository = TempAlertRepo()
            )
        }

        composeTestRule.onNodeWithText("Notifications").performClick()
        composeTestRule.onNodeWithText("You won!").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lakeview Campsite").assertIsDisplayed()
    }
}
