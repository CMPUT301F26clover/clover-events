package com.example.camp_serve

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.rememberNavController
import com.example.camp_serve.ui.admin.campsites.AdminCampsitesViewModel
import com.example.camp_serve.ui.admin.campsites.CampsitesScreen
import org.junit.Rule
import org.junit.Test

class AdminCampsitesScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun launchScreen() {
        composeRule.setContent {
            CampsitesScreen(navController = rememberNavController())
        }
    }

    @Test
    fun showsSampleCampsites() {
        launchScreen()
        composeRule.onNodeWithText("Maligne Ridge").assertIsDisplayed()
    }

    @Test
    fun search_filtersTheList() {
        launchScreen()
        composeRule.onNodeWithText("Search Campsites").performTextInput("Maligne")
        composeRule.onNodeWithText("Maligne Ridge").assertIsDisplayed()
        composeRule.onAllNodesWithText("Grande Cache Cookhouse Site").assertCountEquals(0)
    }

    @Test
    fun remove_showsWarningDialog() {
        launchScreen()
        composeRule.onAllNodesWithText("Remove")[0].performClick()
        composeRule.onNodeWithText("Remove this campsite?").assertIsDisplayed()
    }

    @Test
    fun confirmRemove_removesTheCampsite() {
        launchScreen()
        composeRule.onAllNodesWithText("Remove")[0].performClick()
        composeRule.onAllNodesWithText("Remove").onLast().performClick()
        composeRule.onAllNodesWithText("Grande Cache Cookhouse Site").assertCountEquals(0)
    }
}