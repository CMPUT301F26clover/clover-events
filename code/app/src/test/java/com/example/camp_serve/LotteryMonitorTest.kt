package com.example.camp_serve

import com.example.camp_serve.model.Campsite
import com.example.camp_serve.model.Lottery
import com.example.camp_serve.service.LotteryMonitor
import kotlinx.coroutines.Job
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LotteryMonitorTest {
    private fun lottery(id: String) = Lottery(
        id, "owner", listOf(Campsite(id = "site", ownerId = "owner")), 100, 200, 300, 400, 1
    )

    @Test
    fun monitorsEachUnclosedLotteryOnceAndRestartsStoppedMonitors() {
        val started = mutableListOf<String>()
        val jobs = mutableMapOf<String, Job>()
        val monitor = LotteryMonitor { id -> started += id; Job().also { jobs[id] = it } }
        val a = lottery("a")
        val b = lottery("b")
        val closed = lottery("closed").apply { refreshState(400) }

        monitor.sync(listOf(a, b, closed))
        monitor.sync(listOf(a, b, closed))
        assertEquals(listOf("a", "b"), started)

        jobs.getValue("a").cancel()      // e.g. went offline
        monitor.sync(listOf(a, b))
        assertEquals(listOf("a", "b", "a"), started)

        val bJob = jobs.getValue("b")
        monitor.sync(listOf(a))          // b deleted or closed
        assertTrue(bJob.isCancelled)
        assertEquals(setOf("a"), monitor.monitoredIds())

        monitor.stopAll()
        assertTrue(monitor.monitoredIds().isEmpty())
    }

    @Test
    fun drawSelectsNumberOfWinnersOrEveryoneIfFewerJoined() {
        val few = lottery("few").apply { update("owner", winnerCount = 5, now = 150); join("x", 150); join("y", 150) }
        few.drawIfReady(200, Random(1))
        assertEquals(setOf("x", "y"), few.winnerIds.toSet())

        val many = lottery("many").apply { update("owner", winnerCount = 2, now = 150) }
        (1..6).forEach { many.join("e$it", 150) }
        many.drawIfReady(200, Random(1))
        assertEquals(2, many.winnerIds.size)
        assertEquals(4, many.loserIds.size)
    }
}
