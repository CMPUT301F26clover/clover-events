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
import com.example.camp_serve.data.ReservationRepository
import com.example.camp_serve.data.sampleOffer
import com.example.camp_serve.model.CampsiteOffer
import com.example.camp_serve.ui.theme.CampserveTheme
import kotlinx.coroutines.launch

/**
 * Loads the offer for a lottery winner and shows [ReserveScreen].
 * Reserving or declining saves through the repository and shows a message.
 * Outstanding issues: no navigation yet, so nothing opens this screen.
 *
 * @param repository where the offer is loaded from and the choice is saved to
 * @param onDone called after the entrant reserved or declined
 */
@Composable
fun ReserveRoute(
    repository: ReservationRepository,
    modifier: Modifier = Modifier,
    onDone: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var offer by remember { mutableStateOf(CampsiteOffer()) }
    var result by remember { mutableStateOf<String?>(null) }

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
        ReserveScreen(
            offer = offer,
            onReserve = { date ->
                scope.launch {
                    repository.reserve(offer.id, date)
                    result = "Reserved ${offer.campsite} for $date."
                    onDone()
                }
            },
            onDecline = {
                scope.launch {
                    repository.decline(offer.id)
                    result = "You declined the spot."
                    onDone()
                }
            },
            modifier = modifier
        )
    }
}

@Composable
fun ReserveScreen(
    offer: CampsiteOffer,
    onReserve: (String) -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedDate by remember { mutableStateOf<String?>(null) }
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
        offer.availableDates.forEach { date ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = selectedDate == date, onClick = { selectedDate = date })
                Text(date)
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
            onClick = { selectedDate?.let(onReserve) },
            enabled = selectedDate != null && accepted,
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