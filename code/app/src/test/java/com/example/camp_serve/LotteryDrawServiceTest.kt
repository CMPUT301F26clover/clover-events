package com.example.camp_serve

import com.example.camp_serve.model.Campsite
import com.example.camp_serve.model.Lottery
import com.example.camp_serve.service.LotteryDrawService
import com.example.camp_serve.service.NotificationService
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class LotteryDrawServiceTest {
    private fun lottery(winners: Int = 2) = Lottery(
        id = "lottery", ownerId = "owner",
        campsites = listOf(Campsite(id = "site", ownerId = "owner")),
        registrationStart = 100, registrationEnd = 200,
        bookingStart = 300, bookingEnd = 400, winnerCount = winners
    )

    @Test
    fun drawPartitionsEntrantsAndTriggersNotificationAndResultHooks() = runBlocking {
        val lottery = lottery()
        repeat(8) { lottery.join("entrant-$it", 100) }
        val wins = mutableListOf<String>()
        val losses = mutableListOf<String>()
        val notifications = NotificationService()
        var resultCount = 0
        val service = LotteryDrawService(
            scope = this, notificationService = notifications,
            random = Random(7), clock = { 200 },
            notifyWinner = { sender, drawnLottery, id ->
                assertSame(notifications, sender)
                assertSame(lottery, drawnLottery)
                wins.add(id)
            },
            notifyLoser = { _, _, id -> losses.add(id) },
            onResult = { _, _ -> resultCount++ }
        )

        assertNotNull(service.draw(lottery))
        assertEquals(Lottery.State.DRAWN, lottery.state)
        assertEquals(2, lottery.winnerIds.size)
        assertEquals(lottery.winnerIds, wins)
        assertEquals(lottery.loserIds, losses)
        assertTrue(wins.intersect(losses.toSet()).isEmpty())
        assertEquals((0..7).map { "entrant-$it" }.toSet(), (wins + losses).toSet())
        assertEquals(1, resultCount)
        assertThrows(IllegalStateException::class.java) { service.draw(lottery) }
        assertEquals(1, resultCount)
        assertFalse(lottery.canBook(wins.first(), 299))
        assertTrue(lottery.canBook(wins.first(), 300))
        assertFalse(lottery.canBook(losses.first(), 300))
        assertEquals(Lottery.State.BOOKING, lottery.state)
        assertFalse(lottery.canBook(wins.first(), 400))
        assertEquals(Lottery.State.CLOSED, lottery.state)
    }

    @Test
    fun earlyAndExpiredDrawsAreRejected() = runBlocking {
        val lottery = lottery()
        var now = 199L
        val service = LotteryDrawService(this, clock = { now })
        assertThrows(IllegalStateException::class.java) { service.draw(lottery) }
        now = 400
        assertThrows(IllegalStateException::class.java) { service.draw(lottery) }
        assertEquals(Lottery.State.CLOSED, lottery.state)
    }

    @Test
    fun undersubscribedAndEmptyLotteriesDoNotInventWinners() = runBlocking {
        val service = LotteryDrawService(this, clock = { 300 })
        val lottery = lottery(winners = 5)
        lottery.join("only-entrant", 100)
        service.draw(lottery)
        assertEquals(listOf("only-entrant"), lottery.winnerIds)
        assertTrue(lottery.loserIds.isEmpty())
        assertEquals(Lottery.State.BOOKING, lottery.state)

        val empty = lottery()
        service.draw(empty)
        assertTrue(empty.winnerIds.isEmpty())
        assertTrue(empty.loserIds.isEmpty())
    }

    @Test
    fun ownerRedrawReplacesSelectionAndRejectsUnauthorizedOrClosedRedraws() = runBlocking {
        val lottery = lottery()
        repeat(12) { lottery.join("entrant-$it", 100) }
        var now = 200L
        var notifications = 0
        val service = LotteryDrawService(this, random = Random(42), clock = { now },
            notifyWinner = { _, _, _ -> notifications++ })
        assertThrows(IllegalStateException::class.java) { service.redraw(lottery, "owner") }
        service.draw(lottery)
        val firstWinners = lottery.winnerIds
        assertThrows(IllegalArgumentException::class.java) { service.redraw(lottery, "other") }
        assertEquals(firstWinners, lottery.winnerIds)
        now = 300
        service.redraw(lottery, "owner")
        assertEquals((0..11).map { "entrant-$it" }.toSet(),
            (lottery.winnerIds + lottery.loserIds).toSet())
        assertEquals(2, lottery.winnerIds.size)
        assertEquals(10, lottery.loserIds.size)
        assertEquals(4, notifications)
        assertThrows(IllegalStateException::class.java) {
            lottery.update("owner", winnerCount = 3, now = now)
        }
        now = 400
        assertThrows(IllegalStateException::class.java) { service.redraw(lottery, "owner") }
        Unit
    }

    @Test
    fun schedulerDrawsAutomaticallyAndClosesAtBookingEnd() = runBlocking {
        val lottery = lottery()
        lottery.join("entrant", 100)
        val now = java.util.concurrent.atomic.AtomicLong(199)
        var draws = 0
        val service = LotteryDrawService(this, clock = now::get,
            onResult = { _, _ -> draws++ })
        val job = service.start(lottery)
        // Let the monitor observe the open lottery before moving across registration end.
        yield()
        assertTrue(lottery.winnerIds.isEmpty())
        now.set(300)
        withTimeout(3_000) {
            while (draws == 0) kotlinx.coroutines.delay(10)
        }
        assertEquals(listOf("entrant"), lottery.winnerIds)
        assertEquals(Lottery.State.BOOKING, lottery.state)
        now.set(400)
        withTimeout(3_000) { job.join() }
        assertEquals(Lottery.State.CLOSED, lottery.state)
        assertEquals(1, draws)
    }
}
