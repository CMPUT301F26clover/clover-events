package com.example.camp_serve

import com.example.camp_serve.data.LotteryDocuments
import com.example.camp_serve.model.Campsite
import com.example.camp_serve.model.Lottery
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class LotteryDocumentsTest {
    private fun lottery() = Lottery(
        id = "lottery", ownerId = "owner",
        campsites = listOf(
            Campsite(id = "first-site", ownerId = "owner", name = "Lake", price = 25.0),
            Campsite(id = "second-site", ownerId = "owner")
        ),
        registrationStart = 100, registrationEnd = 200,
        bookingStart = 300, bookingEnd = 400, winnerCount = 2, waitlistCapacity = 4
    )

    @Test
    fun registrationRoundTripPreservesConfigurationEntrantsAndCapacity() {
        val original = lottery()
        repeat(4) { original.join("entrant-$it", 100L + it) }
        val restored = LotteryDocuments.capture(original).restore(original.id)
        assertEquals(original.campsites, restored.campsites)
        assertEquals(original.registrationStart, restored.registrationStart)
        assertEquals(original.registrationEnd, restored.registrationEnd)
        assertEquals(original.bookingStart, restored.bookingStart)
        assertEquals(original.bookingEnd, restored.bookingEnd)
        assertEquals(original.winnerCount, restored.winnerCount)
        assertEquals(original.getWaitingList("owner"), restored.getWaitingList("owner"))
        assertThrows(IllegalStateException::class.java) { restored.join("extra", 150) }
        assertThrows(IllegalArgumentException::class.java) { restored.getWaitingList("other") }
        // Restored data is independent of the original model.
        original.leave("entrant-0", 150)
        assertEquals(4, restored.getWaitingList("owner").size)
    }

    @Test
    fun drawnRoundTripPreservesSelectionAndPreventsAnotherInitialDraw() {
        val original = lottery()
        repeat(4) { original.join("entrant-$it", 100) }
        original.drawIfReady(200, Random(1))
        val restored = LotteryDocuments.capture(original).restore(original.id)
        assertEquals(Lottery.State.DRAWN, restored.state)
        assertEquals(1L, restored.drawNumber)
        assertEquals(original.winnerIds, restored.winnerIds)
        assertEquals(original.loserIds, restored.loserIds)
        assertNull(restored.drawIfReady(200, Random(2)))
        assertTrue(restored.canBook(restored.winnerIds.first(), 300))
        restored.redraw("owner", 300, Random(3))
        val redrawn = LotteryDocuments.capture(restored).restore(restored.id)
        assertEquals(2L, redrawn.drawNumber)
        assertEquals(Lottery.State.BOOKING, redrawn.state)
        assertEquals(restored.winnerIds, redrawn.winnerIds)
        redrawn.refreshState(400)
        val closed = LotteryDocuments.capture(redrawn).restore(redrawn.id)
        assertEquals(Lottery.State.CLOSED, closed.state)
        assertFalse(closed.canBook(closed.winnerIds.first(), 400))
    }

    @Test
    fun expiredUndrawnLotteryCanBeRestored() {
        val original = lottery()
        original.refreshState(400)
        val restored = LotteryDocuments.capture(original).restore(original.id)
        assertEquals(Lottery.State.CLOSED, restored.state)
        assertEquals(0L, restored.drawNumber)
        assertTrue(restored.winnerIds.isEmpty())
    }

    @Test
    fun mismatchedOrCorruptDocumentsAreRejected() {
        val original = lottery()
        repeat(4) { original.join("entrant-$it", 100) }
        original.drawIfReady(200, Random(1))
        val stored = LotteryDocuments.capture(original)
        assertThrows(IllegalArgumentException::class.java) {
            stored.copy(waitingList = stored.waitingList.copy(lotteryId = "other")).restore(original.id)
        }
        assertThrows(IllegalArgumentException::class.java) {
            stored.copy(waitingList = stored.waitingList.copy(capacity = 1)).restore(original.id)
        }
        assertThrows(IllegalArgumentException::class.java) {
            stored.copy(lottery = stored.lottery.copy(loserIds = emptyList())).restore(original.id)
        }
        assertThrows(IllegalArgumentException::class.java) {
            stored.copy(lottery = stored.lottery.copy(winnerIds = listOf("outsider", "outsider")))
                .restore(original.id)
        }
    }
}
