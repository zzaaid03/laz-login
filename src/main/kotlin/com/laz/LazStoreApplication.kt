package com.laz

import android.app.Application
// LazDatabase removed - using pure Firebase architecture

class LazStoreApplication : Application() {
    
    val database by lazy {
        // Database removed - using pure Firebase architecture
    }
    
    override fun onCreate() {
        super.onCreate()
    }
}
