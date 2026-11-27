package com.example.camp_serve

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.camp_serve.data.TempReservationRecordRepo
import com.example.camp_serve.ui.entrant.reservations.MyReservationsScreen
import com.example.camp_serve.ui.entrant.reservations.ReservationConfirmationScreen
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

/*
 * ReservationConfirmationScreenTest.kt
 * Intent tests for the reservation confirmation screen (US 01.05.01),
 * its CSV download buttons (US 01.05.02, US 01.07.02) and the
 * "View confirmation" button on My Reservations.
 * Uses TempReservationRecordRepo so the tests don't need Firestore.
 */
class ReservationConfirmationScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // US 01.05.01: an active reservation shows a confirmation with its details
    @Test
    fun confirmation_active_showsConfirmed() {
        composeTestRule.setContent {
            ReservationConfirmationScreen(
                repository = TempReservationRecordRepo(),
                reservationId = "res-01"
            )
        }
        composeTestRule.onNodeWithText("Reservation confirmed").assertIsDisplayed()
        composeTestRule.onNodeWithText("res-01").assertIsDisplayed()
        composeTestRule.onNodeWithText("Grande Cache Cookhouse Site").assertIsDisplayed()
        composeTestRule.onNodeWithText("2027-05-01").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alex Kim").assertIsDisplayed()
    }

    // a cancelled reservation shows a cancellation instead
    @Test
    fun confirmation_cancelled_showsCancelled() {
        composeTestRule.setContent {
            ReservationConfirmationScreen(
                repository = TempReservationRecordRepo(),
                reservationId = "res-03"
            )
        }
        composeTestRule.onNodeWithText("Reservation cancelled").assertIsDisplayed()
    }

    // a rejected reservation says the owner rejected it
    @Test
    fun confirmation_rejected_showsRejected() {
        composeTestRule.setContent {
            ReservationConfirmationScreen(
                repository = TempReservationRecordRepo(),
                reservationId = "res-04"
            )
        }
        composeTestRule.onNodeWithText("Reservation rejected by owner").assertIsDisplayed()
    }

    // an id that doesn't exist shows a message instead of crashing
    @Test
    fun confirmation_missing_showsNotFound() {
        composeTestRule.setContent {
            ReservationConfirmationScreen(
                repository = TempReservationRecordRepo(),
                reservationId = "not-real"
            )
        }
        composeTestRule.onNodeWithText("Reservation not found.").assertIsDisplayed()
    }

    // US 01.05.02: a confirmed reservation can be downloaded or shared as CSV
    @Test
    fun confirmation_active_showsCsvButtons() {
        composeTestRule.setContent {
            ReservationConfirmationScreen(
                repository = TempReservationRecordRepo(),
                reservationId = "res-01"
            )
        }
        composeTestRule.onNodeWithText("Download CSV").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Share CSV").performScrollTo().assertIsDisplayed()
    }

    // US 01.07.02: a cancelled reservation's confirmation can also be downloaded as CSV
    @Test
    fun confirmation_cancelled_showsCsvButtons() {
        composeTestRule.setContent {
            ReservationConfirmationScreen(
                repository = TempReservationRecordRepo(),
                reservationId = "res-03"
            )
        }
        composeTestRule.onNodeWithText("Reservation cancelled").assertIsDisplayed()
        composeTestRule.onNodeWithText("Download CSV").performScrollTo().assertIsDisplayed()
    }

    // the "View confirmation" button on My Reservations sends the right reservation
    @Test
    fun myReservations_viewConfirmation_sendsReservation() {
        var clickedId = ""
        composeTestRule.setContent {
            MyReservationsScreen(
                repository = TempReservationRecordRepo(),
                entrantId = "fake-id-01",
                today = "2026-11-25",
                onConfirmationClick = { reservation -> clickedId = reservation.id }
            )
        }
        composeTestRule.onNodeWithText("View confirmation").performClick()
        Assert.assertEquals("res-01", clickedId)
    }
}