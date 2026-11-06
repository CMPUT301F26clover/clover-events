/*
 * TempLotteryRepo.kt
 *
 * Temporary lottery repo that keeps everything in memory with some sample data, so the owner
 * screens work before Firestore is connected. Data resets when the app closes.
 * TODO: REPLACE WITH FIRESTORE VERSION LATER
 * Outstanding issues: none.
 */

package com.example.camp_serve.data

import com.example.camp_serve.model.Lottery
import com.example.camp_serve.model.LotteryResult
import com.example.camp_serve.model.LotteryState
import com.example.camp_serve.model.WaitingEntry
import com.example.camp_serve.util.addDays
import com.example.camp_serve.util.startOfDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.Date

/**
 * Stand-in for Firestore that keeps lotteries, waitlists and results in memory.
 *
 * @param withSampleData true to start with two sample lotteries (tests pass false)
 */
class TempLotteryRepo(withSampleData: Boolean = true) : LotteryRepository {
    private val lotteries = mutableListOf<Lottery>()
    private val entries = MutableStateFlow<Map<String, List<WaitingEntry>>>(emptyMap())
    private val results = mutableMapOf<String, List<LotteryResult>>()
    private var nextNumber = 1

    init {
        if (withSampleData) {
            val today = startOfDay(Date())
            lotteries.add(
                Lottery(
                    id = "lottery-01", ownerId = "fake-owner-01",
                    campsiteIds = listOf("camp-01"),
                    registrationStart = addDays(today, -1), registrationEnd = addDays(today, 5),
                    bookingStart = addDays(today, 6), bookingEnd = addDays(today, 12),
                    winnerCount = 5, waitlistCapacity = 30
                )
            )
            lotteries.add(
                Lottery(
                    id = "lottery-02", ownerId = "fake-owner-01",
                    campsiteIds = listOf("camp-02"),
                    registrationStart = addDays(today, -20), registrationEnd = addDays(today, -10),
                    bookingStart = addDays(today, -9), bookingEnd = addDays(today, 3),
                    winnerCount = 5, state = LotteryState.DRAWN, drawnAt = addDays(today, -9)
                )
            )
            nextNumber = 3
            val now = Date()
            entries.value = mapOf(
                "lottery-01" to listOf(
                    WaitingEntry("e1", "Cole Smith", Date(now.time - 3 * 3600_000L)),
                    WaitingEntry("e2", "Ava Tremblay", Date(now.time - 2 * 3600_000L)),
                    WaitingEntry("e3", "Noah Singh", Date(now.time - 30 * 60_000L))
                )
            )
        }
    }

    override suspend fun createLottery(lottery: Lottery): Lottery {
        val saved = if (lottery.id.isBlank()) {
            lottery.copy(id = "lottery-" + String.format("%02d", nextNumber++))
        } else lottery
        lotteries.add(saved)
        return saved
    }

    override suspend fun updateLottery(lottery: Lottery): Boolean {
        for (i in lotteries.indices) {
            if (lotteries[i].id == lottery.id) {
                if (!lotteries[i].canEdit()) return false // already drawn
                // keep the state and draw time the repo knows about
                lotteries[i] = lottery.copy(state = lotteries[i].state, drawnAt = lotteries[i].drawnAt)
                return true
            }
        }
        return false
    }

    override suspend fun getLottery(id: String): Lottery? = lotteries.firstOrNull { it.id == id }

    override suspend fun getLotteriesForOwner(ownerId: String): List<Lottery> =
        lotteries.filter { it.ownerId == ownerId }

    override suspend fun getLotteriesForCampsite(campsiteId: String): List<Lottery> =
        lotteries.filter { campsiteId in it.campsiteIds }

    override fun observeEntries(lotteryId: String): Flow<List<WaitingEntry>> =
        entries.map { (it[lotteryId] ?: emptyList()).sortedBy { e -> e.joinedAt } }

    override suspend fun getEntries(lotteryId: String): List<WaitingEntry> =
        (entries.value[lotteryId] ?: emptyList()).sortedBy { it.joinedAt }

    override suspend fun joinWaitlist(lotteryId: String, entry: WaitingEntry, now: Date): JoinResult {
        val lottery = getLottery(lotteryId) ?: return JoinResult.NOT_FOUND
        if (!lottery.isRegistrationOpen(now)) return JoinResult.REGISTRATION_CLOSED
        val current = entries.value[lotteryId] ?: emptyList()
        if (current.any { it.entrantId == entry.entrantId }) return JoinResult.ALREADY_JOINED
        if (!lottery.hasRoom(current.size)) return JoinResult.WAITLIST_FULL
        entries.value = entries.value + (lotteryId to current + entry)
        return JoinResult.JOINED
    }

    override suspend fun leaveWaitlist(lotteryId: String, entrantId: String) {
        val current = entries.value[lotteryId] ?: return
        entries.value = entries.value + (lotteryId to current.filter { it.entrantId != entrantId })
    }

    override suspend fun getResults(lotteryId: String): List<LotteryResult> =
        results[lotteryId] ?: emptyList()

    override suspend fun applyDraw(lotteryId: String, results: List<LotteryResult>, drawnAt: Date): Boolean {
        for (i in lotteries.indices) {
            if (lotteries[i].id == lotteryId) {
                if (lotteries[i].state != LotteryState.OPEN) return false
                lotteries[i] = lotteries[i].copy(state = LotteryState.DRAWN, drawnAt = drawnAt)
                this.results[lotteryId] = results
                return true
            }
        }
        return false
    }
}
