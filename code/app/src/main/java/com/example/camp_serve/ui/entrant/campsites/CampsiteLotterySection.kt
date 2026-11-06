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

/** Entrant registration controls. Joining and leaving both require confirmation. */
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
    var pendingLeave by rememberSaveable { mutableStateOf(false) }
    var leftWaitlist by rememberSaveable { mutableStateOf(false) }
    var updating by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var membershipCheckedFor by remember { mutableStateOf<String?>(null) }
    var membershipError by remember { mutableStateOf(false) }
    var membershipRetry by remember { mutableIntStateOf(0) }

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
    val checkingMembership = activeLottery != null && membershipCheckedFor != activeLottery.id

    LaunchedEffect(activeLottery?.id, storage, membershipRetry) {
        membershipCheckedFor = null
        membershipError = false
        val lottery = activeLottery ?: return@LaunchedEffect
        val source = storage ?: return@LaunchedEffect
        try {
            val saved = checkNotNull(source.getLottery(lottery.id)) { "Lottery not found." }
            joinedLotteryId = if (saved.isOnWaitingList(entrantId)) lottery.id else null
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            membershipError = true
        }
        membershipCheckedFor = lottery.id
    }

    LaunchedEffect(activeLottery?.id) {
        if (pendingLotteryId != activeLottery?.id) pendingLotteryId = null
        actionError = null
        leftWaitlist = false
    }

    Column {
        Button(
            onClick = {
                val lottery = activeLottery ?: return@Button
                now = System.currentTimeMillis()
                if (now >= lottery.registrationStart && now < lottery.registrationEnd) {
                    actionError = null
                    pendingLeave = alreadyJoined
                    pendingLotteryId = lottery.id
                }
            },
            enabled = !loading && !loadError && activeLottery != null &&
                !checkingMembership && !membershipError && !updating,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                when {
                    loading -> "Checking lottery…"
                    loadError -> "Lottery unavailable"
                    updating -> if (pendingLeave) "Leaving…" else "Joining…"
                    activeLottery == null -> "No active lottery"
                    membershipError -> "Waitlist unavailable"
                    checkingMembership -> "Checking waitlist…"
                    alreadyJoined -> "Leave waitlist"
                    else -> "Join waitlist"
                }
            )
        }

        if (alreadyJoined) {
            Text("You have joined this lottery's waitlist.")
        } else if (leftWaitlist) {
            Text("You have left this lottery's waitlist.")
        }
        if (loadError) {
            Text(
                "Could not load the lottery. Check your connection and try again.",
                color = MaterialTheme.colorScheme.error
            )
            TextButton(onClick = { retry++ }) { Text("Retry") }
        }
        if (!loadError && activeLottery != null && membershipError) {
            Text(
                "Could not check your waitlist registration. Check your connection and try again.",
                color = MaterialTheme.colorScheme.error
            )
            TextButton(onClick = { membershipRetry++ }) { Text("Retry") }
        }
        actionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }

    val lotteryToUpdate = activeLottery?.takeIf { it.id == pendingLotteryId }
    if (lotteryToUpdate != null) {
        val leaving = pendingLeave
        AlertDialog(
            onDismissRequest = { pendingLotteryId = null },
            title = { Text(if (leaving) "Leave waitlist?" else "Join waitlist?") },
            text = {
                Text(
                    if (leaving) "Leave the lottery waitlist for ${campsite.name}?"
                    else "Join the lottery waitlist for ${campsite.name}?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (updating || pendingLotteryId != lotteryToUpdate.id) return@TextButton
                    pendingLotteryId = null
                    updating = true
                    actionError = null
                    scope.launch {
                        try {
                            val source = checkNotNull(storage)
                            if (leaving) {
                                source.leaveLottery(lotteryToUpdate.id, entrantId)
                            } else {
                                source.joinLottery(lotteryToUpdate.id, entrantId)
                            }
                            joinedLotteryId = if (leaving) null else lotteryToUpdate.id
                            leftWaitlist = leaving
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            actionError = waitlistError(e, leaving)
                        } finally {
                            updating = false
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

private fun waitlistError(error: Exception, leaving: Boolean): String {
    val cause = generateSequence<Throwable>(error) { it.cause }.last()
    return when (cause.message) {
        "Registration is not open.", "Registration has ended." -> "Registration is no longer open."
        "Waiting list is full." -> "This lottery's waitlist is full."
        "Entrant has already joined." -> "You have already joined this lottery's waitlist."
        "Lottery not found." -> "This lottery is no longer available."
        else -> "Could not ${if (leaving) "leave" else "join"} the waitlist. " +
            "Check your connection and try again."
    }
}
