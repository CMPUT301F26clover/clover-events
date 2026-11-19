package com.example.camp_serve.ui.owner.lotteries

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camp_serve.data.LotteryRepository
import com.example.camp_serve.model.Lottery
import com.example.camp_serve.ui.theme.CampSurfaceVariant
import kotlinx.coroutines.CancellationException

/*
 * LotteriesScreen.kt
 * "Lotteries" tab: every lottery the owner has created, with a search bar and a
 * Create lottery button. Tapping a lottery opens its details and waitlist.
 */

@Composable
fun LotteriesScreen(
    lotteryRepository: LotteryRepository,
    ownerId: String,
    onLotteryClick: (Lottery) -> Unit,
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var lotteries by remember { mutableStateOf<List<Lottery>?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(ownerId, retry) {
        loadFailed = false
        try {
            lotteryRepository.observeLotteriesForOwner(ownerId).collect { lotteries = it }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            loadFailed = true
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        LotteryHeader(title = "Lotteries", onBack = null)

        TextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search Lotteries", fontSize = 14.sp) },
            trailingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Search") },
            singleLine = true,
            shape = RoundedCornerShape(50),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = CampSurfaceVariant,
                unfocusedContainerColor = CampSurfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            PillButton("Create lottery", onClick = onCreateClick)
        }

        val all = lotteries
        when {
            loadFailed -> Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Could not load your lotteries.", color = Color.Gray)
                TextButton(onClick = { retry++ }) { Text("Retry") }
            }
            all == null -> CenteredLoading()
            all.isEmpty() -> CenteredMessage("You haven't created any lotteries yet.")
            else -> {
                val words = query.trim()
                val shown = if (words.isEmpty()) all else all.filter { lottery ->
                    lottery.campsites.any {
                        it.name.contains(words, ignoreCase = true) ||
                            it.location.contains(words, ignoreCase = true)
                    }
                }
                if (shown.isEmpty()) {
                    CenteredMessage("No lotteries match \"$words\".")
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(shown, key = { it.id }) { lottery ->
                            LotteryListRow(lottery, onClick = { onLotteryClick(lottery) })
                            HorizontalDivider(color = CampSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun LotteryListRow(lottery: Lottery, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(LotteryRowTint)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(CampSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Category, contentDescription = null, tint = Color.Gray)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(lottery.title(), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(
                "Registration: " + LotteryDates.formatPeriod(lottery.registrationStart, lottery.registrationEnd),
                fontSize = 12.sp,
                color = Color.DarkGray
            )
            Text(lottery.statusText(), fontSize = 12.sp, color = Color.DarkGray)
        }
    }
}
