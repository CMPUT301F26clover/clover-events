package com.example.camp_serve

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.camp_serve.data.TempCampsiteRepo
import com.example.camp_serve.ui.entrant.EntrantApp
import com.example.camp_serve.ui.owner.campsites.OwnerCampsiteDetailScreen
import org.junit.Rule
import org.junit.Test

/*
 * CampsiteQrCodeScreenTest.kt
 * Intent tests for campsite QR codes.
 * Uses TempCampsiteRepo so the tests don't need Firestore.
 */
class CampsiteQrCodeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // the Download QR Code button is on the details screen with the other buttons
    @Test
    fun ownerDetails_showsDownloadQrCodeButton() {
        composeTestRule.setContent {
            OwnerCampsiteDetailScreen(repository = TempCampsiteRepo(), campsiteId = "camp-01")
        }
        composeTestRule.onNodeWithText("Create Lottery").assertIsDisplayed()
        composeTestRule.onNodeWithText("Edit Campsite").assertIsDisplayed()
        composeTestRule.onNodeWithText("Download QR Code").assertIsDisplayed()
    }

    // tapping it shows the QR code with Save and Share
    @Test
    fun downloadQrCode_showsQrCodeWithSaveAndShare() {
        composeTestRule.setContent {
            OwnerCampsiteDetailScreen(repository = TempCampsiteRepo(), campsiteId = "camp-01")
        }
        composeTestRule.onNodeWithText("Download QR Code").performClick()

        composeTestRule.onNodeWithContentDescription("QR code for Grande Cache Cookhouse Site")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Save").assertIsDisplayed()
        composeTestRule.onNodeWithText("Share").assertIsDisplayed()
    }

    // Close hides the QR code again
    @Test
    fun closeButton_hidesQrCode() {
        composeTestRule.setContent {
            OwnerCampsiteDetailScreen(repository = TempCampsiteRepo(), campsiteId = "camp-01")
        }
        composeTestRule.onNodeWithText("Download QR Code").performClick()
        composeTestRule.onNodeWithText("Close").performClick()

        composeTestRule.onNodeWithContentDescription("QR code for Grande Cache Cookhouse Site")
            .assertDoesNotExist()
    }

    // scanning a QR code opens that campsite's details for the entrant
    @Test
    fun scannedCampsite_opensEntrantDetails() {
        composeTestRule.setContent {
            EntrantApp(lotteryRepository = TestLotteryRepository(), startCampsiteId = "camp-02")
        }
        composeTestRule.onNodeWithText("Maligne Ridge").assertIsDisplayed()
        composeTestRule.onNodeWithText("Description & Amenities").assertIsDisplayed()
    }
}
