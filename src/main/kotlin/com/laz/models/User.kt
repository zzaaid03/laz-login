package com.laz.models

/**
 * Firebase User Model
 * Represents a user in the LAZ Store system with Firebase integration
 */
data class User(
    val id: Long = 0,
    val firebaseUid: String = "", // Firebase Authentication UID
    val username: String,
    val password: String = "", // Not stored in Firebase for security
    val role: UserRole,
    
    // Customer-specific fields (nullable for admin/employee users)
    val email: String? = null,
    val phoneNumber: String? = null,
    val address: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class UserRole {
    ADMIN,
    CUSTOMER,
    EMPLOYEE
}
