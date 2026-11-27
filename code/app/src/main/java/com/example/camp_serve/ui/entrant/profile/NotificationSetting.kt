package com.example.camp_serve.ui.entrant.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.camp_serve.data.EntrantRepository
import com.example.camp_serve.model.Entrant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/*
 * NotificationSetting.kt
 * The "Receive notifications" switch on the entrant's profile.
 * Turning it off saves entrant.notificationEnabled = false, so the app and campsite
 * owners stop sending this entrant notifications; turning it on starts them again.
 * Outstanding issues: none.
 */

/**
 * Switch that turns notifications on or off and saves the choice to the profile.
 *
 * @param entrant the entrant's current profile
 * @param repository where the profile is saved
 * @param onSaved called with the updated profile once it is saved
 */
@Composable
fun NotificationSetting(
    entrant: Entrant,
    repository: EntrantRepository,
    onSaved: (Entrant) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Receive notifications", fontWeight = FontWeight.Bold)
                Text(
                    "Lottery results and messages from campsite owners",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = entrant.notificationEnabled,
                enabled = !saving,
                onCheckedChange = { enabled ->
                    saving = true
                    message = null
                    scope.launch {
                        val updated = entrant.copy(notificationEnabled = enabled)
                        message = try {
                            repository.updateEntrant(updated)
                            onSaved(updated)
                            notificationToggleMessage(enabled)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            "Could not change your notification setting. Check your connection."
                        } finally {
                            saving = false
                        }
                    }
                }
            )
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }
}

/** Confirmation shown after the entrant turns notifications on or off. */
fun notificationToggleMessage(enabled: Boolean): String =
    if (enabled) "Notifications are on. You will receive lottery results and owner messages."
    else "Notifications are off. You won't receive notifications from the app or campsite owners."
