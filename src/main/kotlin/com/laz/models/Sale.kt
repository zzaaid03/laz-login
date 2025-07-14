package com.laz.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import java.time.LocalDateTime

// In Python, this would be like:
// @dataclass
// class Sale:
//     id: int
//     product_id: int
//     quantity: int
//     user_id: int
//     date: datetime

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "product_id")
    val productId: Long,
    
    @ColumnInfo(name = "product_name")
    val productName: String,
    
    @ColumnInfo(name = "product_price")
    val productPrice: String, // Store as String to avoid BigDecimal complexity
    
    @ColumnInfo(name = "quantity")
    val quantity: Int,
    
    @ColumnInfo(name = "user_id")
    val userId: Long,
    
    @ColumnInfo(name = "user_name")
    val userName: String,
    
    @ColumnInfo(name = "date")
    val date: String, // Store as String to avoid LocalDateTime complexity
    
    @ColumnInfo(name = "is_returned")
    val isReturned: Boolean = false
)
