/*
 * OwnerCampsiteDetailScreen.kt is the owner's view of one campsite with buttons to create a
 * lottery, edit the campsite and download its QR code (US 02.04.01, 02.02.01, 02.06.01).
 * Outstanding issues: QR code generation/download is not built yet (needs a QR library).
 */

package com.example.camp_serve.ui.owner.campsites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.camp_serve.data.CampsiteRepository
import com.example.camp_serve.data.LotteryRepository
import com.example.camp_serve.model.Campsite
import com.example.camp_serve.model.Lottery
import com.example.camp_serve.ui.lottery.lotteryStatusText
import com.example.camp_serve.util.formatDisplayDate

/**
 * Owner's campsite detail.
 *
 * @param campsiteId campsite to show
 * @param ownerId owner using the app
 * @param campsiteRepository campsite data
 * @param lotteryRepository used to list the campsite's lotteries
 * @param onCreateLotteryClick called when the owner taps "Create Lottery"
 * @param onEditClick called when the owner taps "Edit Campsite"
 * @param onQrClick called when the owner taps "Download QR Code"
 */
@Composable
fun OwnerCampsiteDetailScreen(
    campsiteId: String,
    ownerId: String,
    campsiteRepository: CampsiteRepository,
    lotteryRepository: LotteryRepository,
    modifier: Modifier = Modifier,
    onCreateLotteryClick: (Campsite) -> Unit = {},
    onEditClick: (Campsite) -> Unit = {},
    onQrClick: (Campsite) -> Unit = {}
) {
    var loading by remember { mutableStateOf(true) }
    var campsite by remember { mutableStateOf<Campsite?>(null) }
    var lotteries by remember { mutableStateOf(listOf<Lottery>()) }

    LaunchedEffect(campsiteId) {
        campsite = campsiteRepository.getCampsite(campsiteId)
        lotteries = lotteryRepository.getLotteriesForCampsite(campsiteId)
        loading = false
    }

    val current = campsite
    if (current == null) {
        Text(if (loading) "Loading..." else "Campsite not found.", modifier = modifier.padding(16.dp))
        return
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(current.name, style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary)
        Text(current.location)

        Button(onClick = { onCreateLotteryClick(current) },
            modifier = Modifier.fillMaxWidth().testTag("create_lottery_button")) { Text("Create Lottery") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onEditClick(current) },
                modifier = Modifier.weight(1f).testTag("edit_campsite_button")) { Text("Edit Campsite") }
            OutlinedButton(onClick = { onQrClick(current) },
                modifier = Modifier.weight(1f).testTag("qr_button")) { Text("Download QR Code") }
        }

        Text("$${current.price} / night")
        Text(if (current.rating > 0.0) "★ ${current.rating}" else "No rating yet")
        Text("Max occupancy: ${current.maxOccupancy}")
        if (current.tags.isNotEmpty()) Text("Tags: ${current.tags.joinToString(", ")}")

        Text("Description", fontWeight = FontWeight.Bold)
        Text(current.description.ifBlank { "No description." })
        Text("Rules and cancellation policy", fontWeight = FontWeight.Bold)
        Text(current.rules.ifBlank { "No rules listed." })

        Text("Lotteries", fontWeight = FontWeight.Bold)
        if (lotteries.isEmpty()) Text("No lotteries yet.")
        for (lottery in lotteries) Text("${formatDisplayDate(lottery.registrationStart)} to ${formatDisplayDate(lottery.registrationEnd)} · ${lotteryStatusText(lottery)}")

        Text("Available Dates", fontWeight = FontWeight.Bold)
        Text(if (current.availableDates.isEmpty()) "None listed." else current.availableDates.joinToString(", "))

    }
}
