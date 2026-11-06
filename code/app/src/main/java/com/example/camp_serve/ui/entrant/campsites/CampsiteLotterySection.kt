package com.example.camp_serve.ui.entrant.campsites

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.camp_serve.data.DeviceId
import com.example.camp_serve.data.FirestoreLotteryRepo
import com.example.camp_serve.data.LotteryRepository
import com.example.camp_serve.model.Campsite
import com.example.camp_serve.model.Lottery
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/** Entrant registration controls. Only a confirmed action writes to the waiting list. */
@Composable
internal fun CampsiteLotterySection(
    campsite: Campsite,
    repository: LotteryRepository?
) {
    val context = LocalContext.current
    val entrantId = remember(context) { DeviceId(context).get() }
    val scope = rememberCoroutineScope()
    var storage by remember { mutableStateOf<LotteryRepository?>(null) }
    var lotteries by remember { mutableStateOf<List<Lottery>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var pendingLotteryId by rememberSaveable { mutableStateOf<String?>(null) }
    var joinedLotteryId by rememberSaveable { mutableStateOf<String?>(null) }
    var joining by remember { mutableStateOf(false) }
    var joinError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(campsite.id, campsite.ownerId, repository, retry) {
        loading = true
        loadError = false
        try {
            // Initialize inside the effect so configuration/load failures are shown in the UI.
            val source = repository ?: FirestoreLotteryRepo()
            storage = source
            source.observeOpenLotteriesForCampsite(campsite.id, campsite.ownerId)
                .collect {
                    lotteries = it
                    loading = false
                }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            lotteries = emptyList()
            loading = false
            loadError = true
        }
    }

    // Registration can start or end without any document being edited.
    LaunchedEffect(lotteries) {
        while (true) {
            now = System.currentTimeMillis()
            val nextBoundary = lotteries
                .flatMap { listOf(it.registrationStart, it.registrationEnd) }
                .filter { it > now }
                .minOrNull() ?: break
            delay(minOf(nextBoundary - now, 60_000L).coerceAtLeast(1L).milliseconds)
        }
    }

    val activeLottery = lotteries
        .filter {
            it.state == Lottery.State.OPEN &&
                now >= it.registrationStart && now < it.registrationEnd
        }
        .minWithOrNull(compareBy<Lottery> { it.registrationEnd }.thenBy { it.id })
    val alreadyJoined = activeLottery != null && joinedLotteryId == activeLottery.id

    LaunchedEffect(activeLottery?.id) {
        if (pendingLotteryId != activeLottery?.id) pendingLotteryId = null
        joinError = null
    }

    Column {
        Button(
            onClick = {
                val lottery = activeLottery ?: return@Button
                now = System.currentTimeMillis()
                if (now >= lottery.registrationStart && now < lottery.registrationEnd) {
                    joinError = null
                    pendingLotteryId = lottery.id
                }
            },
            enabled = !loading && !loadError && activeLottery != null &&
                !alreadyJoined && !joining,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                when {
                    loading -> "Checking lottery…"
                    loadError -> "Lottery unavailable"
                    joining -> "Joining…"
                    activeLottery == null -> "No active lottery"
                    alreadyJoined -> "Joined waitlist"
                    else -> "Join waitlist"
                }
            )
        }

        if (alreadyJoined) {
            Text("You have joined this lottery's waitlist.")
        }
        if (loadError) {
            Text(
                "Could not load the lottery. Check your connection and try again.",
                color = MaterialTheme.colorScheme.error
            )
            TextButton(onClick = { retry++ }) { Text("Retry") }
        }
        joinError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }

    val lotteryToJoin = activeLottery?.takeIf { it.id == pendingLotteryId }
    if (lotteryToJoin != null) {
        AlertDialog(
            onDismissRequest = { pendingLotteryId = null },
            title = { Text("Join waitlist?") },
            text = { Text("Join the lottery waitlist for ${campsite.name}?") },
            confirmButton = {
                TextButton(onClick = {
                    if (joining || pendingLotteryId != lotteryToJoin.id) return@TextButton
                    pendingLotteryId = null
                    joining = true
                    joinError = null
                    scope.launch {
                        try {
                            val source = checkNotNull(storage)
                            source.joinLottery(lotteryToJoin.id, entrantId)
                            joinedLotteryId = lotteryToJoin.id
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            joinError = waitlistError(e)
                        } finally {
                            joining = false
                            now = System.currentTimeMillis()
                        }
                    }
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingLotteryId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun waitlistError(error: Exception): String {
    val cause = generateSequence<Throwable>(error) { it.cause }.last()
    return when (cause.message) {
        "Registration is not open." -> "Registration is no longer open."
        "Waiting list is full." -> "This lottery's waitlist is full."
        "Entrant has already joined." -> "You have already joined this lottery's waitlist."
        "Lottery not found." -> "This lottery is no longer available."
        else -> "Could not join the waitlist. Check your connection and try again."
    }
}
