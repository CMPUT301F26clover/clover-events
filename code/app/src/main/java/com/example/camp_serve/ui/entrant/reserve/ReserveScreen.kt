package com.example.camp_serve.ui.entrant.reserve

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.camp_serve.data.ReservationRecordRepository
import com.example.camp_serve.data.ReservationRepository
import com.example.camp_serve.data.sampleOffer
import com.example.camp_serve.model.CampsiteOffer
import com.example.camp_serve.model.Reservation
import com.example.camp_serve.model.StayDates
import com.example.camp_serve.ui.theme.CampserveTheme
import kotlinx.coroutines.launch

/*
 * ReserveScreen.kt
 * The screen a lottery winner uses to pick dates, accept the campsite rules and
 * reserve or decline the spot (US 01.04.01, 01.04.02, 01.04.03).
 * Outstanding issues: the offer is still sample data, not built from the
 * winner's lottery result.
 */

/**
 * Whether the Reserve button is enabled: the entrant must have chosen a stay
 * and accepted the rules and policies.
 *
 * @param selectedStay the stay the entrant chose, or null if none yet
 * @param accepted true if the entrant ticked the rules checkbox
 */
fun canReserve(selectedStay: StayDates?, accepted: Boolean): Boolean =
    selectedStay != null && accepted

/**
 * Builds the reservation saved when a winner reserves. The rules are marked
 * accepted because the Reserve button is only enabled after they were.
 *
 * @param offer the offer being reserved
 * @param stay the stay the entrant chose
 * @param entrantId id of the entrant who reserved
 * @param entrantName name of the entrant, saved for the owner's list
 */
fun buildReservation(
    offer: CampsiteOffer,
    stay: StayDates,
    entrantId: String,
    entrantName: String
): Reservation = Reservation(
    campsiteId = offer.campsiteId,
    campsiteName = offer.campsite,
    ownerId = offer.ownerId,
    entrantId = entrantId,
    entrantName = entrantName,
    startDate = stay.startDate,
    endDate = stay.endDate,
    status = "active",
    rulesAccepted = true
)

/**
 * Loads the winner's offer and shows [ReserveScreen]. Reserving saves a
 * reservation through [recordRepository]; declining is recorded through
 * [repository]. Shows a message afterwards, or an error if saving failed.
 *
 * @param repository where the offer is loaded from and the answer recorded
 * @param recordRepository where the reservation is saved
 * @param entrantId id of the signed-in entrant
 * @param entrantName name of the signed-in entrant
 * @param onDone called after the entrant reserved or declined
 */
@Composable
fun ReserveRoute(
    repository: ReservationRepository,
    recordRepository: ReservationRecordRepository,
    entrantId: String,
    entrantName: String,
    modifier: Modifier = Modifier,
    onDone: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var offer by remember { mutableStateOf(CampsiteOffer()) }
    var result by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        offer = repository.getOffer()
        loading = false
    }

    val message = result
    if (loading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (message != null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(message)
        }
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
            ReserveScreen(
                offer = offer,
                onReserve = { stay ->
                    scope.launch {
                        try {
                            repository.reserve(offer.id, stay)
                            recordRepository.addReservation(
                                buildReservation(offer, stay, entrantId, entrantName)
                            )
                            result = "Reserved ${offer.campsite} for ${stay.label}."
                            onDone()
                        } catch (e: Exception) {
                            error = "Couldn't save your reservation. Please try again."
                        }
                    }
                },
                onDecline = {
                    scope.launch {
                        try {
                            repository.decline(offer.id)
                            result = "You declined the spot."
                            onDone()
                        } catch (e: Exception) {
                            error = "Couldn't save your answer. Please try again."
                        }
                    }
                }
            )
        }
    }
}

/**
 * The Reserve form: stay options, rules, the accept checkbox and the Reserve
 * and Decline buttons. Reserve stays disabled until [canReserve] allows it.
 *
 * @param offer the offer to show
 * @param onReserve called with the chosen stay when the entrant reserves
 * @param onDecline called when the entrant declines
 */
@Composable
fun ReserveScreen(
    offer: CampsiteOffer,
    onReserve: (StayDates) -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedStay by remember { mutableStateOf<StayDates?>(null) }
    var accepted by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Reserve",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            offer.campsite,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Choose your dates",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        offer.availableDates.forEach { stay ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = selectedStay == stay, onClick = { selectedStay = stay })
                Text(stay.label)
            }
        }

        HorizontalDivider()

        Text(
            "Rules and policies",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(offer.rules)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = accepted, onCheckedChange = { accepted = it })
            Text("I accept the rules and policies")
        }

        Button(
            onClick = { selectedStay?.let(onReserve) },
            enabled = canReserve(selectedStay, accepted),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reserve")
        }
        OutlinedButton(onClick = onDecline, modifier = Modifier.fillMaxWidth()) {
            Text("Decline")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReserveScreenPreview() {
    CampserveTheme {
        ReserveScreen(offer = sampleOffer, onReserve = {}, onDecline = {})
    }
}