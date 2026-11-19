package com.example.camp_serve.service

import com.example.camp_serve.model.Lottery
import kotlinx.coroutines.Job

/**
 * Keeps one [LotteryDrawService.start] monitor running for each of an owner's lotteries
 * that has not closed, so winners are drawn automatically when registration ends
 * (US 02.04.05) while the owner's app is open.
 *
 * Call [sync] with the latest list of lotteries. A monitor that stopped (for example
 * because the device went offline) is restarted on the next [sync].
 *
 * Outstanding issue: draws only run while some device is monitoring the lottery.
 * A server-side scheduler (e.g. a Cloud Function) would make draws happen on time
 * even when no one has the app open.
 *
 * @param start starts monitoring one lottery id; normally [LotteryDrawService.start]
 */
class LotteryMonitor(private val start: (String) -> Job) {
    private val jobs = mutableMapOf<String, Job>()

    @Synchronized
    fun sync(lotteries: List<Lottery>) {
        val wanted = lotteries.filter { it.state != Lottery.State.CLOSED }.map { it.id }.toSet()
        val gone = jobs.keys - wanted
        gone.forEach { jobs.remove(it)?.cancel() }
        wanted.forEach { id ->
            if (jobs[id]?.isActive != true) jobs[id] = start(id)
        }
    }

    /** The lottery ids currently being monitored. */
    @Synchronized
    fun monitoredIds(): Set<String> = jobs.filterValues { it.isActive }.keys.toSet()

    @Synchronized
    fun stopAll() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
    }
}
