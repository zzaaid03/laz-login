package com.laz.repositories

import com.google.firebase.database.*
import com.laz.models.ChatMessage
import com.laz.models.ChatSession
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.*

class FirebaseChatRepository {
    private val database = FirebaseDatabase.getInstance()
    private val chatMessagesRef = database.getReference("chat_messages")
    private val chatSessionsRef = database.getReference("chat_sessions")

    // Create a new chat session for a customer
    suspend fun createChatSession(customerId: Long, customerName: String): String {
        val chatId = UUID.randomUUID().toString()
        val chatSession = ChatSession(
            chatId = chatId,
            customerId = customerId,
            customerName = customerName,
            lastMessage = "Chat session started",
            lastMessageTime = System.currentTimeMillis(),
            unreadCount = 0,
            isActive = true
        )
        
        chatSessionsRef.child(chatId).setValue(chatSession.toMap()).await()
        return chatId
    }

    // Send a message in a chat
    suspend fun sendMessage(
        chatId: String,
        customerId: Long,
        customerName: String,
        message: String,
        isFromCustomer: Boolean,
        employeeId: Long? = null,
        employeeName: String? = null
    ): String {
        val messageId = UUID.randomUUID().toString()
        val chatMessage = ChatMessage(
            id = messageId,
            chatId = chatId,
            customerId = customerId,
            customerName = customerName,
            message = message,
            isFromCustomer = isFromCustomer,
            employeeId = employeeId,
            employeeName = employeeName,
            timestamp = System.currentTimeMillis(),
            isRead = false
        )
        
        // Save message
        chatMessagesRef.child(chatId).child(messageId).setValue(chatMessage.toMap()).await()
        
        // Update chat session
        updateChatSession(chatId, message, isFromCustomer)
        
        return messageId
    }

    // Update chat session with latest message
    private suspend fun updateChatSession(chatId: String, lastMessage: String, isFromCustomer: Boolean) {
        val sessionRef = chatSessionsRef.child(chatId)
        val updates = mapOf(
            "lastMessage" to lastMessage,
            "lastMessageTime" to System.currentTimeMillis(),
            "unreadCount" to ServerValue.increment(if (isFromCustomer) 1 else 0)
        )
        sessionRef.updateChildren(updates).await()
    }

    // Get all chat sessions for employee dashboard
    fun getAllChatSessions(): Flow<List<ChatSession>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sessions = mutableListOf<ChatSession>()
                for (child in snapshot.children) {
                    child.toChatSession()?.let { sessions.add(it) }
                }
                // Sort by last message time (newest first)
                sessions.sortByDescending { it.lastMessageTime }
                trySend(sessions)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        chatSessionsRef.addValueEventListener(listener)
        awaitClose { chatSessionsRef.removeEventListener(listener) }
    }

    // Get messages for a specific chat
    fun getChatMessages(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = mutableListOf<ChatMessage>()
                for (child in snapshot.children) {
                    child.toChatMessage()?.let { messages.add(it) }
                }
                // Sort by timestamp (oldest first)
                messages.sortBy { it.timestamp }
                trySend(messages)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        chatMessagesRef.child(chatId).addValueEventListener(listener)
        awaitClose { chatMessagesRef.child(chatId).removeEventListener(listener) }
    }

    // Get or create chat session for customer
    suspend fun getOrCreateCustomerChat(customerId: Long, customerName: String): String {
        // Check if customer already has an active chat session
        val snapshot = chatSessionsRef.orderByChild("customerId").equalTo(customerId.toDouble()).get().await()
        
        for (child in snapshot.children) {
            val session = child.toChatSession()
            if (session?.isActive == true) {
                return session.chatId
            }
        }
        
        // Create new chat session if none exists
        return createChatSession(customerId, customerName)
    }

    // Mark messages as read
    suspend fun markMessagesAsRead(chatId: String, isEmployee: Boolean) {
        val snapshot = chatMessagesRef.child(chatId).get().await()
        val updates = mutableMapOf<String, Any>()
        
        for (child in snapshot.children) {
            val message = child.toChatMessage()
            if (message != null && !message.isRead) {
                // Mark as read if it's from the other party
                if ((isEmployee && message.isFromCustomer) || (!isEmployee && !message.isFromCustomer)) {
                    updates["${child.key}/isRead"] = true
                }
            }
        }
        
        if (updates.isNotEmpty()) {
            chatMessagesRef.child(chatId).updateChildren(updates).await()
        }
        
        // Reset unread count for the session
        if (isEmployee) {
            chatSessionsRef.child(chatId).child("unreadCount").setValue(0).await()
        }
    }

    // Assign employee to chat session
    suspend fun assignEmployeeToChat(chatId: String, employeeId: Long, employeeName: String) {
        val updates = mapOf(
            "assignedEmployeeId" to employeeId,
            "assignedEmployeeName" to employeeName
        )
        chatSessionsRef.child(chatId).updateChildren(updates).await()
    }

    // Extension functions for data conversion
    private fun ChatMessage.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "chatId" to chatId,
        "customerId" to customerId,
        "customerName" to customerName,
        "message" to message,
        "isFromCustomer" to isFromCustomer,
        "employeeId" to employeeId,
        "employeeName" to employeeName,
        "timestamp" to timestamp,
        "isRead" to isRead
    )

    private fun ChatSession.toMap(): Map<String, Any?> = mapOf(
        "chatId" to chatId,
        "customerId" to customerId,
        "customerName" to customerName,
        "lastMessage" to lastMessage,
        "lastMessageTime" to lastMessageTime,
        "unreadCount" to unreadCount,
        "isActive" to isActive,
        "assignedEmployeeId" to assignedEmployeeId,
        "assignedEmployeeName" to assignedEmployeeName
    )

    private fun DataSnapshot.toChatMessage(): ChatMessage? {
        return try {
            ChatMessage(
                id = child("id").getValue(String::class.java) ?: "",
                chatId = child("chatId").getValue(String::class.java) ?: "",
                customerId = child("customerId").getValue(Long::class.java) ?: 0,
                customerName = child("customerName").getValue(String::class.java) ?: "",
                message = child("message").getValue(String::class.java) ?: "",
                isFromCustomer = child("isFromCustomer").getValue(Boolean::class.java) ?: true,
                employeeId = child("employeeId").getValue(Long::class.java),
                employeeName = child("employeeName").getValue(String::class.java),
                timestamp = child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis(),
                isRead = child("isRead").getValue(Boolean::class.java) ?: false
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun DataSnapshot.toChatSession(): ChatSession? {
        return try {
            ChatSession(
                chatId = child("chatId").getValue(String::class.java) ?: "",
                customerId = child("customerId").getValue(Long::class.java) ?: 0,
                customerName = child("customerName").getValue(String::class.java) ?: "",
                lastMessage = child("lastMessage").getValue(String::class.java) ?: "",
                lastMessageTime = child("lastMessageTime").getValue(Long::class.java) ?: System.currentTimeMillis(),
                unreadCount = child("unreadCount").getValue(Int::class.java) ?: 0,
                isActive = child("isActive").getValue(Boolean::class.java) ?: true,
                assignedEmployeeId = child("assignedEmployeeId").getValue(Long::class.java),
                assignedEmployeeName = child("assignedEmployeeName").getValue(String::class.java)
            )
        } catch (e: Exception) {
            null
        }
    }
}
