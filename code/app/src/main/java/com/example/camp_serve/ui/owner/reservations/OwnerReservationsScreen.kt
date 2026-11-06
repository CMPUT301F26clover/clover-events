/*
 * OwnerReservationsScreen.kt is the owner's Reservations tab with Active / Past / Cancelled
 * tabs, and Message and Reject buttons on active reservations (US 02.07.01, 02.07.02,
 * 02.07.03, 02.08.01, 02.12.01).
 * Outstanding issues: the rejected entrant is only notified in the owner's sent list.
 */

package com.example.camp_serve.ui.owner.reservations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.camp_serve.data.ReservationRepository
import com.example.camp_serve.data.SentNotificationRepository
import com.example.camp_serve.model.Reservation
import com.example.camp_serve.model.ReservationStatus
import com.example.camp_serve.model.SentNotification
import com.example.camp_serve.util.formatDisplayDate
import com.example.camp_serve.util.nightsBetween
import kotlinx.coroutines.launch
import java.util.Date

private val TAB_NAMES = listOf("Active", "Past", "Cancelled")

/**
 * The owner's reservations.
 *
 * @param ownerId owner using the app
 * @param reservationRepository where reservations are loaded and rejected
 * @param notificationRepository where the "reservation cancelled" notice is recorded
 * @param onMessageClick called with the reservation when the owner taps Message
 * @param onMessage called with a short confirmation text (e.g. after rejecting)
 */
@Composable
fun OwnerReservationsScreen(
    ownerId: String,
    reservationRepository: ReservationRepository,
    notificationRepository: SentNotificationRepository,
    modifier: Modifier = Modifier,
    onMessageClick: (Reservation) -> Unit = {},
    onMessage: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var reservations by remember { mutableStateOf(listOf<Reservation>()) }
    var tab by remember { mutableStateOf(0) }

    LaunchedEffect(ownerId) {
        reservations = reservationRepository.getReservationsForOwner(ownerId)
    }

    val shown = when (tab) {
        0 -> reservations.filter { it.isActive() }
        1 -> reservations.filter { it.status == ReservationStatus.COMPLETED }
        else -> reservations.filter { it.isCancelled() }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Reservations", style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary)
        TabRow(selectedTabIndex = tab) {
            TAB_NAMES.forEachIndexed { index, label ->
                Tab(selected = tab == index, onClick = { tab = index }, text = { Text(label) })
            }
        }

        if (shown.isEmpty()) {
            Text("Nothing here.", modifier = Modifier.padding(top = 16.dp))
        }
        LazyColumn(modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(shown) { r ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(r.entrantName, fontWeight = FontWeight.Bold)
                        Text(r.campsiteName)
                        Text("${formatDisplayDate(r.startDate)} to ${formatDisplayDate(r.endDate)} · ${nightsBetween(r.startDate, r.endDate)} nights")
                        Text(r.status.label, color = MaterialTheme.colorScheme.primary)
                        if (r.isActive()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 8.dp)) {
                                OutlinedButton(onClick = { onMessageClick(r) },
                                    modifier = Modifier.testTag("message_${r.id}")) { Text("Message") }
                                Button(onClick = {
                                    scope.launch {
                                        if (reservationRepository.reject(r.id)) {
                                            notificationRepository.add(
                                                SentNotification(
                                                    ownerId = ownerId, campsiteName = r.campsiteName,
                                                    message = "Your reservation was cancelled by the owner",
                                                    recipientCount = 1, sentAt = Date()
                                                )
                                            )
                                            reservations = reservationRepository.getReservationsForOwner(ownerId)
                                            onMessage("Reservation rejected")
                                        }
                                    }
                                }, modifier = Modifier.testTag("reject_${r.id}")) { Text("Reject") }
                            }
                        }
                    }
                }
            }
        }
    }
}
