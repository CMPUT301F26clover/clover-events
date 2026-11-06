package com.example.camp_serve

import com.example.camp_serve.model.Campsite
import com.example.camp_serve.model.Lottery
import org.junit.Assert.*
import org.junit.Test

class LotteryTest {
    private fun lottery(capacity: Int? = null) = Lottery(
        id = "lottery",
        ownerId = "owner",
        campsites = listOf(Campsite(id = "site", ownerId = "owner")),
        registrationStart = 100,
        registrationEnd = 200,
        bookingStart = 300,
        bookingEnd = 400,
        winnerCount = 2,
        waitlistCapacity = capacity
    )

    @Test
    fun registrationIncludesStartButExcludesEndAndEnforcesCapacity() {
        val lottery = lottery(capacity = 1)
        assertThrows(IllegalStateException::class.java) { lottery.join("early", 99) }
        lottery.join("first", 100)
        assertThrows(IllegalStateException::class.java) { lottery.join("second", 101) }
        assertTrue(lottery.leave("first", 150))
        lottery.join("second", 199)
        assertThrows(IllegalStateException::class.java) { lottery.join("late", 200) }
        assertThrows(IllegalStateException::class.java) { lottery.leave("second", 200) }
        assertEquals("second", lottery.getWaitingList("owner").single().id)
    }

    @Test
    fun membershipTracksJoinLeaveAndRejoinWithoutExposingOtherEntrants() {
        val lottery = lottery()
        assertFalse(lottery.isOnWaitingList("first"))
        lottery.join("first", 100)
        lottery.join("second", 100)
        assertTrue(lottery.isOnWaitingList("first"))
        assertFalse(lottery.isOnWaitingList("stranger"))

        assertTrue(lottery.leave("first", 150))
        assertFalse(lottery.isOnWaitingList("first"))
        assertTrue(lottery.isOnWaitingList("second"))
        assertFalse(lottery.leave("first", 150))

        lottery.join("first", 199)
        assertTrue(lottery.isOnWaitingList("first"))
        assertThrows(IllegalStateException::class.java) { lottery.leave("first", 200) }
        assertTrue(lottery.isOnWaitingList("first"))
    }

    @Test
    fun invalidOwnerUpdatesDoNotPartiallyApply() {
        val lottery = lottery()
        lottery.join("first", 100)
        lottery.join("second", 100)
        assertThrows(IllegalArgumentException::class.java) {
            lottery.update("stranger", winnerCount = 3, now = 150)
        }
        assertThrows(IllegalArgumentException::class.java) {
            lottery.update("owner", winnerCount = 3, waitlistCapacity = 1, now = 150)
        }
        assertEquals(2, lottery.winnerCount)
        assertNull(lottery.waitlistCapacity)
        assertEquals(2, lottery.getWaitingList("owner").size)
        assertThrows(IllegalArgumentException::class.java) {
            lottery.update("owner", campsites = emptyList(), now = 150)
        }
        assertThrows(IllegalArgumentException::class.java) {
            lottery.update("owner",
                campsites = listOf(Campsite(id = "other", ownerId = "stranger")), now = 150)
        }
        assertEquals("site", lottery.campsites.single().id)
    }

    @Test
    fun validUpdateChangesScheduleAndCapacityBeforeRegistrationEnds() {
        val lottery = lottery()
        lottery.update("owner", registrationEnd = 250, bookingStart = 350,
            bookingEnd = 450, winnerCount = 1, waitlistCapacity = 1, now = 150)
        lottery.join("first", 249)
        assertEquals(250L, lottery.registrationEnd)
        assertEquals(350L, lottery.bookingStart)
        assertEquals(450L, lottery.bookingEnd)
        assertEquals(1, lottery.winnerCount)
        assertThrows(IllegalStateException::class.java) { lottery.join("second", 249) }
        assertThrows(IllegalStateException::class.java) {
            lottery.update("owner", registrationEnd = 260, now = 250)
        }
    }

    @Test
    fun periodsAndWinnerCountsAreValidated() {
        val lottery = lottery()
        assertThrows(IllegalArgumentException::class.java) {
            lottery.update("owner", registrationStart = 200, now = 150)
        }
        assertThrows(IllegalArgumentException::class.java) {
            lottery.update("owner", bookingStart = 199, now = 150)
        }
        assertThrows(IllegalArgumentException::class.java) {
            lottery.update("owner", bookingEnd = 300, now = 150)
        }
        assertThrows(IllegalArgumentException::class.java) {
            lottery.update("owner", winnerCount = 0, now = 150)
        }
    }
}
