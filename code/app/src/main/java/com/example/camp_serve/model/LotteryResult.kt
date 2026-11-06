/*
 * LotteryResult.kt
 *
 * Role: Model class for what happened to one entrant in one drawn lottery: whether they won
 * or lost, and (for winners) whether they accepted or declined. Stored in
 * lotteries/{lotteryId}/results/{entrantId}.
 * Outstanding issues: accept/decline screens belong to the entrant section.
 */

package com.example.camp_serve.model

/** Outcome of the draw for one entrant. */
enum class ResultOutcome { WON, LOST }

/** A winner's answer to the invitation. */
enum class ResultResponse { NONE, PENDING, ACCEPTED, DECLINED }

/**
 * One entrant's result.
 *
 * @property lotteryId lottery the result belongs to
 * @property entrantId the entrant
 * @property outcome WON or LOST
 * @property response NONE for losers, PENDING for winners until they answer
 */
data class LotteryResult(
    val lotteryId: String = "",
    val entrantId: String = "",
    val outcome: ResultOutcome = ResultOutcome.LOST,
    val response: ResultResponse = ResultResponse.NONE
) {
    /** True if this entrant won. */
    fun hasWon(): Boolean = outcome == ResultOutcome.WON

    /** True if the entrant won and has not answered yet. */
    fun canRespond(): Boolean = hasWon() && response == ResultResponse.PENDING

    /** Winner accepts the invitation. Ignored if they cannot respond. */
    fun accept(): LotteryResult =
        if (canRespond()) copy(response = ResultResponse.ACCEPTED) else this

    /** Winner declines the invitation. Ignored if they cannot respond. */
    fun decline(): LotteryResult =
        if (canRespond()) copy(response = ResultResponse.DECLINED) else this

    companion object {
        /** A winning result, waiting for the entrant's answer. */
        fun won(lotteryId: String, entrantId: String) =
            LotteryResult(lotteryId, entrantId, ResultOutcome.WON, ResultResponse.PENDING)

        /** A losing result. */
        fun lost(lotteryId: String, entrantId: String) =
            LotteryResult(lotteryId, entrantId, ResultOutcome.LOST, ResultResponse.NONE)
    }
}
