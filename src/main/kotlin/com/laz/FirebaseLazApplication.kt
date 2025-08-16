package com.laz

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import com.laz.config.SecureConfig
import com.laz.services.DefaultUserService
import com.laz.services.FirebaseAuthService
import com.laz.repositories.FirebaseUserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Firebase LAZ Application
 * Initializes Firebase services and enables offline persistence
 */
class FirebaseLazApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        
        android.util.Log.d("FirebaseLazApp", "🚀 LAZ Application starting...")
        
        try {
            // Initialize Firebase
            FirebaseApp.initializeApp(this)
            android.util.Log.d("FirebaseLazApp", "✅ FirebaseApp initialized")
            
            // Initialize SecureConfig for API key management
            applicationScope.launch {
                try {
                    val success = SecureConfig.getInstance().initialize(this@FirebaseLazApplication)
                    if (success) {
                        Log.d("FirebaseLazApp", "✅ SecureConfig initialized successfully")
                    } else {
                        Log.e("FirebaseLazApp", "❌ Failed to initialize SecureConfig")
                    }
                } catch (e: Exception) {
                    Log.e("FirebaseLazApp", "❌ SecureConfig initialization error", e)
                }
            }
            
            // Test Firebase Database connection
            val database = FirebaseDatabase.getInstance()
            android.util.Log.d("FirebaseLazApp", "✅ Firebase Database instance: ${database.reference}")
            
            // Enable offline persistence for Realtime Database
            try {
                database.setPersistenceEnabled(true)
                android.util.Log.d("FirebaseLazApp", "✅ Firebase persistence enabled")
            } catch (e: Exception) {
                android.util.Log.w("FirebaseLazApp", "⚠️ Firebase persistence setup: ${e.message}")
            }
            
            android.util.Log.d("FirebaseLazApp", "✅ Firebase LAZ Application initialized successfully")
            
            // Create default admin and employee users
            createDefaultUsers()
            
        } catch (e: Exception) {
            android.util.Log.e("FirebaseLazApp", "❌ Critical error in Application onCreate: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Create default admin and employee users
     */
    private fun createDefaultUsers() {
        try {
            val firebaseAuthService = FirebaseAuthService()
            val firebaseUserRepository = FirebaseUserRepository()
            val defaultUserService = DefaultUserService(firebaseAuthService, firebaseUserRepository)
            
            defaultUserService.createDefaultUsers()
        } catch (e: Exception) {
            android.util.Log.e("FirebaseLazApp", "Error creating default users: ${e.message}", e)
        }
    }
}
