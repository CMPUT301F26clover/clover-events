package com.example.camp_serve.data

import com.example.camp_serve.model.NotificationLog

/**
 * Where notification logs come from and get saved to. Screens depend on this interface only.
 */
interface NotificationLogRepository {

    /**
     * Gets every notification log (used by admin).
     *
     * @return all logs, most recent first
     */
    suspend fun getAllNotificationLogs(): List<NotificationLog>

    /**
     * Saves a log entry. Call this whenever a notification is sent or fails to send.
     * If the id is empty, a new id is made.
     *
     * @param log the log entry to save
     */
    suspend fun addNotificationLog(log: NotificationLog)
}