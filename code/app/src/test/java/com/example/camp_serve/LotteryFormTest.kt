package com.example.camp_serve

import com.example.camp_serve.model.Campsite
import com.example.camp_serve.model.Lottery
import com.example.camp_serve.ui.owner.lotteries.LotteryDates
import com.example.camp_serve.ui.owner.lotteries.LotteryForm
import com.example.camp_serve.ui.owner.lotteries.isEditable
import com.example.camp_serve.ui.owner.lotteries.lotteryErrorMessage
import com.example.camp_serve.ui.owner.lotteries.toLottery
import com.example.camp_serve.ui.owner.lotteries.validateLotteryForm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class LotteryFormTest {
    private val edmonton = TimeZone.getTimeZone("America/Edmonton")
    private val utc = TimeZone.getTimeZone("UTC")

    /** A date-picker day value (UTC midnight) for 2026-06-[dayOfMonth]. */
    private fun june(dayOfMonth: Int): Long = Calendar.getInstance(utc).apply {
        clear()
        set(2026, Calendar.JUNE, dayOfMonth)
    }.timeInMillis

    private val today = june(1)
    private val sites = listOf(
        Campsite(id = "banff", ownerId = "owner", name = "Banff Pines"),
        Campsite(id = "hinton", ownerId = "owner", name = "Hinton Camp")
    )

    // Storyboard example: registration Jun 1-8, booking Jun 9-12, 5 winners, capacity 30.
    private val valid = LotteryForm(
        campsiteIds = setOf("banff"),
        registrationStartDay = june(1),
        registrationEndDay = june(8),
        bookingStartDay = june(9),
        bookingEndDay = june(12),
        winners = "5",
        waitlistCapacity = "30"
    )

    @Test
    fun validFormHasNoErrors() {
        assertTrue(validateLotteryForm(valid, today, isNew = true).isValid)
        // Capacity is optional; a one-day registration period is allowed.
        val oneDay = valid.copy(registrationEndDay = june(1), bookingStartDay = june(2), waitlistCapacity = " ")
        assertTrue(validateLotteryForm(oneDay, today, isNew = true).isValid)
    }

    @Test
    fun registrationEndBeforeStartIsRejected() {
        val errors = validateLotteryForm(valid.copy(registrationStartDay = june(5), registrationEndDay = june(4)), today, true)
        assertNotNull(errors.registration)
        assertFalse(errors.isValid)
        assertNotNull(validateLotteryForm(valid.copy(registrationStartDay = null), today, true).registration)
    }

    @Test
    fun bookingMustStartAfterRegistrationEnds() {
        assertNotNull(validateLotteryForm(valid.copy(bookingStartDay = june(8)), today, true).booking)
        assertNotNull(validateLotteryForm(valid.copy(bookingStartDay = june(5)), today, true).booking)
        assertNotNull(validateLotteryForm(valid.copy(bookingEndDay = june(10), bookingStartDay = june(11)), today, true).booking)
        assertNull(validateLotteryForm(valid.copy(bookingStartDay = june(9), bookingEndDay = june(9)), today, true).booking)
    }

    @Test
    fun winnersMustBeAWholeNumberOfAtLeastOne() {
        listOf("", "0", "-2", "2.5", "abc").forEach {
            assertNotNull(it, validateLotteryForm(valid.copy(winners = it), today, true).winners)
        }
        assertNull(validateLotteryForm(valid.copy(winners = " 1 "), today, true).winners)
    }

    @Test
    fun capacityIsOptionalButCannotDropBelowCurrentEntrants() {
        assertNull(validateLotteryForm(valid.copy(waitlistCapacity = ""), today, false, currentEntrants = 12).waitlistCapacity)
        assertNotNull(validateLotteryForm(valid.copy(waitlistCapacity = "0"), today, true).waitlistCapacity)
        assertNotNull(validateLotteryForm(valid.copy(waitlistCapacity = "10"), today, false, currentEntrants = 12).waitlistCapacity)
        assertNull(validateLotteryForm(valid.copy(waitlistCapacity = "12"), today, false, currentEntrants = 12).waitlistCapacity)
    }

    @Test
    fun newLotteryCannotStartInThePastButEditsCan() {
        val later = june(3)
        assertNotNull(validateLotteryForm(valid, later, isNew = true).registration)
        assertNull(validateLotteryForm(valid, later, isNew = false).registration)
        // Either way registration must not already be over.
        assertNotNull(validateLotteryForm(valid, june(9), isNew = false).registration)
    }

    @Test
    fun atLeastOneCampsiteIsRequired() {
        assertNotNull(validateLotteryForm(valid.copy(campsiteIds = emptySet()), today, true).campsites)
    }

    @Test
    fun formBecomesALotteryWithWholeDayPeriodsInTheOwnersZone() {
        val lottery = valid.copy(campsiteIds = setOf("banff", "hinton"))
            .toLottery("id", "owner", sites, edmonton)
        val local = Calendar.getInstance(edmonton)
        fun at(day: Int) = local.apply { clear(); set(2026, Calendar.JUNE, day) }.timeInMillis

        assertEquals(at(1), lottery.registrationStart)
        assertEquals(at(9), lottery.registrationEnd) // exclusive end: all of Jun 8 is included
        assertEquals(at(9), lottery.bookingStart)
        assertEquals(at(13), lottery.bookingEnd)
        assertEquals(5, lottery.winnerCount)
        assertEquals(30, lottery.waitlistCapacity)
        assertEquals(listOf("banff", "hinton"), lottery.campsites.map { it.id })

        assertNull(valid.copy(waitlistCapacity = "").toLottery("id", "owner", sites, edmonton).waitlistCapacity)
    }

    @Test
    fun editFormIsPrefilledWithTheSameDays() {
        val lottery = valid.toLottery("id", "owner", sites, edmonton)
        assertEquals(valid, LotteryForm.from(lottery, edmonton))
    }

    @Test
    fun periodsFormatAsInclusiveDayRanges() {
        val lottery = valid.toLottery("id", "owner", sites, edmonton)
        assertEquals("Jun 1 – Jun 8", LotteryDates.formatPeriod(lottery.registrationStart, lottery.registrationEnd, edmonton))
        assertEquals("Jun 9 – Jun 12", LotteryDates.formatPeriod(lottery.bookingStart, lottery.bookingEnd, edmonton))
    }

    @Test
    fun lotteryCanOnlyBeEditedBeforeRegistrationCloses() {
        val lottery = valid.toLottery("id", "owner", sites, edmonton)
        assertTrue(lottery.isEditable(lottery.registrationEnd - 1))
        assertFalse(lottery.isEditable(lottery.registrationEnd))


        // After the draw the lottery is frozen, even before booking starts.
        val drawn = Lottery("drawn", "owner", sites.take(1), 100, 200, 300, 400, 1)
        drawn.join("a", 150)
        drawn.drawIfReady(200, kotlin.random.Random(1))
        assertFalse(drawn.isEditable(250))
    }

    @Test
    fun storageErrorsBecomeReadableMessages() {
        val wrapped = RuntimeException("transaction failed", IllegalStateException("The lottery can no longer be updated."))
        assertTrue(lotteryErrorMessage(wrapped, "update").contains("no longer be edited"))
        assertTrue(lotteryErrorMessage(RuntimeException("offline"), "create").startsWith("Could not create"))
    }
}
