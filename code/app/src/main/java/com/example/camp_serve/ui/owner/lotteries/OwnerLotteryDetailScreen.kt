/*
 * OwnerLotteryDetailScreen.kt shows one lottery with its live waitlist, an "Edit lottery"
 * button and a "Send notifications" button (US 02.04.06, 02.09.01, 02.07.01).
 * Outstanding issues: sent notifications are only saved to the owner's Alerts list; they are
 * not delivered to entrants yet.
 */

package com.example.camp_serve.ui.owner.lotteries

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.camp_serve.data.CampsiteRepository
import com.example.camp_serve.data.LotteryRepository
import com.example.camp_serve.data.SentNotificationRepository
import com.example.camp_serve.model.Lottery
import com.example.camp_serve.model.SentNotification
import com.example.camp_serve.model.WaitingEntry
import com.example.camp_serve.service.LotteryDrawService
import com.example.camp_serve.ui.lottery.LotteryInfoSection
import com.example.camp_serve.util.formatDisplayDateTime
import kotlinx.coroutines.launch
import java.util.Date

/**
 * Lottery detail with the waitlist.
 *
 * @param lotteryId lottery to show
 * @param ownerId owner using the app
 * @param campsiteRepository used for campsite names
 * @param lotteryRepository lottery and waitlist data
 * @param notificationRepository where sent notifications are recorded
 * @param onEditClick called when the owner taps "Edit lottery"
 * @param onNotificationSent called with a confirmation message after sending
 */
@Composable
fun OwnerLotteryDetailScreen(
    lotteryId: String,
    ownerId: String,
    campsiteRepository: CampsiteRepository,
    lotteryRepository: LotteryRepository,
    notificationRepository: SentNotificationRepository,
    modifier: Modifier = Modifier,
    onEditClick: (Lottery) -> Unit = {},
    onNotificationSent: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var lottery by remember { mutableStateOf<Lottery?>(null) }
    var title by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    val entries by remember(lotteryId) { lotteryRepository.observeEntries(lotteryId) }
        .collectAsState(initial = emptyList<WaitingEntry>())

    LaunchedEffect(lotteryId) {
        lottery = lotteryRepository.getLottery(lotteryId)
        val found = lottery
        if (found != null) {
            val names = campsiteRepository.getCampsitesForOwner(ownerId).associate { it.id to it.name }
            title = found.campsiteIds.map { names[it] ?: it }.joinToString(", ")
        }
    }

    val current = lottery
    if (current == null) {
        Text("Lottery not found.", modifier = modifier.padding(16.dp))
        return
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary)
        LotteryInfoSection(current)
        Text("Winners are drawn automatically when registration closes.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        val capacityText = current.waitlistCapacity?.let { " of $it" } ?: ""
        Text("Waitlist (${entries.size}$capacityText)", fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium)
        Text("Total entrants: ${entries.size}", modifier = Modifier.testTag("total_entrants"))
        if (entries.isEmpty()) {
            Text("No entrants have joined yet.", modifier = Modifier.testTag("waitlist_empty"))
        }
        for (entry in entries) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(entry.entrantName)
                Text("Joined ${formatDisplayDateTime(entry.joinedAt)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (current.canEdit()) {
            Button(onClick = { onEditClick(current) },
                modifier = Modifier.fillMaxWidth().testTag("edit_lottery")) {
                Text("Edit lottery")
            }
        } else {
            Text("This lottery has been drawn and can no longer be edited.",
                modifier = Modifier.testTag("cannot_edit"))
        }
        OutlinedButton(onClick = { showDialog = true },
            modifier = Modifier.fillMaxWidth().testTag("send_notifications")) {
            Text("Send notifications")
        }

        if (SHOW_DEMO_TOOLS && current.canEdit()) {
            Text("Demo tools", style = MaterialTheme.typography.labelLarge)
            OutlinedButton(onClick = {
                scope.launch {
                    val n = entries.size + 1
                    lotteryRepository.joinWaitlist(
                        lotteryId, WaitingEntry("demo-$n", "Demo Entrant $n", Date()),
                        now = Date(maxOf(System.currentTimeMillis(), current.registrationStart.time))
                    )
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Add sample entrant") }
            OutlinedButton(onClick = {
                scope.launch {
                    LotteryDrawService(lotteryRepository).drawIfDue(lotteryId, force = true)
                    lottery = lotteryRepository.getLottery(lotteryId)
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Draw winners now") }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Send notification") },
            text = {
                OutlinedTextField(draft, { draft = it }, label = { Text("Message to ${entries.size} entrants") },
                    modifier = Modifier.testTag("notification_text"))
            },
            confirmButton = {
                TextButton(onClick = {
                    if (draft.isNotBlank()) {
                        scope.launch {
                            notificationRepository.add(
                                SentNotification(
                                    ownerId = ownerId, lotteryId = lotteryId, campsiteName = title,
                                    message = draft.trim(), recipientCount = entries.size, sentAt = Date()
                                )
                            )
                            draft = ""
                            showDialog = false
                            onNotificationSent("Notification sent")
                        }
                    }
                }) { Text("Send") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } }
        )
    }
}
