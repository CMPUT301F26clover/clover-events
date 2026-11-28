package com.example.camp_serve

import com.example.camp_serve.data.LotteryRepository
import com.example.camp_serve.data.NotificationAlertRepo
import com.example.camp_serve.model.AlertType
import com.example.camp_serve.model.Campsite
import com.example.camp_serve.model.Lottery
import com.example.camp_serve.model.LotteryResult
import com.example.camp_serve.model.NotificationStatus
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
 * Unit tests for notifications sent after a lottery draw
 * and the notification opt-out.
 */
class NotificationServiceTest {

    private val repo = FakeNotificationRepository()
    private val logs = FakeNotificationLogRepository()
    private val profiles = FakeEntrantProfiles()
    private val service = NotificationService(repo, profiles, logs, clock = { 1_000 })

    private fun lottery(id: String = "lottery") = Lottery(
        id = id, ownerId = "owner",
        campsites = listOf(Campsite(id = "site", ownerId = "owner", name = "Banff Ridge Camp")),
        registrationStart = 100, registrationEnd = 200,
        bookingStart = 300, bookingEnd = 400, winnerCount = 2
    )

    /** Draws a lottery with five entrants and returns it with its result. */
    private fun drawnLottery(entrants: Int = 5, id: String = "lottery"): Pair<Lottery, LotteryResult> {
        val lottery = lottery(id)
        repeat(entrants) { lottery.join("e$it", 100) }
        val (winners, losers) = lottery.drawIfReady(200, Random(3))!!
        return lottery to LotteryResult("r1", lottery.id, winners, losers, 200, lottery.drawNumber)
    }

    @Test
    fun onlyWinnersGetOneWinNoticeEach() = runBlocking {
        val (lottery, result) = drawnLottery()

        service.sendDrawResult(lottery, result)

        val wins = repo.all.filter { it.type == NotificationType.SYSTEM_WIN }
        assertEquals(result.winnerIds.toSet(), wins.map { it.recipientId }.toSet())
        assertEquals(result.winnerIds.size, wins.size)
        // identifies the campsite and says the entrant won
        assertTrue(wins.all { it.campsiteName == "Banff Ridge Camp" && "won" in it.message })
    }

    @Test
    fun sendingTheSameDrawTwiceSendsEachNoticeOnce() = runBlocking {
        val (lottery, result) = drawnLottery()

        service.sendDrawResult(lottery, result)
        service.sendDrawResult(lottery, result)

        assertEquals(5, repo.all.size)
        assertEquals(5, logs.saved.size)
    }

    @Test
    fun onlyEntrantsWhoWereNotSelectedGetOneLoseNoticeEach() = runBlocking {
        val (lottery, result) = drawnLottery()

        service.sendDrawResult(lottery, result)

        val losses = repo.all.filter { it.type == NotificationType.SYSTEM_LOSE }
        assertEquals(result.loserIds.toSet(), losses.map { it.recipientId }.toSet())
        assertEquals(result.loserIds.size, losses.size)
        // identifies the campsite and says the entrant was not selected
        assertTrue(losses.all { it.campsiteName == "Banff Ridge Camp" && "not selected" in it.message })
        // every entrant gets exactly one notice: a win or a loss, never both
        assertEquals(5, repo.all.map { it.recipientId }.toSet().size)
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
    }

    @Test
    fun entrantWhoWasNotSelectedSeesALostAlert() = runBlocking {
        val (lottery, result) = drawnLottery()
        service.sendDrawResult(lottery, result)

        val alert = NotificationAlertRepo(result.loserIds.first(), repo).getAlerts().single()

        assertEquals(AlertType.LOST, alert.type)
        assertEquals("Banff Ridge Camp", alert.campsite)
    }

    @Test
    fun optedOutEntrantsGetNoAlertButAreLogged() = runBlocking {
        val (lottery, result) = drawnLottery()
        val winner = result.winnerIds.first()
        val loser = result.loserIds.first()
        profiles.optedOut += setOf(winner, loser)

        service.sendDrawResult(lottery, result)

        val byRecipient = repo.all.associateBy { it.recipientId }
        assertEquals(NotificationStatus.OPTED_OUT, byRecipient.getValue(winner).status)
        assertEquals(NotificationStatus.OPTED_OUT, byRecipient.getValue(loser).status)
        assertEquals(3, repo.all.count { it.status == NotificationStatus.SENT })
        // no alerts for them
        assertTrue(NotificationAlertRepo(winner, repo).getAlerts().isEmpty())
        assertTrue(NotificationAlertRepo(loser, repo).getAlerts().isEmpty())
        // the draw result itself is still saved on the lottery
        assertTrue(winner in lottery.winnerIds)
        // the admin log shows they were skipped
        assertEquals(NotificationService.LOG_OPTED_OUT, logs.saved.getValue(byRecipient.getValue(winner).id).status)
    }

    @Test
    fun turningNotificationsBackOnDeliversAgain() = runBlocking {
        val (first, firstResult) = drawnLottery(id = "first")
        val entrant = firstResult.winnerIds.first()
        profiles.optedOut += entrant
        service.sendDrawResult(first, firstResult)

        profiles.optedOut -= entrant
        val (second, secondResult) = drawnLottery(id = "second")
        service.sendDrawResult(second, secondResult)

        val alerts = NotificationAlertRepo(entrant, repo).getAlerts()
        assertEquals(1, alerts.size) // only the one sent after turning them back on
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
        assertEquals(5, repo.all.size)
    }
}
