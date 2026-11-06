/*
 * ChatScreen.kt is the owner's message screen for one reservation (US 02.12.01).
 * Outstanding issues: entrant replies are not wired up yet.
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.camp_serve.data.MessageRepository
import com.example.camp_serve.data.ReservationRepository
import com.example.camp_serve.model.ChatMessage
import com.example.camp_serve.model.Reservation
import com.example.camp_serve.util.formatDisplayDate
import kotlinx.coroutines.launch

/**
 * Chat with the entrant of one reservation.
 *
 * @param reservationId reservation the chat is about
 * @param reservationRepository used for the header (entrant, campsite, dates)
 * @param messageRepository where messages are read and sent
 */
@Composable
fun ChatScreen(
    reservationId: String,
    reservationRepository: ReservationRepository,
    messageRepository: MessageRepository,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var reservation by remember { mutableStateOf<Reservation?>(null) }
    var text by remember { mutableStateOf("") }
    val messages by remember(reservationId) { messageRepository.observeMessages(reservationId) }
        .collectAsState(initial = emptyList<ChatMessage>())

    LaunchedEffect(reservationId) {
        reservation = reservationRepository.getReservation(reservationId)
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        val r = reservation
        Text(r?.entrantName ?: "Chat", style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary)
        if (r != null) {
            Text("${r.campsiteName} · ${formatDisplayDate(r.startDate)} to ${formatDisplayDate(r.endDate)}")
        }

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(messages) { m ->
                Column(modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (m.fromOwner) Alignment.End else Alignment.Start) {
                    Text(if (m.fromOwner) "Owner (You)" else (r?.entrantName ?: "Entrant"),
                        style = MaterialTheme.typography.labelSmall)
                    Card { Text(m.text, modifier = Modifier.padding(10.dp)) }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(text, { text = it }, label = { Text("Type a message") },
                modifier = Modifier.weight(1f).testTag("message_box"))
            Button(onClick = {
                val toSend = text
                text = ""
                scope.launch { messageRepository.sendMessage(reservationId, toSend, true) }
            }, modifier = Modifier.padding(start = 8.dp).testTag("send_message")) { Text("Send") }
        }
    }
}
