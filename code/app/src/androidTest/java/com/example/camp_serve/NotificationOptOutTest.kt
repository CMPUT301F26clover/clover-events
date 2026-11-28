package com.example.camp_serve

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.camp_serve.data.TempEntrantRepo
import com.example.camp_serve.ui.entrant.profile.ProfileScreen
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/*
 * NotificationOptOutTest.kt
 * Intent tests for turning notifications off and on from the profile.
 * Uses TempEntrantRepo so the tests don't need Firestore.
 */
class NotificationOptOutTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    // turning the switch off saves the choice and confirms it
    @Test
    fun turningNotificationsOff_savesAndConfirms() {
        val repository = TempEntrantRepo()
        composeTestRule.setContent { ProfileScreen(repository = repository) }

        composeTestRule.onNode(isToggleable()).performScrollTo().performClick()

        composeTestRule.onNodeWithText("Notifications are off", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        assertFalse(runBlocking { repository.getEntrant().notificationEnabled })
    }

    // turning it back on saves that too
    @Test
    fun turningNotificationsBackOn_savesAndConfirms() {
        val repository = TempEntrantRepo()
        composeTestRule.setContent { ProfileScreen(repository = repository) }

        composeTestRule.onNode(isToggleable()).performScrollTo().performClick()
        composeTestRule.onNodeWithText("Notifications are off", substring = true).performScrollTo()
        composeTestRule.onNode(isToggleable()).performScrollTo().performClick()

        composeTestRule.onNodeWithText("Notifications are on", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        assertTrue(runBlocking { repository.getEntrant().notificationEnabled })
    }
}
