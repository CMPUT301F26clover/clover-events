package com.example.camp_serve

import com.example.camp_serve.data.LotteryRepository
import com.example.camp_serve.model.Lottery
import com.example.camp_serve.model.LotteryResult
import com.example.camp_serve.model.WaitingList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.random.Random

/** In-memory storage for UI tests; mutations use the same domain validation as Firestore. */
internal class TestLotteryRepository(initial: List<Lottery> = emptyList()) : LotteryRepository {
    val lotteries = MutableStateFlow(initial)
    val joins = mutableListOf<Pair<String, String>>()
    val leaves = mutableListOf<Pair<String, String>>()
    var failure: Exception? = null
    var commit: CompletableDeferred<Unit>? = null

    override fun observeOpenLotteriesForCampsite(
        campsiteId: String, ownerId: String
    ): Flow<List<Lottery>> = lotteries

    override suspend fun joinLottery(id: String, entrantId: String) {
        joins.add(id to entrantId)
        commit?.await()
        failure?.let { throw it }
        lotteries.value.first { it.id == id }.join(entrantId)
    }

    override suspend fun createLottery(lottery: Lottery): Unit = error("Not used")
    override suspend fun getLottery(id: String): Lottery? =
        lotteries.value.firstOrNull { it.id == id }
    override suspend fun updateLottery(lottery: Lottery, requesterId: String): Unit = error("Not used")
    override suspend fun leaveLottery(id: String, entrantId: String): Boolean {
        leaves.add(id to entrantId)
        commit?.await()
        failure?.let { throw it }
        return lotteries.value.first { it.id == id }.leave(entrantId)
    }
    override suspend fun getWaitingList(id: String, requesterId: String): List<WaitingList.Entry> =
        error("Not used")
    override suspend fun getResults(lotteryId: String): List<LotteryResult> = error("Not used")
    override suspend fun advance(id: String, random: Random): LotteryRepository.Progress =
        error("Not used")
    override suspend fun redraw(
        id: String, requesterId: String, random: Random
    ): LotteryRepository.Progress = error("Not used")
    override fun observeChanges(id: String): Flow<Unit> = error("Not used")
}
