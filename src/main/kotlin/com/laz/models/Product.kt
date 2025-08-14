package com.laz.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import java.math.BigDecimal

// In Python, this would be like:
// @dataclass
// class Product:
//     id: int
//     name: str
//     quantity: int
//     cost: Decimal
//     price: Decimal
//     shelf_location: str

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "quantity")
    val quantity: Int,
    
    @ColumnInfo(name = "cost")
    val cost: BigDecimal,
    
    @ColumnInfo(name = "price")
    val price: BigDecimal,
    
    @ColumnInfo(name = "shelf_location")
    val shelfLocation: String? = null,
    
    @ColumnInfo(name = "image_url")
    val imageUrl: String? = null
)
