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
import com.example.camp_serve.model.UserRole
import com.example.camp_serve.ui.PromptScreen
import com.example.camp_serve.ui.admin.AdminApp
import com.example.camp_serve.ui.entrant.EntrantApp
import com.example.camp_serve.ui.owner.OwnerApp
import com.example.camp_serve.ui.theme.CampserveTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val deviceId = DeviceId(this).get()

        setContent {
            CampserveTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    var userRoles by remember { mutableStateOf<Set<UserRole>?>(null) }
                    var selectedRole by remember { mutableStateOf<UserRole?>(null) }

                    LaunchedEffect(deviceId) {
                        val roles = UserRole.determineUserRoles(deviceId)
                        userRoles = roles
                        // Automatically select role if user only has 1 role
                        if (roles.size == 1) {
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
                                UserRole.ENTRANT -> EntrantApp()
                            }
                            // User has multiple roles: prompt user to pick one
                            roles.size > 1 -> PromptScreen(
                                availableRoles = roles,
                                onRoleSelected = { chosen ->
                                    selectedRole = chosen
                                }
                            )
                            // Default fallback
                            else -> EntrantApp()
                        }
                    }
                }
            }
        }
    }
}
