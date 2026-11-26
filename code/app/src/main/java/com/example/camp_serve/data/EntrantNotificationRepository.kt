package com.example.camp_serve.data

import com.example.camp_serve.model.EntrantNotification

interface EntrantNotificationRepository {
    suspend fun getNotifications(): List<EntrantNotification>
}