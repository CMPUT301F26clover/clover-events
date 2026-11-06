package com.example.camp_serve.ui.entrant.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.camp_serve.R
import com.example.camp_serve.data.EntrantRepository
import com.example.camp_serve.model.Entrant
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction

/**
 * Entrant profile screen. Shows the profile first (name, email, phone).
 * "Edit" switches to a form for changing name, email and phone (US 01.07.03).
 * "Delete Profile" asks for confirmation, then deletes the profile (US 01.08.01).
 * Outstanding issues: no navigation yet and the app policy link
 *
 * @param repository where the profile is loaded from and saved to
 * @param onProfileDeleted called after the profile has been deleted
 */
@Composable
fun ProfileScreen (
    repository: EntrantRepository,
    modifier: Modifier = Modifier,
    onProfileDeleted : () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var editing by remember { mutableStateOf(false) }
    var deleted by remember { mutableStateOf(false) }
    var current by remember { mutableStateOf(Entrant()) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            current = repository.getEntrant()
        } catch (e: Exception) {
            status = "Could not load your profile. Check your connection."
        }
        loading = false
    }

    if (loading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (deleted) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Your profile has been deleted.\n You have been signed out.")
        }
    }  else if (editing) {
        // if in editing mode

        val nameOk = name.isNotBlank()
        val emailOk = isValidEmail(email)
        val phoneOk = isValidPhone(phone)

        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Edit Profile",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; status = null; saveError = null },
                label = { Text("Name") },
                isError = !nameOk,
                supportingText = { if (!nameOk) Text("Name can't be empty") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; status = null; saveError = null },
                label = { Text("Email") },
                isError = !emailOk,
                supportingText = { if (!emailOk) Text("Enter a valid email, like name@example.com") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it; status = null; saveError = null },
                label = { Text("Phone (optional)") },
                isError = !phoneOk,
                supportingText = { if (!phoneOk) Text("Use 10 to 15 digits; only + - ( ) and spaces allowed") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (nameOk && emailOk && phoneOk) showSaveDialog = true
                    }
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = { showSaveDialog = true },
                enabled = nameOk && emailOk && phoneOk,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save changes")
            }

            saveError?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            TextButton(
                onClick = { editing = false },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Cancel")
            }

            if (showSaveDialog) {
                AlertDialog(
                    onDismissRequest = { showSaveDialog = false },
                    title = { Text("Save changes?") },
                    text = { Text("Your profile will be updated with the new information.") },
                    confirmButton = {
                        TextButton(onClick = {
                            showSaveDialog = false
                            scope.launch {
                                try {
                                    val updated = current.copy(
                                        name = name.trim(),
                                        email = email.trim(),
                                        phone = phone.trim()
                                    )
                                    repository.updateEntrant(updated)
                                    current = updated
                                    status = "Profile saved successfully"
                                    editing = false
                                } catch (e: Exception) {
                                    saveError = "Could not save. Check your connection and try again."
                                }
                            }
                        }) {
                            Text("Confirm")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            // discard the edits and keep the original information
                            showSaveDialog = false
                            editing = false
                            status = null
                        }) {
                            Text("Discard changes")
                        }
                    }
                )
            }
        }
    } else {
        // if viewing

        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.blank_profile_picture),
                contentDescription = "Profile picture",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
            )

            Text(
                text = current.name,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Text(
                text = "Signed in with this device",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetailRow("Name", current.name)
                DetailRow("Email", current.email)
                DetailRow("Phone number", current.phone.ifBlank { "Not provided" })
                HorizontalDivider()

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = {
                            name = current.name
                            email = current.email
                            phone = current.phone
                            status = null
                            saveError= null
                            editing = true
                        }
                    ) {
                        Text("Edit")
                    }
                }
                status?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

                HorizontalDivider()

                TextButton(onClick = { showDeleteDialog = true }) {
                    Text("Delete Profile", color = MaterialTheme.colorScheme.error)
                }
            }

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Delete profile?") },
                    text = { Text("This permanently deletes your profile. It cannot be undone.") },
                    confirmButton = {
                        TextButton(onClick = {
                            scope.launch {
                                repository.deleteEntrant(current.id)
                                showDeleteDialog = false
                                deleted = true
                                onProfileDeleted()
                            }
                        }) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row {
        Text(
            text = "$label: ",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(text = value)
    }
}