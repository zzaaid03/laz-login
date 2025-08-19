package com.laz.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.laz.FirebaseMainActivity
import com.laz.R

object NotificationHelper {
    // Notification channels
    const val CHANNEL_ORDERS = "orders"
    const val CHANNEL_CHAT = "chat"
    const val CHANNEL_STOCK = "stock"
    const val CHANNEL_PROMOS = "promos"

    // Notification types for role-based filtering
    enum class NotificationType {
        // Admin notifications
        LOW_STOCK,
        NEW_ORDER_ADMIN,
        
        // Employee notifications
        NEW_ORDER_EMPLOYEE,
        CUSTOMER_CHAT,
        
        // Customer notifications
        ORDER_STATUS_UPDATE,
        SUPPORT_CHAT,
        CART_HOLD_EXPIRY
    }

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_ORDERS,
                    "Order Updates",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications about order status changes"
                },
                NotificationChannel(
                    CHANNEL_CHAT,
                    "Support Chat",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "New messages in support chat"
                },
                NotificationChannel(
                    CHANNEL_STOCK,
                    "Stock Alerts",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Low stock and inventory alerts"
                },
                NotificationChannel(
                    CHANNEL_PROMOS,
                    "Promotions",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Promotional offers and announcements"
                }
            )
            
            channels.forEach { channel ->
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    fun showNotification(
        context: Context,
        channelId: String,
        title: String,
        message: String,
        notificationType: NotificationType,
        data: Map<String, String> = emptyMap()
    ) {
        createNotificationChannels(context)
        
        // Create intent to open the app
        val intent = Intent(context, FirebaseMainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Add extra data for navigation
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // You may want to create a specific notification icon
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(getNotificationPriority(notificationType))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationId = generateNotificationId(notificationType, data)
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun getNotificationPriority(type: NotificationType): Int {
        return when (type) {
            NotificationType.LOW_STOCK,
            NotificationType.NEW_ORDER_ADMIN,
            NotificationType.NEW_ORDER_EMPLOYEE -> NotificationCompat.PRIORITY_DEFAULT
            
            NotificationType.CUSTOMER_CHAT,
            NotificationType.SUPPORT_CHAT -> NotificationCompat.PRIORITY_HIGH
            
            NotificationType.ORDER_STATUS_UPDATE,
            NotificationType.CART_HOLD_EXPIRY -> NotificationCompat.PRIORITY_DEFAULT
        }
    }

    private fun generateNotificationId(type: NotificationType, data: Map<String, String>): Int {
        // Generate unique ID based on type and data
        val baseId = type.ordinal * 1000
        val dataHash = data.values.joinToString("").hashCode()
        return baseId + (dataHash % 1000).let { if (it < 0) -it else it }
    }

    // Role-based notification methods
    fun notifyAdmin(context: Context, type: NotificationType, title: String, message: String, data: Map<String, String> = emptyMap()) {
        when (type) {
            NotificationType.LOW_STOCK -> {
                showNotification(context, CHANNEL_STOCK, title, message, type, data)
            }
            NotificationType.NEW_ORDER_ADMIN -> {
                showNotification(context, CHANNEL_ORDERS, title, message, type, data)
            }
            else -> { /* Admin doesn't receive this type */ }
        }
    }

    fun notifyEmployee(context: Context, type: NotificationType, title: String, message: String, data: Map<String, String> = emptyMap()) {
        when (type) {
            NotificationType.NEW_ORDER_EMPLOYEE -> {
                showNotification(context, CHANNEL_ORDERS, title, message, type, data)
            }
            NotificationType.CUSTOMER_CHAT -> {
                showNotification(context, CHANNEL_CHAT, title, message, type, data)
            }
            else -> { /* Employee doesn't receive this type */ }
        }
    }

    fun notifyCustomer(context: Context, type: NotificationType, title: String, message: String, data: Map<String, String> = emptyMap()) {
        when (type) {
            NotificationType.ORDER_STATUS_UPDATE -> {
                showNotification(context, CHANNEL_ORDERS, title, message, type, data)
            }
            NotificationType.SUPPORT_CHAT -> {
                showNotification(context, CHANNEL_CHAT, title, message, type, data)
            }
            NotificationType.CART_HOLD_EXPIRY -> {
                showNotification(context, CHANNEL_STOCK, title, message, type, data)
            }
            else -> { /* Customer doesn't receive this type */ }
        }
    }
}
