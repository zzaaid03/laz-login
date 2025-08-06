package com.laz.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laz.database.CartDao
import com.laz.database.ProductDao
import com.laz.models.CartItem
import com.laz.models.CartItemWithProduct
import com.laz.models.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CartViewModel(
    private val cartDao: CartDao,
    private val productDao: ProductDao
) : ViewModel() {

    private val _cartItems = MutableStateFlow<List<CartItemWithProduct>>(emptyList())
    val cartItems: StateFlow<List<CartItemWithProduct>> = _cartItems.asStateFlow()

    private val _cartItemCount = MutableStateFlow(0)
    val cartItemCount: StateFlow<Int> = _cartItemCount.asStateFlow()

    private val _cartTotal = MutableStateFlow(0.0)
    val cartTotal: StateFlow<Double> = _cartTotal.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Track checkout state
    private val _isCheckoutDialogVisible = MutableStateFlow(false)
    val isCheckoutDialogVisible: StateFlow<Boolean> = _isCheckoutDialogVisible.asStateFlow()

    fun loadCartItems(userId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                cartDao.getCartItemsWithProductsByUserId(userId).collectLatest { items ->
                    _cartItems.value = items
                }
                cartDao.getCartItemCount(userId).collectLatest { count ->
                    _cartItemCount.value = count
                }
                cartDao.getCartTotal(userId).collectLatest { total ->
                    _cartTotal.value = total ?: 0.0
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error loading cart: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun addToCart(userId: Long, product: Product, quantity: Int = 1) {
        try {
            // Check if product is already in cart
            val existingItem = cartDao.getCartItemByUserAndProductId(userId, product.id)
            
            if (existingItem != null) {
                // Update quantity if product already exists in cart
                val newQuantity = existingItem.quantity + quantity
                // Ensure we don't exceed available stock
                val finalQuantity = if (newQuantity <= product.quantity) newQuantity else product.quantity
                
                cartDao.updateCartItem(existingItem.copy(quantity = finalQuantity))
            } else {
                // Add new item to cart
                val finalQuantity = if (quantity <= product.quantity) quantity else product.quantity
                val cartItem = CartItem(
                    userId = userId,
                    productId = product.id,
                    quantity = finalQuantity
                )
                cartDao.insertCartItem(cartItem)
            }
        } catch (e: Exception) {
            _errorMessage.value = "Error adding to cart: ${e.message}"
        }
    }

    suspend fun updateCartItemQuantity(cartItem: CartItem, newQuantity: Int) {
        try {
            // Get the product to check stock
            val product = productDao.getProductById(cartItem.productId)
            
            if (product != null) {
                // Ensure we don't exceed available stock
                val finalQuantity = if (newQuantity <= product.quantity) newQuantity else product.quantity
                
                if (finalQuantity <= 0) {
                    // Remove item if quantity is 0 or less
                    cartDao.deleteCartItem(cartItem)
                } else {
                    // Update quantity
                    cartDao.updateCartItem(cartItem.copy(quantity = finalQuantity))
                }
            }
        } catch (e: Exception) {
            _errorMessage.value = "Error updating cart: ${e.message}"
        }
    }

    suspend fun removeFromCart(cartItem: CartItem) {
        try {
            cartDao.deleteCartItem(cartItem)
        } catch (e: Exception) {
            _errorMessage.value = "Error removing from cart: ${e.message}"
        }
    }

    suspend fun clearCart(userId: Long) {
        try {
            cartDao.clearCart(userId)
        } catch (e: Exception) {
            _errorMessage.value = "Error clearing cart: ${e.message}"
        }
    }

    fun showCheckoutDialog() {
        _isCheckoutDialogVisible.value = true
    }

    fun hideCheckoutDialog() {
        _isCheckoutDialogVisible.value = false
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
