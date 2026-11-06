package com.example.camp_serve.ui.owner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.camp_serve.data.CampsiteRepository
import com.example.camp_serve.data.TempCampsiteRepo
import com.example.camp_serve.ui.owner.campsites.MyCampsitesScreen
import com.example.camp_serve.ui.owner.campsites.RegisterCampsiteScreen

/**
 * Top-level campsite owner application navigation container.
 *
 * @param ownerId unique ID of the owner
 * @param repository data source for campsite registration and listing
 */
@Composable
fun OwnerApp(
    ownerId: String,
    modifier: Modifier = Modifier,
    repository: CampsiteRepository = remember { TempCampsiteRepo() }
) {
    val navController = rememberNavController()

    // TODO: Replace with actual navigation logic later (someone who is responsible for campsite owner)
    NavHost(
        navController = navController,
        startDestination = "my_campsites",
        modifier = modifier
    ) {
        composable("my_campsites") {
            MyCampsitesScreen(
                repository = repository,
                ownerId = ownerId,
                onRegisterClick = { navController.navigate("register_campsite") }
            )
        }
        composable("register_campsite") {
            RegisterCampsiteScreen(
                repository = repository,
                ownerId = ownerId,
                onSaved = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }
    }
}
