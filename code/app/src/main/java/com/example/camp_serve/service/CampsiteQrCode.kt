package com.example.camp_serve.service

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/*
 * CampsiteQrCode.kt
 * Makes the QR code for a campsite (US 02.02.01).
 * The QR code holds a link like "campserve://campsite/<campsite id>". The app is set up
 * (in AndroidManifest.xml) to open these links, so scanning the code opens that
 * campsite's details screen. Campsite ids are unique, so no two campsites share a QR code.
 * The QR code is made from the id every time it is shown, so nothing extra is saved in Firestore.
 * Outstanding issues: none.
 */

/**
 * Builds and reads the links stored in campsite QR codes, and turns a link into a QR code.
 */
object CampsiteQrCode {

    /** The link scheme the app opens, set in AndroidManifest.xml. */
    const val SCHEME = "campserve"

    /** The link host for campsite links, set in AndroidManifest.xml. */
    const val HOST = "campsite"

    private const val PREFIX = "$SCHEME://$HOST/"

    /**
     * Makes the link that goes inside a campsite's QR code.
     *
     * @param campsiteId the campsite's unique id
     * @return a link like "campserve://campsite/camp-01"
     * @throws IllegalArgumentException if the id is blank or has a "/" in it
     */
    fun linkFor(campsiteId: String): String {
        require(isValidId(campsiteId)) { "Campsite id can't be blank or contain '/'" }
        return PREFIX + campsiteId
    }

    /**
     * Gets the campsite id back out of a scanned link.
     *
     * @param link the text that was scanned, or the link the app was opened with
     * @return the campsite id, or null if it isn't a campsite link
     */
    fun campsiteIdFrom(link: String?): String? {
        if (link == null) return null
        val text = link.trim()
        if (!text.startsWith(PREFIX, ignoreCase = true)) return null
        val id = text.substring(PREFIX.length).trimEnd('/')
        return if (isValidId(id)) id else null
    }

    /**
     * Turns a campsite's link into a square QR code.
     *
     * @param campsiteId the campsite's unique id
     * @param size width and height of the QR code in pixels
     * @return the QR code, where a true pixel is black
     */
    fun encode(campsiteId: String, size: Int = 512): BitMatrix {
        val hints = mapOf(
            EncodeHintType.MARGIN to 2, // white border so scanners can find the code
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
        )
        return QRCodeWriter().encode(linkFor(campsiteId), BarcodeFormat.QR_CODE, size, size, hints)
    }

    /** An id is usable in a link if it isn't blank and has no "/" in it. */
    private fun isValidId(id: String): Boolean {
        return id.isNotBlank() && !id.contains('/') && id == id.trim()
    }
}
