package com.laz.repositories

import com.google.firebase.database.*
import com.laz.models.SupportMessage
import com.laz.models.SupportChat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.*

class SupportChatRepository {
    private val database: FirebaseDatabase by lazy { 
        try {
            FirebaseDatabase.getInstance()
        } catch (e: Exception) {
            throw RuntimeException("Firebase Database not initialized. Check google-services.json and Firebase setup.", e)
        }
    }
    private val chatsRef by lazy { database.getReference("support_chats") }
    private val messagesRef by lazy { database.getReference("support_messages") }

    // Customer starts a new chat or gets existing active chat
    suspend fun startCustomerChat(customerId: Long, customerName: String): String {
        // Check for existing active chat
        val existingChat = getActiveCustomerChat(customerId)
        if (existingChat != null) {
            return existingChat.id
        }

        // Create new chat
        val chatId = UUID.randomUUID().toString()
        val chat = SupportChat(
            id = chatId,
            customerId = customerId,
            customerName = customerName,
            isActive = true,
            lastMessage = "Customer started chat",
            lastMessageTime = System.currentTimeMillis(),
            unreadByEmployee = 0
        )

        chatsRef.child(chatId).setValue(chat.toMap()).await()
        return chatId
    }

    // Send a message
    suspend fun sendMessage(
        chatId: String,
        customerId: Long,
        customerName: String,
        message: String,
        isFromCustomer: Boolean,
        employeeName: String = ""
    ) {
        val messageId = UUID.randomUUID().toString()
        val supportMessage = SupportMessage(
            id = messageId,
            customerId = customerId,
            customerName = customerName,
            message = message,
            isFromCustomer = isFromCustomer,
            employeeName = employeeName,
            timestamp = System.currentTimeMillis()
        )

        // Save message
        messagesRef.child(chatId).child(messageId).setValue(supportMessage.toMap()).await()

        // Update chat with last message
        val updates = mapOf(
            "lastMessage" to message,
            "lastMessageTime" to System.currentTimeMillis(),
            "unreadByEmployee" to if (isFromCustomer) ServerValue.increment(1) else 0
        )
        chatsRef.child(chatId).updateChildren(updates).await()
    }

    // Get active chat for customer
    private suspend fun getActiveCustomerChat(customerId: Long): SupportChat? {
        val snapshot = chatsRef
            .orderByChild("customerId")
            .equalTo(customerId.toDouble())
            .get()
            .await()

        for (child in snapshot.children) {
            val chat = child.toSupportChat()
            if (chat?.isActive == true) {
                return chat
            }
        }
        return null
    }

    // Get all active chats for employees
    fun getAllActiveChats(): Flow<List<SupportChat>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val chats = mutableListOf<SupportChat>()
                for (child in snapshot.children) {
                    val chat = child.toSupportChat()
                    if (chat?.isActive == true) {
                        chats.add(chat)
                    }
                }
                chats.sortByDescending { it.lastMessageTime }
                trySend(chats)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        chatsRef.addValueEventListener(listener)
        awaitClose { chatsRef.removeEventListener(listener) }
    }

    // Get messages for a chat
    fun getChatMessages(chatId: String): Flow<List<SupportMessage>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = mutableListOf<SupportMessage>()
                for (child in snapshot.children) {
                    child.toSupportMessage()?.let { messages.add(it) }
                }
                messages.sortBy { it.timestamp }
                trySend(messages)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        messagesRef.child(chatId).addValueEventListener(listener)
        awaitClose { messagesRef.child(chatId).removeEventListener(listener) }
    }

    // Employee closes chat
    suspend fun closeChat(chatId: String) {
        chatsRef.child(chatId).child("isActive").setValue(false).await()
    }

    // Mark messages as read by employee
    suspend fun markAsReadByEmployee(chatId: String) {
        chatsRef.child(chatId).child("unreadByEmployee").setValue(0).await()
    }

    // Extension functions
    private fun SupportChat.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "customerId" to customerId,
        "customerName" to customerName,
        "isActive" to isActive,
        "lastMessage" to lastMessage,
        "lastMessageTime" to lastMessageTime,
        "unreadByEmployee" to unreadByEmployee
    )

    private fun SupportMessage.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "customerId" to customerId,
        "customerName" to customerName,
        "message" to message,
        "isFromCustomer" to isFromCustomer,
        "employeeName" to employeeName,
        "timestamp" to timestamp
    )

    private fun DataSnapshot.toSupportChat(): SupportChat? {
        return try {
            SupportChat(
                id = child("id").getValue(String::class.java) ?: "",
                customerId = child("customerId").getValue(Long::class.java) ?: 0,
                customerName = child("customerName").getValue(String::class.java) ?: "",
                isActive = child("isActive").getValue(Boolean::class.java) ?: true,
                lastMessage = child("lastMessage").getValue(String::class.java) ?: "",
                lastMessageTime = child("lastMessageTime").getValue(Long::class.java) ?: 0,
                unreadByEmployee = child("unreadByEmployee").getValue(Int::class.java) ?: 0
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun DataSnapshot.toSupportMessage(): SupportMessage? {
        return try {
            SupportMessage(
                id = child("id").getValue(String::class.java) ?: "",
                customerId = child("customerId").getValue(Long::class.java) ?: 0,
                customerName = child("customerName").getValue(String::class.java) ?: "",
                message = child("message").getValue(String::class.java) ?: "",
                isFromCustomer = child("isFromCustomer").getValue(Boolean::class.java) ?: true,
                employeeName = child("employeeName").getValue(String::class.java) ?: "",
                timestamp = child("timestamp").getValue(Long::class.java) ?: 0
            )
        } catch (e: Exception) {
            null
        }
    }
}
