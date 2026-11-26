package com.example.camp_serve.ui.owner.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camp_serve.data.NotificationRepository
import com.example.camp_serve.model.NotificationType
import com.example.camp_serve.model.SentNotification
import com.example.camp_serve.model.timeAgo
import com.example.camp_serve.model.toSentNotifications
import com.example.camp_serve.ui.owner.lotteries.CenteredLoading
import com.example.camp_serve.ui.owner.lotteries.CenteredMessage
import com.example.camp_serve.ui.owner.lotteries.LotteryHeader
import com.example.camp_serve.ui.owner.lotteries.LotteryRowTint
import com.example.camp_serve.ui.theme.CampPurple
import com.example.camp_serve.ui.theme.CampserveTheme
import kotlinx.coroutines.CancellationException

/*
 * SentNotificationsScreen.kt
 * The owner's "Notifications" tab: every notification they have sent, newest first
 * (US 02.10.02). This includes their waitlist messages (US 02.10.01) and the win
 * and lose notices the draw sent on their behalf (US 02.05.01).
 * Outstanding issues: the list loads once; reopen the tab to refresh it.
 */

/**
 * @param repository where notifications are loaded from
 * @param ownerId the signed-in owner
 */
@Composable
fun SentNotificationsScreen(
    repository: NotificationRepository,
    ownerId: String,
    modifier: Modifier = Modifier
) {
    var sent by remember { mutableStateOf<List<SentNotification>?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }

    LaunchedEffect(ownerId, retry) {
        loadFailed = false
        try {
            sent = repository.getSentBy(ownerId).toSentNotifications()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            loadFailed = true
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        LotteryHeader(title = "Sent notifications", onBack = null)
        val list = sent
        when {
            loadFailed -> Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Could not load notifications. Check your connection.", color = Color.Gray)
                TextButton(onClick = { retry++ }) { Text("Retry") }
            }
            list == null -> CenteredLoading()
            list.isEmpty() -> CenteredMessage("You haven't sent any notifications yet.")
            else -> SentNotificationList(list)
        }
    }
}

@Composable
private fun SentNotificationList(sent: List<SentNotification>) {
    val now = remember { System.currentTimeMillis() }
    LazyColumn(Modifier.fillMaxSize()) {
        itemsIndexed(sent, key = { _, item -> item.batchId }) { index, item ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (index % 2 == 0) LotteryRowTint else Color.Transparent)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(typeLabel(item.type), color = CampPurple, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(item.message, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(recipientsLine(item), fontSize = 12.sp, color = Color.Gray)
                Text(timeAgo(item.sentAt, now), fontSize = 12.sp, color = Color.Gray)
            }
            HorizontalDivider()
        }
    }
}

/** Says whether the owner wrote it or the draw sent it on their behalf. */
internal fun typeLabel(type: NotificationType): String = when (type) {
    NotificationType.OWNER -> "Waitlist message"
    NotificationType.SYSTEM_WIN -> "Draw result · winners (sent automatically)"
    NotificationType.SYSTEM_LOSE -> "Draw result · not selected (sent automatically)"
}

/** e.g. "Banff Ridge Camp · 5 recipients · 1 opted out". */
internal fun recipientsLine(item: SentNotification): String {
    val recipients = "${item.recipients} ${if (item.recipients == 1) "recipient" else "recipients"}"
    val optedOut = if (item.optedOut > 0) " · ${item.optedOut} opted out" else ""
    return "${item.campsiteName} · $recipients$optedOut"
}

@Preview(showBackground = true)
@Composable
private fun SentNotificationsPreview() {
    val now = System.currentTimeMillis()
    CampserveTheme {
        SentNotificationList(
            listOf(
                SentNotification("b1", NotificationType.OWNER, "l1", "Banff Ridge Camp",
                    "Registration closes tomorrow. Good luck!", now - 2 * 3_600_000, 5, 0),
                SentNotification("b2", NotificationType.SYSTEM_WIN, "l2", "Jasper Pines",
                    "You won the lottery for Jasper Pines!", now - 26 * 3_600_000, 2, 1)
            )
        )
    }
}
