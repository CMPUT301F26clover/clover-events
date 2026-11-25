package com.example.camp_serve

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.camp_serve.data.TempReservationRecordRepo
import com.example.camp_serve.ui.entrant.EntrantApp
import com.example.camp_serve.ui.entrant.reservations.MyReservationsScreen
import org.junit.Rule
import org.junit.Test

/*
 * MyReservationsScreenTest.kt
 * Intent tests for the entrant's My Reservations screen (US 01.06.01, US 01.06.02)
 * and the button that opens it from the campsites screen.
 * Uses TempReservationRecordRepo so the tests don't need Firestore.
 */
class MyReservationsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // US 01.06.01: active reservations are shown
    @Test
    fun myReservations_showsActiveReservation() {
        composeTestRule.setContent {
            MyReservationsScreen(
                repository = TempReservationRecordRepo(),
                entrantId = "fake-id-01",
                today = "2026-11-25"
            )
        }
        composeTestRule.onNodeWithText("Active").assertIsDisplayed()
        composeTestRule.onNodeWithText("Grande Cache Cookhouse Site").assertIsDisplayed()
        composeTestRule.onNodeWithText("2027-05-01 to 2027-05-02").assertIsDisplayed()
    }

    // US 01.06.02: past reservations are shown in their own section
    @Test
    fun myReservations_showsPastReservation() {
        composeTestRule.setContent {
            MyReservationsScreen(
                repository = TempReservationRecordRepo(),
                entrantId = "fake-id-01",
                today = "2026-11-25"
            )
        }
        composeTestRule.onNodeWithText("Past").assertIsDisplayed()
        composeTestRule.onNodeWithText("Maligne Ridge").assertIsDisplayed()
        composeTestRule.onNodeWithText("2026-08-10 to 2026-08-12").assertIsDisplayed()
    }

    // entrant with no reservations sees both empty messages
    @Test
    fun myReservations_noReservations_showsMessages() {
        composeTestRule.setContent {
            MyReservationsScreen(
                repository = TempReservationRecordRepo(),
                entrantId = "nobody",
                today = "2026-11-25"
            )
        }
        composeTestRule.onNodeWithText("No active reservations.").assertIsDisplayed()
        composeTestRule.onNodeWithText("No past reservations.").assertIsDisplayed()
    }

    // navigation: tapping My Reservations on the campsites screen opens it
    @Test
    fun campsites_myReservationsButton_opensScreen() {
        composeTestRule.setContent {
            EntrantApp(reservationRepository = TempReservationRecordRepo())
        }
        composeTestRule.onNodeWithText("My Reservations").performClick()
        composeTestRule.onNodeWithText("Active").assertIsDisplayed()
        composeTestRule.onNodeWithText("Past").assertIsDisplayed()
    }
}