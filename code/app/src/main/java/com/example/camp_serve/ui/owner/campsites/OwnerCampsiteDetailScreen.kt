package com.example.camp_serve.ui.owner.campsites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.example.camp_serve.data.CampsiteRepository
import com.example.camp_serve.data.LotteryRepository
import com.example.camp_serve.data.ReservationRecordRepository
import com.example.camp_serve.data.TempCampsiteRepo
import com.example.camp_serve.model.Campsite
import com.example.camp_serve.model.Lottery
import com.example.camp_serve.model.Reservation
import com.example.camp_serve.model.activeReservations
import com.example.camp_serve.model.todayString
import com.example.camp_serve.ui.owner.lotteries.LotteryListRow
import com.example.camp_serve.ui.owner.lotteries.PillButton
import kotlinx.coroutines.CancellationException

/*
 * OwnerCampsiteDetailScreen.kt
 * Shows all the details of one of the owner's campsites (US 02.01.04).
 * Has an Edit button that will open the edit screen (US 02.01.03), a Create Lottery
 * button (US 02.04.01), the dates already booked (US 02.06.01) and a list of
 * this campsite's lotteries.
 * TODO: show the map location, and image when those are added.
 */

/**
 * Shows one campsite's saved details to its owner.
 *
 * @param repository where the campsite is loaded from
 * @param campsiteId id of the campsite to show
 * @param onEditClick called when the owner taps Edit
 * @param onBackClick called when the owner taps Back
 * @param lotteryRepository where this campsite's lotteries are loaded from; null hides them
 * @param ownerId the signed-in owner, needed to load their lotteries
 * @param onCreateLotteryClick called when the owner taps Create Lottery
 * @param onLotteryClick called when the owner taps one of this campsite's lotteries
 * @param reservationRepository where this campsite's reservations are loaded from; null hides booked dates
 * @param today today's date as "YYYY-MM-DD" (tests can pass a fixed date)
 */
@Composable
fun OwnerCampsiteDetailScreen(
    repository: CampsiteRepository,
    campsiteId: String,
    modifier: Modifier = Modifier,
    onEditClick: (Campsite) -> Unit = {},
    onBackClick: () -> Unit = {},
    lotteryRepository: LotteryRepository? = null,
    ownerId: String? = null,
    onCreateLotteryClick: (Campsite) -> Unit = {},
    onLotteryClick: (Lottery) -> Unit = {},
    reservationRepository: ReservationRecordRepository? = null,
    today: String = todayString()
) {
    var loading by remember { mutableStateOf(true) }
    var campsite by remember { mutableStateOf<Campsite?>(null) }
    var lotteries by remember { mutableStateOf<List<Lottery>>(emptyList()) }
    var booked by remember { mutableStateOf<List<Reservation>>(emptyList()) }

    // this campsite's lotteries, kept up to date
    LaunchedEffect(campsiteId, ownerId, lotteryRepository) {
        if (lotteryRepository == null || ownerId.isNullOrBlank()) return@LaunchedEffect
        try {
            lotteryRepository.observeLotteriesForOwner(ownerId).collect { all ->
                lotteries = all.filter { lottery -> lottery.campsites.any { it.id == campsiteId } }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            lotteries = emptyList()
        }
    }

    // load the campsite when the screen opens
    LaunchedEffect(campsiteId) {
        campsite = repository.getCampsite(campsiteId)
        loading = false
    }

    // load the booked (active) reservations for this campsite (US 02.06.01)
    LaunchedEffect(campsiteId, reservationRepository) {
        if (reservationRepository != null) {
            val all = reservationRepository.getReservationsForCampsite(campsiteId)
            // earliest dates first
            booked = activeReservations(all, today).sortedBy { it.startDate }
        }
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

            Spacer(modifier = Modifier.height(8.dp))

            // action buttons stacked under the name, like the storyboard
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton("Create Lottery", onClick = { onCreateLotteryClick(site) })
                PillButton("Edit Campsite", onClick = { onEditClick(site) })
                // opens the QR code with Save and Share (US 02.02.01)
                DownloadQrCodeButton(campsite = site)
            }

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

            // booked dates, only if we were given a reservation repository (US 02.06.01)
            if (reservationRepository != null) {
                Text(text = "Booked dates", fontWeight = FontWeight.Bold)
                if (booked.isEmpty()) {
                    Text("No upcoming bookings.")
                } else {
                    for (reservation in booked) {
                        Text(reservation.startDate + " to " + reservation.endDate + " (" + reservation.entrantName + ")")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (lotteries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Lotteries",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    lotteries.forEach { lottery ->
                        LotteryListRow(lottery, onClick = { onLotteryClick(lottery) })
                    }
                }
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