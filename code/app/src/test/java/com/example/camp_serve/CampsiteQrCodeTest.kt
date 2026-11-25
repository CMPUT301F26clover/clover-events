package com.example.camp_serve

import com.example.camp_serve.service.CampsiteQrCode
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

/*
 * CampsiteQrCodeTest.kt
 * Unit tests for campsite QR codes (US 02.02.01).
 */
class CampsiteQrCodeTest {

    /** Reads a QR code back the same way a scanner would. */
    private fun scan(matrix: BitMatrix): String {
        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                pixels[y * width + x] = if (matrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            }
        }
        val image = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(width, height, pixels)))
        return QRCodeReader().decode(image, mapOf(DecodeHintType.PURE_BARCODE to true)).text
    }

    @Test
    fun linkHasCampsiteId() {
        assertEquals("campserve://campsite/camp-01", CampsiteQrCode.linkFor("camp-01"))
    }

    @Test
    fun idCanBeReadBackFromLink() {
        val link = CampsiteQrCode.linkFor("aB3xYz9Q")
        assertEquals("aB3xYz9Q", CampsiteQrCode.campsiteIdFrom(link))
    }

    @Test
    fun linkWithSpacesOrEndSlashStillWorks() {
        assertEquals("camp-01", CampsiteQrCode.campsiteIdFrom("  campserve://campsite/camp-01/ "))
    }

    @Test
    fun otherLinksAreNotCampsites() {
        assertNull(CampsiteQrCode.campsiteIdFrom(null))
        assertNull(CampsiteQrCode.campsiteIdFrom(""))
        assertNull(CampsiteQrCode.campsiteIdFrom("https://example.com/camp-01"))
        assertNull(CampsiteQrCode.campsiteIdFrom("campserve://lottery/abc"))
        assertNull(CampsiteQrCode.campsiteIdFrom("campserve://campsite/"))
        assertNull(CampsiteQrCode.campsiteIdFrom("campserve://campsite/a/b"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankIdIsRejected() {
        CampsiteQrCode.linkFor("  ")
    }

    @Test
    fun scanningQrCodeGivesCampsiteLink() {
        val matrix = CampsiteQrCode.encode("camp-01")
        assertEquals("campserve://campsite/camp-01", scan(matrix))
    }

    @Test
    fun qrCodeIsSquareAndRequestedSize() {
        val matrix = CampsiteQrCode.encode("camp-01", size = 300)
        assertEquals(300, matrix.width)
        assertEquals(300, matrix.height)
    }

    @Test
    fun sameCampsiteAlwaysGetsSameQrCode() {
        assertEquals(CampsiteQrCode.encode("camp-01"), CampsiteQrCode.encode("camp-01"))
    }

    @Test
    fun differentCampsitesGetDifferentQrCodes() {
        val first = CampsiteQrCode.encode("camp-01")
        val second = CampsiteQrCode.encode("camp-02")
        assertNotEquals(first, second)
        assertNotEquals(scan(first), scan(second))
    }
}
