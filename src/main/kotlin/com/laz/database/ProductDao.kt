package com.laz.database

import androidx.room.*
import com.laz.models.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products")
    fun getAllProducts(): Flow<List<Product>>
    
    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Long): Product?
    
    @Query("SELECT * FROM products WHERE name LIKE '%' || :name || '%'")
    suspend fun searchProductsByName(name: String): List<Product>
    
    @Query("SELECT * FROM products WHERE quantity <= :threshold")
    suspend fun getLowStockProducts(threshold: Int = 5): List<Product>
    
    @Insert
    suspend fun insertProduct(product: Product): Long
    
    @Update
    suspend fun updateProduct(product: Product)
    
    @Delete
    suspend fun deleteProduct(product: Product)
    
    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: Long)
    
    @Query("UPDATE products SET quantity = quantity - :quantity WHERE id = :id")
    suspend fun decreaseQuantity(id: Long, quantity: Int)
    
    @Query("UPDATE products SET quantity = quantity + :quantity WHERE id = :id")
    suspend fun increaseQuantity(id: Long, quantity: Int)
    
    @Query("SELECT COUNT(*) FROM products")
    suspend fun getProductCount(): Int
}
