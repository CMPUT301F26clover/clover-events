package com.example.camp_serve.ui.entrant

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.camp_serve.ui.admin.adminnavbar.NavigationItem

/**
 * The entrant's bottom navigation bar: Campsites, Notifications and Profile.
 * Notifications shows lottery results and owner messages.
 */
@Composable
fun BottomNavBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val items = listOf(
        NavigationItem("campsites", "Campsites", Icons.Filled.Home),
        NavigationItem("notifications", "Notifications", Icons.Filled.Notifications),
        NavigationItem("profile", "Profile", Icons.Filled.Person)
    )

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route ||
                    (item.route == "campsites" && currentRoute == "campsite_detail/{campsiteId}"),
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo("campsites") { saveState = true }
                        }
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}
