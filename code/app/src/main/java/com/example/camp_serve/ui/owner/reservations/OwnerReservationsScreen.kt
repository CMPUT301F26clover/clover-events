package com.example.camp_serve.ui.owner.reservations

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.camp_serve.data.ReservationRecordRepository
import com.example.camp_serve.data.TempReservationRecordRepo
import com.example.camp_serve.model.Reservation
import com.example.camp_serve.model.activeReservations
import com.example.camp_serve.model.cancelledReservations
import com.example.camp_serve.model.pastReservations
import com.example.camp_serve.model.todayString
import com.example.camp_serve.ui.entrant.reservations.ReservationRow

/*
 * OwnerReservationsScreen.kt
 * Shows a campsite owner the reservations for their campsites, split into
 * active (US 02.07.01), past (US 02.07.02) and cancelled (US 02.07.03).
 * Outstanding issues: none.
 */

/**
 * Lists reservations for the owner's campsites, one group at a time.
 *
 * @param repository where reservations are loaded from
 * @param ownerId the owner's device id
 * @param today today's date as "YYYY-MM-DD" (tests can pass a fixed date)
 * @param onReservationClick called when the owner taps a reservation
 */
@Composable
fun OwnerReservationsScreen(
    repository: ReservationRecordRepository,
    ownerId: String,
    modifier: Modifier = Modifier,
    today: String = todayString(),
    onReservationClick: (Reservation) -> Unit = {}
) {
    var loading by remember { mutableStateOf(true) }
    var allReservations by remember { mutableStateOf<List<Reservation>>(emptyList()) }

    // which group is showing: "active", "past" or "cancelled"
    var selectedTab by remember { mutableStateOf("active") }

    // load reservations for this owner's campsites when the screen opens
    LaunchedEffect(ownerId) {
        allReservations = repository.getReservationsForOwner(ownerId)
        loading = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Reservations",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // tab buttons: the selected one is filled, the others are outlined
        Row {
            TabButton("Active", selectedTab == "active", onClick = { selectedTab = "active" })
            Spacer(modifier = Modifier.width(8.dp))
            TabButton("Past", selectedTab == "past", onClick = { selectedTab = "past" })
            Spacer(modifier = Modifier.width(8.dp))
            TabButton("Cancelled", selectedTab == "cancelled", onClick = { selectedTab = "cancelled" })
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (loading) {
            CircularProgressIndicator()
        } else {
            // pick the list and empty message for the selected tab
            var shown = activeReservations(allReservations, today)
            var emptyMessage = "No active reservations."
            if (selectedTab == "past") {
                shown = pastReservations(allReservations, today)
                emptyMessage = "No past reservations."
            } else if (selectedTab == "cancelled") {
                shown = cancelledReservations(allReservations)
                emptyMessage = "No cancelled reservations."
            }

            if (shown.isEmpty()) {
                Text(emptyMessage)
            } else {
                for (reservation in shown) {
                    ReservationRow(
                        reservation,
                        showEntrant = true,
                        onClick = { onReservationClick(reservation) }
                    )
                }
            }
        }
    }
}

/**
 * One tab button. Filled if it's the selected tab, outlined if not.
 *
 * @param text the tab's label
 * @param selected true if this tab is showing
 * @param onClick called when the tab is tapped
 */
@Composable
private fun TabButton(text: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick) {
            Text(text)
        }
    } else {
        OutlinedButton(onClick = onClick) {
            Text(text)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OwnerReservationsScreenPreview() {
    OwnerReservationsScreen(
        repository = TempReservationRecordRepo(),
        ownerId = "fake-owner-01",
        today = "2026-11-25"
    )
}