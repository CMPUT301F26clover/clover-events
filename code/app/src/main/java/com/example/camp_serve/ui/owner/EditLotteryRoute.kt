/*
 * EditLotteryRoute.kt loads a lottery by id, then shows the lottery form filled in with it
 * (US 02.04.06). Needed because the form wants the Lottery itself, not just an id.
 * Outstanding issues: none.
 */

package com.example.camp_serve.ui.owner

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import com.example.camp_serve.data.CampsiteRepository
import com.example.camp_serve.data.LotteryRepository
import com.example.camp_serve.model.Lottery
import com.example.camp_serve.ui.owner.lotteries.LotteryFormScreen

/**
 * Edit screen for one lottery.
 *
 * @param lotteryId lottery to edit
 * @param ownerId owner using the app
 * @param onSaved called after saving
 * @param onCancel called when the owner leaves without saving
 */
@Composable
fun EditLotteryRoute(
    lotteryId: String,
    ownerId: String,
    campsiteRepository: CampsiteRepository,
    lotteryRepository: LotteryRepository,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    var lottery by remember { mutableStateOf<Lottery?>(null) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(lotteryId) {
        lottery = lotteryRepository.getLottery(lotteryId)
        loaded = true
    }

    val current = lottery
    if (current == null) {
        Text(if (loaded) "Lottery not found." else "Loading...", modifier = Modifier.padding(16.dp))
    } else {
        LotteryFormScreen(
            ownerId = ownerId,
            campsiteRepository = campsiteRepository,
            lotteryRepository = lotteryRepository,
            existing = current,
            onSaved = { onSaved() },
            onCancel = onCancel
        )
    }
}
