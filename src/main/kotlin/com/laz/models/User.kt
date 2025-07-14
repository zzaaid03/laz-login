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
    val role: UserRole
)

enum class UserRole {
    ADMIN,
    CUSTOMER,
    EMPLOYEE
}
