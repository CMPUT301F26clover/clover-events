package com.example.camp_serve.ui.owner.campsites

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.camp_serve.data.CampsiteRepository
import com.example.camp_serve.data.TempCampsiteRepo
import com.example.camp_serve.model.Campsite

/*
 * OwnerCampsiteDetailScreen.kt
 * Shows all the details of one of the owner's campsites (US 02.01.04).
 * Has an Edit button that will open the edit screen (US 02.01.03).
 * TODO: show the map location, image and QR code when those are added.
 */

/**
 * Shows one campsite's saved details to its owner.
 *
 * @param repository where the campsite is loaded from
 * @param campsiteId id of the campsite to show
 * @param onEditClick called when the owner taps Edit
 * @param onBackClick called when the owner taps Back
 */
@Composable
fun OwnerCampsiteDetailScreen(
    repository: CampsiteRepository,
    campsiteId: String,
    modifier: Modifier = Modifier,
    onEditClick: (Campsite) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    var loading by remember { mutableStateOf(true) }
    var campsite by remember { mutableStateOf<Campsite?>(null) }

    // load the campsite when the screen opens
    LaunchedEffect(campsiteId) {
        campsite = repository.getCampsite(campsiteId)
        loading = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        OutlinedButton(onClick = onBackClick) {
            Text("Back")
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (loading) {
            CircularProgressIndicator()
        } else if (campsite == null) {
            Text("Campsite not found.")
        } else {
            val site = campsite!!

            Text(
                text = site.name,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            DetailRow("Location", site.location)
            DetailRow("Max occupancy", site.maxOccupancy.toString() + " guests")
            DetailRow("Price per night", "$" + site.price)
            DetailRow("Description and amenities", site.description)
            DetailRow("Rules and cancellation policy", site.rules)

            // show tags and dates only if there are any
            if (site.tags.isNotEmpty()) {
                DetailRow("Tags", site.tags.joinToString(", "))
            }
            if (site.availableDates.isNotEmpty()) {
                DetailRow("Available dates", site.availableDates.joinToString(", "))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onEditClick(site) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Edit")
            }
        }
    }
}

/**
 * One label + value pair, like "Location" and "Hinton".
 * Shows "Not provided" if the value is empty.
 */
@Composable
private fun DetailRow(label: String, value: String) {
    Text(text = label, fontWeight = FontWeight.Bold)
    if (value == "") {
        Text("Not provided")
    } else {
        Text(value)
    }
    Spacer(modifier = Modifier.height(12.dp))
}

@Preview(showBackground = true)
@Composable
fun OwnerCampsiteDetailScreenPreview() {
    OwnerCampsiteDetailScreen(repository = TempCampsiteRepo(), campsiteId = "camp-01")
}