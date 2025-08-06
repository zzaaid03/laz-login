package com.laz.repositories

import com.google.firebase.database.*
import com.laz.models.User
import com.laz.models.UserRole
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose

/**
 * Firebase User Repository
 * Manages user data in Firebase Realtime Database
 */
class FirebaseUserRepository {
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference
    private val usersRef: DatabaseReference = database.child("users")

    /**
     * Create or update user in Firebase
     */
    suspend fun createUser(user: User, firebaseUid: String): Result<User> {
        return try {
            val userMap = mapOf(
                "id" to user.id,
                "firebaseUid" to firebaseUid,
                "username" to user.username,
                "email" to user.email,
                "phoneNumber" to user.phoneNumber,
                "address" to user.address,
                "role" to user.role.name,
                "createdAt" to user.createdAt
            )
            
            usersRef.child(user.id.toString()).setValue(userMap).await()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get user by ID
     */
    suspend fun getUserById(userId: Long): Result<User?> {
        return try {
            val snapshot = usersRef.child(userId.toString()).get().await()
            val user = snapshot.toUser()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get user by Firebase UID
     * Uses indexed query for better performance
     */
    suspend fun getUserByFirebaseUid(firebaseUid: String): Result<User?> {
        return try {
            // Use indexed query for better performance
            val query = usersRef.orderByChild("firebaseUid").equalTo(firebaseUid)
            val snapshot = query.get().await()
            
            val user = snapshot.children.firstOrNull()?.toUser()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get user by email
     */
    suspend fun getUserByEmail(email: String): Result<User?> {
        return try {
            val query = usersRef.orderByChild("email").equalTo(email)
            val snapshot = query.get().await()
            
            val user = snapshot.children.firstOrNull()?.toUser()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update user information
     */
    suspend fun updateUser(user: User): Result<User> {
        return try {
            val updates = mapOf(
                "username" to user.username,
                "email" to user.email,
                "phoneNumber" to user.phoneNumber,
                "address" to user.address,
                "role" to user.role.name
            )
            
            usersRef.child(user.id.toString()).updateChildren(updates).await()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete user
     */
    suspend fun deleteUser(userId: Long): Result<Unit> {
        return try {
            usersRef.child(userId.toString()).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all users (for admin purposes)
     */
    suspend fun getAllUsers(): Result<List<User>> {
        return try {
            val snapshot = usersRef.get().await()
            val users = snapshot.children.mapNotNull { it.toUser() }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get users by role
     */
    suspend fun getUsersByRole(role: UserRole): Result<List<User>> {
        return try {
            val query = usersRef.orderByChild("role").equalTo(role.name)
            val snapshot = query.get().await()
            
            val users = snapshot.children.mapNotNull { it.toUser() }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observe user changes in real-time
     */
    fun observeUser(userId: Long): Flow<User?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.toUser()
                trySend(user)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        usersRef.child(userId.toString()).addValueEventListener(listener)
        awaitClose { usersRef.child(userId.toString()).removeEventListener(listener) }
    }

    /**
     * Generate next user ID
     */
    suspend fun getNextUserId(): Long {
        return try {
            val snapshot = usersRef.get().await()
            val maxId = snapshot.children.mapNotNull { 
                it.child("id").getValue(Long::class.java) 
            }.maxOrNull() ?: 0L
            maxId + 1
        } catch (e: Exception) {
            System.currentTimeMillis() // Fallback to timestamp
        }
    }

    /**
     * Extension function to convert DataSnapshot to User
     */
    private fun DataSnapshot.toUser(): User? {
        return try {
            val id = child("id").getValue(Long::class.java) ?: return null
            val username = child("username").getValue(String::class.java) ?: return null
            val email = child("email").getValue(String::class.java)
            val phoneNumber = child("phoneNumber").getValue(String::class.java)
            val address = child("address").getValue(String::class.java)
            val roleString = child("role").getValue(String::class.java) ?: "CUSTOMER"
            val createdAtTimestamp = child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()
            
            val role = try {
                UserRole.valueOf(roleString)
            } catch (e: Exception) {
                UserRole.CUSTOMER
            }

            User(
                id = id,
                username = username,
                password = "", // Password not stored in Firebase (handled by Auth)
                email = email,
                phoneNumber = phoneNumber,
                address = address,
                role = role,
                createdAt = createdAtTimestamp
            )
        } catch (e: Exception) {
            null
        }
    }
}
