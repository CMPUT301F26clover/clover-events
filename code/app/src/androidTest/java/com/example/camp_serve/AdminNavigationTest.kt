package com.example.camp_serve

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.camp_serve.ui.admin.AdminApp
import com.example.camp_serve.ui.theme.CampserveTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AdminNavigationTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun launchAdmin() {
        composeRule.setContent {
            CampserveTheme {
                AdminApp()
            }
        }
        // wait for the sample campsites to load
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Maligne Ridge")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    // A nav bar tab is a selectable node showing the tab name.
    // isSelectable() keeps it from matching the screen heading.
    private fun tab(label: String) =
        composeRule.onNode(hasText(label) and isSelectable())

    private fun openTab(label: String) {
        tab(label).performClick()
        tab(label).assertIsSelected()
    }

    private fun assertGone(text: String) {
        composeRule.onAllNodesWithText(text).assertCountEquals(0)
    }

    // the dialog's confirm button, not the Remove buttons in the list
    private fun confirmRemoveInDialog() {
        composeRule
            .onNode(hasText("Remove") and hasAnyAncestor(isDialog()))
            .performClick()
    }

    @Test
    fun startsOnCampsitesTab() {
        tab("Campsites").assertIsSelected()
        composeRule.onNodeWithText("Grande Cache Cookhouse Site").assertIsDisplayed()
    }

    @Test
    fun deletedCampsite_staysDeletedAfterVisitingEveryTab() {
        composeRule.onAllNodesWithText("Remove")[0].performClick()
        composeRule.onNodeWithText("Remove this campsite?").assertIsDisplayed()
        confirmRemoveInDialog()

        assertGone("Grande Cache Cookhouse Site")
        composeRule.onNodeWithText("Maligne Ridge").assertIsDisplayed()

        // Profiles: entrants, then owners
        openTab("Profiles")
        composeRule.onNodeWithText("Cole Smith Sample").assertIsDisplayed()
        composeRule.onNodeWithText("Owners").performClick()
        composeRule.onNodeWithText("Sam Sample").assertIsDisplayed()

        // Images and Logs
        openTab("Images")
        openTab("Logs")

        // back on Campsites, the deleted one must still be gone
        openTab("Campsites")
        assertGone("Grande Cache Cookhouse Site")
        composeRule.onNodeWithText("Maligne Ridge").assertIsDisplayed()
    }

    @Test
    fun cancelRemove_keepsTheCampsiteAfterVisitingTabs() {
        composeRule.onAllNodesWithText("Remove")[0].performClick()
        composeRule.onNodeWithText("Cancel").performClick()

        openTab("Profiles")
        openTab("Campsites")
        composeRule.onNodeWithText("Grande Cache Cookhouse Site").assertIsDisplayed()
    }

    @Test
    fun deletedProfile_staysDeletedAfterLeavingAndReturning() {
        openTab("Profiles")
        composeRule.onAllNodesWithText("Remove")[0].performClick()
        composeRule.onNodeWithText("Remove this Profile?").assertIsDisplayed()
        confirmRemoveInDialog()
        assertGone("Cole Smith Sample")

        openTab("Logs")
        openTab("Profiles")
        assertGone("Cole Smith Sample")
    }
}