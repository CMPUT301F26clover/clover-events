package com.example.camp_serve.data

import com.example.camp_serve.model.Lottery
import com.example.camp_serve.model.LotteryResult
import com.example.camp_serve.model.WaitingList
import kotlinx.coroutines.flow.Flow
import kotlin.random.Random

/**
 * Persistent lottery operations. Use these methods for shared data rather than
 * saving a locally edited waiting list over another device's registrations.
 * Failures propagate to the caller; transactional mutations require connectivity.
 */
interface LotteryRepository {
    /** An updated lottery and, if a draw occurred, its committed result. */
    data class Progress(val lottery: Lottery, val result: LotteryResult? = null)

    suspend fun createLottery(lottery: Lottery)
    suspend fun getLottery(id: String): Lottery?

    /**
     * Observes undrawn lottery configurations for this campsite, including scheduled
     * registrations. Callers must check the registration dates before offering a join.
     * These snapshots do not include the private waiting list.
     */
    fun observeOpenLotteriesForCampsite(campsiteId: String, ownerId: String): Flow<List<Lottery>>

    /** Applies configuration only, preserving the latest stored entrants and selections. */
    suspend fun updateLottery(lottery: Lottery, requesterId: String)
    suspend fun joinLottery(id: String, entrantId: String)
    suspend fun leaveLottery(id: String, entrantId: String): Boolean
    suspend fun getWaitingList(id: String, requesterId: String): List<WaitingList.Entry>
    suspend fun getResults(lotteryId: String): List<LotteryResult>

    /** Draws once when due, or advances booking/closed state without another draw. */
    suspend fun advance(id: String, random: Random = Random.Default): Progress
    suspend fun redraw(id: String, requesterId: String, random: Random = Random.Default): Progress

    /** Configuration/state invalidations, including an initial event on subscription. */
    fun observeChanges(id: String): Flow<Unit>
}
