package com.laz.models

data class SupportMessage(
    val id: String = "",
    val customerId: Long = 0,
    val customerName: String = "",
    val message: String = "",
    val isFromCustomer: Boolean = true,
    val employeeName: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class SupportChat(
    val id: String = "",
    val customerId: Long = 0,
    val customerName: String = "",
    val isActive: Boolean = true,
    val lastMessage: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val unreadByEmployee: Int = 0
)
