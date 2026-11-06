package com.example.camp_serve.service

import com.example.camp_serve.data.FirestoreLotteryRepo
import com.example.camp_serve.data.LotteryRepository
import com.example.camp_serve.model.Lottery
import com.example.camp_serve.model.LotteryResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import kotlin.random.Random

/**
 * Use the ID overloads for Firestore-backed draws. Results are committed before
 * callbacks run, and competing monitors cannot both commit the initial draw.
 *
 * Object overloads remain available for local previews/tests without persistence.
 * Scheduling runs in a caller-owned scope, not while the app process is stopped.
 * Notification callbacks are integration points while NotificationService is empty.
 */
class LotteryDrawService(
    private val scope: CoroutineScope,
    private val notificationService: NotificationService = NotificationService(),
    private val random: Random = Random.Default,
    private val clock: () -> Long = System::currentTimeMillis,
    private val notifyWinner: (NotificationService, Lottery, String) -> Unit = { _, _, _ -> },
    private val notifyLoser: (NotificationService, Lottery, String) -> Unit = { _, _, _ -> },
    private val onResult: (Lottery, LotteryResult) -> Unit = { _, _ -> },
    repository: LotteryRepository? = null
) {
    // Local tests do not initialize Firebase. Persistent calls use the app's default database.
    private val storage by lazy { repository ?: FirestoreLotteryRepo(clock = clock) }

    /** Monitors a saved lottery, waking at deadlines or when its stored schedule changes. */
    fun start(lotteryId: String): Job = scope.launch {
        val changes = Channel<Unit>(Channel.CONFLATED)
        val observer = launch {
            storage.observeChanges(lotteryId).collect { changes.trySend(Unit) }
        }
        try {
            while (isActive) {
                val progress = storage.advance(lotteryId, random)
                val lottery = progress.lottery
                progress.result?.let { publish(lottery, it) }
                if (lottery.state == Lottery.State.CLOSED) break
                // A document event wakes the monitor without cancelling a draw in progress.
                withTimeoutOrNull((nextChange(lottery) - clock()).coerceAtLeast(1L)) {
                    changes.receive()
                }
            }
        } finally {
            observer.cancel()
            changes.close()
        }
    }

    /** Draws a saved lottery immediately if due, persisting its result atomically. */
    suspend fun draw(lotteryId: String): LotteryResult {
        val progress = storage.advance(lotteryId, random)
        val result = checkNotNull(progress.result) { "The lottery is not ready for a draw." }
        return publish(progress.lottery, result)
    }

    /** Creates another saved result, retaining the previous draw in history. */
    suspend fun redraw(lotteryId: String, requesterId: String): LotteryResult {
        val progress = storage.redraw(lotteryId, requesterId, random)
        return publish(progress.lottery, checkNotNull(progress.result))
    }

    /** Monitors a local object only. Use start(lottery.id) for Firestore storage. */
    fun start(lottery: Lottery): Job = scope.launch {
        while (isActive) {
            val now = clock()
            lottery.drawIfReady(now, random)?.let {
                publish(lottery, resultFor(lottery, it, now))
            }
            if (lottery.refreshState(now) == Lottery.State.CLOSED) break
            delay((nextChange(lottery) - now).coerceIn(1L, 1_000L))
        }
    }

    /** Runs a local draw without storing it. */
    fun draw(lottery: Lottery): LotteryResult {
        val now = clock()
        val selection = checkNotNull(lottery.drawIfReady(now, random)) {
            "The lottery is not ready for a draw."
        }
        return publish(lottery, resultFor(lottery, selection, now))
    }

    /** Reselects local winners without storing the result. */
    fun redraw(lottery: Lottery, requesterId: String): LotteryResult {
        val now = clock()
        return publish(lottery, resultFor(lottery, lottery.redraw(requesterId, now, random), now))
    }

    private fun nextChange(lottery: Lottery): Long = when (lottery.state) {
        Lottery.State.OPEN -> lottery.registrationEnd
        Lottery.State.DRAWN -> lottery.bookingStart
        else -> lottery.bookingEnd
    }

    private fun resultFor(
        lottery: Lottery,
        selection: Pair<List<String>, List<String>>,
        now: Long
    ) = LotteryResult(
        id = UUID.randomUUID().toString(),
        lotteryId = lottery.id,
        winnerIds = selection.first,
        loserIds = selection.second,
        drawnAt = now,
        drawNumber = lottery.drawNumber
    )

    private fun publish(lottery: Lottery, result: LotteryResult): LotteryResult {
        result.winnerIds.forEach { notifyWinner(notificationService, lottery, it) }
        result.loserIds.forEach { notifyLoser(notificationService, lottery, it) }
        onResult(lottery, result)
        return result
    }
}
