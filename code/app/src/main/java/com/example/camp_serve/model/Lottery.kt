package com.example.camp_serve.model

import kotlin.random.Random

/**
 * A campsite lottery. Times are Unix epoch milliseconds, with inclusive
 * starts and exclusive ends. The waiting list is the pool of registered entrants.
 *
 * Use LotteryRepository for persistent changes and LotteryDrawService.start(id)
 * to monitor a stored lottery. Owner IDs must come from the authenticated caller.
 */
class Lottery(
    val id: String,
    val ownerId: String,
    campsites: List<Campsite>,
    registrationStart: Long,
    registrationEnd: Long,
    bookingStart: Long,
    bookingEnd: Long,
    winnerCount: Int,
    waitlistCapacity: Int? = null
) {
    enum class State { OPEN, DRAWN, BOOKING, CLOSED }

    var campsites: List<Campsite> = campsites.toList()
        private set
    var registrationStart: Long = registrationStart
        private set
    var registrationEnd: Long = registrationEnd
        private set
    var bookingStart: Long = bookingStart
        private set
    var bookingEnd: Long = bookingEnd
        private set
    var winnerCount: Int = winnerCount
        private set
    var waitlistCapacity: Int? = waitlistCapacity
        private set
    var state: State = State.OPEN
        private set

    var drawNumber: Long = 0
        private set

    var winnerIds: List<String> = emptyList()
        private set
    var loserIds: List<String> = emptyList()
        private set

    private val waitingList = WaitingList(id, ownerId, waitlistCapacity)

    init {
        require(id.isNotBlank()) { "Lottery ID must not be blank." }
        validate(campsites, registrationStart, registrationEnd, bookingStart,
            bookingEnd, winnerCount, waitlistCapacity)
    }

    @Synchronized
    fun join(entrantId: String, now: Long = System.currentTimeMillis()) {
        check(state == State.OPEN && now >= registrationStart && now < registrationEnd) {
            "Registration is not open."
        }
        waitingList.addEntrant(entrantId, now)
    }

    @Synchronized
    fun leave(entrantId: String, now: Long = System.currentTimeMillis()): Boolean {
        check(state == State.OPEN && now < registrationEnd) {
            "Registration has ended."
        }
        return waitingList.removeEntrant(entrantId)
    }

    @Synchronized
    fun getWaitingList(requesterId: String): List<WaitingList.Entry> =
        waitingList.getEntrants(requesterId) // only the campsite owner can view this

    /** Validate every change before applying any of them. Drawn lotteries are frozen. */
    @Synchronized
    fun update(
        requesterId: String,
        campsites: List<Campsite> = this.campsites,
        registrationStart: Long = this.registrationStart,
        registrationEnd: Long = this.registrationEnd,
        bookingStart: Long = this.bookingStart,
        bookingEnd: Long = this.bookingEnd,
        winnerCount: Int = this.winnerCount,
        waitlistCapacity: Int? = this.waitlistCapacity,
        now: Long = System.currentTimeMillis()
    ) {
        require(requesterId == ownerId) { "Only the owner can update the lottery." }
        check(state == State.OPEN && now < this.registrationEnd) {
            "The lottery can no longer be updated."
        }
        validate(campsites, registrationStart, registrationEnd, bookingStart,
            bookingEnd, winnerCount, waitlistCapacity)
        require(registrationEnd > now) { "Registration must end in the future." }
        val campsiteSnapshot = campsites.toList()
        waitingList.updateCapacity(waitlistCapacity)
        this.campsites = campsiteSnapshot
        this.registrationStart = registrationStart
        this.registrationEnd = registrationEnd
        this.bookingStart = bookingStart
        this.bookingEnd = bookingEnd
        this.winnerCount = winnerCount
        this.waitlistCapacity = waitlistCapacity
    }

    /** OPEN includes scheduled registration; [join] also checks its start time. */
    @Synchronized
    fun refreshState(now: Long = System.currentTimeMillis()): State {
        state = when {
            now >= bookingEnd -> State.CLOSED
            state == State.CLOSED -> State.CLOSED
            state == State.OPEN -> State.OPEN
            now >= bookingStart -> State.BOOKING
            else -> State.DRAWN
        }
        return state
    }

    /** Winners can book only within the configured booking period. */
    @Synchronized
    fun canBook(entrantId: String, now: Long = System.currentTimeMillis()): Boolean =
        refreshState(now) == State.BOOKING && entrantId in winnerIds

    @Synchronized
    internal fun drawIfReady(now: Long, random: Random): Pair<List<String>, List<String>>? {
        refreshState(now)
        if (state != State.OPEN || now < registrationEnd) return null
        return select(random, now)
    }

    /** A redraw replaces the entire selection, including during the booking window. */
    @Synchronized
    internal fun redraw(
        requesterId: String,
        now: Long,
        random: Random
    ): Pair<List<String>, List<String>> {
        require(requesterId == ownerId) { "Only the owner can redraw the lottery." }
        refreshState(now)
        check(state == State.DRAWN || state == State.BOOKING) {
            "Only a drawn lottery with an unexpired booking period can be redrawn."
        }
        return select(random, now)
    }

    private fun select(random: Random, now: Long): Pair<List<String>, List<String>> {
        val ids = waitingList.snapshot().map { it.id }.shuffled(random)
        // If fewer than N entrants registered, everyone wins.
        winnerIds = ids.take(winnerCount)
        loserIds = ids.drop(winnerCount)
        state = if (now >= bookingStart) State.BOOKING else State.DRAWN
        drawNumber++
        return winnerIds.toList() to loserIds.toList()
    }

    /** Rebuild a saved aggregate without weakening its public mutation rules. */
    @Synchronized
    internal fun restore(
        savedState: State,
        entrants: List<WaitingList.Entry>,
        winners: List<String>,
        losers: List<String>,
        drawNumber: Long
    ) {
        require(waitingList.snapshot().isEmpty()) { "Restore requires a fresh lottery." }
        val entrantIds = entrants.map { it.id }
        val selectedIds = winners + losers
        require(entrantIds.distinct().size == entrantIds.size) { "Duplicate stored entrant." }
        require(drawNumber >= 0) { "Invalid draw number." }
        if (drawNumber == 0L) {
            require(savedState == State.OPEN || savedState == State.CLOSED)
            require(selectedIds.isEmpty()) { "An undrawn lottery cannot have selections." }
        } else {
            require(savedState != State.OPEN) { "A drawn lottery cannot be open." }
            require(winners.size == minOf(winnerCount, entrants.size))
            require(selectedIds.distinct().size == selectedIds.size)
            require(selectedIds.toSet() == entrantIds.toSet()) { "Stored selection is incomplete." }
        }
        entrants.forEach { waitingList.addEntrant(it.id, it.timestamp) }
        state = savedState
        winnerIds = winners.toList()
        loserIds = losers.toList()
        this.drawNumber = drawNumber
    }

    private fun validate(
        campsites: List<Campsite>,
        registrationStart: Long,
        registrationEnd: Long,
        bookingStart: Long,
        bookingEnd: Long,
        winnerCount: Int,
        waitlistCapacity: Int?
    ) {
        require(ownerId.isNotBlank()) { "Owner ID must not be blank." }
        require(campsites.isNotEmpty()) { "At least one campsite is required." }
        require(campsites.all { it.id.isNotBlank() && it.ownerId == ownerId }) {
            "Every campsite must have an ID and belong to the lottery owner."
        }
        require(campsites.map { it.id }.distinct().size == campsites.size) {
            "Campsites must not be repeated."
        }
        require(registrationStart < registrationEnd) { "Registration period is invalid." }
        require(registrationEnd <= bookingStart && bookingStart < bookingEnd) {
            "Booking must follow registration and end after it starts."
        }
        require(winnerCount > 0) { "Winner count must be positive." }
        require(waitlistCapacity == null || waitlistCapacity >= 0) {
            "Waitlist capacity must not be negative."
        }
    }
}
