package com.laz.firebase

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase

object FirebaseInitializer {
    
    fun initialize(context: Context) {
        // Initialize Firebase if not already initialized
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        
        // Enable offline persistence for Realtime Database
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        
        // Set database URL for your specific project
        val database = FirebaseDatabase.getInstance("https://laz-41c78-default-rtdb.firebaseio.com/")
        
        // Enable logging in debug mode
        database.setLogLevel(com.google.firebase.database.Logger.Level.DEBUG)
    }
    
    fun getDatabaseInstance(): FirebaseDatabase {
        return FirebaseDatabase.getInstance("https://laz-41c78-default-rtdb.firebaseio.com/")
    }
}
