package com.example.camp_serve.ui.owner.campsites

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.camp_serve.data.CampsiteRepository
import com.example.camp_serve.data.TempCampsiteRepo
import com.example.camp_serve.model.Campsite
import kotlinx.coroutines.launch

/*
 * EditCampsiteScreen.kt
 * Lets an owner change the details of one of their campsites (US 02.01.03).
 * Uses the same checks as the register form (CampsiteValidation.kt).
 * TODO: editing map location, image and available dates isn't added yet.
 */

/**
 * Form filled in with a campsite's current details so the owner can change them.
 *
 * @param repository where the campsite is loaded from and saved to
 * @param campsiteId id of the campsite being edited
 * @param onSaved called after the changes are saved
 * @param onCancel called when the owner leaves without saving
 */
@Composable
fun EditCampsiteScreen(
    repository: CampsiteRepository,
    campsiteId: String,
    modifier: Modifier = Modifier,
    onSaved: () -> Unit = {},
    onCancel: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var original by remember { mutableStateOf<Campsite?>(null) }

    // what's in each box
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var occupancy by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var rules by remember { mutableStateOf("") }

    // error message to show, empty means no error
    var error by remember { mutableStateOf("") }

    // load the campsite and fill the boxes with its current details
    LaunchedEffect(campsiteId) {
        val campsite = repository.getCampsite(campsiteId)
        if (campsite != null) {
            original = campsite
            name = campsite.name
            location = campsite.location
            description = campsite.description
            occupancy = campsite.maxOccupancy.toString()
            price = campsite.price.toString()
            rules = campsite.rules
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
            text = "Edit Campsite",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        if (loading) {
            CircularProgressIndicator()
        } else if (original == null) {
            Text("Campsite not found.")
        } else {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name *") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location *") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description and amenities") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = occupancy,
                onValueChange = { occupancy = it },
                label = { Text("Max occupancy *") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text("Price per night *") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = rules,
                onValueChange = { rules = it },
                label = { Text("Rules and cancellation policy") },
                modifier = Modifier.fillMaxWidth()
            )

            // show the error message if there is one
            if (error != "") {
                Text(text = error, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row {
                // cancel leaves without saving, so the campsite stays the same
                Button(onClick = onCancel) {
                    Text("Cancel")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(onClick = {
                    if (!isValidText(name)) {
                        error = "Name is required"
                    } else if (!isValidText(location)) {
                        error = "Location is required"
                    } else if (!isValidOccupancy(occupancy)) {
                        error = "Max occupancy must be a whole number, at least 1"
                    } else if (!isValidPrice(price)) {
                        error = "Price must be a number, like 65 or 49.99"
                    } else {
                        error = ""
                        // keep the same id and owner, only change the edited details
                        val updated = original!!.copy(
                            name = name.trim(),
                            location = location.trim(),
                            description = description.trim(),
                            maxOccupancy = occupancy.trim().toInt(),
                            price = price.trim().toDouble(),
                            rules = rules.trim()
                        )
                        scope.launch {
                            repository.updateCampsite(updated)
                            onSaved()
                        }
                    }
                }) {
                    Text("Save changes")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EditCampsiteScreenPreview() {
    EditCampsiteScreen(repository = TempCampsiteRepo(), campsiteId = "camp-01")
}