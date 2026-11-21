package com.example.camp_serve.ui.owner.lotteries

import com.example.camp_serve.model.Campsite
import com.example.camp_serve.model.Lottery
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/*
 * LotteryForm.kt
 * Plain Kotlin logic behind the create / update lottery form
 * (US 02.04.01, 02.04.03, 02.04.04, 02.04.05, 02.04.06). No Android code, so it is unit tested.
 *
 * Dates in the form are whole calendar days, stored the way the Material date picker
 * gives them: the UTC midnight of that day, in epoch milliseconds ("day" values).
 * A Lottery stores real instants with inclusive starts and exclusive ends, so a period
 * of June 1 - June 8 is saved as [June 1 00:00, June 9 00:00) in the owner's time zone.
 */

const val DAY_MILLIS = 24L * 60 * 60 * 1000

/** Converts between date-picker "day" values and lottery instants, and formats them. */
object LotteryDates {
    private val utc: TimeZone = TimeZone.getTimeZone("UTC")

    /** The first instant of [day] in [zone]. */
    fun startOf(day: Long, zone: TimeZone = TimeZone.getDefault()): Long {
        val date = Calendar.getInstance(utc).apply { timeInMillis = day }
        return Calendar.getInstance(zone).apply {
            clear()
            set(date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }

    /** The exclusive end of [day] in [zone], i.e. the first instant of the next day. */
    fun endOf(day: Long, zone: TimeZone = TimeZone.getDefault()): Long = startOf(day + DAY_MILLIS, zone)

    /** The day (as a date-picker value) that contains [instant] in [zone]. */
    fun dayOf(instant: Long, zone: TimeZone = TimeZone.getDefault()): Long {
        val local = Calendar.getInstance(zone).apply { timeInMillis = instant }
        return Calendar.getInstance(utc).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }

    /** The last day included in a period that ends (exclusively) at [exclusiveEnd]. */
    fun lastDayOf(exclusiveEnd: Long, zone: TimeZone = TimeZone.getDefault()): Long =
        dayOf(exclusiveEnd - 1, zone)

    fun today(now: Long = System.currentTimeMillis(), zone: TimeZone = TimeZone.getDefault()): Long =
        dayOf(now, zone)

    /** "Jun 1, 2026" for a date-picker day value. */
    fun formatDay(day: Long): String = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        .apply { timeZone = utc }.format(day)

    /** "Jun 1 – Jun 8" for a period stored on a lottery. */
    fun formatPeriod(start: Long, exclusiveEnd: Long, zone: TimeZone = TimeZone.getDefault()): String {
        val format = SimpleDateFormat("MMM d", Locale.getDefault()).apply { timeZone = zone }
        val first = format.format(start)
        val last = format.format(startOf(lastDayOf(exclusiveEnd, zone), zone))
        return if (first == last) first else "$first – $last"
    }

    /** "Jun 1" for an instant. */
    fun formatDate(instant: Long, zone: TimeZone = TimeZone.getDefault()): String =
        SimpleDateFormat("MMM d", Locale.getDefault()).apply { timeZone = zone }.format(instant)

    /** "Jun 3, 2:15 PM" for when an entrant joined. */
    fun formatInstant(instant: Long, zone: TimeZone = TimeZone.getDefault()): String =
        SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).apply { timeZone = zone }.format(instant)
}

/** What the owner has typed or picked in the lottery form. */
data class LotteryForm(
    val campsiteIds: Set<String> = emptySet(),
    val registrationStartDay: Long? = null,
    val registrationEndDay: Long? = null,
    val bookingStartDay: Long? = null,
    val bookingEndDay: Long? = null,
    val winners: String = "",
    val waitlistCapacity: String = ""
) {
    companion object {
        /** The form filled in with a lottery's current settings (US 02.04.06). */
        fun from(lottery: Lottery, zone: TimeZone = TimeZone.getDefault()) = LotteryForm(
            campsiteIds = lottery.campsites.map { it.id }.toSet(),
            registrationStartDay = LotteryDates.dayOf(lottery.registrationStart, zone),
            registrationEndDay = LotteryDates.lastDayOf(lottery.registrationEnd, zone),
            bookingStartDay = LotteryDates.dayOf(lottery.bookingStart, zone),
            bookingEndDay = LotteryDates.lastDayOf(lottery.bookingEnd, zone),
            winners = lottery.winnerCount.toString(),
            waitlistCapacity = lottery.waitlistCapacity?.toString() ?: ""
        )
    }
}

/** One message per form section; null means that section is fine. */
data class LotteryFormErrors(
    val campsites: String? = null,
    val registration: String? = null,
    val booking: String? = null,
    val winners: String? = null,
    val waitlistCapacity: String? = null
) {
    val isValid: Boolean
        get() = listOf(campsites, registration, booking, winners, waitlistCapacity).all { it == null }
}

/**
 * Checks the form before anything is saved.
 *
 * @param today today's day value in the owner's time zone
 * @param isNew true when creating; a new lottery's registration can't start in the past
 * @param currentEntrants entrants already on the waitlist (when editing)
 */
fun validateLotteryForm(
    form: LotteryForm,
    today: Long,
    isNew: Boolean,
    currentEntrants: Int = 0
): LotteryFormErrors {
    val campsites = if (form.campsiteIds.isEmpty()) "Select at least one campsite" else null

    val regStart = form.registrationStartDay
    val regEnd = form.registrationEndDay
    val registration = when {
        regStart == null || regEnd == null -> "Choose a registration start and end date"
        regEnd < regStart -> "Registration end date can't be before the start date"
        isNew && regStart < today -> "Registration can't start in the past"
        regEnd < today -> "Registration must end today or later"
        else -> null
    }

    val bookStart = form.bookingStartDay
    val bookEnd = form.bookingEndDay
    val booking = when {
        bookStart == null || bookEnd == null -> "Choose a booking start and end date"
        bookEnd < bookStart -> "Booking end date can't be before the start date"
        regEnd != null && bookStart <= regEnd -> "Booking must start after registration ends"
        else -> null
    }

    val winnerCount = form.winners.trim().toIntOrNull()
    val winners = if (winnerCount == null || winnerCount < 1) {
        "Number of winners must be a whole number of at least 1"
    } else null

    val capacityText = form.waitlistCapacity.trim()
    val capacity = capacityText.toIntOrNull()
    val waitlistCapacity = when {
        capacityText.isEmpty() -> null
        capacity == null || capacity < 1 -> "Waitlist capacity must be a whole number of at least 1, or blank for no limit"
        capacity < currentEntrants -> "Waitlist capacity can't be less than the $currentEntrants entrants already on it"
        else -> null
    }

    return LotteryFormErrors(campsites, registration, booking, winners, waitlistCapacity)
}

/**
 * Builds the lottery to save from a form that passed [validateLotteryForm].
 *
 * @param ownerCampsites the owner's campsites; the selected ones are stored on the lottery
 */
fun LotteryForm.toLottery(
    id: String,
    ownerId: String,
    ownerCampsites: List<Campsite>,
    zone: TimeZone = TimeZone.getDefault()
): Lottery = Lottery(
    id = id,
    ownerId = ownerId,
    campsites = ownerCampsites.filter { it.id in campsiteIds },
    registrationStart = LotteryDates.startOf(checkNotNull(registrationStartDay), zone),
    registrationEnd = LotteryDates.endOf(checkNotNull(registrationEndDay), zone),
    bookingStart = LotteryDates.startOf(checkNotNull(bookingStartDay), zone),
    bookingEnd = LotteryDates.endOf(checkNotNull(bookingEndDay), zone),
    winnerCount = winners.trim().toInt(),
    waitlistCapacity = waitlistCapacity.trim().takeIf { it.isNotEmpty() }?.toInt()
)

/** Whether the owner may still edit this lottery: not drawn and registration not over. */
fun Lottery.isEditable(now: Long = System.currentTimeMillis()): Boolean =
    state == Lottery.State.OPEN && now < registrationEnd

/** Short status for lists and headers, e.g. "Registration open" or "Drawn · 5 winners". */
fun Lottery.statusText(now: Long = System.currentTimeMillis()): String = when (state) {
    Lottery.State.OPEN -> when {
        now < registrationStart -> "Registration opens ${LotteryDates.formatDate(registrationStart)}"
        now < registrationEnd -> "Registration open"
        else -> "Drawing winners…"
    }
    Lottery.State.DRAWN -> "Drawn · ${winnerIds.size} ${if (winnerIds.size == 1) "winner" else "winners"}"
    Lottery.State.BOOKING -> "Booking open · ${winnerIds.size} ${if (winnerIds.size == 1) "winner" else "winners"}"
    Lottery.State.CLOSED -> "Closed"
}

/** A readable title for a lottery, from its campsites' names. */
fun Lottery.title(): String = campsites.joinToString(", ") { it.name.ifBlank { "Campsite" } }
    .ifBlank { "Lottery" }

/** Turns a storage exception into a message for the owner. */
fun lotteryErrorMessage(error: Throwable, action: String): String {
    val messages = generateSequence(error) { it.cause }.mapNotNull { it.message }.toList()
    return when {
        messages.any { it == "The lottery can no longer be updated." } ->
            "This lottery can no longer be edited because registration has closed."
        messages.any { it.startsWith("Capacity cannot be smaller") } ->
            "Waitlist capacity can't be less than the number of entrants already on it."
        messages.any { it == "Registration must end in the future." } ->
            "Registration must end in the future."
        messages.any { it == "Lottery not found." } -> "This lottery no longer exists."
        else -> "Could not $action the lottery. Check your connection and try again."
    }
}
