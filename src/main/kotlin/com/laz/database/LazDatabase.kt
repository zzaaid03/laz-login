package com.laz.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.laz.models.User
import com.laz.models.Product
import com.laz.models.Sale
import com.laz.models.Return

@Database(
    entities = [User::class, Product::class, Sale::class, Return::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LazDatabase : RoomDatabase() {
    
    abstract fun userDao(): UserDao
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun returnDao(): ReturnDao
    
    companion object {
        @Volatile
        private var INSTANCE: LazDatabase? = null
        
        fun getDatabase(context: Context): LazDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LazDatabase::class.java,
                    "laz_store_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
