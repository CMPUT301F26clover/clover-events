package com.example.camp_serve.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camp_serve.model.UserRole

/**
 * Screen presented to users who possess multiple roles, allowing them to choose
 * which role screen to launch.
 *
 * @param availableRoles set of roles available for this device/user
 * @param onRoleSelected callback when a user taps a role button
 */
@Composable
fun PromptScreen(
    availableRoles: Set<UserRole>,
    onRoleSelected: (UserRole) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF7FF)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Choose Your Role",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF715CAA),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Multiple roles are associated with your account. Please select how you would like to proceed:",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (availableRoles.contains(UserRole.ENTRANT)) {
                    Button(
                        onClick = { onRoleSelected(UserRole.ENTRANT) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Continue as Entrant")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (availableRoles.contains(UserRole.CAMPSITEOWNER)) {
                    Button(
                        onClick = { onRoleSelected(UserRole.CAMPSITEOWNER) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Continue as Campsite Owner")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (availableRoles.contains(UserRole.ADMIN)) {
                    Button(
                        onClick = { onRoleSelected(UserRole.ADMIN) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Continue as Admin")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PromptScreenPreview() {
    PromptScreen(
        availableRoles = setOf(UserRole.ENTRANT, UserRole.CAMPSITEOWNER, UserRole.ADMIN),
        onRoleSelected = {}
    )
}
