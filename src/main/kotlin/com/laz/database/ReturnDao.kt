package com.laz.database

import androidx.room.*
import com.laz.models.Return
import kotlinx.coroutines.flow.Flow

@Dao
interface ReturnDao {
    @Query("SELECT * FROM returns")
    fun getAllReturns(): Flow<List<Return>>
    
    @Query("SELECT * FROM returns WHERE id = :id")
    suspend fun getReturnById(id: Long): Return?
    
    @Query("SELECT * FROM returns WHERE sale_id = :saleId")
    suspend fun getReturnsBySaleId(saleId: Long): List<Return>
    
    @Query("SELECT COUNT(*) FROM returns WHERE date >= :fromDate")
    suspend fun getReturnCountFromDate(fromDate: String): Long
    
    @Insert
    suspend fun insertReturn(returnItem: Return): Long
    
    @Update
    suspend fun updateReturn(returnItem: Return)
    
    @Delete
    suspend fun deleteReturn(returnItem: Return)
    
    @Query("DELETE FROM returns WHERE id = :id")
    suspend fun deleteReturnById(id: Long)
}
