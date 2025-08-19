package com.laz.notifications

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.laz.models.UserRole

class LazMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "LazMessagingService"
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        
        // Print token to console for testing
        println("=== FCM TOKEN FOR TESTING ===")
        println(token)
        println("=============================")
        
        // Save token to Firebase for this user
        saveTokenToFirebase(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Message received from: ${remoteMessage.from}")

        // Extract notification data
        val title = remoteMessage.notification?.title 
            ?: remoteMessage.data["title"] 
            ?: "LAZ Store"
            
        val body = remoteMessage.notification?.body 
            ?: remoteMessage.data["body"] 
            ?: "You have a new notification"
            
        val notificationTypeStr = remoteMessage.data["notification_type"]
        val targetRole = remoteMessage.data["target_role"]
        
        // Parse notification type
        val notificationType = try {
            NotificationHelper.NotificationType.valueOf(notificationTypeStr ?: "")
        } catch (e: Exception) {
            Log.w(TAG, "Unknown notification type: $notificationTypeStr")
            return
        }

        // Check if this notification is for the current user's role
        if (!isNotificationForCurrentUser(targetRole)) {
            Log.d(TAG, "Notification not for current user role: $targetRole")
            return
        }

        // Extract additional data
        val data = remoteMessage.data.filterKeys { 
            it !in listOf("title", "body", "notification_type", "target_role") 
        }

        // Show notification based on role
        showRoleBasedNotification(notificationType, title, body, data)
    }

    private fun saveTokenToFirebase(token: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val database = FirebaseDatabase.getInstance()
            val userTokenRef = database.getReference("user_tokens").child(currentUser.uid)
            
            userTokenRef.setValue(mapOf(
                "fcmToken" to token,
                "lastUpdated" to System.currentTimeMillis()
            )).addOnSuccessListener {
                Log.d(TAG, "FCM token saved successfully")
            }.addOnFailureListener { exception ->
                Log.e(TAG, "Failed to save FCM token", exception)
            }
        }
    }

    private fun isNotificationForCurrentUser(targetRole: String?): Boolean {
        // For now, we'll accept all notifications
        // In a real implementation, you'd check the current user's role
        // against the target role from Firebase Auth or local storage
        return true
    }

    private fun showRoleBasedNotification(
        type: NotificationHelper.NotificationType,
        title: String,
        body: String,
        data: Map<String, String>
    ) {
        when (type) {
            // Admin notifications
            NotificationHelper.NotificationType.LOW_STOCK,
            NotificationHelper.NotificationType.NEW_ORDER_ADMIN -> {
                NotificationHelper.notifyAdmin(this, type, title, body, data)
            }
            
            // Employee notifications
            NotificationHelper.NotificationType.NEW_ORDER_EMPLOYEE,
            NotificationHelper.NotificationType.CUSTOMER_CHAT -> {
                NotificationHelper.notifyEmployee(this, type, title, body, data)
            }
            
            // Customer notifications
            NotificationHelper.NotificationType.ORDER_STATUS_UPDATE,
            NotificationHelper.NotificationType.SUPPORT_CHAT,
            NotificationHelper.NotificationType.CART_HOLD_EXPIRY -> {
                NotificationHelper.notifyCustomer(this, type, title, body, data)
            }
        }
    }
}
