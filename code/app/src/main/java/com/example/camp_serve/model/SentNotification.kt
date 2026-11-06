/*
 * SentNotification.kt
 *
 * Role: Model class for a notification an owner sent to entrants, shown on the owner's
 * Alerts tab (US 02.07.01, 02.07.02, 02.10.02).
 * Outstanding issues: none.
 */

package com.example.camp_serve.model

import java.util.Date

/**
 * A notification the owner sent.
 *
 * @property id unique id
 * @property ownerId owner who sent it
 * @property lotteryId lottery it was about, empty if not about a lottery
 * @property campsiteName campsite name shown under the message
 * @property message text that entrants received
 * @property recipientCount how many entrants it was sent to
 * @property sentAt when it was sent
 */
data class SentNotification(
    val id: String = "",
    val ownerId: String = "",
    val lotteryId: String = "",
    val campsiteName: String = "",
    val message: String = "",
    val recipientCount: Int = 0,
    val sentAt: Date = Date()
)
