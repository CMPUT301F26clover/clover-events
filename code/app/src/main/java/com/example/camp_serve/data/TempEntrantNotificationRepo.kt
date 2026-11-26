package com.example.camp_serve.data

import com.example.camp_serve.model.EntrantNotification
import com.example.camp_serve.model.EntrantNotificationType

val sampleNotifications = listOf(
    EntrantNotification("1", EntrantNotificationType.WON, "Lakeview Campsite", "You won the lottery! Reserve before the booking period ends."),
    EntrantNotification("2", EntrantNotificationType.LOST, "Pine Ridge Campsite", "You were not selected this time."),
    EntrantNotification("3", EntrantNotificationType.RESERVED, "Cedar Hollow", "Your reservation is confirmed."),
    EntrantNotification("4", EntrantNotificationType.CANCELLED, "Birch Point", "Your reservation was cancelled by the owner.")
)

class TempEntrantNotificationRepo : EntrantNotificationRepository {
    override suspend fun getNotifications(): List<EntrantNotification> = sampleNotifications
}