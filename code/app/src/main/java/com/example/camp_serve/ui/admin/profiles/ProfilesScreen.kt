package com.example.camp_serve.ui.admin.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

private enum class AccountType { Entrant, Owner }

private data class Profile (
    val name: String,
    val email: String,
    val status: AccountType,
    val violation: Boolean
)

private val SampleProfile =
    listOf(
        Profile("Jace", "jace@gmail.com", AccountType.Entrant, false),
        Profile("Sam", "sam@gmail.com",AccountType.Owner, false),
        Profile("Alex", "alex@gmail.com",AccountType.Owner, true),
        Profile("George", "george@gmail.com",AccountType.Entrant, true),
        Profile("John", "john@gmail.com", AccountType.Entrant, false)
    )

@Composable
fun ProfileScreen(navController: NavController) {
    val profiles = remember { mutableStateListOf(*SampleProfile.toTypedArray()) }
    var selectedProfile by remember { mutableStateOf(AccountType.Entrant) }
    val shownProfile = profiles.filter { it.status == selectedProfile }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Profiles",
            fontSize = 30.sp,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp),
            color = Color(0xFF715CAA)
        )

        Row(modifier = Modifier.padding(16.dp)) {
            FilterChip( // From https://developer.android.com/develop/ui/compose/components/chip
                onClick = { selectedProfile = AccountType.Entrant },
                label = { Text("Entrants") },
                selected = selectedProfile == AccountType.Entrant
            )

            Spacer(modifier = Modifier.padding(8.dp))

            FilterChip(
                onClick = { selectedProfile = AccountType.Owner },
                label = { Text("Owners") },
                selected = selectedProfile == AccountType.Owner
            )
        }
        LazyColumn {
            items(shownProfile) { profile ->
                ProfileRow(
                    profile = profile,
                    onRemove = { profiles.remove(profile) }
                    )
            }
        }
    }
}

@Composable
private fun ProfileRow(profile: Profile, onRemove : () -> Unit) {
    var removeAlert by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFEF7FF))
            .border(1.dp, Color(0xFF000000))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(profile.name, fontWeight = FontWeight.Bold)
            Text(profile.email, fontSize = 12.sp)
            if (profile.violation) {
                Text(
                    text = "Policy violation reported",
                    fontSize = 12.sp,
                    color = Color.Red
                )
            }
        }
        Button(onClick = {removeAlert = true}) {
            Text("Remove")
        }

        if (removeAlert) {
            AlertDialog(
                onDismissRequest = { removeAlert = false },
                title = { Text("Remove this Profile?") },
                text = { Text("This removes the users profile. This can't be undone") },
                confirmButton = {
                    TextButton(onClick = {
                        removeAlert = false
                        onRemove()
                    }) {
                        Text("Remove")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        removeAlert = false
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfilesScreenPreview() {
    ProfileScreen(navController = rememberNavController())
}