package com.example.camp_serve.ui.entrant.notifications

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.camp_serve.data.EntrantRepository
import com.example.camp_serve.data.NotificationRepository
import com.example.camp_serve.model.pendingPushes
import com.example.camp_serve.service.PushHistory
import com.example.camp_serve.service.PushNotifier
import kotlinx.coroutines.CancellationException

/*
 * PushNotificationListener.kt
 * Watches this device's notifications in Firestore and shows each new one as a
 * device notification (#101), for win/lose results (US 01.03.01, 01.03.02) and
 * owner messages (US 02.10.01).
 * Notifications saved as OPTED_OUT never pop up, and nothing pops up while the
 * entrant has notifications turned off (US 01.10.01).
 * Outstanding issues: see PushNotifier (needs the app process to be running).
 */

/**
 * Put this once near the top of the app. It asks for notification permission on
 * Android 13+ and then posts device notifications as they arrive.
 *
 * @param entrantId this device's entrant id
 * @param notificationRepository where notifications are saved
 * @param entrantRepository used to read the entrant's notification setting
 */
@Composable
fun PushNotificationListener(
    entrantId: String,
    notificationRepository: NotificationRepository,
    entrantRepository: EntrantRepository
) {
    val context = LocalContext.current.applicationContext
    val history = remember(context) { PushHistory(context) }
    val permission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* if denied, show() simply does nothing */ }

    LaunchedEffect(Unit) {
        PushNotifier.createChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && PushNotifier.needsPermission(context)) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(entrantId) {
        val since = history.since()
        try {
            notificationRepository.observeForRecipient(entrantId).collect { notifications ->
                val pending = pendingPushes(notifications, history.shownIds(), since)
                if (pending.isEmpty()) return@collect
                // Check the latest setting, in case it changed after the notice was saved.
                val enabled = try {
                    entrantRepository.getEntrantsByIds(listOf(entrantId))
                        .firstOrNull()?.notificationEnabled ?: true
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    true
                }
                if (enabled) pending.forEach { PushNotifier.show(context, it) }
                // Skipped ones are marked too, so turning notifications back on
                // does not pop up old ones.
                history.markShown(pending.map { it.id })
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Offline or not permitted; notifications still appear on the Notifications screen later.
        }
    }
}
