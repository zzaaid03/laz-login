package com.laz.models

import java.math.BigDecimal

/**
 * Firebase Product Model
 * Represents a product in the LAZ Store inventory system
 */
data class Product(
    val id: Long = 0,
    val name: String,
    val quantity: Int,
    val cost: BigDecimal,
    val price: BigDecimal,
    val shelfLocation: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
