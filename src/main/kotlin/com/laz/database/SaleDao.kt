package com.laz.database

import androidx.room.*
import com.laz.models.Sale
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales")
    fun getAllSalesFlow(): Flow<List<Sale>>
    
    @Query("SELECT * FROM sales ORDER BY date DESC")
    suspend fun getAllSales(): List<Sale>
    
    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun getSaleById(id: Long): Sale?
    
    @Query("SELECT * FROM sales WHERE user_id = :userId")
    suspend fun getSalesByUserId(userId: Long): List<Sale>
    
    @Query("SELECT * FROM sales WHERE product_id = :productId")
    suspend fun getSalesByProductId(productId: Long): List<Sale>
    
    @Query("SELECT * FROM sales WHERE is_returned = 0")
    suspend fun getNonReturnedSales(): List<Sale>
    
    @Query("SELECT * FROM sales WHERE is_returned = 1") 
    suspend fun getReturnedSales(): List<Sale>
    
    @Query("SELECT * FROM sales WHERE date LIKE :date || '%'")
    suspend fun getSalesByDate(date: String): List<Sale>
    
    @Insert
    suspend fun insertSale(sale: Sale): Long
    
    @Update
    suspend fun updateSale(sale: Sale)
    
    @Query("DELETE FROM sales WHERE id = :saleId")
    suspend fun deleteSale(saleId: Long)
    
    @Query("UPDATE sales SET is_returned = 1 WHERE id = :id")
    suspend fun markSaleAsReturned(id: Long)
    
    @Query("SELECT COUNT(*) FROM sales WHERE is_returned = 0")
    suspend fun getNonReturnedSalesCount(): Int
    
    @Query("SELECT COUNT(*) FROM sales WHERE is_returned = 0 AND date LIKE :date || '%'")
    suspend fun getTodaySalesCount(date: String): Int
    
    @Query("SELECT SUM(CAST(product_price AS REAL) * quantity) FROM sales WHERE is_returned = 0 AND date LIKE :date || '%'")
    suspend fun getTodaySalesTotal(date: String): Double?
    
    @Query("SELECT COUNT(*) FROM sales WHERE date >= :fromDate AND is_returned = 0")
    suspend fun getSalesCountFromDate(fromDate: String): Long
}
