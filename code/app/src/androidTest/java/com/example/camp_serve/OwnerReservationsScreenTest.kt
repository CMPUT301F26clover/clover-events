package com.example.camp_serve

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.camp_serve.data.TempCampsiteRepo
import com.example.camp_serve.data.TempEntrantRepo
import com.example.camp_serve.data.TempReservationRecordRepo
import com.example.camp_serve.ui.owner.OwnerApp
import com.example.camp_serve.ui.owner.reservations.OwnerReservationsScreen
import org.junit.Rule
import org.junit.Test

/*
 * OwnerReservationsScreenTest.kt
 * Intent tests for the owner's Reservations screen: active (US 02.07.01),
 * past (US 02.07.02) and cancelled (US 02.07.03), plus the bottom bar tab.
 * Uses fake repos so the tests don't need Firestore.
 */
class OwnerReservationsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // US 02.07.01: active tab shows upcoming reservations and who booked them
    @Test
    fun ownerReservations_activeTab_showsActive() {
        composeTestRule.setContent {
            OwnerReservationsScreen(
                repository = TempReservationRecordRepo(),
                ownerId = "fake-owner-01",
                today = "2026-11-25"
            )
        }
        composeTestRule.onNodeWithText("Grande Cache Cookhouse Site").assertIsDisplayed()
        composeTestRule.onNodeWithText("2027-05-01 to 2027-05-02").assertIsDisplayed()
        composeTestRule.onNodeWithText("Booked by: Alex Kim").assertIsDisplayed()
    }

    // US 02.07.02: past tab shows finished reservations
    @Test
    fun ownerReservations_pastTab_showsPast() {
        composeTestRule.setContent {
            OwnerReservationsScreen(
                repository = TempReservationRecordRepo(),
                ownerId = "fake-owner-01",
                today = "2026-11-25"
            )
        }
        composeTestRule.onNodeWithText("Past").performClick()
        composeTestRule.onNodeWithText("Maligne Ridge").assertIsDisplayed()
        composeTestRule.onNodeWithText("2026-08-10 to 2026-08-12").assertIsDisplayed()
    }

    // US 02.07.03: cancelled tab shows cancelled and rejected reservations
    @Test
    fun ownerReservations_cancelledTab_showsCancelledAndRejected() {
        composeTestRule.setContent {
            OwnerReservationsScreen(
                repository = TempReservationRecordRepo(),
                ownerId = "fake-owner-01",
                today = "2026-11-25"
            )
        }
        composeTestRule.onNodeWithText("Cancelled").performClick()
        composeTestRule.onNodeWithText("Status: cancelled").assertIsDisplayed()
        composeTestRule.onNodeWithText("Status: rejected").assertIsDisplayed()
    }

    // owner with no reservations sees an empty message
    @Test
    fun ownerReservations_noReservations_showsMessage() {
        composeTestRule.setContent {
            OwnerReservationsScreen(
                repository = TempReservationRecordRepo(),
                ownerId = "new-owner",
                today = "2026-11-25"
            )
        }
        composeTestRule.onNodeWithText("No active reservations.").assertIsDisplayed()
    }

    // navigation: the Reservations tab in the bottom bar opens the screen
    @Test
    fun ownerApp_reservationsTab_opensScreen() {
        composeTestRule.setContent {
            OwnerApp(
                ownerId = "fake-owner-01",
                repository = TempCampsiteRepo(),
                lotteryRepository = TestLotteryRepository(),
                entrantRepository = TempEntrantRepo(),
                reservationRepository = TempReservationRecordRepo(),
                monitorDraws = false
            )
        }
        composeTestRule.onNodeWithText("Reservations").performClick()
        composeTestRule.onNodeWithText("Booked by: Alex Kim").assertIsDisplayed()
    }
}