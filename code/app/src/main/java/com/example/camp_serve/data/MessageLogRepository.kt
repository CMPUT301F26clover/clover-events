package com.example.camp_serve.data

import com.example.camp_serve.model.MessageLog

/**
 * Where message logs come from and get saved to. Screens depend on this interface only.
 */
interface MessageLogRepository {

    /**
     * Gets every message log (used by admin).
     *
     * @return all logs, most recent first
     */
    suspend fun getAllMessageLogs(): List<MessageLog>

    /**
     * Saves a log entry. Call this whenever an owner sends a message to an entrant.
     * If the id is empty, a new id is made.
     *
     * @param log the log entry to save
     */
    suspend fun addMessageLog(log: MessageLog)
}
