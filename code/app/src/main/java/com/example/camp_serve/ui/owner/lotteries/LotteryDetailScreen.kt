package com.example.camp_serve.ui.owner.lotteries

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Person
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camp_serve.data.EntrantRepository
import com.example.camp_serve.data.LotteryRepository
import com.example.camp_serve.model.Lottery
import com.example.camp_serve.model.NotificationStatus
import com.example.camp_serve.model.WaitingList
import com.example.camp_serve.service.NotificationService
import com.example.camp_serve.ui.theme.CampPurple
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/*
 * LotteryDetailScreen.kt
 * One lottery as its owner sees it: its periods, number of winners and the live
 * waitlist of entrants with their names and when they joined (US 02.09.01),
 * plus the Edit Lottery option (US 02.04.06) while it can still be edited.
 * After registration closes it shows the draw result and winners (US 02.05.01),
 * and the owner can message everyone on the waitlist (US 02.10.01).
 */

/**
 * @param onEditClick called when the owner taps Edit Lottery
 * @param notificationService sends the owner's message to the waitlist; the
 *   Notify Waitlist button is hidden when this is null
 */
@Composable
fun LotteryDetailScreen(
    lotteryRepository: LotteryRepository,
    entrantRepository: EntrantRepository,
    ownerId: String,
    lotteryId: String,
    onEditClick: (String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    notificationService: NotificationService? = null
) {
    val scope = rememberCoroutineScope()
    var composing by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var sendError by remember { mutableStateOf<String?>(null) }
    var sentMessage by remember { mutableStateOf<String?>(null) }
    var lottery by remember { mutableStateOf<Lottery?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var names by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Live: re-emits whenever an entrant joins or leaves, or the settings change.
    LaunchedEffect(lotteryId, retry) {
        loading = true
        loadFailed = false
        try {
            lotteryRepository.observeLottery(lotteryId).collect {
                lottery = it?.takeIf { saved -> saved.ownerId == ownerId }
                loading = false
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            loadFailed = true
            loading = false
        }
    }

    val current = lottery
    val entries: List<WaitingList.Entry> = remember(current) {
        current?.getWaitingList(ownerId)?.sortedBy { it.timestamp } ?: emptyList()
    }

    // Look up names only for entrants we haven't seen yet.
    LaunchedEffect(entries.map { it.id }.toSet()) {
        val missing = entries.map { it.id }.filter { it !in names }
        if (missing.isEmpty()) return@LaunchedEffect
        try {
            val found = entrantRepository.getEntrantsByIds(missing).associate { it.id to it.name }
            names = names + missing.associateWith { found[it].orEmpty() }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Names are optional; rows fall back to a placeholder.
        }
    }

    // Refresh the Edit button and status when registration opens or closes.
    LaunchedEffect(current?.registrationStart, current?.registrationEnd) {
        val lot = current ?: return@LaunchedEffect
        while (true) {
            now = System.currentTimeMillis()
            val next = listOf(lot.registrationStart, lot.registrationEnd).filter { it > now }.minOrNull() ?: break
            delay(minOf(next - now, 60_000L).coerceAtLeast(1L).milliseconds)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        LotteryHeader(title = current?.title() ?: "Lottery", onBack = onBackClick)

        when {
            loading -> CenteredLoading()
            loadFailed -> Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Could not load the lottery. Check your connection and try again.", color = Color.Gray)
                TextButton(onClick = { retry++ }) { Text("Retry") }
            }
            current == null -> CenteredMessage("Lottery not found.")
            else -> {
                LotterySummary(current, entries.size, now)

                if (current.drawNumber > 0) {
                    DrawResult(
                        winners = current.winnerIds.map { names[it].orEmpty() },
                        entrants = entries.size
                    )
                }

                Text(
                    text = "Waitlist (${entries.size})",
                    color = CampPurple,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )

                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (entries.isEmpty()) {
                        Text(
                            "No entrants have joined the waitlist yet.",
                            color = Color.Gray,
                            modifier = Modifier.align(Alignment.Center).padding(24.dp)
                        )
                    } else {
                        LazyColumn(Modifier.fillMaxSize()) {
                            itemsIndexed(entries, key = { _, entry -> entry.id }) { index, entry ->
                                WaitlistRow(
                                    name = names[entry.id].orEmpty(),
                                    joinedAt = entry.timestamp,
                                    isWinner = entry.id in current.winnerIds,
                                    shaded = index % 2 == 0
                                )
                            }
                        }
                    }
                }

                sentMessage?.let {
                    Text(
                        it,
                        color = CampPurple,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (notificationService != null) {
                        PillButton(
                            "Notify Waitlist",
                            onClick = {
                                sendError = null
                                sentMessage = null
                                composing = true
                            }
                        )
                    }
                    if (current.isEditable(now)) {
                        PillButton("Edit Lottery", onClick = { onEditClick(current.id) })
                    } else {
                        Text(
                            "Registration has closed, so this lottery can no longer be edited.",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }

                if (composing && notificationService != null) {
                    NotifyWaitlistDialog(
                        sending = sending,
                        error = sendError,
                        onDismiss = { if (!sending) composing = false },
                        onSend = { message ->
                            if (message.isBlank()) {
                                sendError = NotificationService.EMPTY_MESSAGE
                                return@NotifyWaitlistDialog
                            }
                            if (entries.isEmpty()) {
                                sendError = "No one has joined the waitlist yet."
                                return@NotifyWaitlistDialog
                            }
                            sending = true
                            sendError = null
                            scope.launch {
                                try {
                                    val sent = notificationService.sendToWaitlist(current, ownerId, message)
                                    sentMessage = sentConfirmation(
                                        delivered = sent.count { it.status == NotificationStatus.SENT },
                                        optedOut = sent.count { it.status == NotificationStatus.OPTED_OUT }
                                    )
                                    composing = false
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: IllegalArgumentException) {
                                    sendError = e.message
                                } catch (_: Exception) {
                                    sendError = "Could not send. Check your connection and try again."
                                } finally {
                                    sending = false
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

/** The light purple block at the top: periods, winners, waitlist size and status. */
@Composable
private fun LotterySummary(lottery: Lottery, entrants: Int, now: Long) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LotteryRowTint)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        InfoLine("Registration", LotteryDates.formatPeriod(lottery.registrationStart, lottery.registrationEnd))
        InfoLine("Booking", LotteryDates.formatPeriod(lottery.bookingStart, lottery.bookingEnd))
        InfoLine("Winners", lottery.winnerCount.toString())
        InfoLine(
            "Waitlist",
            lottery.waitlistCapacity?.let { "$entrants of $it" } ?: "$entrants (no limit)"
        )
        InfoLine("Status", lottery.statusText(now))
        Spacer(Modifier.height(6.dp))
        Text(
            "Winners are drawn automatically when registration closes.",
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

/** One entrant on the waitlist: name and when they joined. */
@Composable
private fun WaitlistRow(name: String, joinedAt: Long, isWinner: Boolean, shaded: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (shaded) LotteryRowTint else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name.ifBlank { "Unnamed entrant" }, fontSize = 15.sp)
            Text(
                "Joined ${LotteryDates.formatInstant(joinedAt)}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isWinner) {
            Icon(
                Icons.Outlined.EmojiEvents,
                contentDescription = null,
                tint = CampPurple,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("Winner", color = CampPurple, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}


/** Confirmation shown after the owner sends a waitlist message (US 02.10.01). */
internal fun sentConfirmation(delivered: Int, optedOut: Int): String {
    val sent = "Notification sent to $delivered ${if (delivered == 1) "entrant" else "entrants"}."
    return if (optedOut == 0) sent
    else "$sent $optedOut opted out of notifications and did not receive it."
}

/**
 * Draw result for the owner (US 02.05.01): the winners, or a note that no one joined.
 *
 * @param winners winner names, blank when a name is unknown
 * @param entrants how many entrants were on the waitlist
 */
@Composable
private fun DrawResult(winners: List<String>, entrants: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text("Draw result", color = CampPurple, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        if (winners.isEmpty()) {
            Text(
                "No one joined the waitlist, so no winners were drawn.",
                color = Color.Gray,
                fontSize = 14.sp
            )
        } else {
            Text(
                "${winners.size} ${if (winners.size == 1) "winner" else "winners"} drawn from " +
                    "$entrants ${if (entrants == 1) "entrant" else "entrants"}. " +
                    "Results were sent to everyone with notifications turned on.",
                fontSize = 14.sp
            )
            winners.forEach { name ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(
                        Icons.Outlined.EmojiEvents,
                        contentDescription = null,
                        tint = CampPurple,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(name.ifBlank { "Unnamed entrant" }, fontSize = 14.sp)
                }
            }
        }
    }
}

/** Lets the owner write a message to everyone on the waitlist (US 02.10.01). */
@Composable
private fun NotifyWaitlistDialog(
    sending: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    var message by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notify waitlist") },
        text = {
            Column {
                Text(
                    "Everyone on this lottery's waitlist will get this message, " +
                        "except entrants who turned notifications off.",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    placeholder = { Text("Write a message") },
                    isError = error != null,
                    shape = RoundedCornerShape(8.dp),
                    colors = lotteryFieldColors(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp)
                )
                FieldError(error)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSend(message) }, enabled = !sending) {
                Text(if (sending) "Sending…" else "Send")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !sending) { Text("Cancel") }
        }
    )
}
