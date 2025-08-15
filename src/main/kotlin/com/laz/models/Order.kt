package com.laz.models

import java.math.BigDecimal

/**
 * Order Model
 * Represents customer orders in the system
 */
data class Order(
    val id: Long = 0,
    val customerId: Long,
    val customerUsername: String,
    val items: List<OrderItem>,
    val totalAmount: BigDecimal,
    val status: OrderStatus,
    val paymentMethod: String,
    val shippingAddress: String,
    val orderDate: Long = System.currentTimeMillis(),
    val estimatedDelivery: Long? = null,
    val trackingNumber: String? = null,
    val notes: String? = null
)

/**
 * Order Item - Individual product in an order
 */
data class OrderItem(
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val totalPrice: BigDecimal
)

/**
 * Order Status Enum
 */
enum class OrderStatus(val displayName: String) {
    PENDING("Pending Payment"),
    CONFIRMED("Order Confirmed"),
    PROCESSING("Processing"),
    SHIPPED("Shipped"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled"),
    RETURNED("Returned")
}

/**
 * Extension function to convert Order to Firebase-compatible map
 */
fun Order.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "customerId" to customerId,
    "customerUsername" to customerUsername,
    "items" to items.map { item ->
        mapOf(
            "productId" to item.productId,
            "productName" to item.productName,
            "quantity" to item.quantity,
            "unitPrice" to item.unitPrice.toString(),
            "totalPrice" to item.totalPrice.toString()
        )
    },
    "totalAmount" to totalAmount.toString(),
    "status" to status.name,
    "paymentMethod" to paymentMethod,
    "shippingAddress" to shippingAddress,
    "orderDate" to orderDate,
    "estimatedDelivery" to estimatedDelivery,
    "trackingNumber" to trackingNumber,
    "notes" to notes
)
