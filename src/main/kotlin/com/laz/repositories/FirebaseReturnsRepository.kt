package com.laz.repositories

import com.google.firebase.database.*
import com.laz.models.Return
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.text.SimpleDateFormat
import java.util.*

/**
 * Firebase Returns Repository
 * Manages returns data in Firebase Realtime Database
 */
class FirebaseReturnsRepository {
    private val database = FirebaseDatabase.getInstance()
    private val returnsRef = database.getReference("returns")
    
    /**
     * Create new return
     */
    suspend fun createReturn(returnItem: Return): Result<Return> {
        return try {
            val returnId = getNextReturnId()
            val returnWithId = returnItem.copy(id = returnId)
            
            val returnMap = mapOf(
                "id" to returnWithId.id,
                "saleId" to returnWithId.saleId,
                "reason" to returnWithId.reason,
                "date" to returnWithId.date,
                "createdAt" to System.currentTimeMillis()
            )
            
            returnsRef.child(returnId.toString()).setValue(returnMap).await()
            Result.success(returnWithId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get return by ID
     */
    suspend fun getReturnById(returnId: Long): Result<Return?> {
        return try {
            val snapshot = returnsRef.child(returnId.toString()).get().await()
            val returnItem = snapshot.toReturn()
            Result.success(returnItem)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update existing return
     */
    suspend fun updateReturn(returnItem: Return): Result<Return> {
        return try {
            val updates = mapOf(
                "saleId" to returnItem.saleId,
                "reason" to returnItem.reason,
                "date" to returnItem.date,
                "updatedAt" to System.currentTimeMillis()
            )
            
            returnsRef.child(returnItem.id.toString()).updateChildren(updates).await()
            Result.success(returnItem)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete return
     */
    suspend fun deleteReturn(returnId: Long): Result<Unit> {
        return try {
            returnsRef.child(returnId.toString()).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all returns as Flow for real-time updates
     */
    fun getAllReturns(): Flow<List<Return>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val returns = snapshot.children.mapNotNull { it.toReturn() }
                    .sortedByDescending { it.id }
                trySend(returns)
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        returnsRef.addValueEventListener(listener)
        awaitClose { returnsRef.removeEventListener(listener) }
    }

    /**
     * Get returns by user ID (Note: Return model doesn't have userId field)
     * This method is kept for interface compatibility but returns all returns
     */
    suspend fun getReturnsByUserId(userId: Long): Result<List<Return>> {
        return try {
            // Since Return model doesn't have userId, return all returns
            val snapshot = returnsRef.get().await()
            val returns = snapshot.children.mapNotNull { it.toReturn() }
                .sortedByDescending { it.id }
            Result.success(returns)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get returns by sale ID
     */
    suspend fun getReturnsBySaleId(saleId: Long): Result<List<Return>> {
        return try {
            val snapshot = returnsRef.orderByChild("saleId").equalTo(saleId.toDouble()).get().await()
            val returns = snapshot.children.mapNotNull { it.toReturn() }
                .sortedByDescending { it.id }
            Result.success(returns)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get returns by date range
     */
    suspend fun getReturnsByDateRange(startDate: String, endDate: String): Result<List<Return>> {
        return try {
            val snapshot = returnsRef.get().await()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val startDateTime = dateFormat.parse(startDate)
            val endDateTime = dateFormat.parse(endDate)
            
            val returns = snapshot.children.mapNotNull { it.toReturn() }
                .filter { returnItem ->
                    try {
                        val returnDate = dateFormat.parse(returnItem.date)
                        returnDate != null && returnDate.after(startDateTime) && returnDate.before(endDateTime)
                    } catch (e: Exception) {
                        false
                    }
                }
                .sortedByDescending { it.id }
            
            Result.success(returns)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get recent returns (last N days)
     */
    suspend fun getRecentReturns(days: Int): Result<List<Return>> {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val cutoffDate = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -days)
            }.time
            
            val snapshot = returnsRef.get().await()
            val returns = snapshot.children.mapNotNull { it.toReturn() }
                .filter { returnItem ->
                    try {
                        val returnDate = dateFormat.parse(returnItem.date)
                        returnDate != null && returnDate.after(cutoffDate)
                    } catch (e: Exception) {
                        false
                    }
                }
                .sortedByDescending { it.id }
            
            Result.success(returns)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get total returns amount (Note: Return model doesn't store price/quantity, returns count only)
     */
    suspend fun getTotalReturnsAmount(): Result<Double> {
        return try {
            // Since Return model doesn't have price/quantity fields,
            // we can only return the count as a double for now
            val snapshot = returnsRef.get().await()
            val count = snapshot.children.count().toDouble()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get returns count
     */
    suspend fun getReturnsCount(): Result<Int> {
        return try {
            val snapshot = returnsRef.get().await()
            val count = snapshot.children.count()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get next available return ID
     */
    private suspend fun getNextReturnId(): Long {
        return try {
            val snapshot = returnsRef.get().await()
            val maxId = snapshot.children.mapNotNull { 
                it.child("id").getValue(Long::class.java) 
            }.maxOrNull() ?: 0L
            maxId + 1
        } catch (e: Exception) {
            System.currentTimeMillis() // Fallback to timestamp
        }
    }

    /**
     * Extension function to convert DataSnapshot to Return
     */
    private fun DataSnapshot.toReturn(): Return? {
        return try {
            Return(
                id = child("id").getValue(Long::class.java) ?: 0L,
                saleId = child("saleId").getValue(Long::class.java) ?: 0L,
                reason = child("reason").getValue(String::class.java) ?: "",
                date = child("date").getValue(String::class.java) ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }
}
