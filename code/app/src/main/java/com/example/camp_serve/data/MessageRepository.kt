/*
 * MessageRepository.kt lists how the owner reads and sends chat messages about a reservation
 * and holds a temporary in-memory version.
 * TODO: REPLACE TempMessageRepo WITH FIRESTORE VERSION LATER
 * Outstanding issues: the entrant side cannot reply yet.
 */

package com.example.camp_serve.data

import com.example.camp_serve.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.Date

/**
 * Where chat messages come from and get saved to.
 */
interface MessageRepository {

    /**
     * Live list of messages for one reservation, oldest first.
     *
     * @param reservationId reservation the chat is about
     */
    fun observeMessages(reservationId: String): Flow<List<ChatMessage>>

    /**
     * Sends a message.
     *
     * @param reservationId reservation the chat is about
     * @param text the message text
     * @param fromOwner true if the owner is sending
     */
    suspend fun sendMessage(reservationId: String, text: String, fromOwner: Boolean = true)
}

/**
 * Stand-in for Firestore that keeps messages in memory.
 */
class TempMessageRepo : MessageRepository {
    private val messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage("m1", "res-01", false, "Hi! Is early check-in possible?", Date(System.currentTimeMillis() - 3600_000L)),
            ChatMessage("m2", "res-01", true, "Yes, from 1 pm.", Date(System.currentTimeMillis() - 1800_000L))
        )
    )

    override fun observeMessages(reservationId: String): Flow<List<ChatMessage>> =
        messages.map { list -> list.filter { it.reservationId == reservationId }.sortedBy { it.sentAt } }

    override suspend fun sendMessage(reservationId: String, text: String, fromOwner: Boolean) {
        if (text.isBlank()) return
        val id = "m" + (messages.value.size + 1)
        messages.value = messages.value + ChatMessage(id, reservationId, fromOwner, text.trim(), Date())
    }
}
