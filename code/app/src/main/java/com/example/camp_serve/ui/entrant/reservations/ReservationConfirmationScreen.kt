package com.example.camp_serve.ui.entrant.reservations

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.camp_serve.data.ReservationRecordRepository
import com.example.camp_serve.data.TempReservationRecordRepo
import com.example.camp_serve.model.Reservation
import com.example.camp_serve.model.csvFileName
import com.example.camp_serve.model.reservationToCsv
import java.io.File

/*
 * ReservationConfirmationScreen.kt
 * Shows the entrant a confirmation of one reservation (US 01.05.01):
 * a confirmation number, the campsite, dates and status. Cancelled or
 * rejected reservations show a cancellation instead. The entrant can
 * download the confirmation as a CSV file or share it (US 01.05.02, US 01.07.02).
 * Outstanding issues: none.
 */

/**
 * Confirmation details for one reservation.
 *
 * @param repository where the reservation is loaded from
 * @param reservationId id of the reservation to show
 * @param onBackClick called when the entrant taps Back
 */
@Composable
fun ReservationConfirmationScreen(
    repository: ReservationRecordRepository,
    reservationId: String,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var loading by remember { mutableStateOf(true) }
    var reservation by remember { mutableStateOf<Reservation?>(null) }

    // opens Android's "Save as" screen; when the entrant picks a spot, we write the CSV there
    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val r = reservation
        if (uri != null && r != null) {
            val stream = context.contentResolver.openOutputStream(uri)
            if (stream != null) {
                stream.write(reservationToCsv(r).toByteArray())
                stream.close()
                Toast.makeText(context, "Confirmation saved", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // load the reservation when the screen opens
    LaunchedEffect(reservationId) {
        reservation = repository.getReservation(reservationId)
        loading = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        OutlinedButton(onClick = onBackClick) {
            Text("Back")
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (loading) {
            CircularProgressIndicator()
        } else if (reservation == null) {
            Text("Reservation not found.")
        } else {
            val r = reservation!!

            Text(
                text = confirmationTitle(r.status),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            ConfirmationRow("Confirmation number", r.id)
            ConfirmationRow("Campsite", r.campsiteName)
            ConfirmationRow("Check-in", r.startDate)
            ConfirmationRow("Check-out", r.endDate)
            ConfirmationRow("Booked by", r.entrantName)
            ConfirmationRow("Status", r.status)

            Spacer(modifier = Modifier.height(8.dp))

            // download the confirmation as a CSV file (US 01.05.02, US 01.07.02)
            Button(onClick = { saveLauncher.launch(csvFileName(r)) }) {
                Text("Download CSV")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // or send it to another app (Drive, Gmail, ...)
            OutlinedButton(onClick = { shareCsv(context, r) }) {
                Text("Share CSV")
            }
        }
    }
}

/**
 * The big heading at the top, based on the reservation's status.
 *
 * @param status "active", "cancelled" or "rejected"
 * @return the heading to show
 */
fun confirmationTitle(status: String): String {
    if (status == "cancelled") {
        return "Reservation cancelled"
    } else if (status == "rejected") {
        return "Reservation rejected by owner"
    }
    return "Reservation confirmed"
}

/**
 * Writes the reservation's CSV to a file and opens the share sheet,
 * so the entrant can send it to another app (e.g. Drive or Gmail).
 *
 * @param context the app context, used to reach the cache folder and start the share sheet
 * @param reservation the reservation to put in the CSV
 */
fun shareCsv(context: Context, reservation: Reservation) {
    // write the CSV into the app's cache, in the folder file_paths.xml allows
    val folder = File(context.cacheDir, "confirmations")
    folder.mkdirs()
    val file = File(folder, csvFileName(reservation))
    file.writeText(reservationToCsv(reservation))

    // turn the file into a link other apps are allowed to read
    val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)

    // open the share sheet with the CSV attached
    val intent = Intent(Intent.ACTION_SEND)
    intent.type = "text/csv"
    intent.putExtra(Intent.EXTRA_STREAM, uri)
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(intent, "Share confirmation"))
}

/**
 * One label + value pair on the confirmation, like "Campsite" and "Banff Ridge".
 */
@Composable
private fun ConfirmationRow(label: String, value: String) {
    Text(text = label, fontWeight = FontWeight.Bold)
    if (value == "") {
        Text("Not provided")
    } else {
        Text(value)
    }
    Spacer(modifier = Modifier.height(12.dp))
}

@Preview(showBackground = true)
@Composable
fun ReservationConfirmationScreenPreview() {
    ReservationConfirmationScreen(
        repository = TempReservationRecordRepo(),
        reservationId = "res-01"
    )
}