package com.example.camp_serve.ui.entrant

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
import com.example.camp_serve.data.CampsiteRepository
import com.example.camp_serve.data.DeviceId
import com.example.camp_serve.data.EntrantRepository
import com.example.camp_serve.data.FirestoreReservationRecordRepo
import com.example.camp_serve.data.FirestoreReviewRepo
import com.example.camp_serve.data.LotteryRepository
import com.example.camp_serve.data.ReservationRecordRepository
import com.example.camp_serve.data.ReviewRepository
import com.example.camp_serve.data.TempCampsiteRepo
import com.example.camp_serve.data.TempEntrantRepo
import com.example.camp_serve.ui.entrant.campsites.CampsiteDetailScreen
import com.example.camp_serve.ui.entrant.campsites.CampsitesScreen
import com.example.camp_serve.ui.entrant.profile.ProfileScreen
import com.example.camp_serve.ui.entrant.reservations.MyReservationsScreen
import com.example.camp_serve.ui.entrant.reservations.ReservationConfirmationScreen
import com.example.camp_serve.ui.entrant.reviews.ReviewScreen
import kotlin.String

/**
 * Top-level entrant application navigation container.
 *
 * @param campsiteRepository repository for fetching campsite data
 * @param entrantRepository repository for managing entrant profile data
 * @param lotteryRepository lottery storage; null uses the app's default Firestore database
 * @param reservationRepository where the entrant's reservations are loaded from
 * @param reviewRepository where the entrant's ratings and reviews are saved
 * @param startCampsiteId id of a campsite to open right away, e.g. from a scanned QR code
 */
@Composable
fun EntrantApp(
    modifier: Modifier = Modifier,
    campsiteRepository: CampsiteRepository = remember { TempCampsiteRepo() },
    entrantRepository: EntrantRepository = remember { TempEntrantRepo() },
    lotteryRepository: LotteryRepository? = null,
    reservationRepository: ReservationRecordRepository = remember { FirestoreReservationRecordRepo() },
    reviewRepository: ReviewRepository = remember { FirestoreReviewRepo() },
    startCampsiteId: String? = null
) {
    val navController = rememberNavController()

    // this device's id, so we know whose reservations to load
    val context = LocalContext.current
    val entrantId = remember { DeviceId(context).get() }

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
                lotteryRepository = lotteryRepository,
                reviewRepository = reviewRepository
            )
        }

        // 3. Profile Screen
        composable("profile") {
            ProfileScreen(
                repository = entrantRepository
            )
        }

        // 4. My Reservations Screen (US 01.06.01, US 01.06.02)
        // tapping a past reservation opens the rating and review screen,
        // "View confirmation" opens the confirmation screen
        composable("my_reservations") {
            MyReservationsScreen(
                repository = reservationRepository,
                entrantId = entrantId,
                onReservationClick = { reservation ->
                    navController.navigate("review/" + reservation.id)
                },
                onConfirmationClick = { reservation ->
                    navController.navigate("confirmation/" + reservation.id)
                }
            )
        }

        // 5. Rate and Review Screen (US 01.13.01, US 01.13.02)
        composable("review/{reservationId}") { backStackEntry ->
            val reservationId = backStackEntry.arguments?.getString("reservationId") ?: ""
            ReviewScreen(
                reservationId = reservationId,
                reservationRepository = reservationRepository,
                reviewRepository = reviewRepository,
                campsiteRepository = campsiteRepository,
                onDone = { navController.popBackStack() }
            )
        }

        // 6. Reservation Confirmation Screen (US 01.05.01)
        composable("confirmation/{reservationId}") { backStackEntry ->
            val reservationId = backStackEntry.arguments?.getString("reservationId") ?: ""
            ReservationConfirmationScreen(
                repository = reservationRepository,
                reservationId = reservationId,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}