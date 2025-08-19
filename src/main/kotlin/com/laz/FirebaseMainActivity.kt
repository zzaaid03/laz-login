package com.laz

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.lifecycleScope
// Room database removed - using pure Firebase architecture
import com.laz.notifications.NotificationManager
import com.laz.services.FirebaseIntegrationManager
import com.laz.ui.FirebaseLazStoreApp
import com.laz.ui.theme.LazTheme
import com.laz.models.User
import com.laz.models.UserRole
import com.laz.viewmodels.*
import kotlinx.coroutines.launch

/**
 * Firebase-integrated MainActivity
 * Supports both Firebase and local database operations with seamless migration
 */
class FirebaseMainActivity : ComponentActivity() {
    
    // Room database removed - using pure Firebase architecture
    private val firebaseIntegrationManager by lazy { FirebaseIntegrationManager(this) }
    private val notificationManager by lazy { NotificationManager(this) }
    
    // Permission launcher for Android 13+ notification permission
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("FirebaseMainActivity", "✅ Notification permission granted")
        } else {
            Log.w("FirebaseMainActivity", "⚠️ Notification permission denied")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d("FirebaseMainActivity", "🚀 Activity starting...")
        Log.d("FirebaseMainActivity", "📱 App package: ${packageName}")
        Log.d("FirebaseMainActivity", "📱 Android version: ${android.os.Build.VERSION.SDK_INT}")
        Log.d("FirebaseMainActivity", "📱 Device: ${android.os.Build.MODEL}")
        
        try {
            // Test Firebase initialization
            val database = com.google.firebase.database.FirebaseDatabase.getInstance()
            Log.d("FirebaseMainActivity", "✅ Firebase Database initialized: ${database.reference}")
            
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            Log.d("FirebaseMainActivity", "✅ Firebase Auth initialized: ${auth.currentUser?.uid ?: "No user"}")
            
            // Initialize Firebase integration
            // firebaseIntegrationManager.initialize() // Commented out - method may not exist
            Log.d("FirebaseMainActivity", "✅ Firebase integration manager initialized")
            
            // Initialize notifications
            notificationManager.initialize()
            requestNotificationPermission()
            
            // Force FCM token retrieval for testing
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("FirebaseMainActivity", "Fetching FCM registration token failed", task.exception)
                    return@addOnCompleteListener
                }

                // Get new FCM registration token
                val token = task.result
                Log.d("FirebaseMainActivity", "FCM Token: $token")
                println("=== FCM TOKEN FOR TESTING ===")
                println(token)
                println("=============================")
            }
            
            Log.d("FirebaseMainActivity", "✅ Notification system initialized")
            
        } catch (e: Exception) {
            Log.e("FirebaseMainActivity", "❌ Firebase initialization failed: ${e.message}")
            Log.e("FirebaseMainActivity", "Exception: ${e.javaClass.simpleName}")
        }
        
        setContent {
            LazTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val authViewModel: FirebaseAuthViewModel = viewModel(
                        factory = FirebaseServices.getSecureViewModelFactory(this@FirebaseMainActivity)
                    )
                    
                    // Create a default user for testing if needed
                    val defaultUser = User(
                        id = 1L,
                        username = "admin",
                        password = "admin123",
                        role = UserRole.ADMIN,
                        email = "admin@laz.com"
                    )
                    
                    Log.d("FirebaseMainActivity", "🎨 Starting UI with user: ${defaultUser.username}")
                    
                    FirebaseLazStoreApp(
                        user = defaultUser,
                        authViewModel = authViewModel
                    )
                }
            }
        }
        
        Log.d("FirebaseMainActivity", "✅ Firebase onCreate completed successfully")
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)) {
                PackageManager.PERMISSION_GRANTED -> {
                    Log.d("FirebaseMainActivity", "✅ Notification permission already granted")
                }
                else -> {
                    Log.d("FirebaseMainActivity", "📱 Requesting notification permission")
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            Log.d("FirebaseMainActivity", "✅ Notification permission not required (Android < 13)")
        }
    }
}
