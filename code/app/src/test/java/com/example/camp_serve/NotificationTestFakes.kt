package com.example.camp_serve

import com.example.camp_serve.data.EntrantRepository
import com.example.camp_serve.data.LotteryRepository
import com.example.camp_serve.data.NotificationLogRepository
import com.example.camp_serve.data.NotificationRepository
import com.example.camp_serve.model.Entrant
import com.example.camp_serve.model.Lottery
import com.example.camp_serve.model.LotteryResult
import com.example.camp_serve.model.Notification
import com.example.camp_serve.model.NotificationLog
import com.example.camp_serve.model.WaitingList
import com.example.camp_serve.model.newestFirst
import kotlinx.coroutines.flow.Flow
import kotlin.random.Random

/*
 * NotificationTestFakes.kt
 * In-memory stand-ins for Firestore used by the notification unit tests.
 * They live in the test folder only; the app uses the Firestore versions.
 */

/** In-memory "notifications" collection. */
class FakeNotificationRepository : NotificationRepository {
    private val saved = linkedMapOf<String, Notification>()

    /** Everything saved so far, newest first. */
    val all: List<Notification> get() = saved.values.toList().newestFirst()

    override suspend fun saveAll(notifications: List<Notification>) {
        notifications.forEach { saved[it.id] = it }
    }

    override suspend fun getForRecipient(recipientId: String): List<Notification> =
        all.filter { it.recipientId == recipientId }
}

/** In-memory admin notification log. */
class FakeNotificationLogRepository : NotificationLogRepository {
    val saved = linkedMapOf<String, NotificationLog>()

    override suspend fun getAllNotificationLogs(): List<NotificationLog> =
        saved.values.sortedByDescending { it.sentAt }

    override suspend fun addNotificationLog(log: NotificationLog) {
        saved[log.id] = log
    }
}

/**
 * Entrant profiles where every entrant is named "Name <id>".
 *
 * @param optedOut ids of entrants who turned notifications off; can change during a test
 */
class FakeEntrantProfiles(val optedOut: MutableSet<String> = mutableSetOf()) : EntrantRepository {
    override suspend fun getEntrant(): Entrant = error("Not used")
    override suspend fun updateEntrant(entrant: Entrant) = error("Not used")
    override suspend fun deleteEntrant(id: String) = error("Not used")
    override suspend fun getAllEntrants(): List<Entrant> = error("Not used")
    override suspend fun getEntrantsByIds(ids: Collection<String>): List<Entrant> =
        ids.map { Entrant(id = it, name = "Name $it", notificationEnabled = it !in optedOut) }
}

/** Lottery storage whose draw has already been saved. */
class CommittedDrawRepository(private val progress: LotteryRepository.Progress) : LotteryRepository {
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
