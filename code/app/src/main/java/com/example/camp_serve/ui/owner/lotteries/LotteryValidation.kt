/*
 * LotteryValidation.kt holds the checks and error messages for the create/edit lottery form
 * (US 02.04.03, 02.04.04, 02.04.05).
 * Outstanding issues: none.
 */

package com.example.camp_serve.ui.owner.lotteries

import java.util.Date

const val ERROR_NO_CAMPSITE = "Select at least one campsite"
const val ERROR_DATE_FORMAT = "Enter dates like 2026-06-09"
const val ERROR_REGISTRATION_ORDER = "Registration end can't be before registration start"
const val ERROR_BOOKING_START = "Booking must start after registration ends"
const val ERROR_BOOKING_ORDER = "Booking end can't be before booking start"
const val ERROR_WINNERS = "Number of winners must be a whole number, at least 1"
const val ERROR_CAPACITY = "Waitlist capacity must be a whole number, at least 1, or empty"

// winners has to be a whole number that's at least 1
fun isValidWinnerCount(text: String): Boolean {
    val number = text.trim().toIntOrNull() ?: return false
    return number >= 1
}

// capacity is optional: empty is fine, otherwise a whole number of at least 1
fun isValidWaitlistCapacity(text: String): Boolean {
    if (text.isBlank()) return true
    val number = text.trim().toIntOrNull() ?: return false
    return number >= 1
}

// registration end must not be before registration start
fun isValidRegistrationPeriod(start: Date, end: Date): Boolean = !end.before(start)

// booking has to start after the last registration day
fun isValidBookingStart(registrationEnd: Date, bookingStart: Date): Boolean =
    bookingStart.after(registrationEnd)

// booking end must not be before booking start
fun isValidBookingPeriod(start: Date, end: Date): Boolean = !end.before(start)
