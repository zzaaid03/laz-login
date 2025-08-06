package com.laz.repositories

import com.google.firebase.database.*
import com.laz.models.CartItem
import com.laz.models.CartItemWithProduct
import com.laz.models.Product
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import java.math.BigDecimal

/**
 * Firebase Cart Repository
 * Manages cart data in Firebase Realtime Database with real-time updates
 */
class FirebaseCartRepository {
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference
    private val cartRef: DatabaseReference = database.child("cart")
    private val productsRef: DatabaseReference = database.child("products")

    /**
     * Add item to cart
     */
    suspend fun addCartItem(cartItem: CartItem): Result<CartItem> {
        return try {
            val cartItemId = "${cartItem.userId}_${cartItem.productId}"
            val cartItemMap = mapOf(
                "userId" to cartItem.userId,
                "productId" to cartItem.productId,
                "quantity" to cartItem.quantity,
                "addedAt" to System.currentTimeMillis()
            )
            
            cartRef.child(cartItemId).setValue(cartItemMap).await()
            Result.success(cartItem)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update cart item quantity
     */
    suspend fun updateCartItem(cartItem: CartItem): Result<CartItem> {
        return try {
            val cartItemId = "${cartItem.userId}_${cartItem.productId}"
            val updates = mapOf(
                "quantity" to cartItem.quantity,
                "updatedAt" to System.currentTimeMillis()
            )
            
            cartRef.child(cartItemId).updateChildren(updates).await()
            Result.success(cartItem)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Remove item from cart
     */
    suspend fun removeCartItem(userId: Long, productId: Long): Result<Unit> {
        return try {
            val cartItemId = "${userId}_${productId}"
            cartRef.child(cartItemId).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get cart item by user and product ID
     */
    suspend fun getCartItem(userId: Long, productId: Long): Result<CartItem?> {
        return try {
            val cartItemId = "${userId}_${productId}"
            val snapshot = cartRef.child(cartItemId).get().await()
            val cartItem = snapshot.toCartItem()
            Result.success(cartItem)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all cart items for a user
     */
    suspend fun getCartItemsByUserId(userId: Long): Result<List<CartItem>> {
        return try {
            val query = cartRef.orderByChild("userId").equalTo(userId.toDouble())
            val snapshot = query.get().await()
            val cartItems = snapshot.children.mapNotNull { it.toCartItem() }
            Result.success(cartItems)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get cart items with product details for a user
     */
    suspend fun getCartItemsWithProductsByUserId(userId: Long): Result<List<CartItemWithProduct>> {
        return try {
            val cartItems = getCartItemsByUserId(userId).getOrThrow()
            val cartItemsWithProducts = mutableListOf<CartItemWithProduct>()
            
            for (cartItem in cartItems) {
                val productSnapshot = productsRef.child(cartItem.productId.toString()).get().await()
                val product = productSnapshot.toProduct()
                
                if (product != null) {
                    cartItemsWithProducts.add(
                        CartItemWithProduct(
                            cartItem = cartItem,
                            product = product
                        )
                    )
                }
            }
            
            Result.success(cartItemsWithProducts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get cart item count for a user
     */
    suspend fun getCartItemCount(userId: Long): Result<Int> {
        return try {
            val cartItems = getCartItemsByUserId(userId).getOrThrow()
            val totalCount = cartItems.sumOf { it.quantity }
            Result.success(totalCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get cart total for a user
     */
    suspend fun getCartTotal(userId: Long): Result<Double> {
        return try {
            val cartItemsWithProducts = getCartItemsWithProductsByUserId(userId).getOrThrow()
            val total = cartItemsWithProducts.sumOf { 
                it.product.price.multiply(BigDecimal(it.cartItem.quantity)).toDouble()
            }
            Result.success(total)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Clear all cart items for a user
     */
    suspend fun clearCart(userId: Long): Result<Unit> {
        return try {
            val cartItems = getCartItemsByUserId(userId).getOrThrow()
            cartItems.forEach { cartItem ->
                removeCartItem(userId, cartItem.productId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observe cart items for a user in real-time
     */
    fun observeCartItemsByUserId(userId: Long): Flow<List<CartItem>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val cartItems = snapshot.children.mapNotNull { it.toCartItem() }
                    .filter { it.userId == userId }
                trySend(cartItems)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        cartRef.addValueEventListener(listener)
        awaitClose { cartRef.removeEventListener(listener) }
    }

    /**
     * Observe cart items with products for a user in real-time
     */
    fun observeCartItemsWithProductsByUserId(userId: Long): Flow<List<CartItemWithProduct>> = callbackFlow {
        val cartListener = object : ValueEventListener {
            override fun onDataChange(cartSnapshot: DataSnapshot) {
                val cartItems = cartSnapshot.children.mapNotNull { it.toCartItem() }
                    .filter { it.userId == userId }
                
                // Listen for product changes
                val productListener = object : ValueEventListener {
                    override fun onDataChange(productSnapshot: DataSnapshot) {
                        val cartItemsWithProducts = mutableListOf<CartItemWithProduct>()
                        
                        for (cartItem in cartItems) {
                            val product = productSnapshot.child(cartItem.productId.toString()).toProduct()
                            if (product != null) {
                                cartItemsWithProducts.add(
                                    CartItemWithProduct(
                                        cartItem = cartItem,
                                        product = product
                                    )
                                )
                            }
                        }
                        
                        trySend(cartItemsWithProducts)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        close(error.toException())
                    }
                }
                
                productsRef.addValueEventListener(productListener)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        cartRef.addValueEventListener(cartListener)
        awaitClose { cartRef.removeEventListener(cartListener) }
    }

    /**
     * Observe cart item count for a user in real-time
     */
    fun observeCartItemCount(userId: Long): Flow<Int> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val cartItems = snapshot.children.mapNotNull { it.toCartItem() }
                    .filter { it.userId == userId }
                val totalCount = cartItems.sumOf { it.quantity }
                trySend(totalCount)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        cartRef.addValueEventListener(listener)
        awaitClose { cartRef.removeEventListener(listener) }
    }

    /**
     * Observe cart total for a user in real-time
     */
    fun observeCartTotal(userId: Long): Flow<Double> = callbackFlow {
        val cartListener = object : ValueEventListener {
            override fun onDataChange(cartSnapshot: DataSnapshot) {
                val cartItems = cartSnapshot.children.mapNotNull { it.toCartItem() }
                    .filter { it.userId == userId }
                
                val productListener = object : ValueEventListener {
                    override fun onDataChange(productSnapshot: DataSnapshot) {
                        var total = 0.0
                        
                        for (cartItem in cartItems) {
                            val product = productSnapshot.child(cartItem.productId.toString()).toProduct()
                            if (product != null) {
                                total += product.price.multiply(BigDecimal(cartItem.quantity)).toDouble()
                            }
                        }
                        
                        trySend(total)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        close(error.toException())
                    }
                }
                
                productsRef.addValueEventListener(productListener)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        cartRef.addValueEventListener(cartListener)
        awaitClose { cartRef.removeEventListener(cartListener) }
    }

    /**
     * Extension function to convert DataSnapshot to CartItem
     */
    private fun DataSnapshot.toCartItem(): CartItem? {
        return try {
            val userId = child("userId").getValue(Long::class.java) ?: return null
            val productId = child("productId").getValue(Long::class.java) ?: return null
            val quantity = child("quantity").getValue(Int::class.java) ?: return null

            CartItem(
                userId = userId,
                productId = productId,
                quantity = quantity
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Extension function to convert DataSnapshot to Product
     */
    private fun DataSnapshot.toProduct(): Product? {
        return try {
            val id = child("id").getValue(Long::class.java) ?: return null
            val name = child("name").getValue(String::class.java) ?: return null
            val priceString = child("price").getValue(String::class.java) ?: return null
            val quantity = child("quantity").getValue(Int::class.java) ?: 0
            val costString = child("cost").getValue(String::class.java) ?: "0.00"
            val shelfLocation = child("shelfLocation").getValue(String::class.java)

            Product(
                id = id,
                name = name,
                quantity = quantity,
                cost = BigDecimal(costString),
                price = BigDecimal(priceString),
                shelfLocation = shelfLocation
            )
        } catch (e: Exception) {
            null
        }
    }
}
