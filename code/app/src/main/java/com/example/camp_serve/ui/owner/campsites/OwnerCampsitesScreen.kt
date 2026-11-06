/*
 * OwnerCampsitesScreen.kt is the owner's "My Campsites" tab: search bar, a list with rating
 * and lottery status, and an "Add campsite" button (US 02.03.01).
 * Outstanding issues: none.
 */

package com.example.camp_serve.ui.owner.campsites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.example.camp_serve.model.Campsite
import java.util.Date

/**
 * The owner's campsites with search.
 *
 * @param ownerId owner using the app
 * @param campsiteRepository where campsites are loaded from
 * @param lotteryRepository used to show "Lottery Open" / "No Lottery"
 * @param onAddClick called when the owner taps "Add campsite"
 * @param onCampsiteClick called when the owner taps a campsite
 */
@Composable
fun OwnerCampsitesScreen(
    ownerId: String,
    campsiteRepository: CampsiteRepository,
    lotteryRepository: LotteryRepository,
    modifier: Modifier = Modifier,
    onAddClick: () -> Unit = {},
    onCampsiteClick: (Campsite) -> Unit = {}
) {
    var loading by remember { mutableStateOf(true) }
    var campsites by remember { mutableStateOf(listOf<Campsite>()) }
    var openIds by remember { mutableStateOf(setOf<String>()) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(ownerId) {
        campsites = campsiteRepository.getCampsitesForOwner(ownerId)
        val now = Date()
        // campsites that have a lottery with registration open right now
        openIds = lotteryRepository.getLotteriesForOwner(ownerId)
            .filter { it.isRegistrationOpen(now) }
            .flatMap { it.campsiteIds }
            .toSet()
        loading = false
    }

    val filtered = campsites.filter {
        it.name.contains(query, ignoreCase = true) || it.location.contains(query, ignoreCase = true)
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("My Campsites", style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary)
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            label = { Text("Search campsites") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (filtered.isEmpty()) {
                Text("No campsites found.", modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered) { campsite ->
                        Card(modifier = Modifier.fillMaxWidth().clickable { onCampsiteClick(campsite) }) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(campsite.name, fontWeight = FontWeight.Bold)
                                Text(campsite.location)
                                Row(modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween) {
                                    val rating = if (campsite.rating > 0.0) "★ ${campsite.rating}" else "No rating"
                                    Text(rating)
                                    Text(
                                        if (campsite.id in openIds) "Lottery Open" else "No Lottery",
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Button(onClick = onAddClick, modifier = Modifier.fillMaxWidth().testTag("add_campsite")) {
            Text("Add campsite")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OwnerCampsitesScreenPreview() {
    OwnerCampsitesScreen("fake-owner-01", TempCampsiteRepo(), TempLotteryRepo())
}
