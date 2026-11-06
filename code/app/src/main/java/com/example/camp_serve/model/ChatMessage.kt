/*
 * ChatMessage.kt
 *
 * Role: Model class for one message between an owner and an entrant about a reservation
 * (US 02.12.01).
 * Outstanding issues: none.
 */

package com.example.camp_serve.model

import java.util.Date

/**
 * One chat message.
 *
 * @property id unique id
 * @property reservationId reservation the conversation is about
 * @property fromOwner true if the owner wrote it, false if the entrant did
 * @property text message text
 * @property sentAt when it was sent
 */
data class ChatMessage(
    val id: String = "",
    val reservationId: String = "",
    val fromOwner: Boolean = true,
    val text: String = "",
    val sentAt: Date = Date()
)
