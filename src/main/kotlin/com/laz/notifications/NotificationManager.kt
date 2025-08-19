package com.laz.notifications

import android.content.Context
import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.laz.models.UserRole
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class NotificationManager(private val context: Context) {

    companion object {
        private const val TAG = "NotificationManager"
        private const val LOW_STOCK_THRESHOLD = 5
    }

    // Initialize notification channels
    fun initialize() {
        NotificationHelper.createNotificationChannels(context)
    }

    // Admin Notifications
    suspend fun checkLowStockAndNotify() {
        try {
            val database = FirebaseDatabase.getInstance()
            val productsRef = database.getReference("products")
            
            val snapshot = productsRef.get().await()
            val lowStockProducts = mutableListOf<String>()
            
            snapshot.children.forEach { productSnapshot ->
                val quantity = productSnapshot.child("quantity").getValue(Int::class.java) ?: 0
                val name = productSnapshot.child("name").getValue(String::class.java) ?: "Unknown"
                
                if (quantity <= LOW_STOCK_THRESHOLD) {
                    lowStockProducts.add("$name (${quantity} left)")
                }
            }
            
            if (lowStockProducts.isNotEmpty()) {
                val title = "Low Stock Alert"
                val message = if (lowStockProducts.size == 1) {
                    "Product running low: ${lowStockProducts.first()}"
                } else {
                    "${lowStockProducts.size} products running low: ${lowStockProducts.take(3).joinToString(", ")}${if (lowStockProducts.size > 3) "..." else ""}"
                }
                
                NotificationHelper.notifyAdmin(
                    context,
                    NotificationHelper.NotificationType.LOW_STOCK,
                    title,
                    message,
                    mapOf("low_stock_count" to lowStockProducts.size.toString())
                )
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error checking low stock", e)
        }
    }

    fun notifyNewOrder(orderId: String, customerName: String, totalAmount: String) {
        // Notify Admin
        NotificationHelper.notifyAdmin(
            context,
            NotificationHelper.NotificationType.NEW_ORDER_ADMIN,
            "New Order Received",
            "Order #$orderId from $customerName - Total: $totalAmount JOD",
            mapOf(
                "order_id" to orderId,
                "customer_name" to customerName,
                "total_amount" to totalAmount
            )
        )
        
        // Notify Employee
        NotificationHelper.notifyEmployee(
            context,
            NotificationHelper.NotificationType.NEW_ORDER_EMPLOYEE,
            "New Order to Process",
            "Order #$orderId needs processing - Customer: $customerName",
            mapOf(
                "order_id" to orderId,
                "customer_name" to customerName
            )
        )
    }

    // Employee Notifications
    fun notifyCustomerChatMessage(chatId: String, customerName: String, message: String) {
        NotificationHelper.notifyEmployee(
            context,
            NotificationHelper.NotificationType.CUSTOMER_CHAT,
            "New Customer Message",
            "$customerName: ${message.take(50)}${if (message.length > 50) "..." else ""}",
            mapOf(
                "chat_id" to chatId,
                "customer_name" to customerName
            )
        )
    }

    // Customer Notifications
    fun notifyOrderStatusUpdate(orderId: String, newStatus: String, trackingNumber: String? = null) {
        val message = when (newStatus.uppercase()) {
            "CONFIRMED" -> "Your order has been confirmed and is being prepared"
            "PROCESSING" -> "Your order is being processed"
            "SHIPPED" -> "Your order has been shipped${trackingNumber?.let { " - Tracking: $it" } ?: ""}"
            "DELIVERED" -> "Your order has been delivered successfully"
            "CANCELLED" -> "Your order has been cancelled"
            "RETURNED" -> "Your return has been processed"
            else -> "Your order status has been updated to $newStatus"
        }
        
        NotificationHelper.notifyCustomer(
            context,
            NotificationHelper.NotificationType.ORDER_STATUS_UPDATE,
            "Order Update",
            message,
            mapOf(
                "order_id" to orderId,
                "status" to newStatus,
                "tracking_number" to (trackingNumber ?: "")
            )
        )
    }

    fun notifySupportChatMessage(chatId: String, agentName: String, message: String) {
        NotificationHelper.notifyCustomer(
            context,
            NotificationHelper.NotificationType.SUPPORT_CHAT,
            "Support Reply",
            "$agentName: ${message.take(50)}${if (message.length > 50) "..." else ""}",
            mapOf(
                "chat_id" to chatId,
                "agent_name" to agentName
            )
        )
    }

    fun scheduleCartHoldExpiryWarning(cartItemId: String, productName: String, expiryTime: Long) {
        val warningTime = expiryTime - (60 * 1000) // 1 minute before expiry
        val delay = warningTime - System.currentTimeMillis()
        
        if (delay > 0) {
            val workRequest = OneTimeWorkRequestBuilder<CartHoldExpiryWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(
                    Data.Builder()
                        .putString("cart_item_id", cartItemId)
                        .putString("product_name", productName)
                        .build()
                )
                .build()
            
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }

    // FCM Token Management
    suspend fun updateFCMToken(token: String) {
        try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                val database = FirebaseDatabase.getInstance()
                val tokenRef = database.getReference("user_tokens").child(currentUser.uid)
                
                tokenRef.setValue(mapOf(
                    "fcmToken" to token,
                    "lastUpdated" to System.currentTimeMillis()
                )).await()
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error updating FCM token", e)
        }
    }
}

// WorkManager worker for cart hold expiry warnings
class CartHoldExpiryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val cartItemId = inputData.getString("cart_item_id") ?: return Result.failure()
        val productName = inputData.getString("product_name") ?: "Item"
        
        NotificationHelper.notifyCustomer(
            applicationContext,
            NotificationHelper.NotificationType.CART_HOLD_EXPIRY,
            "Cart Item Expiring Soon",
            "$productName will be removed from your cart in 1 minute. Complete your purchase to secure it!",
            mapOf(
                "cart_item_id" to cartItemId,
                "product_name" to productName
            )
        )
        
        return Result.success()
    }
}
