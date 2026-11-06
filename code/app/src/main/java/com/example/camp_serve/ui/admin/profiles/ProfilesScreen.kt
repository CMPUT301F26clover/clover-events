package com.example.camp_serve.ui.admin.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController


@Composable
fun ProfileScreen(navController: NavController, vm: AdminProfilesViewModel = viewModel()) {
    var showOwners by remember { mutableStateOf(false) }
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
            FilterChip(
                onClick = { showOwners = false },
                label = { Text("Entrants") },
                selected = !showOwners
            )

            Spacer(modifier = Modifier.width(8.dp))

            FilterChip(
                onClick = { showOwners = true },
                label = { Text("Owners") },
                selected = showOwners
            )
        }
        if (!showOwners) {
            LazyColumn {
                items(vm.entrants) { entrant ->
                    ProfileRow(
                        name = entrant.name,
                        email = entrant.email,
                        onRemove = { vm.removeEntrant(entrant) }
                    )
                }
            }
        } else {
            LazyColumn {
                items(vm.owners) { owner ->
                    ProfileRow(
                        name = owner.name,
                        email = owner.email,
                        onRemove = { vm.removeOwner(owner) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileRow(name: String, email: String, onRemove: () -> Unit) {
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
            Text(name, fontWeight = FontWeight.Bold)
            Text(email, fontSize = 12.sp)
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
