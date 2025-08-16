package com.laz.models

import java.util.Date

data class PotentialOrder(
    val id: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val chatSessionId: String = "",
    val status: PotentialOrderStatus = PotentialOrderStatus.PENDING,
    val requestedParts: List<RequestedPart> = emptyList(),
    val chatHistory: List<AIChatMessage> = emptyList(),
    val totalEstimatedCost: Double = 0.0,
    val totalSellingPrice: Double = 0.0,
    val profitMargin: Double = 0.20, // 20% default
    val adminNotes: String = "",
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

data class RequestedPart(
    val id: String = "",
    val partName: String = "",
    val description: String = "",
    val customerImages: List<String> = emptyList(), // Firebase Storage URLs
    val aliexpressLinks: List<AliexpressProduct> = emptyList(),
    val selectedProduct: AliexpressProduct? = null,
    val quantity: Int = 1,
    val estimatedCost: Double = 0.0,
    val sellingPrice: Double = 0.0
)

data class AliexpressProduct(
    val productId: String = "",
    val title: String = "",
    val price: Double = 0.0,
    val shippingCost: Double = 0.0,
    val totalCost: Double = 0.0,
    val productUrl: String = "",
    val imageUrl: String = "",
    val rating: Double = 0.0,
    val soldCount: Int = 0,
    val deliveryTime: String = ""
)

enum class PotentialOrderStatus {
    PENDING,        // Waiting for admin review
    APPROVED,       // Admin approved, ready to order
    ORDERED,        // Parts ordered from supplier
    RECEIVED,       // Parts received, ready to sell
    COMPLETED,      // Sold to customer
    REJECTED,       // Admin rejected
    CANCELLED       // Customer cancelled
}

data class AIChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderType: MessageSenderType = MessageSenderType.CUSTOMER,
    val message: String = "",
    val imageUrls: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val aiProcessed: Boolean = false,
    val aiResponse: String = ""
)

enum class MessageSenderType {
    CUSTOMER,
    AI_BOT,
    ADMIN
}
