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

    // the app's saved settings, where we keep the id
    private val prefs = context.getSharedPreferences("camp_serve_prefs", Context.MODE_PRIVATE)

    /**
     * Gets the id for this device. The first time, a new id is made and
     * saved, so the same id comes back every time the app is opened.
     *
     * @return this device's unique id
     */
    fun get(): String {
        // look for an id we saved before
        val savedId = prefs.getString("device_id", null)
        if (savedId != null) {
            return savedId
        }

        // first time opening the app, so make a new random id and save it
        val newId = UUID.randomUUID().toString()
        prefs.edit().putString("device_id", newId).apply()
        return newId
    }
}