package com.example.camp_serve

import com.example.camp_serve.data.EntrantRepository
import com.example.camp_serve.data.LotteryRepository
import com.example.camp_serve.data.NotificationLogRepository
import com.example.camp_serve.data.FirestoreEntrantNotificationRepo
import com.example.camp_serve.data.NotificationRepository
import com.example.camp_serve.model.EntrantNotificationType
import com.example.camp_serve.model.Campsite
import com.example.camp_serve.model.Entrant
import com.example.camp_serve.model.Lottery
import com.example.camp_serve.model.LotteryResult
import com.example.camp_serve.model.Notification
import com.example.camp_serve.model.NotificationLog
import com.example.camp_serve.model.NotificationStatus
import com.example.camp_serve.model.NotificationType
import com.example.camp_serve.model.pendingPushes
import com.example.camp_serve.model.toSentNotifications
import com.example.camp_serve.model.WaitingList
import com.example.camp_serve.model.newestFirst
import com.example.camp_serve.service.LotteryDrawService
import com.example.camp_serve.service.NotificationService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Tests for notifications: draw results (US 01.03.01, 01.03.02, 02.05.01), the opt-out
 * (US 01.10.01), waitlist messages (US 02.10.01), the owner's sent list (US 02.10.02)
 * and device notification selection (#101).
 */
class NotificationServiceTest {

    /** Test double standing in for the "notifications" collection; the app uses Firestore. */
    private class SavedNotifications : NotificationRepository {
        private val saved = MutableStateFlow(emptyMap<String, Notification>())
        val all: List<Notification> get() = saved.value.values.toList().newestFirst()
        override suspend fun saveAll(notifications: List<Notification>) {
            saved.value = saved.value + notifications.associateBy { it.id }
        }
        override suspend fun getForRecipient(recipientId: String) = all.filter { it.recipientId == recipientId }
        override fun observeForRecipient(recipientId: String): Flow<List<Notification>> =
            saved.map { m -> m.values.filter { it.recipientId == recipientId }.newestFirst() }
        override suspend fun getSentBy(senderId: String) = all.filter { it.senderId == senderId }
    }

    /** Test double for the admin notification log (US 03.08.01). */
    private class SavedLogs : NotificationLogRepository {
        val saved = linkedMapOf<String, NotificationLog>()
        override suspend fun getAllNotificationLogs() = saved.values.sortedByDescending { it.sentAt }
        override suspend fun addNotificationLog(log: NotificationLog) { saved[log.id] = log }
    }

    private val logs = SavedLogs()

    /** Entrant profiles in memory; ids in [optedOut] have notifications turned off. */
    private class FakeEntrants(private val optedOut: Set<String> = emptySet()) : EntrantRepository {
        override suspend fun getEntrant(): Entrant = error("Not used")
        override suspend fun updateEntrant(entrant: Entrant) = error("Not used")
        override suspend fun deleteEntrant(id: String) = error("Not used")
        override suspend fun getAllEntrants(): List<Entrant> = error("Not used")
        override suspend fun getEntrantsByIds(ids: Collection<String>): List<Entrant> =
            ids.map { Entrant(id = it, name = "Name $it", notificationEnabled = it !in optedOut) }
    }

    private fun lottery() = Lottery(
        id = "lottery", ownerId = "owner",
        campsites = listOf(Campsite(id = "site", ownerId = "owner", name = "Banff Ridge Camp")),
        registrationStart = 100, registrationEnd = 200,
        bookingStart = 300, bookingEnd = 400, winnerCount = 2
    )

    /** Draws [lottery] with five entrants and returns the result. */
    private fun drawnLottery(): Pair<Lottery, LotteryResult> {
        val lottery = lottery()
        repeat(5) { lottery.join("e$it", 100) }
        val (winners, losers) = lottery.drawIfReady(200, Random(3))!!
        return lottery to LotteryResult("r1", lottery.id, winners, losers, 200, lottery.drawNumber)
    }

    private fun service(repo: SavedNotifications, optedOut: Set<String> = emptySet()) =
        NotificationService(repo, FakeEntrants(optedOut), logs, clock = { 1_000 }, newId = { "batch-1" })

    @Test
    fun drawSendsOneWinOrLoseNoticePerEntrant() = runBlocking {
        val repo = SavedNotifications()
        val (lottery, result) = drawnLottery()

        service(repo).sendDrawResult(lottery, result)

        val saved = repo.all
        assertEquals(5, saved.size)
        assertEquals(5, saved.map { it.recipientId }.toSet().size)
        val wins = saved.filter { it.type == NotificationType.SYSTEM_WIN }
        val losses = saved.filter { it.type == NotificationType.SYSTEM_LOSE }
        assertEquals(result.winnerIds.toSet(), wins.map { it.recipientId }.toSet())
        assertEquals(result.loserIds.toSet(), losses.map { it.recipientId }.toSet())
        // identifies the campsite and the outcome
        assertTrue(wins.all { it.campsiteName == "Banff Ridge Camp" && "won" in it.message })
        assertTrue(losses.all { "not selected" in it.message })
        // sent on the owner's behalf, so the owner's sent list includes them
        assertTrue(saved.all { it.senderId == "owner" && it.lotteryId == "lottery" })
    }

    @Test
    fun sendingTheSameDrawTwiceDoesNotDuplicate() = runBlocking {
        val repo = SavedNotifications()
        val (lottery, result) = drawnLottery()
        val service = service(repo)

        service.sendDrawResult(lottery, result)
        service.sendDrawResult(lottery, result)

        assertEquals(5, repo.all.size)
    }

    @Test
    fun optedOutEntrantsAreLoggedButNotDelivered() = runBlocking {
        val repo = SavedNotifications()
        val (lottery, result) = drawnLottery()
        val winner = result.winnerIds.first()
        val loser = result.loserIds.first()

        service(repo, optedOut = setOf(winner, loser)).sendDrawResult(lottery, result)

        val byRecipient = repo.all.associateBy { it.recipientId }
        assertEquals(NotificationStatus.OPTED_OUT, byRecipient.getValue(winner).status)
        assertEquals(NotificationStatus.OPTED_OUT, byRecipient.getValue(loser).status)
        assertEquals(3, repo.all.count { it.status == NotificationStatus.SENT })
        // the draw result itself is still saved on the lottery
        assertTrue(winner in lottery.winnerIds)

        // an opted-out entrant sees no notification
        assertTrue(FirestoreEntrantNotificationRepo(winner, repo).getNotifications().isEmpty())
        val other = result.winnerIds.last()
        assertEquals(EntrantNotificationType.WON, FirestoreEntrantNotificationRepo(other, repo).getNotifications().single().type)
    }

    @Test
    fun emptyWaitlistDrawSendsNothing() = runBlocking {
        val repo = SavedNotifications()
        val lottery = lottery()
        val (winners, losers) = lottery.drawIfReady(200, Random(1))!!
        assertTrue(winners.isEmpty() && losers.isEmpty())

        val sent = service(repo).sendDrawResult(
            lottery, LotteryResult("r", lottery.id, winners, losers, 200, lottery.drawNumber)
        )

        assertTrue(sent.isEmpty())
        assertTrue(repo.all.isEmpty())
    }

    @Test
    fun waitlistMessageGoesToEveryEntrantExceptOptedOut() = runBlocking {
        val repo = SavedNotifications()
        val lottery = lottery()
        listOf("a", "b", "c").forEach { lottery.join(it, 150) }

        val sent = service(repo, optedOut = setOf("b"))
            .sendToWaitlist(lottery, "owner", "  Registration closes tomorrow  ")

        assertEquals(listOf("a", "b", "c"), sent.map { it.recipientId })
        assertTrue(sent.all { it.type == NotificationType.OWNER && it.batchId == "batch-1" })
        assertTrue(sent.all { it.message == "Registration closes tomorrow" })
        assertEquals(NotificationStatus.OPTED_OUT, sent.single { it.recipientId == "b" }.status)
        assertEquals(EntrantNotificationType.MESSAGE, FirestoreEntrantNotificationRepo("a", repo).getNotifications().single().type)
        assertTrue(FirestoreEntrantNotificationRepo("b", repo).getNotifications().isEmpty())
    }

    @Test
    fun emptyMessageOrOtherSenderIsRejected() {
        val repo = SavedNotifications()
        val lottery = lottery().apply { join("a", 150) }
        val service = service(repo)

        val empty = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { service.sendToWaitlist(lottery, "owner", "   ") }
        }
        assertEquals(NotificationService.EMPTY_MESSAGE, empty.message)
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { service.sendToWaitlist(lottery, "someone-else", "Hi") }
        }
        assertTrue(repo.all.isEmpty())
    }

    @Test
    fun sentListGroupsEachSendNewestFirstWithCounts() = runBlocking {
        val repo = SavedNotifications()
        val (lottery, result) = drawnLottery()
        var now = 1_000L
        val service = NotificationService(
            repo, FakeEntrants(setOf(result.loserIds.first())), logs, clock = { now }, newId = { "msg" }
        )
        service.sendDrawResult(lottery, result)
        now = 5_000
        service.sendToWaitlist(lottery, "owner", "Booking opens Jun 9")

        val sent = repo.getSentBy("owner").toSentNotifications()

        assertEquals(3, sent.size)
        assertEquals("Booking opens Jun 9", sent.first().message)
        assertEquals(NotificationType.OWNER, sent.first().type)
        assertEquals(4, sent.first().recipients)
        assertEquals(1, sent.first().optedOut)
        val win = sent.single { it.type == NotificationType.SYSTEM_WIN }
        val lose = sent.single { it.type == NotificationType.SYSTEM_LOSE }
        assertEquals(2, win.recipients)
        assertEquals(2, lose.recipients)
        assertEquals(1, lose.optedOut)
        assertTrue(repo.getSentBy("other-owner").isEmpty())
    }

    @Test
    fun everyNotificationIsWrittenToTheAdminLogOnce() = runBlocking {
        val repo = SavedNotifications()
        val (lottery, result) = drawnLottery()
        val optedOut = result.loserIds.first()
        val service = service(repo, optedOut = setOf(optedOut))

        service.sendDrawResult(lottery, result)
        service.sendDrawResult(lottery, result) // same draw again: no duplicate logs

        assertEquals(5, logs.saved.size)
        val entry = logs.saved.values.first { it.recipientId == result.winnerIds.first() }
        assertEquals("Name ${entry.recipientId}", entry.recipientName)
        assertEquals("Banff Ridge Camp", entry.campsite)
        assertEquals(1_000L, entry.sentAt)
        assertEquals(NotificationService.LOG_SENT, entry.status)
        assertEquals(
            NotificationService.LOG_OPTED_OUT,
            logs.saved.values.single { it.recipientId == optedOut }.status
        )
    }

    @Test
    fun pendingPushesSkipsOptedOutShownAndOldNotifications() {
        fun n(id: String, at: Long, status: NotificationStatus = NotificationStatus.SENT) =
            Notification(id = id, recipientId = "e", sentAt = at, status = status)
        val list = listOf(
            n("new-2", 300), n("new-1", 200), n("shown", 250),
            n("old", 50), n("opted-out", 260, NotificationStatus.OPTED_OUT)
        )

        val pending = pendingPushes(list, alreadyShown = setOf("shown"), since = 100)

        assertEquals(listOf("new-1", "new-2"), pending.map { it.id })
    }

    @Test
    fun savedDrawRunsTheNotificationHookAndRetriesAFailure() = runBlocking {
        val (lottery, result) = drawnLottery()
        val repo = SavedNotifications()
        val notifier = service(repo)
        var attempts = 0
        val draws = LotteryDrawService(
            this,
            repository = CommittedDraw(LotteryRepository.Progress(lottery, result)),
            retryDelayMillis = 1,
            onDrawSaved = { drawn, saved ->
                attempts++
                if (attempts == 1) error("offline")
                notifier.sendDrawResult(drawn, saved)
            }
        )

        draws.draw("lottery")

        assertEquals(2, attempts)
        assertEquals(5, repo.all.size)
    }

    /** Storage whose draw has already been committed. */
    private class CommittedDraw(private val progress: LotteryRepository.Progress) : LotteryRepository {
        override suspend fun advance(id: String, random: Random) = progress
        override suspend fun redraw(id: String, requesterId: String, random: Random) = progress
        override suspend fun createLottery(lottery: Lottery) = error("Not used")
        override suspend fun getLottery(id: String): Lottery? = error("Not used")
        override fun observeOpenLotteriesForCampsite(campsiteId: String, ownerId: String): Flow<List<Lottery>> =
            error("Not used")
        override fun observeLotteriesForOwner(ownerId: String): Flow<List<Lottery>> = error("Not used")
        override fun observeLottery(id: String): Flow<Lottery?> = error("Not used")
        override suspend fun updateLottery(lottery: Lottery, requesterId: String) = error("Not used")
        override suspend fun joinLottery(id: String, entrantId: String) = error("Not used")
        override suspend fun leaveLottery(id: String, entrantId: String): Boolean = error("Not used")
        override suspend fun getWaitingList(id: String, requesterId: String): List<WaitingList.Entry> =
            error("Not used")
        override suspend fun getResults(lotteryId: String): List<LotteryResult> = error("Not used")
        override fun observeChanges(id: String): Flow<Unit> = error("Not used")
    }
}
