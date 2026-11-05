package com.example.camp_serve.ui.entrant.campsites

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.camp_serve.data.CampsiteRepository
import com.example.camp_serve.data.TempCampsiteRepo
import com.example.camp_serve.model.Campsite

/**
 * Screen that displays detailed information for a selected campsite.
 *
 * @param campsiteId ID of the campsite to display
 * @param navController NavController for navigating back or to other screens
 * @param repository Data source for fetching campsite details (defaults to [TempCampsiteRepo])
 * @param onBackClick Callback when the user taps the back button
 */
@Composable
fun CampsiteDetailScreen(
    campsiteId: String,
    navController: NavController,
    repository: CampsiteRepository = remember { TempCampsiteRepo() },
    onBackClick: () -> Unit = { navController.popBackStack() }
) {
    var campsite by remember { mutableStateOf<Campsite?>(null) }
    var loading by remember { mutableStateOf(true) }

    // TODO: Replace with actual lottery status check from repository/backend later
    var isLotteryOpen by remember { mutableStateOf(true) }

    LaunchedEffect(campsiteId) {
        campsite = repository.getCampsite(campsiteId)
        loading = false
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top navigation bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBackClick) {
                Text(
                    text = "< Back",
                    color = Color(0xFF715CAA),
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = campsite?.name ?: "Campsite Details",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF715CAA),
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Detail content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else if (campsite == null) {
                Text(
                    text = "Campsite not found.",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = Color.Gray,
                    fontSize = 18.sp
                )
            } else {
                val site = campsite!!

                // Main Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF7FF)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = site.name,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF715CAA)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Lottery status indicator
                        Text(
                            text = if (isLotteryOpen) "Lottery open" else "Lottery closed",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isLotteryOpen) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Lottery Waitlist Button (Skeleton)
                        Button(
                            onClick = {
                                // TODO: Implement lottery waitlist entry logic here
                            },
                            enabled = isLotteryOpen,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isLotteryOpen) "Enter Waitlist" else "No active lottery")
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Location: ${site.location.ifBlank { "N/A" }}",
                            fontSize = 16.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Maximum Occupancy: ${site.maxOccupancy} guests",
                            fontSize = 16.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        if (site.price > 0) {
                            Text(
                                text = "Price: $${site.price} / night",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        val ratingText = if (site.rating > 0.0) "${site.rating} ⭐" else "no rating"
                        Text(
                            text = "Rating: $ratingText",
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Description section
                if (site.description.isNotBlank()) {
                    Text(
                        text = "Description & Amenities",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF715CAA)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = site.description,
                        fontSize = 14.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Rules section
                if (site.rules.isNotBlank()) {
                    Text(
                        text = "Rules & Policies",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF715CAA)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = site.rules,
                        fontSize = 14.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Availability
                if (site.availableDates.isNotEmpty()) {
                    Text(
                        text = "Available Dates",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF715CAA)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = site.availableDates.joinToString(", "),
                        fontSize = 14.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Owner section
                Text(
                    text = "Owner",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF715CAA)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = site.ownerId.ifBlank { "N/A" },
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CampsiteDetailScreenPreview() {
    CampsiteDetailScreen(
        campsiteId = "camp-01",
        navController = rememberNavController()
    )
}
