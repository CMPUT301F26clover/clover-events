package com.example.camp_serve.ui.admin

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.camp_serve.ui.admin.adminnavbar.BottomNavBar
import com.example.camp_serve.ui.admin.campsites.CampsitesScreen
import com.example.camp_serve.ui.admin.images.ImagesScreen
import com.example.camp_serve.ui.admin.logs.LogScreen
import com.example.camp_serve.ui.admin.profiles.ProfileScreen

@Composable
fun AdminApp() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavBar(navController) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "campsites",
            modifier = Modifier.padding(padding)
        ) {
            composable("campsites") { CampsitesScreen(navController) }
            composable("profiles") { ProfileScreen(navController) }
            composable("images") { ImagesScreen(navController) }
            composable("logs") { LogScreen(navController) }
        }
    }
}