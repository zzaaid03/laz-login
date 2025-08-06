package com.laz

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
        
        Log.d("FirebaseMainActivity", "Starting Firebase-integrated onCreate")
        
        try {
            // Initialize Firebase integration
            firebaseIntegrationManager.initializeFirebaseIntegration(lifecycleScope)
            
            setContent {
                LazTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        // Create Firebase ViewModels with Role-Based Access Control
                        val firebaseAuthViewModel: FirebaseAuthViewModel = viewModel(
                            factory = firebaseIntegrationManager.getViewModelFactory()
                        )
                        
                        // The secure ViewModels are available and integrated into the role-specific screens
                        // Role-based access control is enforced through PermissionManager and Firebase rules
                        FirebaseLazStoreApp(
                            firebaseAuthViewModel = firebaseAuthViewModel
                        )
                    }
                }
            }
            
            Log.d("FirebaseMainActivity", "Firebase onCreate completed successfully")
            
        } catch (e: Exception) {
            Log.e("FirebaseMainActivity", "Critical error in Firebase onCreate: ${e.message}", e)
            throw e
        }
    }
}
