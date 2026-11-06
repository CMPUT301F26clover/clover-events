/*
 * EditCampsiteScreen.kt is the form for changing a campsite the owner already registered
 * (US 02.02.01). It looks like RegisterCampsiteScreen but starts filled in.
 * Outstanding issues: image upload is not added yet.
 */

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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.camp_serve.data.CampsiteRepository
import com.example.camp_serve.model.Campsite
import kotlinx.coroutines.launch

/**
 * Edit form for one campsite.
 *
 * @param campsiteId campsite to edit
 * @param repository where the campsite is loaded and saved
 * @param onSaved called after saving
 * @param onCancel called when the owner leaves without saving
 */
@Composable
fun EditCampsiteScreen(
    campsiteId: String,
    repository: CampsiteRepository,
    modifier: Modifier = Modifier,
    onSaved: () -> Unit = {},
    onCancel: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var original by remember { mutableStateOf<Campsite?>(null) }
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var occupancy by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var rules by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    // fill the boxes with the campsite's current details
    LaunchedEffect(campsiteId) {
        val c = repository.getCampsite(campsiteId)
        original = c
        if (c != null) {
            name = c.name
            location = c.location
            description = c.description
            occupancy = c.maxOccupancy.toString()
            price = c.price.toString()
            rules = c.rules
            tags = c.tags.joinToString(", ")
        }
    }

    if (original == null) {
        Text("Loading...", modifier = modifier.padding(16.dp))
        return
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Edit Campsite", style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary)
        OutlinedTextField(name, { name = it }, label = { Text("Name *") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(location, { location = it }, label = { Text("Location *") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(price, { price = it }, label = { Text("Price per night *") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(occupancy, { occupancy = it }, label = { Text("Max occupancy *") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(description, { description = it }, label = { Text("Description and amenities") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(tags, { tags = it }, label = { Text("Tags (comma separated)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(rules, { rules = it }, label = { Text("Rules and cancellation policy") }, modifier = Modifier.fillMaxWidth())

        if (error != "") Text(error, color = MaterialTheme.colorScheme.error)

        Spacer(modifier = Modifier.height(16.dp))
        Row {
            OutlinedButton(onClick = onCancel) { Text("Cancel") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(modifier = Modifier.testTag("save_campsite"), onClick = {
                val old = original
                if (old == null) return@Button
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
                    val updated = old.copy(
                        name = name.trim(),
                        location = location.trim(),
                        description = description.trim(),
                        maxOccupancy = occupancy.trim().toInt(),
                        price = price.trim().toDouble(),
                        rules = rules.trim(),
                        tags = tags.split(",").map { it.trim() }.filter { it != "" }
                    )
                    scope.launch {
                        repository.updateCampsite(updated)
                        onSaved()
                    }
                }
            }) { Text("Save Changes") }
        }
    }
}
