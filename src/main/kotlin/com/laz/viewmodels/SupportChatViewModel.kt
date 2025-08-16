package com.laz.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laz.models.SupportMessage
import com.laz.models.SupportChat
import com.laz.repositories.SupportChatRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SupportChatViewModel(
    private val repository: SupportChatRepository
) : ViewModel() {

    private val _currentChatId = MutableStateFlow<String?>(null)
    val currentChatId: StateFlow<String?> = _currentChatId.asStateFlow()

    private val _messages = MutableStateFlow<List<SupportMessage>>(emptyList())
    val messages: StateFlow<List<SupportMessage>> = _messages.asStateFlow()

    private val _allChats = MutableStateFlow<List<SupportChat>>(emptyList())
    val allChats: StateFlow<List<SupportChat>> = _allChats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        // Load all chats for employees
        loadAllChats()
    }

    // Customer starts chat
    fun startCustomerChat(customerId: Long, customerName: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                println("DEBUG: Starting customer chat for user $customerId ($customerName)")
                val chatId = repository.startCustomerChat(customerId, customerName)
                println("DEBUG: Chat started with ID: $chatId")
                _currentChatId.value = chatId
                loadChatMessages(chatId)
            } catch (e: Exception) {
                println("ERROR: Failed to start chat: ${e.message}")
                e.printStackTrace()
                _error.value = "Failed to start chat: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Send message
    fun sendMessage(
        customerId: Long,
        customerName: String,
        message: String,
        isFromCustomer: Boolean,
        employeeName: String = ""
    ) {
        val chatId = _currentChatId.value ?: return
        
        viewModelScope.launch {
            try {
                repository.sendMessage(
                    chatId = chatId,
                    customerId = customerId,
                    customerName = customerName,
                    message = message,
                    isFromCustomer = isFromCustomer,
                    employeeName = employeeName
                )
            } catch (e: Exception) {
                _error.value = "Failed to send message: ${e.message}"
            }
        }
    }

    // Load messages for current chat
    private fun loadChatMessages(chatId: String) {
        viewModelScope.launch {
            repository.getChatMessages(chatId).collect { messageList ->
                _messages.value = messageList
            }
        }
    }

    // Load all chats for employee view
    private fun loadAllChats() {
        viewModelScope.launch {
            repository.getAllActiveChats().collect { chatList ->
                _allChats.value = chatList
            }
        }
    }

    // Employee selects a chat
    fun selectChat(chatId: String) {
        _currentChatId.value = chatId
        loadChatMessages(chatId)
        
        // Mark as read by employee
        viewModelScope.launch {
            try {
                repository.markAsReadByEmployee(chatId)
            } catch (e: Exception) {
                _error.value = "Failed to mark as read: ${e.message}"
            }
        }
    }

    // Employee closes chat
    fun closeChat(chatId: String) {
        viewModelScope.launch {
            try {
                repository.closeChat(chatId)
                if (_currentChatId.value == chatId) {
                    _currentChatId.value = null
                    _messages.value = emptyList()
                }
            } catch (e: Exception) {
                _error.value = "Failed to close chat: ${e.message}"
            }
        }
    }

    // Clear error
    fun clearError() {
        _error.value = null
    }

    // Get total unread count for employee badge
    val totalUnreadCount: StateFlow<Int> = _allChats.map { chats ->
        chats.sumOf { it.unreadByEmployee }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )
}
