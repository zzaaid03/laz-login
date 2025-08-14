package com.laz.models

import java.util.*

data class ChatMessage(
    val id: String = "",
    val chatId: String = "", // Unique chat session ID per customer
    val customerId: Long = 0,
    val customerName: String = "",
    val message: String = "",
    val isFromCustomer: Boolean = true, // true if from customer, false if from employee
    val employeeId: Long? = null,
    val employeeName: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

data class ChatSession(
    val chatId: String = "",
    val customerId: Long = 0,
    val customerName: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val isActive: Boolean = true,
    val assignedEmployeeId: Long? = null,
    val assignedEmployeeName: String? = null
)

enum class ChatStatus {
    ACTIVE,
    RESOLVED,
    WAITING_FOR_CUSTOMER,
    WAITING_FOR_EMPLOYEE
}
