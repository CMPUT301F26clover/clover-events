package com.example.camp_serve.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.camp_serve.MainActivity
import com.example.camp_serve.model.Notification
import com.example.camp_serve.model.NotificationType

/*
 * PushNotifier.kt shows app notifications as device notifications in the phone's
 * notification shade (#101). It supports US 01.03.01, 01.03.02 and 02.10.01.
 *
 * Outstanding issues: notifications are shown while the app process is running
 * (open or in the background). Delivery when the app is fully closed needs
 * Firebase Cloud Messaging sent from a server, e.g. a Cloud Function.
 */

/** Creates the notification channel and posts device notifications. */
object PushNotifier {
    const val CHANNEL_ID = "lottery_notifications"

    /** Intent extra that tells MainActivity to open the entrant's Notifications screen. */
    const val EXTRA_OPEN_NOTIFICATIONS = "com.example.camp_serve.OPEN_NOTIFICATIONS"

    /** Creates the channel the notifications use. Safe to call more than once. */
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Lottery notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Lottery results and messages from campsite owners"
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    /** Whether Android 13+ still needs the user to allow notifications. */
    fun needsPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED

    /** The title shown for each kind of notification. */
    fun title(type: NotificationType): String = when (type) {
        NotificationType.SYSTEM_WIN -> "You won the lottery!"
        NotificationType.SYSTEM_LOSE -> "Lottery result"
        NotificationType.OWNER -> "Message from the campsite owner"
    }

    /**
     * Posts one device notification. Tapping it opens the entrant's Notifications screen.
     * Does nothing if the user has not allowed notifications for the app.
     *
     * @return true if it was posted
     */
    fun show(context: Context, notification: Notification): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (needsPermission(context) || !manager.areNotificationsEnabled()) return false

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_OPEN_NOTIFICATIONS, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notification.id.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val built = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title(notification.type))
            .setContentText(notification.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.message))
            .setSubText(notification.campsiteName)
            .setWhen(notification.sentAt)
            .setShowWhen(true)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        return try {
            manager.notify(notification.id.hashCode(), built)
            true
        } catch (_: SecurityException) {
            false // permission was revoked while the app was running
        }
    }
}

/**
 * Remembers, on this device, which notifications were already shown, so each one
 * pops up only once even after the app restarts.
 */
class PushHistory(context: Context) {
    private val prefs = context.getSharedPreferences("camp_serve_push", Context.MODE_PRIVATE)

    /** When this device started listening; older notifications are not popped up. */
    fun since(): Long {
        val saved = prefs.getLong(KEY_SINCE, -1L)
        if (saved >= 0) return saved
        val now = System.currentTimeMillis()
        prefs.edit().putLong(KEY_SINCE, now).apply()
        return now
    }

    fun shownIds(): Set<String> = prefs.getStringSet(KEY_SHOWN, emptySet()).orEmpty().toSet()

    fun markShown(ids: Collection<String>) {
        if (ids.isEmpty()) return
        prefs.edit().putStringSet(KEY_SHOWN, shownIds() + ids).apply()
    }

    private companion object {
        const val KEY_SINCE = "since"
        const val KEY_SHOWN = "shown_ids"
    }
}
