package com.laz.repositories

import com.google.firebase.database.*
import com.laz.models.Order
import com.laz.models.OrderItem
import com.laz.models.OrderStatus
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.math.BigDecimal

/**
 * Firebase Orders Repository
 * Manages customer orders in Firebase Realtime Database
 */
class FirebaseOrdersRepository {
    private val database = FirebaseDatabase.getInstance()
    private val ordersRef = database.getReference("orders")
    
    /**
     * Create new order
     */
    suspend fun createOrder(order: Order): Result<Order> {
        return try {
            val orderId = getNextOrderId()
            val orderWithId = order.copy(id = orderId)
            
            val orderMap = mapOf(
                "id" to orderWithId.id,
                "customerId" to orderWithId.customerId,
                "customerUsername" to orderWithId.customerUsername,
                "items" to orderWithId.items.map { item ->
                    mapOf(
                        "productId" to item.productId,
                        "productName" to item.productName,
                        "quantity" to item.quantity,
                        "unitPrice" to item.unitPrice.toString(),
                        "totalPrice" to item.totalPrice.toString()
                    )
                },
                "totalAmount" to orderWithId.totalAmount.toString(),
                "status" to orderWithId.status.name,
                "paymentMethod" to orderWithId.paymentMethod,
                "shippingAddress" to orderWithId.shippingAddress,
                "orderDate" to orderWithId.orderDate,
                "estimatedDelivery" to orderWithId.estimatedDelivery,
                "trackingNumber" to orderWithId.trackingNumber,
                "notes" to orderWithId.notes
            )
            
            ordersRef.child(orderId.toString()).setValue(orderMap).await()
            Result.success(orderWithId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get order by ID
     */
    suspend fun getOrderById(orderId: Long): Result<Order?> {
        return try {
            val snapshot = ordersRef.child(orderId.toString()).get().await()
            val order = snapshot.toOrder()
            Result.success(order)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all orders (admin/employee only)
     */
    suspend fun getAllOrders(): Result<List<Order>> {
        return try {
            println("DEBUG: Loading all orders from Firebase")
            val snapshot = ordersRef.get().await()
            println("DEBUG: Orders snapshot exists: ${snapshot.exists()}")
            println("DEBUG: Orders snapshot children count: ${snapshot.childrenCount}")
            
            val orders = snapshot.children.mapNotNull { childSnapshot ->
                println("DEBUG: Processing order child: ${childSnapshot.key}")
                val order = childSnapshot.toOrder()
                if (order != null) {
                    println("DEBUG: Successfully converted order: ${order.id}")
                } else {
                    println("DEBUG: Failed to convert order from snapshot: ${childSnapshot.key}")
                }
                order
            }.sortedByDescending { it.orderDate }
            
            println("DEBUG: Successfully loaded ${orders.size} orders")
            Result.success(orders)
        } catch (e: Exception) {
            println("DEBUG: Exception loading all orders: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get orders by customer ID
     */
    suspend fun getOrdersByCustomerId(customerId: Long): Result<List<Order>> {
        return try {
            println("DEBUG: Loading orders for customer ID: $customerId")
            val snapshot = ordersRef.orderByChild("customerId").equalTo(customerId.toDouble()).get().await()
            println("DEBUG: Customer orders snapshot exists: ${snapshot.exists()}")
            println("DEBUG: Customer orders snapshot children count: ${snapshot.childrenCount}")
            
            val orders = snapshot.children.mapNotNull { childSnapshot ->
                println("DEBUG: Processing customer order child: ${childSnapshot.key}")
                val order = childSnapshot.toOrder()
                if (order != null) {
                    println("DEBUG: Successfully converted customer order: ${order.id} for customer ${order.customerId}")
                } else {
                    println("DEBUG: Failed to convert customer order from snapshot: ${childSnapshot.key}")
                }
                order
            }.sortedByDescending { it.orderDate }
            
            println("DEBUG: Successfully loaded ${orders.size} orders for customer $customerId")
            Result.success(orders)
        } catch (e: Exception) {
            println("DEBUG: Exception loading orders for customer $customerId: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Update order status with automatic stock restoration for cancelled/returned orders
     */
    suspend fun updateOrderStatus(orderId: Long, status: OrderStatus, trackingNumber: String? = null): Result<Unit> {
        return try {
            // First, get the current order to check if we need to restore stock
            val orderResult = getOrderById(orderId)
            if (orderResult.isFailure) {
                return Result.failure(orderResult.exceptionOrNull() ?: Exception("Failed to get order"))
            }
            
            val order = orderResult.getOrNull()
            if (order == null) {
                return Result.failure(Exception("Order not found"))
            }
            
            // Check if we need to restore stock (when changing to CANCELLED or RETURNED)
            val shouldRestoreStock = (status == OrderStatus.CANCELLED || status == OrderStatus.RETURNED) &&
                    (order.status != OrderStatus.CANCELLED && order.status != OrderStatus.RETURNED)
            
            // Update order status
            val updates = mutableMapOf<String, Any?>(
                "status" to status.name
            )
            if (trackingNumber != null) {
                updates["trackingNumber"] = trackingNumber
            }
            
            ordersRef.child(orderId.toString()).updateChildren(updates).await()
            
            // Restore stock if needed
            if (shouldRestoreStock) {
                restoreOrderStock(order)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Restore product stock for cancelled or returned orders
     */
    private suspend fun restoreOrderStock(order: Order) {
        try {
            val productsRef = database.getReference("products")
            
            for (item in order.items) {
                // Get current product data
                val productSnapshot = productsRef.child(item.productId.toString()).get().await()
                if (productSnapshot.exists()) {
                    val currentQuantity = productSnapshot.child("quantity").getValue(Int::class.java) ?: 0
                    val restoredQuantity = currentQuantity + item.quantity
                    
                    // Update product quantity
                    productsRef.child(item.productId.toString())
                        .child("quantity")
                        .setValue(restoredQuantity)
                        .await()
                    
                    println("DEBUG: Restored stock for product ${item.productId}: ${item.quantity} units (${currentQuantity} -> ${restoredQuantity})")
                }
            }
            
            println("DEBUG: Successfully restored stock for order ${order.id}")
        } catch (e: Exception) {
            println("ERROR: Failed to restore stock for order ${order.id}: ${e.message}")
            // Don't throw exception here - order status update should still succeed
        }
    }

    /**
     * Get orders by status
     */
    suspend fun getOrdersByStatus(status: OrderStatus): Result<List<Order>> {
        return try {
            val snapshot = ordersRef.orderByChild("status").equalTo(status.name).get().await()
            val orders = snapshot.children.mapNotNull { it.toOrder() }
                .sortedByDescending { it.orderDate }
            Result.success(orders)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get recent orders (last 30 days)
     */
    suspend fun getRecentOrders(limit: Int = 10): Result<List<Order>> {
        return try {
            val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000)
            val snapshot = ordersRef.orderByChild("orderDate").startAt(thirtyDaysAgo.toDouble()).get().await()
            val orders = snapshot.children.mapNotNull { it.toOrder() }
                .sortedByDescending { it.orderDate }
                .take(limit)
            Result.success(orders)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get orders count (only completed orders - SHIPPED, DELIVERED)
     */
    suspend fun getOrdersCount(): Result<Int> {
        return try {
            val snapshot = ordersRef.get().await()
            val completedOrdersCount = snapshot.children.mapNotNull { it.toOrder() }
                .count { order -> 
                    order.status == OrderStatus.SHIPPED || order.status == OrderStatus.DELIVERED 
                }
            Result.success(completedOrdersCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get total orders amount (only completed orders - SHIPPED, DELIVERED)
     */
    suspend fun getTotalOrdersAmount(): Result<BigDecimal> {
        return try {
            val snapshot = ordersRef.get().await()
            val total = snapshot.children.mapNotNull { it.toOrder() }
                .filter { order -> 
                    order.status == OrderStatus.SHIPPED || order.status == OrderStatus.DELIVERED 
                }
                .sumOf { it.totalAmount }
            Result.success(total)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get real-time orders flow
     */
    fun getAllOrdersFlow(): Flow<List<Order>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val orders = snapshot.children.mapNotNull { it.toOrder() }
                    .sortedByDescending { it.orderDate }
                trySend(orders)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        ordersRef.addValueEventListener(listener)
        awaitClose { ordersRef.removeEventListener(listener) }
    }

    /**
     * Get real-time orders flow for specific customer
     */
    fun getOrdersByCustomerIdFlow(customerId: Long): Flow<List<Order>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val orders = snapshot.children.mapNotNull { it.toOrder() }
                    .filter { it.customerId == customerId }
                    .sortedByDescending { it.orderDate }
                trySend(orders)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        ordersRef.addValueEventListener(listener)
        awaitClose { ordersRef.removeEventListener(listener) }
    }

    /**
     * Get next order ID
     */
    private suspend fun getNextOrderId(): Long {
        return try {
            val snapshot = ordersRef.get().await()
            val maxId = snapshot.children.mapNotNull { 
                it.child("id").getValue(Long::class.java) 
            }.maxOrNull() ?: 0
            maxId + 1
        } catch (e: Exception) {
            System.currentTimeMillis() // Fallback to timestamp
        }
    }

    /**
     * Convert DataSnapshot to Order
     */
    private fun DataSnapshot.toOrder(): Order? {
        return try {
            println("DEBUG: Converting order snapshot: ${this.key}")
            println("DEBUG: Order data: ${this.value}")
            
            // Get ID with flexible type handling
            val id = child("id").getValue(Long::class.java) 
                ?: child("id").getValue(Int::class.java)?.toLong()
                ?: child("id").getValue(String::class.java)?.toLongOrNull()
            if (id == null) {
                println("DEBUG: Order conversion failed - missing or invalid id")
                return null
            }
            
            // Get customer ID with flexible type handling
            val customerId = child("customerId").getValue(Long::class.java)
                ?: child("customerId").getValue(Int::class.java)?.toLong()
                ?: child("customerId").getValue(String::class.java)?.toLongOrNull()
            if (customerId == null) {
                println("DEBUG: Order conversion failed - missing or invalid customerId")
                return null
            }
            
            // Get customer username
            val customerUsername = child("customerUsername").getValue(String::class.java) ?: ""
            if (customerUsername.isEmpty()) {
                println("DEBUG: Order conversion failed - missing customerUsername")
                return null
            }
            
            // Get total amount with flexible type handling
            val totalAmount = child("totalAmount").getValue(String::class.java)?.let { 
                try { BigDecimal(it) } catch (e: Exception) { null }
            } ?: child("totalAmount").getValue(Double::class.java)?.let { BigDecimal(it) }
            ?: child("totalAmount").getValue(Float::class.java)?.let { BigDecimal(it.toDouble()) }
            ?: BigDecimal.ZERO
            
            // Get status with flexible handling and default fallback
            val status = child("status").getValue(String::class.java)?.let { 
                try { OrderStatus.valueOf(it) } 
                catch (e: Exception) { 
                    println("DEBUG: Invalid order status: $it, using PENDING as default")
                    OrderStatus.PENDING
                }
            } ?: OrderStatus.PENDING
            
            // Get payment method with default fallback
            val paymentMethod = child("paymentMethod").getValue(String::class.java) ?: "Unknown"
            
            // Get shipping address with default fallback
            val shippingAddress = child("shippingAddress").getValue(String::class.java) ?: "No address provided"
            
            val orderDate = child("orderDate").getValue(Long::class.java) ?: System.currentTimeMillis()
            val estimatedDelivery = child("estimatedDelivery").getValue(Long::class.java)
            val trackingNumber = child("trackingNumber").getValue(String::class.java)
            val notes = child("notes").getValue(String::class.java)

            val items = child("items").children.mapNotNull { itemSnapshot ->
                try {
                    // Get product ID with flexible type handling
                    val productId = itemSnapshot.child("productId").getValue(Long::class.java)
                        ?: itemSnapshot.child("productId").getValue(Int::class.java)?.toLong()
                        ?: itemSnapshot.child("productId").getValue(String::class.java)?.toLongOrNull()
                        ?: return@mapNotNull null
                    
                    // Get product name
                    val productName = itemSnapshot.child("productName").getValue(String::class.java) 
                        ?: return@mapNotNull null
                    
                    // Get quantity with flexible type handling
                    val quantity = itemSnapshot.child("quantity").getValue(Int::class.java)
                        ?: itemSnapshot.child("quantity").getValue(Long::class.java)?.toInt()
                        ?: itemSnapshot.child("quantity").getValue(String::class.java)?.toIntOrNull()
                        ?: return@mapNotNull null
                    
                    // Get unit price with flexible type handling
                    val unitPrice = itemSnapshot.child("unitPrice").getValue(String::class.java)?.let { 
                        try { BigDecimal(it) } catch (e: Exception) { null }
                    } ?: itemSnapshot.child("unitPrice").getValue(Double::class.java)?.let { BigDecimal(it) }
                    ?: itemSnapshot.child("unitPrice").getValue(Float::class.java)?.let { BigDecimal(it.toDouble()) }
                    ?: return@mapNotNull null
                    
                    // Get total price with flexible type handling
                    val totalPrice = itemSnapshot.child("totalPrice").getValue(String::class.java)?.let { 
                        try { BigDecimal(it) } catch (e: Exception) { null }
                    } ?: itemSnapshot.child("totalPrice").getValue(Double::class.java)?.let { BigDecimal(it) }
                    ?: itemSnapshot.child("totalPrice").getValue(Float::class.java)?.let { BigDecimal(it.toDouble()) }
                    ?: return@mapNotNull null

                    OrderItem(
                        productId = productId,
                        productName = productName,
                        quantity = quantity,
                        unitPrice = unitPrice,
                        totalPrice = totalPrice
                    )
                } catch (e: Exception) {
                    println("DEBUG: Failed to convert order item: ${e.message}")
                    null
                }
            }

            Order(
                id = id,
                customerId = customerId,
                customerUsername = customerUsername,
                items = items,
                totalAmount = totalAmount,
                status = status,
                paymentMethod = paymentMethod,
                shippingAddress = shippingAddress,
                orderDate = orderDate,
                estimatedDelivery = estimatedDelivery,
                trackingNumber = trackingNumber,
                notes = notes
            )
        } catch (e: Exception) {
            null
        }
    }
}
