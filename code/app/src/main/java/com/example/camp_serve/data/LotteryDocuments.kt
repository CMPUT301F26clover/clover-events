package com.example.camp_serve.data

import com.example.camp_serve.model.Campsite
import com.example.camp_serve.model.Lottery
import com.example.camp_serve.model.WaitingList

// These are intermediate classes that store lottery data in Firestore. Default values let Firestore load them.
// The Lottery class handles validation and controls changes.
internal data class LotteryDocument(
    val ownerId: String = "",
    val campsites: List<Campsite> = emptyList(),
    val registrationStart: Long = 0,
    val registrationEnd: Long = 0,
    val bookingStart: Long = 0,
    val bookingEnd: Long = 0,
    val winnerCount: Int = 0,
    val waitlistCapacity: Int? = null,
    val state: String = Lottery.State.OPEN.name,
    val winnerIds: List<String> = emptyList(),
    val loserIds: List<String> = emptyList(),
    val drawNumber: Long = 0
)

internal data class WaitingListDocument(
    val lotteryId: String = "",
    val ownerId: String = "",
    val capacity: Int? = null,
    val entries: List<WaitingList.Entry> = emptyList()
)

/** A consistent snapshot of the two documents that make up one lottery. */
internal data class LotteryDocuments(
    val lottery: LotteryDocument,
    val waitingList: WaitingListDocument
) {
    fun restore(id: String): Lottery {
        require(waitingList.lotteryId == id && waitingList.ownerId == lottery.ownerId) {
            "Waiting list does not belong to this lottery."
        }
        require(waitingList.capacity == lottery.waitlistCapacity) {
            "Stored waiting list capacity does not match the lottery."
        }
        return Lottery(
            id = id,
            ownerId = lottery.ownerId,
            campsites = lottery.campsites,
            registrationStart = lottery.registrationStart,
            registrationEnd = lottery.registrationEnd,
            bookingStart = lottery.bookingStart,
            bookingEnd = lottery.bookingEnd,
            winnerCount = lottery.winnerCount,
            waitlistCapacity = lottery.waitlistCapacity
        ).apply {
            restore(
                Lottery.State.valueOf(lottery.state),
                waitingList.entries,
                lottery.winnerIds,
                lottery.loserIds,
                lottery.drawNumber
            )
        }
    }

    companion object {
        fun capture(lottery: Lottery): LotteryDocuments = synchronized(lottery) {
            LotteryDocuments(
                lottery = LotteryDocument(
                    ownerId = lottery.ownerId,
                    campsites = lottery.campsites.toList(),
                    registrationStart = lottery.registrationStart,
                    registrationEnd = lottery.registrationEnd,
                    bookingStart = lottery.bookingStart,
                    bookingEnd = lottery.bookingEnd,
                    winnerCount = lottery.winnerCount,
                    waitlistCapacity = lottery.waitlistCapacity,
                    state = lottery.state.name,
                    winnerIds = lottery.winnerIds.toList(),
                    loserIds = lottery.loserIds.toList(),
                    drawNumber = lottery.drawNumber
                ),
                waitingList = WaitingListDocument(
                    lotteryId = lottery.id,
                    ownerId = lottery.ownerId,
                    capacity = lottery.waitlistCapacity,
                    entries = lottery.getWaitingList(lottery.ownerId)
                )
            )
        }
    }
}
