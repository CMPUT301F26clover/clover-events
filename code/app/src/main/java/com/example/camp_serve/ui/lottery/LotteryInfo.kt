/*
 * LotteryInfo.kt holds a small section that shows a lottery's periods and winners. Owner
 * screens use it, and the entrant's campsite details screen can call it too (US 02.04.01,
 * 02.04.03, 02.04.04).
 * Outstanding issues: none.
 */

package com.example.camp_serve.ui.lottery

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.camp_serve.model.Lottery
import com.example.camp_serve.model.LotteryState
import com.example.camp_serve.util.formatDisplayDate
import java.util.Date

/**
 * Short status text for a lottery, like "Drawn · 5 winners".
 *
 * @param lottery the lottery
 * @param now current time
 */
fun lotteryStatusText(lottery: Lottery, now: Date = Date()): String = when {
    lottery.state == LotteryState.DRAWN ->
        "Drawn · ${lottery.winnerCount} winner" + (if (lottery.winnerCount == 1) "" else "s")
    lottery.isRegistrationOpen(now) -> "Registration open"
    now.before(lottery.registrationStart) -> "Registration opens ${formatDisplayDate(lottery.registrationStart)}"
    else -> "Registration closed"
}

/**
 * Shows status, registration period, booking period and number of winners.
 *
 * @param lottery the lottery to show
 */
@Composable
fun LotteryInfoSection(lottery: Lottery, modifier: Modifier = Modifier) {
    Column(modifier = modifier.testTag("lottery_info")) {
        Text(lotteryStatusText(lottery))
        Text("Registration: ${formatDisplayDate(lottery.registrationStart)} to ${formatDisplayDate(lottery.registrationEnd)}")
        Text("Booking: ${formatDisplayDate(lottery.bookingStart)} to ${formatDisplayDate(lottery.bookingEnd)}")
        Text("Winners: ${lottery.winnerCount}")
        val capacity = lottery.waitlistCapacity
        if (capacity != null) Text("Waitlist capacity: $capacity")
    }
}
