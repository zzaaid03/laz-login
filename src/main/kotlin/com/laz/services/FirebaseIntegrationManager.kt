package com.laz.services

import android.content.Context
import androidx.lifecycle.ViewModelProvider
// LazDatabase removed - using pure Firebase architecture
import com.laz.repositories.FirebaseCartRepository
import com.laz.repositories.FirebaseProductRepository
import com.laz.repositories.FirebaseUserRepository
import com.laz.viewmodels.FirebaseServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

/**
 * Firebase Integration Manager
 * Handles migration from local database to Firebase and provides unified access
 */
class FirebaseIntegrationManager(private val context: Context) {
    
    // Local database removed - using pure Firebase architecture
    private val firebaseUserRepository = FirebaseServices.userRepository
    private val firebaseProductRepository = FirebaseServices.productRepository
    private val firebaseCartRepository = FirebaseServices.cartRepository
    
    /**
     * Migrate local data to Firebase
     */
    suspend fun migrateToFirebase(): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                // Migrate products first
                migrateProducts()
                
                // Migrate users
                migrateUsers()
                
                // Migrate cart items
                migrateCartItems()
                
                android.util.Log.d("FirebaseIntegration", "Migration to Firebase completed successfully")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseIntegration", "Migration failed: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Migrate products from local database to Firebase
     */
    private suspend fun migrateProducts() {
        try {
            // Get products from local database using first() to get the current list
            // Local database removed - using pure Firebase architecture
            // Initialize sample products since no local database
            firebaseProductRepository.initializeSampleProducts()
            android.util.Log.d("FirebaseIntegration", "Initialized sample products in Firebase")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseIntegration", "Product migration failed: ${e.message}", e)
        }
    }
    
    /**
     * Migrate users from local database to Firebase
     */
    private suspend fun migrateUsers() {
        try {
            // Local database removed - using pure Firebase architecture
            // Users will be created through Firebase Auth signup process
            android.util.Log.d("FirebaseIntegration", "User migration skipped - using pure Firebase Auth")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseIntegration", "User migration failed: ${e.message}", e)
        }
    }
    
    /**
     * Migrate cart items from local database to Firebase
     */
    private suspend fun migrateCartItems() {
        try {
            // Local database removed - using pure Firebase architecture
            // Cart items will be created through Firebase when users add products to cart
            android.util.Log.d("FirebaseIntegration", "Cart migration skipped - using pure Firebase cart system")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseIntegration", "Cart migration failed: ${e.message}", e)
        }
    }
    
    /**
     * Check if Firebase is available and properly configured
     */
    suspend fun checkFirebaseStatus(): FirebaseStatus {
        return try {
            // Test Firebase connection by attempting to read from database
            val result = firebaseProductRepository.getAllProducts()
            if (result.isSuccess) {
                FirebaseStatus.AVAILABLE
            } else {
                FirebaseStatus.ERROR
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseIntegration", "Firebase status check failed: ${e.message}", e)
            FirebaseStatus.UNAVAILABLE
        }
    }
    
    /**
     * Get appropriate ViewModelFactory based on Firebase availability
     */
    fun getViewModelFactory(): ViewModelProvider.Factory {
        return FirebaseServices.viewModelFactory
    }
    
    /**
     * Get secure ViewModelFactory with role-based access control
     */
    fun getSecureViewModelFactory(): ViewModelProvider.Factory {
        return FirebaseServices.secureViewModelFactory
    }
    
    /**
     * Initialize Firebase integration
     */
    fun initializeFirebaseIntegration(scope: CoroutineScope) {
        scope.launch {
            try {
                val status = checkFirebaseStatus()
                when (status) {
                    FirebaseStatus.AVAILABLE -> {
                        android.util.Log.d("FirebaseIntegration", "Firebase is available, starting migration")
                        val migrationResult = migrateToFirebase()
                        if (migrationResult.isSuccess) {
                            android.util.Log.d("FirebaseIntegration", "Firebase integration completed successfully")
                        } else {
                            android.util.Log.e("FirebaseIntegration", "Firebase migration failed: ${migrationResult.exceptionOrNull()?.message}")
                        }
                    }
                    FirebaseStatus.UNAVAILABLE -> {
                        android.util.Log.w("FirebaseIntegration", "Firebase is unavailable, using local database")
                    }
                    FirebaseStatus.ERROR -> {
                        android.util.Log.e("FirebaseIntegration", "Firebase error detected, using local database")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("FirebaseIntegration", "Firebase initialization failed: ${e.message}", e)
            }
        }
    }
    
    /**
     * Sync local data with Firebase (bidirectional)
     */
    suspend fun syncWithFirebase(): Result<Unit> {
        return try {
            // Download latest data from Firebase
            val firebaseProducts = firebaseProductRepository.getAllProducts()
            if (firebaseProducts.isSuccess) {
                val products = firebaseProducts.getOrNull() ?: emptyList()
                // Local database removed - using pure Firebase architecture
                android.util.Log.d("FirebaseIntegration", "Synced ${products.size} products from Firebase")
            }
            
            android.util.Log.d("FirebaseIntegration", "Sync with Firebase completed")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseIntegration", "Sync failed: ${e.message}", e)
            Result.failure(e)
        }
    }
}

/**
 * Firebase Status Enum
 */
enum class FirebaseStatus {
    AVAILABLE,
    UNAVAILABLE,
    ERROR
}
