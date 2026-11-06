/*
 * LotteryFormScreen.kt is the form for creating and editing a lottery (US 02.04.01,
 * 02.04.03, 02.04.04, 02.04.05, 02.04.06). Pass an existing lottery to edit it.
 * Outstanding issues: dates are typed as yyyy-MM-dd (no date picker yet).
 */

package com.example.camp_serve.ui.owner.lotteries

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.camp_serve.data.CampsiteRepository
import com.example.camp_serve.data.LotteryRepository
import com.example.camp_serve.data.TempCampsiteRepo
import com.example.camp_serve.data.TempLotteryRepo
import com.example.camp_serve.model.Campsite
import com.example.camp_serve.model.Lottery
import com.example.camp_serve.util.formatInputDate
import com.example.camp_serve.util.parseInputDate
import kotlinx.coroutines.launch

/**
 * Form to create a lottery, or to edit [existing] one.
 *
 * @param ownerId owner creating the lottery
 * @param campsiteRepository where the owner's campsites are loaded from
 * @param lotteryRepository where the lottery is saved
 * @param existing the lottery to edit, or null to create a new one
 * @param preselectedCampsiteId campsite ticked by default when creating
 * @param onSaved called with the saved lottery
 * @param onCancel called when the owner leaves without saving (nothing is created or changed)
 */
@Composable
fun LotteryFormScreen(
    ownerId: String,
    campsiteRepository: CampsiteRepository,
    lotteryRepository: LotteryRepository,
    modifier: Modifier = Modifier,
    existing: Lottery? = null,
    preselectedCampsiteId: String? = null,
    onSaved: (Lottery) -> Unit = {},
    onCancel: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var campsites by remember { mutableStateOf(listOf<Campsite>()) }
    val selected = remember {
        mutableStateListOf<String>().apply {
            if (existing != null) addAll(existing.campsiteIds)
            else if (preselectedCampsiteId != null) add(preselectedCampsiteId)
        }
    }
    var regStart by remember { mutableStateOf(existing?.let { formatInputDate(it.registrationStart) } ?: "") }
    var regEnd by remember { mutableStateOf(existing?.let { formatInputDate(it.registrationEnd) } ?: "") }
    var bookStart by remember { mutableStateOf(existing?.let { formatInputDate(it.bookingStart) } ?: "") }
    var bookEnd by remember { mutableStateOf(existing?.let { formatInputDate(it.bookingEnd) } ?: "") }
    var winners by remember { mutableStateOf(existing?.winnerCount?.toString() ?: "") }
    var capacity by remember { mutableStateOf(existing?.waitlistCapacity?.toString() ?: "") }
    var error by remember { mutableStateOf("") }

    LaunchedEffect(ownerId) {
        campsites = campsiteRepository.getCampsitesForOwner(ownerId)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = if (existing == null) "Create Lottery" else "Update Lottery",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Text("Select campsites", style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 12.dp))
        for (campsite in campsites) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = campsite.id in selected,
                    onCheckedChange = { checked ->
                        if (checked) selected.add(campsite.id) else selected.remove(campsite.id)
                    },
                    modifier = Modifier.testTag("campsite_check_${campsite.id}")
                )
                Text(campsite.name)
            }
        }

        Text("Registration period", style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 12.dp))
        OutlinedTextField(regStart, { regStart = it }, label = { Text("Start (yyyy-MM-dd)") },
            singleLine = true, modifier = Modifier.fillMaxWidth().testTag("registration_start"))
        OutlinedTextField(regEnd, { regEnd = it }, label = { Text("End (yyyy-MM-dd)") },
            singleLine = true, modifier = Modifier.fillMaxWidth().testTag("registration_end"))

        Text("Booking period", style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 12.dp))
        OutlinedTextField(bookStart, { bookStart = it }, label = { Text("Start (yyyy-MM-dd)") },
            singleLine = true, modifier = Modifier.fillMaxWidth().testTag("booking_start"))
        OutlinedTextField(bookEnd, { bookEnd = it }, label = { Text("End (yyyy-MM-dd)") },
            singleLine = true, modifier = Modifier.fillMaxWidth().testTag("booking_end"))

        OutlinedTextField(winners, { winners = it }, label = { Text("Number of winners") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).testTag("winners"))
        OutlinedTextField(capacity, { capacity = it }, label = { Text("Waitlist capacity (optional)") },
            singleLine = true, modifier = Modifier.fillMaxWidth().testTag("waitlist_capacity"))

        if (error != "") {
            Text(error, color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("form_error"))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row {
            OutlinedButton(onClick = onCancel, modifier = Modifier.testTag("cancel_lottery")) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                modifier = Modifier.testTag("save_lottery"),
                onClick = {
                    // check the boxes one by one, same order as the form
                    val rs = parseInputDate(regStart)
                    val re = parseInputDate(regEnd)
                    val bs = parseInputDate(bookStart)
                    val be = parseInputDate(bookEnd)
                    if (selected.isEmpty()) {
                        error = ERROR_NO_CAMPSITE
                    } else if (rs == null || re == null || bs == null || be == null) {
                        error = ERROR_DATE_FORMAT
                    } else if (!isValidRegistrationPeriod(rs, re)) {
                        error = ERROR_REGISTRATION_ORDER
                    } else if (!isValidBookingStart(re, bs)) {
                        error = ERROR_BOOKING_START
                    } else if (!isValidBookingPeriod(bs, be)) {
                        error = ERROR_BOOKING_ORDER
                    } else if (!isValidWinnerCount(winners)) {
                        error = ERROR_WINNERS
                    } else if (!isValidWaitlistCapacity(capacity)) {
                        error = ERROR_CAPACITY
                    } else {
                        error = ""
                        val base = existing ?: Lottery(ownerId = ownerId)
                        val lottery = base.copy(
                            campsiteIds = selected.toList(),
                            registrationStart = rs, registrationEnd = re,
                            bookingStart = bs, bookingEnd = be,
                            winnerCount = winners.trim().toInt(),
                            waitlistCapacity = capacity.trim().toIntOrNull()
                        )
                        scope.launch {
                            if (existing == null) {
                                onSaved(lotteryRepository.createLottery(lottery))
                            } else if (lotteryRepository.updateLottery(lottery)) {
                                onSaved(lottery)
                            } else {
                                error = "This lottery has already been drawn and can't be edited"
                            }
                        }
                    }
                }
            ) {
                Text(if (existing == null) "Submit" else "Save Changes")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LotteryFormScreenPreview() {
    LotteryFormScreen(
        ownerId = "fake-owner-01",
        campsiteRepository = TempCampsiteRepo(),
        lotteryRepository = TempLotteryRepo()
    )
}
