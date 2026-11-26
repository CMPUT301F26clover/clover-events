package com.example.camp_serve.model

/*
 * EntrantNotification.kt
 * What the entrant's Notifications screen shows. The stored record is [Notification];
 * FirestoreEntrantNotificationRepo turns those into these for the screen.
 * Outstanding issues: RESERVED and CANCELLED are not sent yet (US 01.05.01, 01.07.02).
 */

/** MESSAGE is a message the campsite owner sent to their waitlist (US 02.10.01). */
enum class EntrantNotificationType { WON, LOST, RESERVED, CANCELLED, MESSAGE }

/**
 * A notification shown to an entrant (US 01.03.01, 01.03.02, 01.05.01, 01.07.02).
 *
 * @property id unique id of the notification
 * @property type what happened: won, lost, reserved, cancelled or an owner message
 * @property campsite name of the campsite the notification is about
 * @property message text shown to the entrant
 */
data class EntrantNotification(
    val id: String = "",
    val type: EntrantNotificationType = EntrantNotificationType.LOST,
    val campsite: String = "",
    val message: String = ""
)