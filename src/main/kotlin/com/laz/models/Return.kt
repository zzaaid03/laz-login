package com.laz.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

// In Python, this would be like:
// @dataclass
// class Return:
//     id: int
//     sale_id: int
//     reason: str
//     date: datetime

@Entity(tableName = "returns")
data class Return(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "sale_id")
    val saleId: Long,
    
    @ColumnInfo(name = "reason")
    val reason: String,
    
    @ColumnInfo(name = "date")
    val date: String // Store as String to avoid LocalDateTime complexity
)
