package com.example.camp_serve.service

import com.example.camp_serve.data.FirestoreLotteryRepo
import com.example.camp_serve.data.LotteryRepository
import com.example.camp_serve.model.Lottery
import com.example.camp_serve.model.LotteryResult
import kotlinx.coroutines.CancellationException
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
 *
 * After a saved draw, [onDrawSaved] runs once with the committed result; the owner app
 * passes [NotificationService.sendDrawResult] so entrants are notified.
 * It is retried a few times if it fails, and a failure never undoes the saved draw
 * or stops the monitor.
 */
class LotteryDrawService(
    private val scope: CoroutineScope,
    private val notificationService: NotificationService = NotificationService(),
    private val random: Random = Random.Default,
    private val clock: () -> Long = System::currentTimeMillis,
    private val notifyWinner: (NotificationService, Lottery, String) -> Unit = { _, _, _ -> },
    private val notifyLoser: (NotificationService, Lottery, String) -> Unit = { _, _, _ -> },
    private val onResult: (Lottery, LotteryResult) -> Unit = { _, _ -> },
    repository: LotteryRepository? = null,
    private val onDrawSaved: (suspend (Lottery, LotteryResult) -> Unit)? = null,
    private val retryDelayMillis: Long = 2_000
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
                progress.result?.let { publishSaved(lottery, it) }
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
        return publishSaved(progress.lottery, result)
    }

    /** Creates another saved result, retaining the previous draw in history. */
    suspend fun redraw(lotteryId: String, requesterId: String): LotteryResult {
        val progress = storage.redraw(lotteryId, requesterId, random)
        return publishSaved(progress.lottery, checkNotNull(progress.result))
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

    /** Publishes a saved result, then runs [onDrawSaved] (with retries). */
    private suspend fun publishSaved(lottery: Lottery, result: LotteryResult): LotteryResult {
        publish(lottery, result)
        val hook = onDrawSaved ?: return result
        repeat(DRAW_HOOK_ATTEMPTS) { attempt ->
            try {
                hook(lottery, result)
                return result
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (attempt < DRAW_HOOK_ATTEMPTS - 1) delay(retryDelayMillis)
            }
        }
        return result
    }

    private fun publish(lottery: Lottery, result: LotteryResult): LotteryResult {
        result.winnerIds.forEach { notifyWinner(notificationService, lottery, it) }
        result.loserIds.forEach { notifyLoser(notificationService, lottery, it) }
        onResult(lottery, result)
        return result
    }

    private companion object {
        const val DRAW_HOOK_ATTEMPTS = 3
    }
}
