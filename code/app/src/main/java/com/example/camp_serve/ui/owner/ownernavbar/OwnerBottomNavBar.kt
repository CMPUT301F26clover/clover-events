/*
 * OwnerBottomNavBar.kt is the owner's bottom bar with four tabs: Campsites, Lotteries,
 * Reservations and Alerts. Same pattern as the admin BottomNavBar.
 * Outstanding issues: none.
 */

package com.example.camp_serve.ui.owner.ownernavbar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.camp_serve.ui.owner.OwnerRoutes

private data class OwnerTab(val route: String, val label: String, val icon: ImageVector)

/**
 * Bottom bar for the owner section.
 *
 * @param navController controller used to switch tabs
 */
@Composable
fun OwnerBottomNavBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = OwnerRoutes.tabFor(backStackEntry?.destination?.route)

    val tabs = listOf(
        OwnerTab(OwnerRoutes.CAMPSITES, "Campsites", Icons.Filled.Home),
        OwnerTab(OwnerRoutes.LOTTERIES, "Lotteries", Icons.Filled.Star),
        OwnerTab(OwnerRoutes.RESERVATIONS, "Reservations", Icons.Filled.DateRange),
        OwnerTab(OwnerRoutes.ALERTS, "Alerts", Icons.Filled.Notifications)
    )

    NavigationBar {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = currentTab == tab.route,
                onClick = {
                    navController.navigate(tab.route) {
                        launchSingleTop = true
                        popUpTo(OwnerRoutes.CAMPSITES) { inclusive = false }
                    }
                },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) }
            )
        }
    }
}
