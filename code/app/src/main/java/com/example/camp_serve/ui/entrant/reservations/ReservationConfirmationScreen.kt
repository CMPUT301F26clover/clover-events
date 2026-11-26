package com.example.camp_serve.ui.entrant.reservations

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.camp_serve.data.ReservationRecordRepository
import com.example.camp_serve.data.TempReservationRecordRepo
import com.example.camp_serve.model.Reservation

/*
 * ReservationConfirmationScreen.kt
 * Shows the entrant a confirmation of one reservation (US 01.05.01):
 * a confirmation number, the campsite, dates and status. Cancelled or
 * rejected reservations show a cancellation instead.
 * Outstanding issues: downloading as CSV (US 01.05.02, 01.07.02) is not added yet.
 */

/**
 * Confirmation details for one reservation.
 *
 * @param repository where the reservation is loaded from
 * @param reservationId id of the reservation to show
 * @param onBackClick called when the entrant taps Back
 */
@Composable
fun ReservationConfirmationScreen(
    repository: ReservationRecordRepository,
    reservationId: String,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    var loading by remember { mutableStateOf(true) }
    var reservation by remember { mutableStateOf<Reservation?>(null) }

    // load the reservation when the screen opens
    LaunchedEffect(reservationId) {
        reservation = repository.getReservation(reservationId)
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
        } else if (reservation == null) {
            Text("Reservation not found.")
        } else {
            val r = reservation!!

            Text(
                text = confirmationTitle(r.status),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            ConfirmationRow("Confirmation number", r.id)
            ConfirmationRow("Campsite", r.campsiteName)
            ConfirmationRow("Check-in", r.startDate)
            ConfirmationRow("Check-out", r.endDate)
            ConfirmationRow("Booked by", r.entrantName)
            ConfirmationRow("Status", r.status)
        }
    }
}

/**
 * The big heading at the top, based on the reservation's status.
 *
 * @param status "active", "cancelled" or "rejected"
 * @return the heading to show
 */
fun confirmationTitle(status: String): String {
    if (status == "cancelled") {
        return "Reservation cancelled"
    } else if (status == "rejected") {
        return "Reservation rejected by owner"
    }
    return "Reservation confirmed"
}

/**
 * One label + value pair on the confirmation, like "Campsite" and "Banff Ridge".
 */
@Composable
private fun ConfirmationRow(label: String, value: String) {
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
fun ReservationConfirmationScreenPreview() {
    ReservationConfirmationScreen(
        repository = TempReservationRecordRepo(),
        reservationId = "res-01"
    )
}