package com.example.camp_serve

import com.example.camp_serve.data.DeviceId
import org.junit.Assert
import org.junit.Test

/**
 * Intent tests for device-based identification (US 01.09.01).
 */
class DeviceIdTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun deviceId_isNotEmpty() {
        Assert.assertTrue(DeviceId(context).get().isNotEmpty())
    }

    @Test
    fun deviceId_isSameEveryTime() {
        val first = DeviceId(context).get()
        val second = DeviceId(context).get()
        Assert.assertEquals(first, second)
    }
}