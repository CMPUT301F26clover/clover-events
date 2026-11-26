package com.example.camp_serve.ui.entrant.reviews

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camp_serve.data.CampsiteRepository
import com.example.camp_serve.data.ReservationRecordRepository
import com.example.camp_serve.data.ReviewRepository
import com.example.camp_serve.data.TempCampsiteRepo
import com.example.camp_serve.data.TempReservationRecordRepo
import com.example.camp_serve.data.TempReviewRepo
import com.example.camp_serve.model.MAX_RATING
import com.example.camp_serve.model.Reservation
import com.example.camp_serve.model.Review
import com.example.camp_serve.model.averageRating
import com.example.camp_serve.model.validateReview
import kotlinx.coroutines.launch

/*
 * ReviewScreen.kt
 * Lets an entrant rate a campsite they stayed at (US 01.13.01) and write an
 * optional review (US 01.13.02). Opened from a past reservation. Asks the
 * entrant to confirm before posting; dismissing the prompt saves nothing.
 * Outstanding issues: none.
 */

/**
 * Screen for rating and reviewing one past reservation.
 * If the entrant already reviewed it, their old rating and text are filled in
 * and saving replaces them, so each stay has at most one review.
 *
 * @param reservationId id of the reservation being reviewed
 * @param reservationRepository where the reservation is loaded from
 * @param reviewRepository where the review is loaded from and saved to
 * @param campsiteRepository used to update the campsite's average rating
 * @param onDone called when the entrant taps the back button after posting
 */
@Composable
fun ReviewScreen(
    reservationId: String,
    reservationRepository: ReservationRecordRepository,
    reviewRepository: ReviewRepository,
    campsiteRepository: CampsiteRepository,
    modifier: Modifier = Modifier,
    onDone: () -> Unit = {}
) {
    var loading by remember { mutableStateOf(true) }
    var reservation by remember { mutableStateOf<Reservation?>(null) }
    var rating by remember { mutableStateOf(0) }
    var text by remember { mutableStateOf("") }
    var alreadyReviewed by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    var justSaved by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // load the reservation and any review the entrant already left
    LaunchedEffect(reservationId) {
        reservation = reservationRepository.getReservation(reservationId)
        val existing = reviewRepository.getReviewForReservation(reservationId)
        if (existing != null) {
            rating = existing.rating
            text = existing.text
            alreadyReviewed = true
        }
        loading = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Rate your stay",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        val current = reservation
        if (loading) {
            CircularProgressIndicator()
        } else if (current == null) {
            Text("Reservation not found.")
        } else {
            Text(text = current.campsiteName, fontWeight = FontWeight.Bold)
            Text(text = current.startDate + " to " + current.endDate)

            Spacer(modifier = Modifier.height(16.dp))

            // star picker: tap star number i to give i stars
            Row {
                for (i in 1..MAX_RATING) {
                    Text(
                        text = if (i <= rating) "★" else "☆",
                        fontSize = 36.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .testTag("star_" + i)
                            .clickable { rating = i }
                            .padding(end = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Review (optional)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // check the review, then ask the entrant to confirm before posting
            Button(onClick = {
                justSaved = false
                val error = validateReview(rating, text.trim())
                if (error != null) {
                    message = error
                } else {
                    message = ""
                    showConfirm = true
                }
            }) {
                Text(if (alreadyReviewed) "Update review" else "Submit review")
            }

            if (message != "") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(message)
            }

            if (justSaved) {
                TextButton(onClick = onDone) {
                    Text("Back to my reservations")
                }
            }

            // confirmation prompt: dismissing it saves nothing
            if (showConfirm) {
                val starWord = if (rating == 1) "star" else "stars"
                AlertDialog(
                    onDismissRequest = { showConfirm = false },
                    title = { Text("Post this review?") },
                    text = { Text("You gave " + current.campsiteName + " " + rating + " " + starWord + ".") },
                    confirmButton = {
                        TextButton(onClick = {
                            showConfirm = false
                            scope.launch {
                                try {
                                    reviewRepository.saveReview(
                                        Review(
                                            id = current.id,
                                            campsiteId = current.campsiteId,
                                            campsiteName = current.campsiteName,
                                            entrantId = current.entrantId,
                                            entrantName = current.entrantName,
                                            rating = rating,
                                            text = text.trim(),
                                            createdAt = System.currentTimeMillis()
                                        )
                                    )
                                    updateCampsiteRating(
                                        current.campsiteId,
                                        reviewRepository,
                                        campsiteRepository
                                    )
                                    alreadyReviewed = true
                                    justSaved = true
                                    message = "Thanks! Your review was saved."
                                } catch (e: Exception) {
                                    message = "Could not save your review. Try again."
                                }
                            }
                        }) {
                            Text("Yes, post it")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirm = false }) {
                            Text("Not yet")
                        }
                    }
                )
            }
        }
    }
}

/**
 * Recalculates a campsite's average rating from all of its reviews and saves it
 * on the campsite, so the rating shown in the campsite list stays up to date.
 *
 * @param campsiteId id of the campsite that was just reviewed
 * @param reviewRepository where the campsite's reviews are loaded from
 * @param campsiteRepository where the campsite is loaded from and saved to
 */
suspend fun updateCampsiteRating(
    campsiteId: String,
    reviewRepository: ReviewRepository,
    campsiteRepository: CampsiteRepository
) {
    val reviews = reviewRepository.getReviewsForCampsite(campsiteId)
    val campsite = campsiteRepository.getCampsite(campsiteId)
    if (campsite != null) {
        val average = Math.round(averageRating(reviews) * 10) / 10.0
        campsiteRepository.updateCampsite(campsite.copy(rating = average))
    }
}

@Preview(showBackground = true)
@Composable
fun ReviewScreenPreview() {
    ReviewScreen(
        reservationId = "res-02",
        reservationRepository = TempReservationRecordRepo(),
        reviewRepository = TempReviewRepo(),
        campsiteRepository = TempCampsiteRepo()
    )
}