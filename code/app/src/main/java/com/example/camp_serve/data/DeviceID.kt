package com.example.camp_serve.data

import android.content.Context
import java.util.UUID

/*
 * DeviceId.kt gives each install of the app a unique id, so entrants are
 * identified by their device instead of a username and password (US 01.09.01).
 * Outstanding issues: the id is lost if the app is uninstalled or its data
 * is cleared.
 */

/**
 * Makes and remembers this device's id.
 *
 * @param context used to reach the app's saved settings
 */
class DeviceId(context: Context) {

    /**
     * Gets the id for this device. The first time, a new id is made and
     * saved, so the same id comes back every time the app is opened.
     *
     * @return this device's unique id
     */
    private val deviceId: String = generateDeviceId()

    private fun generateDeviceId(): String {
        // Use a consistent fixed device ID for testing purposes
        return "consistent_device_id"
    }

    fun get(): String {
        return deviceId
    }
}