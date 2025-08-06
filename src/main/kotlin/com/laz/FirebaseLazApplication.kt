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
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Enable offline persistence for Realtime Database
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        } catch (e: Exception) {
            // Persistence is already enabled or failed to enable
            android.util.Log.w("FirebaseLazApp", "Firebase persistence setup: ${e.message}")
        }
        
        android.util.Log.d("FirebaseLazApp", "Firebase LAZ Application initialized successfully")
        
        // Create default admin and employee users
        createDefaultUsers()
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
