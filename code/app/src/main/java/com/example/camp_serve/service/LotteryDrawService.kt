/*
 * LotteryDrawService.kt
 *
 * Role: Controller class that draws the winners of a lottery (US 02.04.05, 02.05.01).
 * Outstanding issues: the draw runs when the owner opens the lotteries list or taps the demo
 * button. A truly automatic draw at the closing time needs a Cloud Function or WorkManager job.
 */

package com.example.camp_serve.service

import com.example.camp_serve.data.LotteryRepository
import com.example.camp_serve.model.LotteryResult
import com.example.camp_serve.model.WaitingEntry
import java.util.Date
import kotlin.random.Random

/**
 * What a draw produced.
 *
 * @property lotteryId lottery that was drawn
 * @property winners entrants who won
 * @property losers entrants who did not win
 */
data class DrawOutcome(
    val lotteryId: String,
    val winners: List<WaitingEntry>,
    val losers: List<WaitingEntry>
)

/**
 * Draws lotteries using a [LotteryRepository].
 *
 * @param repository where lotteries and waitlists live
 * @param random source of randomness (tests pass a seeded one)
 */
class LotteryDrawService(
    private val repository: LotteryRepository,
    private val random: Random = Random.Default
) {

    /**
     * Picks winners at random. If there are fewer entrants than [count], everyone wins.
     *
     * @param entries the waitlist
     * @param count how many winners to pick
     * @return the winners
     */
    fun selectWinners(entries: List<WaitingEntry>, count: Int): List<WaitingEntry> {
        if (count >= entries.size) return entries
        return entries.shuffled(random).take(count)
    }

    /**
     * Draws one lottery if registration has closed (or if [force] is true).
     *
     * @param lotteryId lottery to draw
     * @param now current time
     * @param force draw even if registration is still open (demo only)
     * @return the outcome, or null if nothing was drawn
     */
    suspend fun drawIfDue(lotteryId: String, now: Date = Date(), force: Boolean = false): DrawOutcome? {
        val lottery = repository.getLottery(lotteryId) ?: return null
        if (!lottery.canEdit()) return null // already drawn
        if (!force && !lottery.isDrawDue(now)) return null

        val entries = repository.getEntries(lotteryId)
        val winners = selectWinners(entries, lottery.winnerCount)
        val winnerIds = winners.map { it.entrantId }.toSet()
        val losers = entries.filter { it.entrantId !in winnerIds }

        val results = winners.map { LotteryResult.won(lotteryId, it.entrantId) } +
            losers.map { LotteryResult.lost(lotteryId, it.entrantId) }
        val applied = repository.applyDraw(lotteryId, results, now)
        return if (applied) DrawOutcome(lotteryId, winners, losers) else null
    }

    /**
     * Draws every owner lottery whose registration has closed.
     *
     * @param ownerId the owner
     * @param now current time
     * @return outcomes of the lotteries that were drawn
     */
    suspend fun drawAllDue(ownerId: String, now: Date = Date()): List<DrawOutcome> {
        val outcomes = mutableListOf<DrawOutcome>()
        for (lottery in repository.getLotteriesForOwner(ownerId)) {
            val outcome = drawIfDue(lottery.id, now)
            if (outcome != null) outcomes.add(outcome)
        }
        return outcomes
    }
}
