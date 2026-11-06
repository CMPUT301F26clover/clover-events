package com.example.camp_serve

import com.example.camp_serve.data.LotteryRepository
import com.example.camp_serve.model.Campsite
import com.example.camp_serve.model.Lottery
import com.example.camp_serve.model.LotteryResult
import com.example.camp_serve.model.WaitingList
import com.example.camp_serve.service.LotteryDrawService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class PersistentLotteryDrawServiceTest {
    private fun lottery(scale: Long = 1) = Lottery(
        id = "lottery", ownerId = "owner",
        campsites = listOf(Campsite(id = "site", ownerId = "owner")),
        registrationStart = 100 * scale, registrationEnd = 200 * scale,
        bookingStart = 300 * scale, bookingEnd = 400 * scale, winnerCount = 1
    )

    private fun committedDraw(): LotteryRepository.Progress {
        val lottery = lottery()
        lottery.join("first", 100)
        lottery.join("second", 100)
        lottery.drawIfReady(200, Random(1))
        return LotteryRepository.Progress(lottery, LotteryResult(
            id = "saved-result", lotteryId = lottery.id,
            winnerIds = lottery.winnerIds, loserIds = lottery.loserIds,
            drawnAt = 200, drawNumber = 1
        ))
    }

    @Test
    fun persistentDrawWaitsForCommitBeforePublishingTheSavedResult() = runBlocking {
        val repository = ControlledRepository()
        val saved = committedDraw()
        val winners = mutableListOf<String>()
        val losers = mutableListOf<String>()
        var published: LotteryResult? = null
        val service = LotteryDrawService(this, repository = repository,
            notifyWinner = { _, _, id -> winners.add(id) },
            notifyLoser = { _, _, id -> losers.add(id) },
            onResult = { _, result -> published = result })
        val drawing = async { service.draw("lottery") }
        val pendingCommit = withTimeout(2_000) { repository.requests.receive() }
        assertNull(published)
        assertTrue(winners.isEmpty())
        assertTrue(losers.isEmpty())
        pendingCommit.complete(saved)
        assertSame(saved.result, drawing.await())
        assertSame(saved.result, published)
        assertEquals(saved.result!!.winnerIds, winners)
        assertEquals(saved.result.loserIds, losers)
        assertEquals("lottery", repository.requestedId)
    }

    @Test
    fun failedCommitDoesNotSendNotifications() = runBlocking {
        val repository = ControlledRepository()
        val failure = IllegalStateException("Commit failed")
        var notifications = 0
        val service = LotteryDrawService(this, repository = repository,
            notifyWinner = { _, _, _ -> notifications++ },
            notifyLoser = { _, _, _ -> notifications++ })
        val drawing = async { runCatching { service.draw("lottery") } }
        val pendingCommit = withTimeout(2_000) { repository.requests.receive() }
        pendingCommit.completeExceptionally(failure)
        val reportedFailure = drawing.await().exceptionOrNull()
        assertTrue(reportedFailure is IllegalStateException)
        assertEquals(failure.message, reportedFailure?.message)
        assertEquals(0, notifications)
    }

    @Test
    fun persistentRedrawUsesOwnerAndReturnsStoredResult() = runBlocking {
        val repository = ControlledRepository()
        val service = LotteryDrawService(this, repository = repository)
        val redrawing = async { service.redraw("lottery", "owner") }
        val pendingCommit = withTimeout(2_000) { repository.requests.receive() }
        val saved = committedDraw()
        pendingCommit.complete(saved)
        assertSame(saved.result, redrawing.await())
        assertEquals("owner", repository.requestedOwner)
    }

    @Test
    fun monitorRechecksOnDocumentChangesAndStopsWhenClosed() = runBlocking {
        val repository = ControlledRepository()
        val open = lottery(scale = 1_000)
        val closed = lottery(scale = 1_000).apply { refreshState(400_000) }
        val service = LotteryDrawService(this, repository = repository, clock = { 0 })
        val job = service.start("lottery")
        withTimeout(2_000) {
            repository.requests.receive().complete(LotteryRepository.Progress(open))
            // The initial snapshot causes one recheck, then the deadline is far in the future.
            repository.requests.receive().complete(LotteryRepository.Progress(open))
            repository.changes.send(Unit)
            repository.requests.receive().complete(LotteryRepository.Progress(closed))
            job.join()
        }
        assertTrue(job.isCompleted)
        assertEquals("lottery", repository.requestedId)
    }

    /** Controls when storage completes without implementing another copy of draw logic. */
    private class ControlledRepository : LotteryRepository {
        val requests = Channel<CompletableDeferred<LotteryRepository.Progress>>(Channel.UNLIMITED)
        val changes = Channel<Unit>(Channel.UNLIMITED).apply { trySend(Unit) }
        var requestedId: String? = null
        var requestedOwner: String? = null

        override suspend fun advance(id: String, random: Random): LotteryRepository.Progress {
            requestedId = id
            val response = CompletableDeferred<LotteryRepository.Progress>()
            requests.send(response)
            return response.await()
        }

        override suspend fun redraw(
            id: String, requesterId: String, random: Random
        ): LotteryRepository.Progress {
            requestedOwner = requesterId
            return advance(id, random)
        }

        override fun observeOpenLotteriesForCampsite(
            campsiteId: String, ownerId: String
        ): Flow<List<Lottery>> = error("Not used")

        override fun observeChanges(id: String): Flow<Unit> = changes.receiveAsFlow()
        override suspend fun createLottery(lottery: Lottery): Unit = error("Not used")
        override suspend fun getLottery(id: String): Lottery? = error("Not used")
        override suspend fun updateLottery(lottery: Lottery, requesterId: String): Unit = error("Not used")
        override suspend fun joinLottery(id: String, entrantId: String): Unit = error("Not used")
        override suspend fun leaveLottery(id: String, entrantId: String): Boolean = error("Not used")
        override suspend fun getWaitingList(id: String, requesterId: String): List<WaitingList.Entry> =
            error("Not used")
        override suspend fun getResults(lotteryId: String): List<LotteryResult> = error("Not used")
    }
}
