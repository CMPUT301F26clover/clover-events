package com.example.camp_serve

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.example.camp_serve.data.TempCampsiteRepo
import com.example.camp_serve.data.TempReservationRecordRepo
import com.example.camp_serve.ui.owner.campsites.OwnerCampsiteDetailScreen
import org.junit.Rule
import org.junit.Test

/*
 * OwnerBookedDatesTest.kt
 * Intent tests for the booked dates section on the owner's campsite
 * details screen (US 02.06.01). Uses fake repos so no Firestore is needed.
 */
class OwnerBookedDatesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // US 02.06.01: only active bookings are listed, cancelled ones are not
    @Test
    fun bookedDates_showsOnlyActiveBookings() {
        composeTestRule.setContent {
            OwnerCampsiteDetailScreen(
                repository = TempCampsiteRepo(),
                campsiteId = "camp-01",
                reservationRepository = TempReservationRecordRepo(),
                today = "2026-11-25"
            )
        }
        composeTestRule.onNodeWithText("Booked dates").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("2027-05-01 to 2027-05-02 (Alex Kim)").performScrollTo().assertIsDisplayed()
        // Jordan Lee's booking was cancelled, so it shouldn't be listed
        composeTestRule.onNodeWithText("2027-05-08 to 2027-05-09 (Jordan Lee)").assertDoesNotExist()
    }

    // US 02.06.01: a campsite with no upcoming bookings shows a message
    @Test
    fun bookedDates_noUpcoming_showsMessage() {
        composeTestRule.setContent {
            OwnerCampsiteDetailScreen(
                repository = TempCampsiteRepo(),
                campsiteId = "camp-02",
                reservationRepository = TempReservationRecordRepo(),
                today = "2026-11-25"
            )
        }
        composeTestRule.onNodeWithText("No upcoming bookings.").performScrollTo().assertIsDisplayed()
    }

    // without a reservation repo, the section is hidden (so older screens/tests still work)
    @Test
    fun bookedDates_noRepo_sectionHidden() {
        composeTestRule.setContent {
            OwnerCampsiteDetailScreen(
                repository = TempCampsiteRepo(),
                campsiteId = "camp-01"
            )
        }
        composeTestRule.onNodeWithText("Grande Cache Cookhouse Site").assertIsDisplayed()
        composeTestRule.onNodeWithText("Booked dates").assertDoesNotExist()
    }
}