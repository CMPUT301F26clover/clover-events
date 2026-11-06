package com.example.camp_serve.ui.owner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.camp_serve.data.CampsiteRepository
import com.example.camp_serve.data.FirestoreCampsiteRepo
import com.example.camp_serve.ui.owner.campsites.EditCampsiteScreen
import com.example.camp_serve.ui.owner.campsites.MyCampsitesScreen
import com.example.camp_serve.ui.owner.campsites.OwnerCampsiteDetailScreen
import com.example.camp_serve.ui.owner.campsites.RegisterCampsiteScreen

/*
 * OwnerApp.kt
 * Navigation for the campsite owner side of the app.
 * Screens: my campsites list -> register, and list -> details -> edit.
 * Campsites are saved in Firestore.
 */

/**
 * Top-level campsite owner navigation.
 *
 * @param ownerId unique ID of the owner (the device id)
 * @param repository where the owner's campsites are saved (Firestore by default)
 */
@Composable
fun OwnerApp(
    ownerId: String,
    modifier: Modifier = Modifier,
    repository: CampsiteRepository = remember { FirestoreCampsiteRepo() }
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "my_campsites",
        modifier = modifier
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

        // details of one campsite (US 02.01.04)
        composable("campsite_detail/{campsiteId}") { backStackEntry ->
            val campsiteId = backStackEntry.arguments?.getString("campsiteId") ?: ""
            OwnerCampsiteDetailScreen(
                repository = repository,
                campsiteId = campsiteId,
                onEditClick = { campsite ->
                    navController.navigate("edit_campsite/" + campsite.id)
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        // edit one campsite (US 02.01.03)
        composable("edit_campsite/{campsiteId}") { backStackEntry ->
            val campsiteId = backStackEntry.arguments?.getString("campsiteId") ?: ""
            EditCampsiteScreen(
                repository = repository,
                campsiteId = campsiteId,
                onSaved = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }
    }
}