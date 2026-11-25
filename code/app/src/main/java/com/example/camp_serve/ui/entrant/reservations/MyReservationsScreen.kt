package com.example.camp_serve.ui.entrant.reservations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.example.camp_serve.model.activeReservations
import com.example.camp_serve.model.pastReservations
import com.example.camp_serve.model.todayString

/*
 * MyReservationsScreen.kt
 * Shows an entrant their active reservations (US 01.06.01) and their
 * past reservations (US 01.06.02).
 * Outstanding issues: none.
 */

/**
 * Lists the entrant's reservations in two sections: active and past.
 *
 * @param repository where reservations are loaded from
 * @param entrantId the entrant's device id
 * @param today today's date as "YYYY-MM-DD" (tests can pass a fixed date)
 * @param onReservationClick called when the entrant taps a reservation
 */
@Composable
fun MyReservationsScreen(
    repository: ReservationRecordRepository,
    entrantId: String,
    modifier: Modifier = Modifier,
    today: String = todayString(),
    onReservationClick: (Reservation) -> Unit = {}
) {
    var loading by remember { mutableStateOf(true) }
    var allReservations by remember { mutableStateOf<List<Reservation>>(emptyList()) }

    // load this entrant's reservations when the screen opens
    LaunchedEffect(entrantId) {
        allReservations = repository.getReservationsForEntrant(entrantId)
        loading = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "My Reservations",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (loading) {
            CircularProgressIndicator()
        } else {
            val active = activeReservations(allReservations, today)
            val past = pastReservations(allReservations, today)

            // active section
            Text(text = "Active", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            if (active.isEmpty()) {
                Text("No active reservations.")
            } else {
                for (reservation in active) {
                    ReservationRow(reservation, onClick = { onReservationClick(reservation) })
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // past section
            Text(text = "Past", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            if (past.isEmpty()) {
                Text("No past reservations.")
            } else {
                for (reservation in past) {
                    ReservationRow(reservation, onClick = { onReservationClick(reservation) })
                }
            }
        }
    }
}

/**
 * One reservation in a list: campsite name, dates and status.
 * Also used by the owner's reservation screens.
 *
 * @param reservation the reservation to show
 * @param showEntrant true to also show who booked it (owner screens)
 * @param onClick called when the row is tapped
 */
@Composable
fun ReservationRow(
    reservation: Reservation,
    showEntrant: Boolean = false,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = reservation.campsiteName, fontWeight = FontWeight.Bold)
            Text(text = reservation.startDate + " to " + reservation.endDate)
            if (showEntrant) {
                Text(text = "Booked by: " + reservation.entrantName)
            }
            Text(text = "Status: " + reservation.status)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MyReservationsScreenPreview() {
    MyReservationsScreen(
        repository = TempReservationRecordRepo(),
        entrantId = "fake-id-01",
        today = "2026-11-25"
    )
}