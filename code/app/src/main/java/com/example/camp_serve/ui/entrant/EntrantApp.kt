package com.example.camp_serve.ui.entrant

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.camp_serve.data.EntrantNotificationRepository
import com.example.camp_serve.data.CampsiteRepository
import com.example.camp_serve.data.DeviceId
import com.example.camp_serve.data.EntrantRepository
import com.example.camp_serve.data.FirestoreEntrantRepo
import com.example.camp_serve.data.FirestoreReservationRecordRepo
import com.example.camp_serve.data.LotteryRepository
import com.example.camp_serve.data.FirestoreEntrantNotificationRepo
import com.example.camp_serve.data.ReservationRecordRepository
import com.example.camp_serve.data.TempCampsiteRepo
import com.example.camp_serve.ui.entrant.notifications.NotificationsRoute
import com.example.camp_serve.ui.entrant.campsites.CampsiteDetailScreen
import com.example.camp_serve.ui.entrant.campsites.CampsitesScreen
import com.example.camp_serve.ui.entrant.profile.ProfileScreen
import com.example.camp_serve.ui.entrant.reservations.MyReservationsScreen
import kotlin.String

/**
 * Top-level entrant application navigation container.
 *
 * @param campsiteRepository repository for fetching campsite data
 * @param entrantRepository repository for managing entrant profile data; null uses Firestore
 * @param lotteryRepository lottery storage; null uses the app's default Firestore database
 * @param reservationRepository where the entrant's reservations are loaded from
 * @param startCampsiteId id of a campsite to open right away, e.g. from a scanned QR code
 * @param entrantNotificationRepository where the entrant's notifications come from (US 01.03.01, 01.03.02); null uses Firestore
 * @param openNotifications open the Notifications screen right away, e.g. from a tapped device notification
 */
@Composable
fun EntrantApp(
    modifier: Modifier = Modifier,
    campsiteRepository: CampsiteRepository = remember { TempCampsiteRepo() },
    entrantRepository: EntrantRepository? = null,
    lotteryRepository: LotteryRepository? = null,
    reservationRepository: ReservationRecordRepository = remember { FirestoreReservationRecordRepo() },
    startCampsiteId: String? = null,
    entrantNotificationRepository: EntrantNotificationRepository? = null,
    openNotifications: Boolean = false
) {
    val navController = rememberNavController()

    // this device's id, so we know whose reservations to load
    val context = LocalContext.current
    val entrantId = remember { DeviceId(context).get() }

    // Entrant data and notifications come from Firestore, keyed by this device's id (US 01.09.01).
    val entrants = entrantRepository ?: remember(entrantId) { FirestoreEntrantRepo(entrantId) }
    val entrantNotifications = entrantNotificationRepository ?: remember(entrantId) { FirestoreEntrantNotificationRepo(entrantId) }

    // open the scanned campsite once (not again after the screen rotates)
    var openedScannedCampsite by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(startCampsiteId) {
        if (startCampsiteId != null && !openedScannedCampsite) {
            openedScannedCampsite = true
            navController.navigate("campsite_detail/$startCampsiteId")
        }
    }

    // open Notifications once when the app was opened from a device notification (#101)
    var openedNotifications by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(openNotifications) {
        if (openNotifications && !openedNotifications) {
            openedNotifications = true
            navController.navigate("notifications")
        }
    }

    // MainActivity's Scaffold already handles the system bars, so no extra insets here.
    Scaffold(
        modifier = modifier,
        bottomBar = { BottomNavBar(navController) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "campsites",
            modifier = Modifier.padding(innerPadding)
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
                    repository = entrants
                )
            }

            // Notifications: lottery results and owner messages (US 01.03.01, 01.03.02, 02.10.01)
            composable("notifications") {
                NotificationsRoute(
                    entrantNotificationRepository = entrantNotifications,
                    entrantRepository = entrants
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
}
