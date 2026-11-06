/*
 * LotteryRepository.kt lists what we can do with lotteries, their waitlists and results.
 * Screens only use this interface, so the in-memory repo can be swapped for Firestore later.
 * Outstanding issues: only the in-memory TempLotteryRepo exists, there is no Firestore version yet.
 */

package com.example.camp_serve.data

import com.example.camp_serve.model.Lottery
import com.example.camp_serve.model.LotteryResult
import com.example.camp_serve.model.WaitingEntry
import kotlinx.coroutines.flow.Flow
import java.util.Date

/** What happened when an entrant tried to join a waitlist. */
enum class JoinResult { JOINED, ALREADY_JOINED, REGISTRATION_CLOSED, WAITLIST_FULL, NOT_FOUND }

/**
 * Where lottery data comes from and gets saved to.
 */
interface LotteryRepository {

    /**
     * Saves a new lottery.
     *
     * @param lottery the lottery to save (its id is replaced if blank)
     * @return the saved lottery with its id
     */
    suspend fun createLottery(lottery: Lottery): Lottery

    /**
     * Saves changes to a lottery. Refuses if the lottery was already drawn.
     *
     * @param lottery the lottery with its new details
     * @return true if saved, false if it does not exist or was already drawn
     */
    suspend fun updateLottery(lottery: Lottery): Boolean

    /**
     * Gets one lottery.
     *
     * @param id lottery id
     * @return the lottery or null
     */
    suspend fun getLottery(id: String): Lottery?

    /**
     * Gets all lotteries created by one owner.
     *
     * @param ownerId the owner's id
     */
    suspend fun getLotteriesForOwner(ownerId: String): List<Lottery>

    /**
     * Gets all lotteries that include a campsite.
     *
     * @param campsiteId the campsite's id
     */
    suspend fun getLotteriesForCampsite(campsiteId: String): List<Lottery>

    /**
     * Live list of the waitlist; emits again whenever entrants join or leave (US 02.09.01).
     *
     * @param lotteryId lottery id
     */
    fun observeEntries(lotteryId: String): Flow<List<WaitingEntry>>

    /**
     * Gets the waitlist once.
     *
     * @param lotteryId lottery id
     */
    suspend fun getEntries(lotteryId: String): List<WaitingEntry>

    /**
     * Adds an entrant to the waitlist if registration is open and there is room.
     *
     * @param lotteryId lottery id
     * @param entry who is joining
     * @param now current time (passed in so tests can control it)
     */
    suspend fun joinWaitlist(lotteryId: String, entry: WaitingEntry, now: Date = Date()): JoinResult

    /**
     * Removes an entrant from the waitlist.
     *
     * @param lotteryId lottery id
     * @param entrantId entrant to remove
     */
    suspend fun leaveWaitlist(lotteryId: String, entrantId: String)

    /**
     * Gets the draw results.
     *
     * @param lotteryId lottery id
     */
    suspend fun getResults(lotteryId: String): List<LotteryResult>

    /**
     * Saves the draw results and marks the lottery DRAWN in one step.
     *
     * @param lotteryId lottery id
     * @param results one result per entrant
     * @param drawnAt when the draw happened
     * @return false if the lottery was already drawn or does not exist
     */
    suspend fun applyDraw(lotteryId: String, results: List<LotteryResult>, drawnAt: Date): Boolean
}
