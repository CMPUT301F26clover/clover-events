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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
 * Form for an owner to register a new campsite (US 02.01.01, US 02.01.02).
 * TODO: map location and image upload aren't added yet.
 */

/**
 * Form where the owner fills in campsite details and saves it.
 *
 * @param repository where the new campsite gets saved
 * @param ownerId id of the owner registering it
 * @param onSaved called after the campsite is saved
 * @param onCancel called when the owner leaves without saving
 */
@Composable
fun RegisterCampsiteScreen(
    repository: CampsiteRepository,
    ownerId: String,
    modifier: Modifier = Modifier,
    onSaved: () -> Unit = {},
    onCancel: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var occupancy by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var rules by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Register Campsite",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

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

        OutlinedTextField(
            value = tags,
            onValueChange = { tags = it },
            label = { Text("Tags (comma separated, e.g. lake, pet friendly)") },
            modifier = Modifier.fillMaxWidth()
        )

        // show the error message if there is one
        if (error != "") {
            Text(text = error, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row {
            Button(onClick = onCancel) {
                Text("Cancel")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(onClick = {
                // check the required boxes one by one
                if (!isValidText(name)) {
                    error = "Name is required"
                } else if (!isValidText(location)) {
                    error = "Location is required"
                } else if (!isValidOccupancy(occupancy)) {
                    error = "Max occupancy must be a whole number, at least 1"
                } else if (!isValidPrice(price)) {
                    error = "Price must be a number, like 65 or 49.99"
                } else {
                    // if everything is okay, so make the campsite and save it
                    error = ""
                    val campsite = Campsite(
                        ownerId = ownerId,
                        name = name.trim(),
                        location = location.trim(),
                        description = description.trim(),
                        maxOccupancy = occupancy.trim().toInt(),
                        price = price.trim().toDouble(),
                        rules = rules.trim(),
                        tags = parseTags(tags)
                    )
                    scope.launch {
                        repository.addCampsite(campsite)
                        onSaved()
                    }
                }
            }) {
                Text("Save")
            }
        }
    }
}

/**
 * Turns "lake, pet friendly, fire pit" into ["lake", "pet friendly", "fire pit"].
 *
 * @param text what the owner typed in the tags box
 * @return the list of tags, with empty ones removed
 */
fun parseTags(text: String): List<String> {
    val result = mutableListOf<String>()
    val pieces = text.split(",")
    for (piece in pieces) {
        val tag = piece.trim()
        if (tag != "") {
            result.add(tag)
        }
    }
    return result
}

@Preview(showBackground = true)
@Composable
fun RegisterCampsiteScreenPreview() {
    RegisterCampsiteScreen(repository = TempCampsiteRepo(), ownerId = "fake-owner-01")
}