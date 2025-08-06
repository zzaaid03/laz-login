package com.laz.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

// In Python, this would be like:
// @dataclass
// class User:
//     id: int
//     username: str
//     password: str
//     role: UserRole

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "username")
    val username: String,
    
    @ColumnInfo(name = "password")
    val password: String,
    
    @ColumnInfo(name = "role")
    val role: UserRole,
    
    // Customer-specific fields (nullable for admin/employee users)
    @ColumnInfo(name = "email")
    val email: String? = null,
    
    @ColumnInfo(name = "phone_number")
    val phoneNumber: String? = null,
    
    @ColumnInfo(name = "address")
    val address: String? = null,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

enum class UserRole {
    ADMIN,
    CUSTOMER,
    EMPLOYEE
}
