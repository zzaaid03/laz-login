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
    
    init {
        android.util.Log.d("FirebaseOrders", "Repository initialized")
        android.util.Log.d("FirebaseOrders", "Database URL: ${database.reference.toString()}")
        android.util.Log.d("FirebaseOrders", "Orders reference: ${ordersRef.toString()}")
    }
    
    suspend fun createOrder(order: Order): Result<Order> {
        return try {
            android.util.Log.d("FirebaseOrders", "Starting createOrder process...")
            
            // Test Firebase connection first
            try {
                val testRef = database.getReference("test")
                testRef.setValue("connection_test_${System.currentTimeMillis()}").await()
                android.util.Log.d("FirebaseOrders", "✅ Firebase connection test successful")
            } catch (e: Exception) {
                android.util.Log.e("FirebaseOrders", "❌ Firebase connection test failed: ${e.message}")
                return Result.failure(Exception("Firebase connection failed: ${e.message}"))
            }
            
            val orderId = getNextOrderId()
            android.util.Log.d("FirebaseOrders", "Generated order ID: $orderId")
            val orderWithId = order.copy(id = orderId)
            
            // Check stock availability
            android.util.Log.d("FirebaseOrders", "Checking stock availability for ${orderWithId.items.size} items")
            val stockCheckResult = checkStockAvailability(orderWithId.items)
            if (!stockCheckResult.first) {
                android.util.Log.e("FirebaseOrders", "Stock check failed: ${stockCheckResult.second}")
                return Result.failure(Exception("Insufficient stock: ${stockCheckResult.second}"))
            }
            android.util.Log.d("FirebaseOrders", "Stock check passed")
            
            // Check Firebase Auth status
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            android.util.Log.d("FirebaseOrders", "Current Firebase user: ${currentUser?.uid ?: "NOT AUTHENTICATED"}")
            android.util.Log.d("FirebaseOrders", "Firebase Auth instance: ${com.google.firebase.auth.FirebaseAuth.getInstance()}")
            
            // For testing, allow order creation without authentication
            if (currentUser == null) {
                android.util.Log.w("FirebaseOrders", "⚠️ User not authenticated - proceeding anyway for testing")
                // Don't return failure, continue with order creation
            }
            
            // Save order to Firebase
            android.util.Log.d("FirebaseOrders", "Preparing order data for Firebase...")
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
            android.util.Log.d("FirebaseOrders", "Attempting to save order to Firebase path: orders/$orderId")
            android.util.Log.d("FirebaseOrders", "Order data: $orderMap")
            
            try {
                ordersRef.child(orderId.toString()).setValue(orderMap).await()
                android.util.Log.d("FirebaseOrders", "✅ Order successfully saved to Firebase with ID: $orderId")
            } catch (e: Exception) {
                android.util.Log.e("FirebaseOrders", "❌ Failed to save order to Firebase: ${e.message}")
                android.util.Log.e("FirebaseOrders", "Exception details: ${e.javaClass.simpleName}")
                throw e
            }
            
            // Deduct stock for fulfilled orders
            if (orderWithId.status !in listOf(OrderStatus.CANCELLED, OrderStatus.RETURNED)) {
                deductOrderStock(orderWithId)
            }
            
            Result.success(orderWithId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOrderById(orderId: Long): Result<Order?> {
        return try {
            val snapshot = ordersRef.child(orderId.toString()).get().await()
            Result.success(snapshot.toOrder())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllOrders(): Result<List<Order>> {
        return try {
            val snapshot = ordersRef.get().await()
            val orders = snapshot.children.mapNotNull { it.toOrder() }
                .sortedByDescending { it.orderDate }
            Result.success(orders)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOrdersByCustomerId(customerId: Long): Result<List<Order>> {
        return try {
            val snapshot = ordersRef.orderByChild("customerId").equalTo(customerId.toDouble()).get().await()
            val orders = snapshot.children.mapNotNull { it.toOrder() }
                .sortedByDescending { it.orderDate }
            Result.success(orders)
        } catch (e: Exception) {
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
            
            // Check stock management needs based on status changes
            val wasOrderCancelledOrReturned = (order.status == OrderStatus.CANCELLED || order.status == OrderStatus.RETURNED)
            val isOrderBeingCancelledOrReturned = (status == OrderStatus.CANCELLED || status == OrderStatus.RETURNED)
            val isOrderBeingFulfilled = (status == OrderStatus.DELIVERED || status == OrderStatus.SHIPPED || status == OrderStatus.PROCESSING)
            
            val shouldRestoreStock = isOrderBeingCancelledOrReturned && !wasOrderCancelledOrReturned
            val shouldDeductStock = isOrderBeingFulfilled && wasOrderCancelledOrReturned
            
            // Update order status
            val updates = mutableMapOf<String, Any?>(
                "status" to status.name
            )
            if (trackingNumber != null) {
                updates["trackingNumber"] = trackingNumber
            }
            
            ordersRef.child(orderId.toString()).updateChildren(updates).await()
            
            // Handle stock changes based on status transition
            if (shouldRestoreStock) {
                restoreOrderStock(order)
            } else if (shouldDeductStock) {
                deductOrderStock(order)
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
                }
            }
        } catch (e: Exception) {
            // Don't throw exception here - order status update should still succeed
        }
    }

    /**
     * Deduct product stock when order changes from cancelled/returned to fulfilled status
     */
    private suspend fun deductOrderStock(order: Order) {
        try {
            val productsRef = database.getReference("products")
            
            for (item in order.items) {
                // Get current product data
                val productSnapshot = productsRef.child(item.productId.toString()).get().await()
                if (productSnapshot.exists()) {
                    val currentQuantity = productSnapshot.child("quantity").getValue(Int::class.java) ?: 0
                    val deductedQuantity = maxOf(0, currentQuantity - item.quantity) // Ensure we don't go below 0
                    
                    // Update product quantity
                    productsRef.child(item.productId.toString())
                        .child("quantity")
                        .setValue(deductedQuantity)
                        .await()
                }
            }
        } catch (e: Exception) {
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
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24L * 60L * 60L * 1000L)
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
                android.util.Log.d("FirebaseOrders", "Orders snapshot received: ${snapshot.childrenCount} orders")
                val orders = snapshot.children.mapNotNull { 
                    val order = it.toOrder()
                    if (order == null) {
                        android.util.Log.w("FirebaseOrders", "Failed to parse order: ${it.key}")
                    }
                    order
                }.sortedByDescending { it.orderDate }
                android.util.Log.d("FirebaseOrders", "Parsed ${orders.size} orders successfully")
                trySend(orders)
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("FirebaseOrders", "Orders listener cancelled: ${error.message}")
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
     * Check stock availability for order items
     */
    private suspend fun checkStockAvailability(items: List<OrderItem>): Pair<Boolean, String> {
        return try {
            val productsRef = database.getReference("products")
            
            for (item in items) {
                val productSnapshot = productsRef.child(item.productId.toString()).get().await()
                if (productSnapshot.exists()) {
                    val currentQuantity = productSnapshot.child("quantity").getValue(Int::class.java) ?: 0
                    if (currentQuantity < item.quantity) {
                        return Pair(false, "Product '${item.productName}' has insufficient stock. Available: $currentQuantity, Required: ${item.quantity}")
                    }
                } else {
                    return Pair(false, "Product '${item.productName}' not found")
                }
            }
            
            Pair(true, "Stock available")
        } catch (e: Exception) {
            Pair(false, "Error checking stock: ${e.message}")
        }
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
            // Get ID with flexible type handling
            val id = child("id").getValue(Long::class.java) 
                ?: child("id").getValue(Int::class.java)?.toLong()
                ?: child("id").getValue(String::class.java)?.toLongOrNull()
            if (id == null) return null
            
            // Get customer ID with flexible type handling
            val customerId = child("customerId").getValue(Long::class.java)
                ?: child("customerId").getValue(Int::class.java)?.toLong()
                ?: child("customerId").getValue(String::class.java)?.toLongOrNull()
            if (customerId == null) return null
            
            // Get customer username
            val customerUsername = child("customerUsername").getValue(String::class.java) ?: ""
            if (customerUsername.isEmpty()) return null
            
            // Get total amount with flexible type handling
            val totalAmount = child("totalAmount").getValue(String::class.java)?.let { 
                try { BigDecimal(it) } catch (e: Exception) { null }
            } ?: child("totalAmount").getValue(Double::class.java)?.let { BigDecimal(it) }
            ?: child("totalAmount").getValue(Float::class.java)?.let { BigDecimal(it.toDouble()) }
            ?: BigDecimal.ZERO
            
            // Get status with flexible handling and default fallback
            val status = child("status").getValue(String::class.java)?.let { 
                try { OrderStatus.valueOf(it) } 
                catch (e: Exception) { OrderStatus.PENDING }
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
