package com.example.camp_serve.ui.entrant.reviews

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camp_serve.data.ReviewRepository
import com.example.camp_serve.model.MAX_RATING
import com.example.camp_serve.model.Review

/*
 * CampsiteReviewsSection.kt
 * The "Reviews" part of the campsite details page: every review left by
 * entrants who stayed there (US 01.13.02).
 * Outstanding issues: none.
 */

/**
 * Lists the reviews of one campsite, or a message if there are none yet.
 *
 * @param campsiteId id of the campsite whose reviews are shown
 * @param repository where the reviews are loaded from
 */
@Composable
fun CampsiteReviewsSection(
    campsiteId: String,
    repository: ReviewRepository,
    modifier: Modifier = Modifier
) {
    var reviews by remember { mutableStateOf<List<Review>>(emptyList()) }

    LaunchedEffect(campsiteId) {
        reviews = try {
            repository.getReviewsForCampsite(campsiteId)
        } catch (e: Exception) {
            emptyList()
        }
    }

    Column(modifier = modifier) {
        Text(
            text = "Reviews",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF715CAA)
        )
        Spacer(modifier = Modifier.height(4.dp))

        if (reviews.isEmpty()) {
            Text(text = "No reviews yet.", fontSize = 14.sp, color = Color.DarkGray)
        } else {
            for (review in reviews) {
                // stars like ★★★★☆
                val stars = review.rating.coerceIn(0, MAX_RATING)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = review.entrantName, fontWeight = FontWeight.Bold)
                        Text("★".repeat(stars) + "☆".repeat(MAX_RATING - stars))
                        if (review.text != "") {
                            Text(text = review.text)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}