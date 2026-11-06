/*
 * Lottery.kt
 *
 * Role: Model class for one lottery that an owner runs for one or more of their campsites
 * (US 02.04.01, 02.04.03, 02.04.04, 02.04.05, 02.04.06). Entrants join during the
 * registration period; winners are drawn when registration closes and may book during the
 * booking period.
 * Outstanding issues: none.
 */

package com.example.camp_serve.model

import com.example.camp_serve.util.dayAfter
import com.example.camp_serve.util.startOfDay
import java.util.Date

/** Where a lottery is in its life. */
enum class LotteryState { OPEN, DRAWN, CLOSED }

/**
 * A lottery for one or more campsites. Every field has a default so Firestore can rebuild it.
 *
 * @property id unique id of the lottery
 * @property ownerId id of the owner who created it
 * @property campsiteIds ids of the campsites the lottery is for
 * @property registrationStart first day entrants can join
 * @property registrationEnd last day entrants can join (the whole day counts)
 * @property bookingStart first day winners can reserve
 * @property bookingEnd last day winners can reserve (the whole day counts)
 * @property winnerCount how many winners are drawn, at least 1
 * @property waitlistCapacity most entrants allowed on the waitlist, null means no limit
 * @property state OPEN until drawn
 * @property drawnAt when the draw happened, null before that
 */
data class Lottery(
    val id: String = "",
    val ownerId: String = "",
    val campsiteIds: List<String> = emptyList(),
    val registrationStart: Date = Date(0),
    val registrationEnd: Date = Date(0),
    val bookingStart: Date = Date(0),
    val bookingEnd: Date = Date(0),
    val winnerCount: Int = 1,
    val waitlistCapacity: Int? = null,
    val state: LotteryState = LotteryState.OPEN,
    val drawnAt: Date? = null
) {
    /** The moment registration closes: midnight after the last registration day. */
    fun registrationClosesAt(): Date = dayAfter(registrationEnd)

    /** The moment booking closes: midnight after the last booking day. */
    fun bookingClosesAt(): Date = dayAfter(bookingEnd)

    /** True if entrants can join at time [now]. */
    fun isRegistrationOpen(now: Date = Date()): Boolean =
        state == LotteryState.OPEN &&
            !now.before(startOfDay(registrationStart)) &&
            now.before(registrationClosesAt())

    /** True if registration is over and the winners have not been drawn yet. */
    fun isDrawDue(now: Date = Date()): Boolean =
        state == LotteryState.OPEN && !now.before(registrationClosesAt())

    /** True if [day] falls inside the booking period. */
    fun isWithinBookingPeriod(day: Date): Boolean =
        !day.before(startOfDay(bookingStart)) && day.before(bookingClosesAt())

    /** True if winners can reserve at time [now]. */
    fun isBookingOpen(now: Date = Date()): Boolean =
        state == LotteryState.DRAWN && isWithinBookingPeriod(now)

    /** True if the owner can still edit this lottery (not once it is drawn). */
    fun canEdit(): Boolean = state == LotteryState.OPEN

    /** True if one more entrant fits on the waitlist given [currentEntries] already joined. */
    fun hasRoom(currentEntries: Int): Boolean =
        waitlistCapacity == null || currentEntries < waitlistCapacity
}
