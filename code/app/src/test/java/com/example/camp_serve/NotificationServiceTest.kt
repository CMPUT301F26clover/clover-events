package com.example.camp_serve

import com.example.camp_serve.data.LotteryRepository
import com.example.camp_serve.data.NotificationAlertRepo
import com.example.camp_serve.model.AlertType
import com.example.camp_serve.model.Campsite
import com.example.camp_serve.model.Lottery
import com.example.camp_serve.model.LotteryResult
import com.example.camp_serve.model.NotificationType
import com.example.camp_serve.service.LotteryDrawService
import com.example.camp_serve.service.NotificationService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/*
 * NotificationServiceTest.kt
 * Unit tests for notifications sent after a lottery draw.
 */
class NotificationServiceTest {

    private val repo = FakeNotificationRepository()
    private val logs = FakeNotificationLogRepository()
    private val service = NotificationService(repo, FakeEntrantProfiles(), logs, clock = { 1_000 })

    private fun lottery() = Lottery(
        id = "lottery", ownerId = "owner",
        campsites = listOf(Campsite(id = "site", ownerId = "owner", name = "Banff Ridge Camp")),
        registrationStart = 100, registrationEnd = 200,
        bookingStart = 300, bookingEnd = 400, winnerCount = 2
    )

    /** Draws a lottery with five entrants and returns it with its result. */
    private fun drawnLottery(entrants: Int = 5): Pair<Lottery, LotteryResult> {
        val lottery = lottery()
        repeat(entrants) { lottery.join("e$it", 100) }
        val (winners, losers) = lottery.drawIfReady(200, Random(3))!!
        return lottery to LotteryResult("r1", lottery.id, winners, losers, 200, lottery.drawNumber)
    }

    @Test
    fun onlyWinnersGetOneWinNoticeEach() = runBlocking {
        val (lottery, result) = drawnLottery()

        service.sendDrawResult(lottery, result)

        val saved = repo.all
        assertEquals(result.winnerIds.toSet(), saved.map { it.recipientId }.toSet())
        assertEquals(result.winnerIds.size, saved.size)
        assertTrue(saved.all { it.type == NotificationType.SYSTEM_WIN })
        // identifies the campsite and says the entrant won
        assertTrue(saved.all { it.campsiteName == "Banff Ridge Camp" && "won" in it.message })
        assertTrue(result.loserIds.none { id -> saved.any { it.recipientId == id } })
    }

    @Test
    fun sendingTheSameDrawTwiceSendsEachNoticeOnce() = runBlocking {
        val (lottery, result) = drawnLottery()

        service.sendDrawResult(lottery, result)
        service.sendDrawResult(lottery, result)

        assertEquals(result.winnerIds.size, repo.all.size)
        assertEquals(result.winnerIds.size, logs.saved.size)
    }

    @Test
    fun emptyWaitlistSendsNothing() = runBlocking {
        val (lottery, result) = drawnLottery(entrants = 0)

        assertTrue(service.sendDrawResult(lottery, result).isEmpty())
        assertTrue(repo.all.isEmpty())
    }

    @Test
    fun winNoticesAreWrittenToTheAdminLog() = runBlocking {
        val (lottery, result) = drawnLottery()

        service.sendDrawResult(lottery, result)

        val winner = result.winnerIds.first()
        val entry = logs.saved.values.single { it.recipientId == winner }
        assertEquals("Name $winner", entry.recipientName)
        assertEquals("Banff Ridge Camp", entry.campsite)
        assertEquals(1_000L, entry.sentAt)
        assertEquals(NotificationService.LOG_SENT, entry.status)
    }

    @Test
    fun winnerSeesAWonAlert() = runBlocking {
        val (lottery, result) = drawnLottery()
        service.sendDrawResult(lottery, result)

        val alert = NotificationAlertRepo(result.winnerIds.first(), repo).getAlerts().single()

        assertEquals(AlertType.WON, alert.type)
        assertEquals("Banff Ridge Camp", alert.campsite)
        assertTrue(NotificationAlertRepo(result.loserIds.first(), repo).getAlerts().isEmpty())
    }

    @Test
    fun savedDrawRunsTheNotificationHookAndRetriesAFailure() = runBlocking {
        val (lottery, result) = drawnLottery()
        var attempts = 0
        val draws = LotteryDrawService(
            this,
            repository = CommittedDrawRepository(LotteryRepository.Progress(lottery, result)),
            retryDelayMillis = 1,
            onDrawSaved = { drawn, saved ->
                attempts++
                if (attempts == 1) error("offline")
                service.sendDrawResult(drawn, saved)
            }
        )

        draws.draw("lottery")

        assertEquals(2, attempts)
        assertEquals(result.winnerIds.size, repo.all.size)
    }
}
