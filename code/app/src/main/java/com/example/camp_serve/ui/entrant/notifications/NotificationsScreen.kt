package com.example.camp_serve.ui.entrant.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.camp_serve.data.EntrantNotificationRepository
import com.example.camp_serve.data.EntrantRepository
import com.example.camp_serve.data.sampleNotifications
import com.example.camp_serve.model.EntrantNotification
import com.example.camp_serve.model.EntrantNotificationType
import com.example.camp_serve.model.Entrant
import com.example.camp_serve.ui.theme.CampserveTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Loads the entrant's notifications and notification setting, then shows [NotificationsScreen].
 * Toggling the switch saves entrant.notificationEnabled.
 * Outstanding issues: Reserve is not connected to the Reserve screen yet.
 *
 * @param entrantNotificationRepository where notifications are loaded from
 * @param entrantRepository where the notification opt-out is saved
 * @param onReserve called when the entrant taps Reserve on a winning notification
 */
@Composable
fun NotificationsRoute(
    entrantNotificationRepository: EntrantNotificationRepository,
    entrantRepository: EntrantRepository,
    modifier: Modifier = Modifier,
    onReserve: (EntrantNotification) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var notifications by remember { mutableStateOf<List<EntrantNotification>>(emptyList()) }
    var entrant by remember { mutableStateOf(Entrant()) }

    LaunchedEffect(Unit) {
        try {
            notifications = entrantNotificationRepository.getNotifications()
            entrant = entrantRepository.getEntrant()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // offline: show what loaded instead of crashing
        }
        loading = false
    }

    if (loading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        NotificationsScreen(
            notifications = notifications,
            notificationsEnabled = entrant.notificationEnabled,
            onNotificationsToggle = { enabled ->
                scope.launch {
                    val updated = entrant.copy(notificationEnabled = enabled)
                    entrantRepository.updateEntrant(updated)
                    entrant = updated
                }
            },
            onReserve = onReserve,
            modifier = modifier
        )
    }
}

@Composable
fun NotificationsScreen(
    notifications: List<EntrantNotification>,
    notificationsEnabled: Boolean,
    onNotificationsToggle: (Boolean) -> Unit,
    onReserve: (EntrantNotification) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Notifications",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Receive notifications")
            Switch(checked = notificationsEnabled, onCheckedChange = onNotificationsToggle)
        }

        if (notifications.isEmpty()) {
            Text("No notifications yet.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(notifications) { notification -> NotificationCard(notification, onReserve) }
            }
        }
    }
}

@Composable
private fun NotificationCard(notification: EntrantNotification, onReserve: (EntrantNotification) -> Unit) {
    val title = when (notification.type) {
        EntrantNotificationType.WON -> "You won!"
        EntrantNotificationType.LOST -> "Not selected"
        EntrantNotificationType.RESERVED -> "Reservation confirmed"
        EntrantNotificationType.CANCELLED -> "Reservation cancelled"
        EntrantNotificationType.MESSAGE -> "Message from the campsite owner"
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(notification.campsite, style = MaterialTheme.typography.titleMedium)
            Text(notification.message, style = MaterialTheme.typography.bodyMedium)
            if (notification.type == EntrantNotificationType.WON) {
                Button(onClick = { onReserve(notification) }, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Reserve")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NotificationsScreenPreview() {
    CampserveTheme {
        NotificationsScreen(
            notifications = sampleNotifications,
            notificationsEnabled = true,
            onNotificationsToggle = {},
            onReserve = {}
        )
    }
}