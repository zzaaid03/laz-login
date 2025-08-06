package com.laz.models

/**
 * Firebase CartItem Model
 * Represents an item in a user's shopping cart
 */
data class CartItem(
    val id: Long = 0,
    val firebaseUid: String, // Firebase Authentication UID of the user
    val productId: Long,
    val quantity: Int,
    val addedAt: Long = System.currentTimeMillis()
)

/**
 * CartItem with Product details
 * Used for displaying cart items with full product information
 */
data class CartItemWithProduct(
    val cartItem: CartItem,
    val product: Product
)
