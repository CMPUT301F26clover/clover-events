package com.example.camp_serve.ui.entrant.campsites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.camp_serve.data.CampsiteRepository
import com.example.camp_serve.data.TempCampsiteRepo
import com.example.camp_serve.model.Campsite

/**
 * Entrant campsites screen that displays available campsites loaded from the repository.
 *
 * @param navController navigation controller for screen transitions
 * @param repository data source for campsites (defaults to [TempCampsiteRepo])
 * @param onCampsiteClick callback when a campsite item is clicked
 */
@Composable
fun CampsitesScreen(
    navController: NavController,
    repository: CampsiteRepository = remember { TempCampsiteRepo() },
    onCampsiteClick: (Campsite) -> Unit = { campsite ->
        if (campsite.id.isNotBlank()) {
            navController.navigate("campsite_detail/${campsite.id}")
        }
    }
) {
    var userInput by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var campsites by remember { mutableStateOf(listOf<Campsite>()) }
    var loading by remember { mutableStateOf(true) }

    // Load campsites from the repository when screen opens
    LaunchedEffect(Unit) {
        campsites = repository.getAllCampsites()
        loading = false
    }

    val filtered = if (query.isBlank()) {
        campsites
    } else {
        campsites.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.location.contains(query, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // title
        Text(
            text = "Campsites",
            fontSize = 30.sp,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp),
            color = Color(0xFF715CAA)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // search bar & search button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = userInput,
                onValueChange = {
                    userInput = it
                    query = it
                },
                label = { Text("Search Campsites") },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = {
                    query = userInput
                }
            ) {
                Text("Search")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Campsite list
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(32.dp)
            )
        } else if (filtered.isEmpty()) {
            Text(
                text = "No campsites found.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = TextAlign.Center,
                color = Color.Gray,
                fontSize = 18.sp
            )
        } else {
            LazyColumn {
                items(filtered) { campsite ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCampsiteClick(campsite) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = campsite.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = "Location: ${campsite.location.ifBlank { "N/A" }}",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "Maximum Occupancy: ${campsite.maxOccupancy}",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        val ratingText = if (campsite.rating > 0.0) campsite.rating.toString() else "no rating"
                        Text(
                            text = "Rating: $ratingText",
                            fontSize = 14.sp,
                            color = Color(0xFF715CAA)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CampsitesScreenPreview() {
    CampsitesScreen(navController = rememberNavController())
}
