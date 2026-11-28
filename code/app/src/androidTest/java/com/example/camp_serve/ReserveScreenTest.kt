package com.example.camp_serve

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.camp_serve.data.sampleOffer
import com.example.camp_serve.model.StayDates
import com.example.camp_serve.ui.entrant.reserve.ReserveScreen
import com.example.camp_serve.ui.theme.CampserveTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/*
 * ReserveScreenTest.kt
 * Intent tests for the Reserve screen (US 01.04.01, 01.04.02, 01.04.03).
 * Uses the sample offer so the tests don't need Firestore.
 */
class ReserveScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    // "Reserve" is also the screen title, so match the button by its click action
    private fun reserveButton() =
        composeTestRule.onNode(hasText("Reserve") and hasClickAction())

    // US 01.04.01: the offer's campsite and stays are shown
    @Test
    fun showsCampsiteAndStays() {
        composeTestRule.setContent {
            CampserveTheme { ReserveScreen(sampleOffer, onReserve = {}, onDecline = {}) }
        }
        composeTestRule.onNodeWithText("Lakeview Campsite").assertExists()
        composeTestRule.onNodeWithText("2027-07-04 to 2027-07-06").assertExists()
    }

    // US 01.04.02: Reserve needs a stay and accepted rules
    @Test
    fun reserveStaysDisabledUntilStayChosenAndRulesAccepted() {
        composeTestRule.setContent {
            CampserveTheme { ReserveScreen(sampleOffer, onReserve = {}, onDecline = {}) }
        }
        reserveButton().performScrollTo().assertIsNotEnabled()

        composeTestRule.onAllNodes(isSelectable())[0].performClick()
        reserveButton().assertIsNotEnabled()

        composeTestRule.onNode(isToggleable()).performScrollTo().performClick()
        reserveButton().assertIsEnabled()
    }

    // US 01.04.02: reserving passes back the stay that was chosen
    @Test
    fun reservePassesChosenStay() {
        var reserved: StayDates? = null
        composeTestRule.setContent {
            CampserveTheme { ReserveScreen(sampleOffer, onReserve = { reserved = it }, onDecline = {}) }
        }
        composeTestRule.onAllNodes(isSelectable())[1].performClick()
        composeTestRule.onNode(isToggleable()).performScrollTo().performClick()
        reserveButton().performScrollTo().performClick()

        assertEquals(StayDates("2027-07-11", "2027-07-13"), reserved)
    }

    // US 01.04.03: declining needs nothing chosen
    @Test
    fun declineWorksWithoutChoosingAnything() {
        var declined = false
        composeTestRule.setContent {
            CampserveTheme { ReserveScreen(sampleOffer, onReserve = {}, onDecline = { declined = true }) }
        }
        composeTestRule.onNodeWithText("Decline").performScrollTo().performClick()
        assertTrue(declined)
    }
}