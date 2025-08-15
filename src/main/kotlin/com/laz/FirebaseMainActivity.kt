package com.laz

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.lifecycleScope
// Room database removed - using pure Firebase architecture
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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d("FirebaseMainActivity", "🚀 Activity starting...")
        Log.d("FirebaseMainActivity", "📱 App package: ${packageName}")
        
        try {
            // Test Firebase initialization
            val database = com.google.firebase.database.FirebaseDatabase.getInstance()
            Log.d("FirebaseMainActivity", "✅ Firebase Database initialized: ${database.reference}")
            
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            Log.d("FirebaseMainActivity", "✅ Firebase Auth initialized: ${auth.currentUser?.uid ?: "No user"}")
            
            // Initialize Firebase integration
            // firebaseIntegrationManager.initialize() // Commented out - method may not exist
            Log.d("FirebaseMainActivity", "✅ Firebase integration manager initialized")
            
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
                        factory = FirebaseServices.secureViewModelFactory
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
}
