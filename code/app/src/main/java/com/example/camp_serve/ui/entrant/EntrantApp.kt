package com.example.camp_serve.ui.entrant

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.camp_serve.data.CampsiteRepository
import com.example.camp_serve.data.EntrantRepository
import com.example.camp_serve.data.LotteryRepository
import com.example.camp_serve.data.TempCampsiteRepo
import com.example.camp_serve.data.TempEntrantRepo
import com.example.camp_serve.ui.entrant.campsites.CampsiteDetailScreen
import com.example.camp_serve.ui.entrant.campsites.CampsitesScreen
import com.example.camp_serve.ui.entrant.profile.ProfileScreen
import kotlin.String

/**
 * Top-level entrant application navigation container.
 * Manages navigation between entrant screens.
 *
 * @param campsiteRepository repository for fetching campsite data
 * @param entrantRepository repository for managing entrant profile data
 * @param lotteryRepository lottery storage; null uses the app's default Firestore database
 * @param startCampsiteId id of a campsite to open right away, e.g. from a scanned QR code
 */
@Composable
fun EntrantApp(
    modifier: Modifier = Modifier,
    campsiteRepository: CampsiteRepository = remember { TempCampsiteRepo() },
    entrantRepository: EntrantRepository = remember { TempEntrantRepo() },
    lotteryRepository: LotteryRepository? = null,
    startCampsiteId: String? = null
) {
    val navController = rememberNavController()

    // open the scanned campsite once (not again after the screen rotates)
    var openedScannedCampsite by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(startCampsiteId) {
        if (startCampsiteId != null && !openedScannedCampsite) {
            openedScannedCampsite = true
            navController.navigate("campsite_detail/$startCampsiteId")
        }
    }

    NavHost(
        navController = navController,
        startDestination = "campsites",
        modifier = modifier
    ) {
        // 1. Campsites List Screen
        composable("campsites") {
            CampsitesScreen(
                navController = navController,
                repository = campsiteRepository
            )
        }

        // 2. Campsite Detail Screen
        composable("campsite_detail/{campsiteId}") { backStackEntry ->
            val campsiteId = backStackEntry.arguments?.getString("campsiteId") ?: ""
            CampsiteDetailScreen(
                campsiteId = campsiteId,
                navController = navController,
                repository = campsiteRepository,
                lotteryRepository = lotteryRepository
            )
        }

        // 3. Profile Screen
        composable("profile") {
            ProfileScreen(
                repository = entrantRepository
            )
        }
    }
}
