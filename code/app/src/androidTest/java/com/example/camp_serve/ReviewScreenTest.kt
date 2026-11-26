package com.example.camp_serve

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.camp_serve.data.TempCampsiteRepo
import com.example.camp_serve.data.TempReservationRecordRepo
import com.example.camp_serve.data.TempReviewRepo
import com.example.camp_serve.ui.entrant.EntrantApp
import com.example.camp_serve.ui.entrant.reviews.CampsiteReviewsSection
import com.example.camp_serve.ui.entrant.reviews.ReviewScreen
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import androidx.test.platform.app.InstrumentationRegistry
import com.example.camp_serve.data.DeviceId
import com.example.camp_serve.model.Reservation

/*
 * ReviewScreenTest.kt
 * Intent tests for rating and reviewing a campsite (US 01.13.01, US 01.13.02):
 * the review screen, the confirmation prompt, the reviews shown on the campsite
 * details page and the button on the reservations page that opens the screen.
 * Uses Temp repos so the tests don't need Firestore.
 */
class ReviewScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // US 01.13.01: the screen shows which stay is being rated
    @Test
    fun reviewScreen_showsCampsite() {
        composeTestRule.setContent {
            ReviewScreen(
                reservationId = "res-01",
                reservationRepository = TempReservationRecordRepo(),
                reviewRepository = TempReviewRepo(),
                campsiteRepository = TempCampsiteRepo()
            )
        }
        composeTestRule.onNodeWithText("Rate your stay").assertIsDisplayed()
        composeTestRule.onNodeWithText("Grande Cache Cookhouse Site").assertIsDisplayed()
        composeTestRule.onNodeWithText("Submit review").assertIsDisplayed()
    }

    // US 01.13.01: a rating is required
    @Test
    fun submit_noRating_showsError() {
        composeTestRule.setContent {
            ReviewScreen(
                reservationId = "res-01",
                reservationRepository = TempReservationRecordRepo(),
                reviewRepository = TempReviewRepo(),
                campsiteRepository = TempCampsiteRepo()
            )
        }
        composeTestRule.onNodeWithText("Submit review").performClick()
        composeTestRule.onNodeWithText("Please choose a rating from 1 to 5 stars.").assertIsDisplayed()
    }

    // US 01.13.01: confirming the prompt saves the rating and shows a message
    @Test
    fun submit_confirm_savesReview() {
        val reviews = TempReviewRepo()
        composeTestRule.setContent {
            ReviewScreen(
                reservationId = "res-01",
                reservationRepository = TempReservationRecordRepo(),
                reviewRepository = reviews,
                campsiteRepository = TempCampsiteRepo()
            )
        }
        composeTestRule.onNodeWithTag("star_4").performClick()
        composeTestRule.onNodeWithText("Submit review").performClick()
        composeTestRule.onNodeWithText("Post this review?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Yes, post it").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Thanks! Your review was saved.").assertIsDisplayed()
        val saved = runBlocking { reviews.getReviewForReservation("res-01") }
        Assert.assertNotNull(saved)
        Assert.assertEquals(4, saved!!.rating)
    }

    // US 01.13.01: dismissing the prompt saves nothing
    @Test
    fun submit_dismiss_savesNothing() {
        val reviews = TempReviewRepo()
        composeTestRule.setContent {
            ReviewScreen(
                reservationId = "res-01",
                reservationRepository = TempReservationRecordRepo(),
                reviewRepository = reviews,
                campsiteRepository = TempCampsiteRepo()
            )
        }
        composeTestRule.onNodeWithTag("star_4").performClick()
        composeTestRule.onNodeWithText("Submit review").performClick()
        composeTestRule.onNodeWithText("Not yet").performClick()
        composeTestRule.waitForIdle()
        Assert.assertNull(runBlocking { reviews.getReviewForReservation("res-01") })
    }

    // US 01.13.02: an earlier review is filled in and can be updated
    @Test
    fun existingReview_showsUpdateButtonAndText() {
        composeTestRule.setContent {
            ReviewScreen(
                reservationId = "res-02",
                reservationRepository = TempReservationRecordRepo(),
                reviewRepository = TempReviewRepo(),
                campsiteRepository = TempCampsiteRepo()
            )
        }
        composeTestRule.onNodeWithText("Update review").assertIsDisplayed()
        composeTestRule.onNodeWithText("Great views and quiet at night.").assertIsDisplayed()
    }

    // reservation that doesn't exist
    @Test
    fun unknownReservation_showsMessage() {
        composeTestRule.setContent {
            ReviewScreen(
                reservationId = "res-99",
                reservationRepository = TempReservationRecordRepo(),
                reviewRepository = TempReviewRepo(),
                campsiteRepository = TempCampsiteRepo()
            )
        }
        composeTestRule.onNodeWithText("Reservation not found.").assertIsDisplayed()
    }

    // US 01.13.02: reviews are shown on the campsite details page
    @Test
    fun reviewsSection_showsReviews() {
        composeTestRule.setContent {
            CampsiteReviewsSection(campsiteId = "camp-02", repository = TempReviewRepo())
        }
        composeTestRule.onNodeWithText("Reviews").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alex Kim").assertIsDisplayed()
        composeTestRule.onNodeWithText("Great views and quiet at night.").assertIsDisplayed()
    }

    // campsite nobody reviewed yet
    @Test
    fun reviewsSection_noReviews_showsMessage() {
        composeTestRule.setContent {
            CampsiteReviewsSection(campsiteId = "camp-01", repository = TempReviewRepo())
        }
        composeTestRule.onNodeWithText("No reviews yet.").assertIsDisplayed()
    }

    // navigation: the button on a past reservation opens the review screen
    @Test
    fun pastReservation_button_opensReviewScreen() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val reservations = TempReservationRecordRepo()
        runBlocking {
            reservations.addReservation(
                Reservation(
                    id = "res-test",
                    campsiteId = "camp-02",
                    campsiteName = "Maligne Ridge",
                    ownerId = "fake-owner-01",
                    entrantId = DeviceId(context).get(),
                    entrantName = "Test Entrant",
                    startDate = "2020-01-01",
                    endDate = "2020-01-02",
                    status = "active"
                )
            )
        }
        composeTestRule.setContent {
            EntrantApp(
                reservationRepository = reservations,
                reviewRepository = TempReviewRepo()
            )
        }
        composeTestRule.onNodeWithText("My Reservations").performClick()
        composeTestRule.onNodeWithText("Rate / review this stay").performClick()
        composeTestRule.onNodeWithText("Rate your stay").assertIsDisplayed()
    }
}