package com.example.camp_serve.ui.owner

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.camp_serve.data.CampsiteRepository
import com.example.camp_serve.data.EntrantRepository
import com.example.camp_serve.data.FirestoreCampsiteRepo
import com.example.camp_serve.data.FirestoreEntrantRepo
import com.example.camp_serve.data.FirestoreLotteryRepo
import com.example.camp_serve.data.FirestoreNotificationRepo
import com.example.camp_serve.data.FirestoreReservationRecordRepo
import com.example.camp_serve.data.LotteryRepository
import com.example.camp_serve.data.NotificationRepository
import com.example.camp_serve.data.ReservationRecordRepository
import com.example.camp_serve.service.LotteryDrawService
import com.example.camp_serve.service.LotteryMonitor
import com.example.camp_serve.service.NotificationService
import com.example.camp_serve.ui.owner.campsites.EditCampsiteScreen
import com.example.camp_serve.ui.owner.campsites.MyCampsitesScreen
import com.example.camp_serve.ui.owner.campsites.OwnerCampsiteDetailScreen
import com.example.camp_serve.ui.owner.campsites.RegisterCampsiteScreen
import com.example.camp_serve.ui.owner.lotteries.CreateLotteryScreen
import com.example.camp_serve.ui.owner.lotteries.EditLotteryScreen
import com.example.camp_serve.ui.owner.lotteries.LotteriesScreen
import com.example.camp_serve.ui.owner.lotteries.LotteryDetailScreen
import com.example.camp_serve.ui.owner.notifications.SentNotificationsScreen
import com.example.camp_serve.ui.owner.reservations.OwnerReservationsScreen
import com.example.camp_serve.ui.theme.CampPurpleSecondary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/*
 * OwnerApp.kt
 * Navigation for the campsite owner side of the app.
 * Tabs: Campsites (list -> register, list -> details -> edit / create lottery),
 * Lotteries (list -> create, list -> lottery details + waitlist -> edit / notify),
 * Reservations (active / past / cancelled) and Notifications (notifications sent, US 02.10.02).
 * Campsites, lotteries, reservations and notifications are saved in Firestore.
 */

private const val CREATE_LOTTERY_ROUTE = "create_lottery?campsiteId={campsiteId}"

/** Screens that hide the bottom bar (forms), like the storyboard. */
private val formRoutes = setOf(
    "register_campsite",
    "edit_campsite/{campsiteId}",
    CREATE_LOTTERY_ROUTE,
    "edit_lottery/{lotteryId}"
)

/**
 * Top-level campsite owner navigation.
 *
 * @param ownerId unique ID of the owner (the device id)
 * @param repository where the owner's campsites are saved (Firestore by default)
 * @param lotteryRepository where lotteries and waitlists are saved (Firestore by default)
 * @param entrantRepository used to show entrant names on waitlists and check opt-outs
 * @param reservationRepository where reservations for the owner's campsites are loaded from
 * @param notificationRepository where sent notifications are saved (Firestore by default)
 * @param notificationService sends draw results and waitlist messages
 * @param monitorDraws whether this app should draw winners when registration closes
 */
@Composable
fun OwnerApp(
    ownerId: String,
    modifier: Modifier = Modifier,
    repository: CampsiteRepository = remember { FirestoreCampsiteRepo() },
    lotteryRepository: LotteryRepository = remember { FirestoreLotteryRepo() },
    // FirestoreEntrantRepo needs an id for its own profile methods; only the
    // id lookup (getEntrantsByIds) is used here, which does not depend on it.
    entrantRepository: EntrantRepository = remember(ownerId) { FirestoreEntrantRepo(ownerId) },
    reservationRepository: ReservationRecordRepository = remember { FirestoreReservationRecordRepo() },
    notificationRepository: NotificationRepository = remember { FirestoreNotificationRepo() },
    notificationService: NotificationService = remember(notificationRepository, entrantRepository) {
        NotificationService(notificationRepository, entrantRepository)
    },
    monitorDraws: Boolean = true
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route

    if (monitorDraws) {
        DrawMonitor(ownerId, lotteryRepository, notificationService)
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if ((route ?: "") !in formRoutes) OwnerBottomBar(navController, route)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "my_campsites",
            modifier = Modifier.padding(innerPadding)
        ) {
            // list of the owner's campsites (US 02.03.01)
            composable("my_campsites") {
                MyCampsitesScreen(
                    repository = repository,
                    ownerId = ownerId,
                    onRegisterClick = { navController.navigate("register_campsite") },
                    onCampsiteClick = { campsite ->
                        navController.navigate("campsite_detail/" + campsite.id)
                    }
                )
            }

            // register a new campsite (US 02.01.01)
            composable("register_campsite") {
                RegisterCampsiteScreen(
                    repository = repository,
                    ownerId = ownerId,
                    onSaved = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            }

            // details of one campsite (US 02.01.04), with Create Lottery (US 02.04.01)
            composable("campsite_detail/{campsiteId}") { entry ->
                val campsiteId = entry.arguments?.getString("campsiteId") ?: ""
                OwnerCampsiteDetailScreen(
                    repository = repository,
                    campsiteId = campsiteId,
                    onEditClick = { campsite ->
                        navController.navigate("edit_campsite/" + campsite.id)
                    },
                    onBackClick = { navController.popBackStack() },
                    lotteryRepository = lotteryRepository,
                    ownerId = ownerId,
                    onCreateLotteryClick = { campsite ->
                        navController.navigate("create_lottery?campsiteId=" + campsite.id)
                    },
                    onLotteryClick = { lottery ->
                        navController.navigate("lottery_detail/" + lottery.id)
                    }
                )
            }

            // edit one campsite (US 02.01.03)
            composable("edit_campsite/{campsiteId}") { entry ->
                val campsiteId = entry.arguments?.getString("campsiteId") ?: ""
                EditCampsiteScreen(
                    repository = repository,
                    campsiteId = campsiteId,
                    onSaved = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            }

            // all of the owner's lotteries
            composable("lotteries") {
                LotteriesScreen(
                    lotteryRepository = lotteryRepository,
                    ownerId = ownerId,
                    onLotteryClick = { lottery ->
                        navController.navigate("lottery_detail/" + lottery.id)
                    },
                    onCreateClick = { navController.navigate("create_lottery") }
                )
            }

            // create a lottery (US 02.04.01, 02.04.03, 02.04.04, 02.04.05)
            composable(
                CREATE_LOTTERY_ROUTE,
                arguments = listOf(navArgument("campsiteId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { entry ->
                CreateLotteryScreen(
                    lotteryRepository = lotteryRepository,
                    campsiteRepository = repository,
                    ownerId = ownerId,
                    preselectedCampsiteId = entry.arguments?.getString("campsiteId"),
                    // "Publish & View Lottery": replace the form with the new lottery's page
                    onCreated = { lotteryId ->
                        navController.navigate("lottery_detail/$lotteryId") {
                            popUpTo(CREATE_LOTTERY_ROUTE) { inclusive = true }
                        }
                    },
                    onCancel = { navController.popBackStack() }
                )
            }

            // one lottery with its waitlist (US 02.09.01)
            composable("lottery_detail/{lotteryId}") { entry ->
                val lotteryId = entry.arguments?.getString("lotteryId") ?: ""
                LotteryDetailScreen(
                    lotteryRepository = lotteryRepository,
                    entrantRepository = entrantRepository,
                    ownerId = ownerId,
                    lotteryId = lotteryId,
                    onEditClick = { navController.navigate("edit_lottery/$it") },
                    onBackClick = { navController.popBackStack() },
                    notificationService = notificationService
                )
            }

            // edit a lottery (US 02.04.06)
            composable("edit_lottery/{lotteryId}") { entry ->
                val lotteryId = entry.arguments?.getString("lotteryId") ?: ""
                EditLotteryScreen(
                    lotteryRepository = lotteryRepository,
                    campsiteRepository = repository,
                    ownerId = ownerId,
                    lotteryId = lotteryId,
                    onSaved = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            }

            // reservations for the owner's campsites (US 02.07.01, 02.07.02, 02.07.03)
            composable("owner_reservations") {
                OwnerReservationsScreen(
                    repository = reservationRepository,
                    ownerId = ownerId
                )
            }

            // notifications the owner has sent, including draw results (US 02.10.02)
            composable("sent_notifications") {
                SentNotificationsScreen(
                    repository = notificationRepository,
                    ownerId = ownerId
                )
            }
        }
    }
}

/** Purple bottom bar from the storyboard, with the owner tabs. */
@Composable
private fun OwnerBottomBar(navController: NavHostController, route: String?) {
    val lotteryRoutes = setOf("lotteries", "lottery_detail/{lotteryId}")
    val items = listOf(
        Triple("my_campsites", "Campsites", Icons.Filled.Home),
        Triple("lotteries", "Lotteries", Icons.AutoMirrored.Filled.List),
        Triple("owner_reservations", "Reservations", Icons.Filled.DateRange),
        Triple("sent_notifications", "Notifications", Icons.Filled.Notifications)
    )
    NavigationBar(containerColor = CampPurpleSecondary) {
        items.forEach { (target, label, icon) ->
            // work out which tab should look selected
            val current = route ?: ""
            var selected = false
            if (target == "lotteries") {
                selected = current in lotteryRoutes
            } else if (target == "owner_reservations" || target == "sent_notifications") {
                selected = current == target
            } else {
                selected = current !in lotteryRoutes &&
                    current != "owner_reservations" && current != "sent_notifications"
            }
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(target) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CampPurpleSecondary,
                    selectedTextColor = Color.White,
                    indicatorColor = Color.White,
                    unselectedIconColor = Color.White,
                    unselectedTextColor = Color.White
                )
            )
        }
    }
}

/**
 * While the owner app is open, watches the owner's lotteries and draws winners
 * automatically when each one's registration period ends (US 02.05.01), then
 * notifies winners and entrants who were not selected (US 01.03.01, 01.03.02).
 * Storage errors stop that lottery's monitor; it restarts on the next list update.
 */
@Composable
private fun DrawMonitor(
    ownerId: String,
    lotteryRepository: LotteryRepository,
    notificationService: NotificationService
) {
    val scope = remember(ownerId, lotteryRepository) {
        CoroutineScope(SupervisorJob() + Dispatchers.Main + CoroutineExceptionHandler { _, _ -> })
    }
    val monitor = remember(scope, notificationService) {
        val service = LotteryDrawService(
            scope,
            repository = lotteryRepository,
            onDrawSaved = { drawn, result -> notificationService.sendDrawResult(drawn, result) }
        )
        LotteryMonitor(service::start)
    }
    DisposableEffect(scope) {
        onDispose {
            monitor.stopAll()
            scope.cancel()
        }
    }
    LaunchedEffect(monitor) {
        try {
            lotteryRepository.observeLotteriesForOwner(ownerId).collect { monitor.sync(it) }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Offline or not permitted; lotteries will be drawn the next time this runs.
        }
    }
}