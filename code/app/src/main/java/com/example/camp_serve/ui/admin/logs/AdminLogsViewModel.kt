package com.example.camp_serve.ui.admin.logs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.camp_serve.data.FirestoreMessageLogRepo
import com.example.camp_serve.data.FirestoreNotificationLogRepo
import com.example.camp_serve.data.MessageLogRepository
import com.example.camp_serve.data.NotificationLogRepository
import com.example.camp_serve.model.MessageLog
import com.example.camp_serve.model.NotificationLog
import kotlinx.coroutines.launch

class AdminLogsViewModel(
    private val repo: NotificationLogRepository = FirestoreNotificationLogRepo(),
    private val messageRepo: MessageLogRepository = FirestoreMessageLogRepo()
) : ViewModel() {

    var notifications by mutableStateOf<List<NotificationLog>>(emptyList())
        private set
    var isLoading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    var messages by mutableStateOf<List<MessageLog>>(emptyList())
        private set
    var messagesLoading by mutableStateOf(true)
        private set
    var messagesError by mutableStateOf<String?>(null)
        private set

    init { load() }

    fun load() {
        viewModelScope.launch {
            isLoading = true
            try {
                notifications = repo.getAllNotificationLogs()
                error = null
            } catch (e: Exception) {
                error = e.message ?: "Could not load notification logs"
            } finally {
                isLoading = false
            }
        }
        viewModelScope.launch {
            messagesLoading = true
            try {
                messages = messageRepo.getAllMessageLogs()
                messagesError = null
            } catch (e: Exception) {
                messagesError = e.message ?: "Could not load message logs"
            } finally {
                messagesLoading = false
            }
        }
    }
}