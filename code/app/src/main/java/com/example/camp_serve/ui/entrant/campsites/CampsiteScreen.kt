package com.example.camp_serve.ui.entrant.campsites

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.TimeZone
import java.util.Locale

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
    var query by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var checkIn by rememberSaveable { mutableStateOf("") }
    var checkOut by rememberSaveable { mutableStateOf("") }
    var guests by rememberSaveable { mutableStateOf("") }
    var appliedLocation by rememberSaveable { mutableStateOf("") }
    var appliedCheckIn by rememberSaveable { mutableStateOf("") }
    var appliedCheckOut by rememberSaveable { mutableStateOf("") }
    var appliedGuests by rememberSaveable { mutableStateOf("") }
    var filtersExpanded by remember { mutableStateOf(false) }
    val guestCount = guests.toIntOrNull()
    val invalidGuests = guests.isNotEmpty() && (guestCount == null || guestCount < 1)
    val invalidDates = (checkIn.isEmpty() != checkOut.isEmpty()) ||
        (checkIn.isNotEmpty() && checkOut.isNotEmpty() && checkOut <= checkIn)
    val appliedGuestCount = appliedGuests.toIntOrNull()
    var campsites by remember { mutableStateOf(listOf<Campsite>()) }
    var loading by remember { mutableStateOf(true) }

    // Load campsites from the repository when screen opens
    LaunchedEffect(Unit) {
        campsites = repository.getAllCampsites()
        loading = false
    }

    val filtered = campsites.filter { campsite ->
        (query.isBlank() || campsite.name.contains(query.trim(), ignoreCase = true) ||
            campsite.location.contains(query.trim(), ignoreCase = true)) &&
            (appliedLocation.isBlank() || campsite.location.contains(appliedLocation.trim(), ignoreCase = true)) &&
            matchesStay(campsite.availableDates, appliedCheckIn, appliedCheckOut) &&
            (appliedGuestCount == null || campsite.maxOccupancy >= appliedGuestCount)
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

        // opens the entrant's reservations (US 01.06.01, US 01.06.02)
        Button(
            onClick = { navController.navigate("my_reservations") },
            modifier = Modifier.padding(start = 16.dp, top = 8.dp)
        ) {
            Text("My Reservations")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search and filters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search Campsites") },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            Box {
                TextButton(onClick = {
                    location = appliedLocation
                    checkIn = appliedCheckIn
                    checkOut = appliedCheckOut
                    guests = appliedGuests
                    filtersExpanded = true
                }) { Text("Filters") }
                DropdownMenu(
                    expanded = filtersExpanded,
                    onDismissRequest = { filtersExpanded = false }
                ) {
                    Column(modifier = Modifier.width(280.dp).padding(horizontal = 16.dp)) {
                        TextField(
                            value = location,
                            onValueChange = { location = it },
                            label = { Text("Location") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        DateFilterField("Check-in", checkIn) { checkIn = it }
                        DateFilterField("Check-out", checkOut) { checkOut = it }
                        if (checkIn.isNotEmpty() && checkOut.isNotEmpty() && checkOut <= checkIn) {
                            Text("Check-out must be after check-in", color = Color.Red)
                        } else if (checkIn.isEmpty() != checkOut.isEmpty()) {
                            Text("Select both dates to filter your stay")
                        }
                        TextField(
                            value = guests,
                            onValueChange = { value -> if (value.all { it.isDigit() }) guests = value },
                            label = { Text("Guests") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = invalidGuests,
                            supportingText = if (invalidGuests) ({ Text("Enter a valid number of guests") }) else null,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextButton(onClick = {
                            location = ""
                            checkIn = ""
                            checkOut = ""
                            guests = ""
                            appliedLocation = ""
                            appliedCheckIn = ""
                            appliedCheckOut = ""
                            appliedGuests = ""
                        }) { Text("Clear filters") }
                        Button(
                            onClick = {
                                appliedLocation = location
                                appliedCheckIn = checkIn
                                appliedCheckOut = checkOut
                                appliedGuests = guests
                                filtersExpanded = false
                            },
                            enabled = !invalidGuests && !invalidDates,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Apply filters") }
                    }
                }
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

// A checkout date is the departure day, so only nights before it must be available.
internal fun matchesStay(availableDates: List<String>, checkIn: String, checkOut: String): Boolean {
    if (checkIn.isEmpty() || checkOut.isEmpty()) return true
    if (checkOut <= checkIn) return false

    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        isLenient = false
        timeZone = TimeZone.getTimeZone("UTC")
    }
    val start = runCatching { format.parse(checkIn) }.getOrNull() ?: return false
    val end = runCatching { format.parse(checkOut) }.getOrNull() ?: return false
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { time = start }
    val available = availableDates.toSet()
    var nights = 0
    while (calendar.time.before(end)) {
        if (nights >= available.size || format.format(calendar.time) !in available) return false
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        nights++
    }
    return true
}

@Composable
private fun DateFilterField(label: String, date: String, onDateChange: (String) -> Unit) {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxWidth()) {
        TextField(
            value = date,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text("Select date") },
            singleLine = true,
            trailingIcon = if (date.isNotEmpty()) {
                { TextButton(onClick = { onDateChange("") }) { Text("Clear") } }
            } else null,
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(end = if (date.isNotEmpty()) 72.dp else 0.dp)
                .clickable(onClickLabel = "Select $label date") {
                    val calendar = Calendar.getInstance()
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            onDateChange(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day))
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CampsitesScreenPreview() {
    CampsitesScreen(navController = rememberNavController())
}