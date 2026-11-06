/*
 * OwnerLotteriesScreen.kt lists the owner's lotteries with a search bar and a
 * "Create lottery" button (storyboard step 3, US 02.04.06).
 * Outstanding issues: due lotteries are drawn when this screen opens (see LotteryDrawService).
 */

package com.example.camp_serve.ui.owner.lotteries

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.camp_serve.data.CampsiteRepository
import com.example.camp_serve.data.LotteryRepository
import com.example.camp_serve.data.TempCampsiteRepo
import com.example.camp_serve.data.TempLotteryRepo
import com.example.camp_serve.model.Lottery
import com.example.camp_serve.service.LotteryDrawService
import com.example.camp_serve.ui.lottery.lotteryStatusText
import com.example.camp_serve.util.formatDisplayDate

/**
 * The owner's lotteries.
 *
 * @param ownerId owner using the app
 * @param campsiteRepository used to show campsite names
 * @param lotteryRepository where the lotteries are loaded from
 * @param onCreateClick called when the owner taps "Create lottery"
 * @param onLotteryClick called when the owner taps a lottery
 */
@Composable
fun OwnerLotteriesScreen(
    ownerId: String,
    campsiteRepository: CampsiteRepository,
    lotteryRepository: LotteryRepository,
    modifier: Modifier = Modifier,
    onCreateClick: () -> Unit = {},
    onLotteryClick: (Lottery) -> Unit = {}
) {
    var loading by remember { mutableStateOf(true) }
    var lotteries by remember { mutableStateOf(listOf<Lottery>()) }
    var names by remember { mutableStateOf(mapOf<String, String>()) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(ownerId) {
        // draw any lottery whose registration has closed (US 02.05.01 stand-in)
        LotteryDrawService(lotteryRepository).drawAllDue(ownerId)
        lotteries = lotteryRepository.getLotteriesForOwner(ownerId)
        names = campsiteRepository.getCampsitesForOwner(ownerId).associate { it.id to it.name }
        loading = false
    }

    fun titleOf(lottery: Lottery): String =
        lottery.campsiteIds.map { names[it] ?: it }.joinToString(", ")

    val filtered = lotteries.filter { titleOf(it).contains(query, ignoreCase = true) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Lotteries", style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary)
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            label = { Text("Search lotteries") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (filtered.isEmpty()) {
                Text("No lotteries found.", modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered) { lottery ->
                        Card(modifier = Modifier.fillMaxWidth().clickable { onLotteryClick(lottery) }) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(titleOf(lottery), fontWeight = FontWeight.Bold)
                                Text("Registration: ${formatDisplayDate(lottery.registrationStart)} to ${formatDisplayDate(lottery.registrationEnd)}")
                                Text(lotteryStatusText(lottery), color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }

        Button(onClick = onCreateClick, modifier = Modifier.fillMaxWidth().testTag("create_lottery")) {
            Text("Create lottery")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OwnerLotteriesScreenPreview() {
    OwnerLotteriesScreen("fake-owner-01", TempCampsiteRepo(), TempLotteryRepo())
}
