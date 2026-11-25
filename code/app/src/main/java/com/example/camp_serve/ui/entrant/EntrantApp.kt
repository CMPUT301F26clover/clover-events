package com.example.camp_serve.ui.entrant

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.camp_serve.data.CampsiteRepository
import com.example.camp_serve.data.DeviceId
import com.example.camp_serve.data.EntrantRepository
import com.example.camp_serve.data.FirestoreReservationRecordRepo
import com.example.camp_serve.data.LotteryRepository
import com.example.camp_serve.data.ReservationRecordRepository
import com.example.camp_serve.data.TempCampsiteRepo
import com.example.camp_serve.data.TempEntrantRepo
import com.example.camp_serve.ui.entrant.campsites.CampsiteDetailScreen
import com.example.camp_serve.ui.entrant.campsites.CampsitesScreen
import com.example.camp_serve.ui.entrant.profile.ProfileScreen
import com.example.camp_serve.ui.entrant.reservations.MyReservationsScreen

/**
 * Top-level entrant application navigation container.
 *
 * @param campsiteRepository repository for fetching campsite data
 * @param entrantRepository repository for managing entrant profile data
 * @param lotteryRepository lottery storage; null uses the app's default Firestore database
 * @param reservationRepository where the entrant's reservations are loaded from
 */
@Composable
fun EntrantApp(
    modifier: Modifier = Modifier,
    campsiteRepository: CampsiteRepository = remember { TempCampsiteRepo() },
    entrantRepository: EntrantRepository = remember { TempEntrantRepo() },
    lotteryRepository: LotteryRepository? = null,
    reservationRepository: ReservationRecordRepository = remember { FirestoreReservationRecordRepo() }
) {
    val navController = rememberNavController()

    // this device's id, so we know whose reservations to load
    val context = LocalContext.current
    val entrantId = remember { DeviceId(context).get() }

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

        // 4. My Reservations Screen (US 01.06.01, US 01.06.02)
        composable("my_reservations") {
            MyReservationsScreen(
                repository = reservationRepository,
                entrantId = entrantId
            )
        }
    }
}