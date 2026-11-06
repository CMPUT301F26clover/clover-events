/*
 * WaitingEntry.kt
 *
 * Role: Model class for one entrant on one lottery's waiting list. Stored as a document in
 * lotteries/{lotteryId}/entries/{entrantId}. It only records who joined and when; the draw
 * outcome and the entrant's response live in [LotteryResult].
 *
 */

package com.example.camp_serve.model

import java.util.Date

/**
 * One entrant's entry on a lottery's waiting list.
 *
 * @property entrantId ID of the entrant (also the Firestore document ID, so one entry per entrant).
 * @property entrantName Display name copied from the entrant's profile when they joined.
 * @property joinedAt When the entrant joined the waiting list.
 */
data class WaitingEntry(
    val entrantId: String = "",
    val entrantName: String = "",
    val joinedAt: Date = Date(0)
)
