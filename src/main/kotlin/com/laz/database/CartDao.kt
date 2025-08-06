package com.laz.database

import androidx.room.*
import com.laz.models.CartItem
import com.laz.models.CartItemWithProduct
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(cartItem: CartItem): Long

    @Update
    suspend fun updateCartItem(cartItem: CartItem)

    @Delete
    suspend fun deleteCartItem(cartItem: CartItem)

    @Query("DELETE FROM cart_items WHERE userId = :userId")
    suspend fun clearCart(userId: Long)

    @Query("SELECT * FROM cart_items WHERE userId = :userId")
    fun getCartItemsByUserId(userId: Long): Flow<List<CartItem>>

    @Query("SELECT * FROM cart_items WHERE userId = :userId AND productId = :productId LIMIT 1")
    suspend fun getCartItemByUserAndProductId(userId: Long, productId: Long): CartItem?

    @Transaction
    @Query("SELECT * FROM cart_items WHERE userId = :userId")
    fun getCartItemsWithProductsByUserId(userId: Long): Flow<List<CartItemWithProduct>>

    @Query("SELECT COUNT(*) FROM cart_items WHERE userId = :userId")
    fun getCartItemCount(userId: Long): Flow<Int>

    @Query("SELECT SUM(products.price * cart_items.quantity) FROM cart_items INNER JOIN products ON cart_items.productId = products.id WHERE cart_items.userId = :userId")
    fun getCartTotal(userId: Long): Flow<Double?>
}
