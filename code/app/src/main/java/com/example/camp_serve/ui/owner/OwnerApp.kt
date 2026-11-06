/*
 * OwnerApp.kt is the campsite owner's whole section: a Scaffold with the bottom bar and a
 * NavHost that connects the owner screens. Call it from MainActivity the same way as AdminApp.
 * Outstanding issues: uses the in-memory Temp repositories until the Firestore ones exist.
 */

package com.example.camp_serve.ui.owner

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.camp_serve.data.CampsiteRepository
import com.example.camp_serve.data.LotteryRepository
import com.example.camp_serve.data.MessageRepository
import com.example.camp_serve.data.ReservationRepository
import com.example.camp_serve.data.SentNotificationRepository
import com.example.camp_serve.data.TempCampsiteRepo
import com.example.camp_serve.data.TempLotteryRepo
import com.example.camp_serve.data.TempMessageRepo
import com.example.camp_serve.data.TempReservationRepo
import com.example.camp_serve.data.TempSentNotificationRepo
import com.example.camp_serve.model.Lottery
import com.example.camp_serve.ui.owner.alerts.OwnerAlertsScreen
import com.example.camp_serve.ui.owner.campsites.EditCampsiteScreen
import com.example.camp_serve.ui.owner.campsites.OwnerCampsiteDetailScreen
import com.example.camp_serve.ui.owner.campsites.OwnerCampsitesScreen
import com.example.camp_serve.ui.owner.campsites.RegisterCampsiteScreen
import com.example.camp_serve.ui.owner.lotteries.LotteryFormScreen
import com.example.camp_serve.ui.owner.lotteries.OwnerLotteriesScreen
import com.example.camp_serve.ui.owner.lotteries.OwnerLotteryDetailScreen
import com.example.camp_serve.ui.owner.ownernavbar.OwnerBottomNavBar
import com.example.camp_serve.ui.owner.reservations.ChatScreen
import com.example.camp_serve.ui.owner.reservations.OwnerReservationsScreen
import kotlinx.coroutines.launch

/**
 * The campsite owner's app.
 *
 * @param ownerId id of the owner using the app (fake until sign-in exists)
 * @param campsiteRepository campsite data
 * @param lotteryRepository lottery data
 * @param reservationRepository reservation data
 * @param messageRepository chat data
 * @param notificationRepository sent notification data
 */
@Composable
fun OwnerApp(
    ownerId: String = "fake-owner-01",
    campsiteRepository: CampsiteRepository = remember { TempCampsiteRepo() },
    lotteryRepository: LotteryRepository = remember { TempLotteryRepo() },
    reservationRepository: ReservationRepository = remember { TempReservationRepo() },
    messageRepository: MessageRepository = remember { TempMessageRepo() },
    notificationRepository: SentNotificationRepository = remember { TempSentNotificationRepo() }
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun toast(text: String) {
        scope.launch { snackbarHostState.showSnackbar(text) }
    }

    Scaffold(
        bottomBar = { OwnerBottomNavBar(navController) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = OwnerRoutes.CAMPSITES,
            modifier = Modifier.padding(padding)
        ) {
            // ---- Campsites tab ----
            composable(OwnerRoutes.CAMPSITES) {
                OwnerCampsitesScreen(
                    ownerId, campsiteRepository, lotteryRepository,
                    onAddClick = { navController.navigate(OwnerRoutes.REGISTER_CAMPSITE) },
                    onCampsiteClick = { navController.navigate(OwnerRoutes.campsite(it.id)) }
                )
            }
            composable(OwnerRoutes.REGISTER_CAMPSITE) {
                RegisterCampsiteScreen(
                    repository = campsiteRepository, ownerId = ownerId,
                    onSaved = {
                        toast("Campsite registered")
                        navController.popBackStack()
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable(OwnerRoutes.CAMPSITE) { entry ->
                val id = entry.arguments?.getString("id") ?: ""
                OwnerCampsiteDetailScreen(
                    id, ownerId, campsiteRepository, lotteryRepository, reservationRepository,
                    onCreateLotteryClick = { navController.navigate(OwnerRoutes.createLottery(it.id)) },
                    onEditClick = { navController.navigate(OwnerRoutes.editCampsite(it.id)) },
                    onQrClick = { toast("QR code download isn't available yet") }
                )
            }
            composable(OwnerRoutes.EDIT_CAMPSITE) { entry ->
                val id = entry.arguments?.getString("id") ?: ""
                EditCampsiteScreen(
                    campsiteId = id, repository = campsiteRepository,
                    onSaved = {
                        toast("Changes saved")
                        navController.popBackStack()
                    },
                    onCancel = { navController.popBackStack() }
                )
            }

            // ---- Lotteries tab ----
            composable(OwnerRoutes.LOTTERIES) {
                OwnerLotteriesScreen(
                    ownerId, campsiteRepository, lotteryRepository,
                    onCreateClick = { navController.navigate(OwnerRoutes.createLottery()) },
                    onLotteryClick = { navController.navigate(OwnerRoutes.lottery(it.id)) }
                )
            }
            composable(OwnerRoutes.CREATE_LOTTERY) { entry ->
                val campsiteId = entry.arguments?.getString("campsiteId")
                LotteryFormScreen(
                    ownerId = ownerId,
                    campsiteRepository = campsiteRepository,
                    lotteryRepository = lotteryRepository,
                    preselectedCampsiteId = if (campsiteId == OwnerRoutes.NO_CAMPSITE) null else campsiteId,
                    onSaved = { saved: Lottery ->
                        toast("Lottery created")
                        navController.navigate(OwnerRoutes.lottery(saved.id)) {
                            popUpTo(OwnerRoutes.LOTTERIES)
                        }
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable(OwnerRoutes.LOTTERY) { entry ->
                val id = entry.arguments?.getString("id") ?: ""
                OwnerLotteryDetailScreen(
                    id, ownerId, campsiteRepository, lotteryRepository, notificationRepository,
                    onEditClick = { navController.navigate(OwnerRoutes.editLottery(it.id)) },
                    onNotificationSent = { toast(it) }
                )
            }
            composable(OwnerRoutes.EDIT_LOTTERY) { entry ->
                val id = entry.arguments?.getString("id") ?: ""
                EditLotteryRoute(
                    lotteryId = id, ownerId = ownerId,
                    campsiteRepository = campsiteRepository, lotteryRepository = lotteryRepository,
                    onSaved = {
                        toast("Changes saved")
                        navController.popBackStack()
                    },
                    onCancel = { navController.popBackStack() }
                )
            }

            // ---- Reservations tab ----
            composable(OwnerRoutes.RESERVATIONS) {
                OwnerReservationsScreen(
                    ownerId, reservationRepository, notificationRepository,
                    onMessageClick = { navController.navigate(OwnerRoutes.chat(it.id)) },
                    onMessage = { toast(it) }
                )
            }
            composable(OwnerRoutes.CHAT) { entry ->
                val id = entry.arguments?.getString("id") ?: ""
                ChatScreen(id, reservationRepository, messageRepository)
            }

            // ---- Alerts tab ----
            composable(OwnerRoutes.ALERTS) {
                OwnerAlertsScreen(ownerId, notificationRepository)
            }
        }
    }
}
