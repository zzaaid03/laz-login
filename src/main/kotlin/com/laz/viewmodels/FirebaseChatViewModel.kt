package com.laz.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laz.models.ChatMessage
import com.laz.models.ChatSession
import com.laz.repositories.FirebaseChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FirebaseChatViewModel(
    private val chatRepository: FirebaseChatRepository = FirebaseChatRepository()
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _currentChatId = MutableStateFlow<String?>(null)
    val currentChatId: StateFlow<String?> = _currentChatId.asStateFlow()

    // For employee dashboard - all chat sessions
    private val _chatSessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val chatSessions: StateFlow<List<ChatSession>> = _chatSessions.asStateFlow()

    // For current chat messages
    private val _currentChatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val currentChatMessages: StateFlow<List<ChatMessage>> = _currentChatMessages.asStateFlow()

    // Job for managing message collection lifecycle
    private var currentMessageJob: Job? = null
    private var chatSessionsJob: Job? = null

    // Load all chat sessions for employee dashboard
    fun loadChatSessions() {
        // Cancel any existing chat sessions collection
        chatSessionsJob?.cancel()
        
        _isLoading.value = true
        _errorMessage.value = null
        
        // Start collecting chat sessions with proper lifecycle management
        chatSessionsJob = viewModelScope.launch {
            try {
                chatRepository.getAllChatSessions().collect { sessions ->
                    _chatSessions.value = sessions
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load chat sessions: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    // Start or get existing chat for customer
    fun startCustomerChat(customerId: Long, customerName: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                val chatId = chatRepository.getOrCreateCustomerChat(customerId, customerName)
                _currentChatId.value = chatId
                loadChatMessages(chatId)
                
            } catch (e: Exception) {
                _errorMessage.value = "Failed to start chat: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    // Load messages for a specific chat
    fun loadChatMessages(chatId: String) {
        // Cancel any existing message collection
        currentMessageJob?.cancel()
        
        _isLoading.value = true
        _errorMessage.value = null
        _currentChatId.value = chatId
        
        // Start collecting messages with proper lifecycle management
        currentMessageJob = viewModelScope.launch {
            try {
                chatRepository.getChatMessages(chatId).collect { messages ->
                    _currentChatMessages.value = messages
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load messages: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    // Send message from customer
    fun sendCustomerMessage(
        customerId: Long,
        customerName: String,
        message: String
    ) {
        viewModelScope.launch {
            try {
                val chatId = _currentChatId.value ?: return@launch
                
                chatRepository.sendMessage(
                    chatId = chatId,
                    customerId = customerId,
                    customerName = customerName,
                    message = message,
                    isFromCustomer = true
                )
                
                // Clear any error messages on successful send
                _errorMessage.value = null
                
            } catch (e: Exception) {
                _errorMessage.value = "Failed to send message: ${e.message}"
            }
        }
    }

    // Send message from employee
    fun sendEmployeeMessage(
        chatId: String,
        customerId: Long,
        customerName: String,
        message: String,
        employeeId: Long,
        employeeName: String
    ) {
        viewModelScope.launch {
            try {
                chatRepository.sendMessage(
                    chatId = chatId,
                    customerId = customerId,
                    customerName = customerName,
                    message = message,
                    isFromCustomer = false,
                    employeeId = employeeId,
                    employeeName = employeeName
                )
                
                // Assign employee to chat if not already assigned
                assignEmployeeToChat(chatId, employeeId, employeeName)
                
                // Clear any error messages on successful send
                _errorMessage.value = null
                
            } catch (e: Exception) {
                _errorMessage.value = "Failed to send message: ${e.message}"
            }
        }
    }

    // Assign employee to chat session
    fun assignEmployeeToChat(chatId: String, employeeId: Long, employeeName: String) {
        viewModelScope.launch {
            try {
                chatRepository.assignEmployeeToChat(chatId, employeeId, employeeName)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to assign employee: ${e.message}"
            }
        }
    }

    // Mark messages as read
    fun markMessagesAsRead(chatId: String, isEmployee: Boolean) {
        viewModelScope.launch {
            try {
                chatRepository.markMessagesAsRead(chatId, isEmployee)
            } catch (e: Exception) {
                // Silent fail for read status - not critical
            }
        }
    }

    // Clear error message
    fun clearError() {
        _errorMessage.value = null
    }

    // Clear current chat
    fun clearCurrentChat() {
        currentMessageJob?.cancel()
        currentMessageJob = null
        _currentChatId.value = null
        _currentChatMessages.value = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        currentMessageJob?.cancel()
        chatSessionsJob?.cancel()
    }

    // Get unread message count for employee dashboard
    fun getTotalUnreadCount(): StateFlow<Int> {
        return _chatSessions.map { sessions ->
            sessions.sumOf { it.unreadCount }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = 0
        )
    }

    // Get active chat sessions count
    fun getActiveChatCount(): StateFlow<Int> {
        return _chatSessions.map { sessions ->
            sessions.count { it.isActive }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = 0
        )
    }
}
