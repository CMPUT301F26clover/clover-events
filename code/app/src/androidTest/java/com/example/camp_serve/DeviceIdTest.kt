package com.example.camp_serve

import androidx.test.platform.app.InstrumentationRegistry
import com.example.camp_serve.data.DeviceId
import org.junit.Assert
import org.junit.Test

/*
 * DeviceIdTest.kt checks that the device id is made once and then reused.
 */
class DeviceIdTest {

    @Test
    fun get_returnsSameIdEveryTime() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val first = DeviceId(context).get()
        val second = DeviceId(context).get()
        Assert.assertEquals(first, second)
    }

    @Test
    fun get_isNotEmpty() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        Assert.assertTrue(DeviceId(context).get().isNotEmpty())
    }
}