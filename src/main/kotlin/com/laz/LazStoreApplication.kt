package com.laz

import android.app.Application
import com.laz.database.LazDatabase

class LazStoreApplication : Application() {
    
    val database by lazy {
        LazDatabase.getDatabase(this)
    }
    
    override fun onCreate() {
        super.onCreate()
    }
}
