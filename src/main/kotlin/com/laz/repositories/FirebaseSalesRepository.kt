package com.laz.repositories

import com.google.firebase.database.*
import com.laz.models.Sale
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.text.SimpleDateFormat
import java.util.*

/**
 * Firebase Sales Repository
 * Manages sales data in Firebase Realtime Database
 */
class FirebaseSalesRepository {
    private val database = FirebaseDatabase.getInstance()
    private val salesRef = database.getReference("sales")
    
    /**
     * Create new sale
     */
    suspend fun createSale(sale: Sale): Result<Sale> {
        return try {
            val saleId = getNextSaleId()
            val saleWithId = sale.copy(id = saleId)
            
            val saleMap = mapOf(
                "id" to saleWithId.id,
                "userId" to saleWithId.userId,
                "productId" to saleWithId.productId,
                "productName" to saleWithId.productName,
                "productPrice" to saleWithId.productPrice,
                "quantity" to saleWithId.quantity,
                "userName" to saleWithId.userName,
                "date" to saleWithId.date,
                "isReturned" to saleWithId.isReturned,
                "createdAt" to System.currentTimeMillis()
            )
            
            salesRef.child(saleId.toString()).setValue(saleMap).await()
            Result.success(saleWithId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get sale by ID
     */
    suspend fun getSaleById(saleId: Long): Result<Sale?> {
        return try {
            val snapshot = salesRef.child(saleId.toString()).get().await()
            val sale = snapshot.toSale()
            Result.success(sale)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update existing sale
     */
    suspend fun updateSale(sale: Sale): Result<Sale> {
        return try {
            val updates = mapOf(
                "userId" to sale.userId,
                "productId" to sale.productId,
                "productName" to sale.productName,
                "productPrice" to sale.productPrice,
                "quantity" to sale.quantity,
                "userName" to sale.userName,
                "date" to sale.date,
                "isReturned" to sale.isReturned,
                "updatedAt" to System.currentTimeMillis()
            )
            
            salesRef.child(sale.id.toString()).updateChildren(updates).await()
            Result.success(sale)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete sale
     */
    suspend fun deleteSale(saleId: Long): Result<Unit> {
        return try {
            salesRef.child(saleId.toString()).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all sales as Flow for real-time updates
     */
    fun getAllSales(): Flow<List<Sale>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sales = snapshot.children.mapNotNull { it.toSale() }
                    .sortedByDescending { it.id }
                trySend(sales)
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        salesRef.addValueEventListener(listener)
        awaitClose { salesRef.removeEventListener(listener) }
    }

    /**
     * Get sales by user ID
     */
    suspend fun getSalesByUserId(userId: Long): Result<List<Sale>> {
        return try {
            val snapshot = salesRef.orderByChild("userId").equalTo(userId.toDouble()).get().await()
            val sales = snapshot.children.mapNotNull { it.toSale() }
                .sortedByDescending { it.id }
            Result.success(sales)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get sales by date range
     */
    suspend fun getSalesByDateRange(startDate: String, endDate: String): Result<List<Sale>> {
        return try {
            val snapshot = salesRef.get().await()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val startDateTime = dateFormat.parse(startDate)
            val endDateTime = dateFormat.parse(endDate)
            
            val sales = snapshot.children.mapNotNull { it.toSale() }
                .filter { sale ->
                    try {
                        val saleDate = dateFormat.parse(sale.date)
                        saleDate != null && saleDate.after(startDateTime) && saleDate.before(endDateTime)
                    } catch (e: Exception) {
                        false
                    }
                }
                .sortedByDescending { it.id }
            
            Result.success(sales)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get recent sales (last N days)
     */
    suspend fun getRecentSales(days: Int): Result<List<Sale>> {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val cutoffDate = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -days)
            }.time
            
            val snapshot = salesRef.get().await()
            val sales = snapshot.children.mapNotNull { it.toSale() }
                .filter { sale ->
                    try {
                        val saleDate = dateFormat.parse(sale.date)
                        saleDate != null && saleDate.after(cutoffDate)
                    } catch (e: Exception) {
                        false
                    }
                }
                .sortedByDescending { it.id }
            
            Result.success(sales)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get total sales amount
     */
    suspend fun getTotalSalesAmount(): Result<Double> {
        return try {
            val snapshot = salesRef.get().await()
            val totalAmount = snapshot.children.mapNotNull { it.toSale() }
                .filter { !it.isReturned }
                .sumOf { 
                    (it.productPrice.toDoubleOrNull() ?: 0.0) * it.quantity 
                }
            Result.success(totalAmount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get sales count
     */
    suspend fun getSalesCount(): Result<Int> {
        return try {
            val snapshot = salesRef.get().await()
            val count = snapshot.children.mapNotNull { it.toSale() }
                .filter { !it.isReturned }
                .size
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get next available sale ID
     */
    private suspend fun getNextSaleId(): Long {
        return try {
            val snapshot = salesRef.get().await()
            val maxId = snapshot.children.mapNotNull { 
                it.child("id").getValue(Long::class.java) 
            }.maxOrNull() ?: 0L
            maxId + 1
        } catch (e: Exception) {
            System.currentTimeMillis() // Fallback to timestamp
        }
    }

    /**
     * Extension function to convert DataSnapshot to Sale
     */
    private fun DataSnapshot.toSale(): Sale? {
        return try {
            Sale(
                id = child("id").getValue(Long::class.java) ?: 0L,
                userId = child("userId").getValue(Long::class.java) ?: 0L,
                productId = child("productId").getValue(Long::class.java) ?: 0L,
                productName = child("productName").getValue(String::class.java) ?: "",
                productPrice = child("productPrice").getValue(String::class.java) ?: "0.0",
                quantity = child("quantity").getValue(Int::class.java) ?: 0,
                userName = child("userName").getValue(String::class.java) ?: "",
                date = child("date").getValue(String::class.java) ?: "",
                isReturned = child("isReturned").getValue(Boolean::class.java) ?: false
            )
        } catch (e: Exception) {
            null
        }
    }
}
