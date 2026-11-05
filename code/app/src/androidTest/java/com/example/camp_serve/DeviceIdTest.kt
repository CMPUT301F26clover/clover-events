package com.example.camp_serve

import androidx.test.platform.app.InstrumentationRegistry
import com.example.camp_serve.data.DeviceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Intent tests for device-based identification (US 01.09.01).
 */
class DeviceIdTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun deviceId_isNotEmpty() {
        assertTrue(DeviceId(context).get().isNotEmpty())
    }

    @Test
    fun deviceId_isSameEveryTime() {
        val first = DeviceId(context).get()
        val second = DeviceId(context).get()
        assertEquals(first, second)
    }
}