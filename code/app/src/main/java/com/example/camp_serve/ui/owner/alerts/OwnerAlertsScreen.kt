/*
 * OwnerAlertsScreen.kt is the owner's Alerts tab: the notifications the owner has sent, with
 * campsite, recipient count and time (US 02.07.01, 02.07.02, 02.10.02).
 * Outstanding issues: none.
 */

package com.example.camp_serve.ui.owner.alerts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
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
import com.example.camp_serve.data.SentNotificationRepository
import com.example.camp_serve.data.TempSentNotificationRepo
import com.example.camp_serve.model.SentNotification
import com.example.camp_serve.util.timeAgo

/**
 * Sent notifications list.
 *
 * @param ownerId owner using the app
 * @param repository where sent notifications are loaded from
 */
@Composable
fun OwnerAlertsScreen(
    ownerId: String,
    repository: SentNotificationRepository,
    modifier: Modifier = Modifier
) {
    var items by remember { mutableStateOf(listOf<SentNotification>()) }

    LaunchedEffect(ownerId) {
        items = repository.getForOwner(ownerId)
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Alerts", style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary)
        Text("Sent notifications", style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 4.dp))

        if (items.isEmpty()) {
            Text("You haven't sent any notifications yet.", modifier = Modifier.padding(top = 16.dp))
        }
        LazyColumn(modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items) { n ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(n.message, fontWeight = FontWeight.Bold)
                        Text(n.campsiteName)
                        Text("${n.recipientCount} recipients · ${timeAgo(n.sentAt)}")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OwnerAlertsScreenPreview() {
    OwnerAlertsScreen("fake-owner-01", TempSentNotificationRepo())
}
