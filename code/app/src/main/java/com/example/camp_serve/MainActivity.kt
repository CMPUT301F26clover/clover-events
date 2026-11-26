package com.example.camp_serve

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.camp_serve.data.DeviceId
import com.example.camp_serve.data.FirestoreEntrantRepo
import com.example.camp_serve.data.FirestoreNotificationRepo
import com.example.camp_serve.data.FirestoreEntrantNotificationRepo
import com.example.camp_serve.model.UserRole
import com.example.camp_serve.ui.PromptScreen
import com.example.camp_serve.ui.admin.AdminApp
import com.example.camp_serve.ui.entrant.EntrantApp
import com.example.camp_serve.ui.entrant.notifications.PushNotificationListener
import com.example.camp_serve.ui.owner.OwnerApp
import com.example.camp_serve.ui.theme.CampserveTheme
import com.example.camp_serve.service.CampsiteQrCode
import com.example.camp_serve.service.PushNotifier

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val deviceId = DeviceId(this).get()
        // set if the app was opened by scanning a campsite QR code (US 02.02.01)
        val scannedCampsiteId = CampsiteQrCode.campsiteIdFrom(intent?.data?.toString())
        // set if the app was opened by tapping a device notification (#101)
        val openNotifications = intent?.getBooleanExtra(PushNotifier.EXTRA_OPEN_NOTIFICATIONS, false) == true

        setContent {
            CampserveTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    var userRoles by remember { mutableStateOf<Set<UserRole>?>(null) }
                    var selectedRole by remember { mutableStateOf<UserRole?>(null) }
                    // entrant data lives in Firestore, keyed by this device's id (US 01.09.01)
                    val entrantRepository = remember { FirestoreEntrantRepo(deviceId) }
                    val notificationRepository = remember { FirestoreNotificationRepo() }
                    val entrantNotificationRepository = remember {
                        FirestoreEntrantNotificationRepo(deviceId, notificationRepository)
                    }

                    // device notifications for this entrant, whichever view is open (#101)
                    PushNotificationListener(deviceId, notificationRepository, entrantRepository)

                    LaunchedEffect(deviceId) {
                        val roles = UserRole.determineUserRoles(deviceId)
                        userRoles = roles
                        // Automatically select role if user only has 1 role
                        if ((scannedCampsiteId != null || openNotifications) && UserRole.ENTRANT in roles) {
                            selectedRole = UserRole.ENTRANT
                        } else if (roles.size == 1) {
                            // Automatically select role if user only has 1 role
                            selectedRole = roles.first()
                        }
                    }

                    Box(modifier = Modifier.padding(innerPadding)) {
                        val roles = userRoles
                        when {
                            // Loading state while checking roles from Firestore
                            roles == null -> CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center)
                            )
                            // Role selected or auto-assigned
                            selectedRole != null -> when (selectedRole!!) {
                                UserRole.ADMIN -> AdminApp()
                                UserRole.CAMPSITEOWNER -> OwnerApp(ownerId = deviceId)
                                UserRole.ENTRANT -> EntrantApp(
                                    entrantRepository = entrantRepository,
                                    entrantNotificationRepository = entrantNotificationRepository,
                                    openNotifications = openNotifications
                                )
                            }
                            // User has multiple roles: prompt user to pick one
                            roles.size > 1 -> PromptScreen(
                                availableRoles = roles,
                                onRoleSelected = { chosen ->
                                    selectedRole = chosen
                                }
                            )
                            // Default fallback
                            else -> EntrantApp(
                                entrantRepository = entrantRepository,
                                entrantNotificationRepository = entrantNotificationRepository,
                                openNotifications = openNotifications
                            )
                        }
                    }
                }
            }
        }
    }
}
