package com.example.camp_serve.ui.owner.campsites

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.camp_serve.model.Campsite
import com.example.camp_serve.service.CampsiteQrCode
import com.example.camp_serve.ui.owner.lotteries.PillButton
import com.example.camp_serve.ui.theme.CampPurple
import com.google.zxing.common.BitMatrix
import java.io.File

/*
 * DownloadQrCodeButton.kt
 * The "Download QR Code" button on the owner's campsite details screen (US 02.02.01).
 * Tapping it shows the campsite's QR code with buttons to save it as a picture or share it.
 * Outstanding issues: none.
 */

/**
 * "Download QR Code" button. Opens a window with the campsite's QR code and
 * Save / Share buttons.
 *
 * @param campsite the campsite to show the QR code for
 */
@Composable
fun DownloadQrCodeButton(campsite: Campsite, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var showQrCode by remember { mutableStateOf(false) }
    // only remake the picture if the campsite changes
    val bitmap = remember(campsite.id) { qrBitmap(CampsiteQrCode.encode(campsite.id)) }

    // asks the owner where to save the picture, then writes it there
    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        if (uri != null) {
            val saved = saveQrCode(context, bitmap, uri)
            val message = if (saved) "QR code saved" else "Couldn't save the QR code"
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            if (saved) showQrCode = false
        }
    }

    PillButton("Download QR Code", onClick = { showQrCode = true }, modifier = modifier)

    if (showQrCode) {
        AlertDialog(
            onDismissRequest = { showQrCode = false },
            title = { Text(campsite.name + " QR Code", color = CampPurple) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "QR code for " + campsite.name,
                        modifier = Modifier
                            .size(220.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Entrants can scan this code to open this campsite in the app.")
                }
            },
            confirmButton = {
                TextButton(onClick = { saveLauncher.launch(qrFileName(campsite)) }) {
                    Text("Save", color = CampPurple)
                }
                TextButton(onClick = { shareQrCode(context, bitmap, campsite) }) {
                    Text("Share", color = CampPurple)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQrCode = false }) {
                    Text("Close", color = CampPurple)
                }
            }
        )
    }
}

/**
 * Turns a QR code into a black and white picture.
 *
 * @param matrix the QR code
 * @return the picture of the QR code
 */
fun qrBitmap(matrix: BitMatrix): Bitmap {
    val width = matrix.width
    val height = matrix.height
    val pixels = IntArray(width * height)
    for (y in 0 until height) {
        for (x in 0 until width) {
            pixels[y * width + x] = if (matrix.get(x, y)) BLACK else WHITE
        }
    }
    return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
}

private const val BLACK = 0xFF000000.toInt()
private const val WHITE = 0xFFFFFFFF.toInt()

/**
 * Makes a file name like "maligne-ridge-qr.png" for the saved picture.
 *
 * @param campsite the campsite the QR code is for
 * @return the file name
 */
fun qrFileName(campsite: Campsite): String {
    val name = campsite.name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
    return (if (name == "") campsite.id else name) + "-qr.png"
}

/**
 * Writes the QR code picture to the place the owner picked.
 *
 * @return true if it was saved
 */
private fun saveQrCode(context: Context, bitmap: Bitmap, uri: Uri): Boolean {
    return try {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        } ?: false
    } catch (_: Exception) {
        false
    }
}

/**
 * Opens the share menu (messages, email, Drive, ...) with the QR code picture.
 * The picture is put in the app's cache folder and shared through the FileProvider
 * set up in AndroidManifest.xml.
 */
private fun shareQrCode(context: Context, bitmap: Bitmap, campsite: Campsite) {
    try {
        val folder = File(context.cacheDir, "qr_codes")
        folder.mkdirs()
        val file = File(folder, qrFileName(campsite))
        file.outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }

        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Scan this QR code in camp-serve to see " + campsite.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share QR code"))
    } catch (_: Exception) {
        Toast.makeText(context, "Couldn't share the QR code", Toast.LENGTH_SHORT).show()
    }
}
