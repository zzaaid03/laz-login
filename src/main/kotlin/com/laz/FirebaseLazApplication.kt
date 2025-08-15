package com.laz

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import com.laz.services.DefaultUserService
import com.laz.services.FirebaseAuthService
import com.laz.repositories.FirebaseUserRepository

/**
 * Firebase LAZ Application
 * Initializes Firebase services and enables offline persistence
 */
class FirebaseLazApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        android.util.Log.d("FirebaseLazApp", "🚀 LAZ Application starting...")
        
        try {
            // Initialize Firebase
            FirebaseApp.initializeApp(this)
            android.util.Log.d("FirebaseLazApp", "✅ FirebaseApp initialized")
            
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
