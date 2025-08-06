package com.laz.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.laz.models.User
import com.laz.models.Product
import com.laz.models.Sale
import com.laz.models.Return
import com.laz.models.CartItem

@Database(
    entities = [User::class, Product::class, Sale::class, Return::class, CartItem::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LazDatabase : RoomDatabase() {
    
    abstract fun userDao(): UserDao
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun returnDao(): ReturnDao
    abstract fun cartDao(): CartDao
    
    companion object {
        @Volatile
        private var INSTANCE: LazDatabase? = null
        
        // Migration from version 1 to 2
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create the cart_items table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS cart_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId INTEGER NOT NULL,
                        productId INTEGER NOT NULL,
                        quantity INTEGER NOT NULL,
                        addedAt INTEGER NOT NULL,
                        FOREIGN KEY (userId) REFERENCES users(id) ON DELETE CASCADE,
                        FOREIGN KEY (productId) REFERENCES products(id) ON DELETE CASCADE
                    )
                    """
                )
            }
        }
        
        fun getDatabase(context: Context): LazDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LazDatabase::class.java,
                    "laz_store_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
