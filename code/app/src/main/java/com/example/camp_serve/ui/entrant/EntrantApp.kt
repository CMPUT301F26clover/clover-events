package com.example.camp_serve.ui.entrant

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.camp_serve.data.CampsiteRepository
import com.example.camp_serve.data.EntrantRepository
import com.example.camp_serve.data.TempCampsiteRepo
import com.example.camp_serve.data.TempEntrantRepo
import com.example.camp_serve.ui.entrant.campsites.CampsiteDetailScreen
import com.example.camp_serve.ui.entrant.campsites.CampsitesScreen
import com.example.camp_serve.ui.entrant.profile.ProfileScreen

/**
 * Top-level entrant application navigation container.
 * Manages navigation between entrant screens.
 *
 * @param campsiteRepository repository for fetching campsite data
 * @param entrantRepository repository for managing entrant profile data
 */
@Composable
fun EntrantApp(
    modifier: Modifier = Modifier,
    campsiteRepository: CampsiteRepository = remember { TempCampsiteRepo() },
    entrantRepository: EntrantRepository = remember { TempEntrantRepo() }
) {
    val navController = rememberNavController()

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
                repository = campsiteRepository
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
