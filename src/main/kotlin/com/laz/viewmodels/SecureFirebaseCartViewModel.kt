package com.laz.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laz.models.CartItem
import com.laz.models.User
import com.laz.repositories.FirebaseCartRepository
import com.laz.repositories.FirebaseProductRepository
import com.laz.security.PermissionManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal

/**
 * Secure Firebase Cart ViewModel
 * Enforces customer-only permissions for cart operations
 */
class SecureFirebaseCartViewModel(
    private val cartRepository: FirebaseCartRepository,
    private val productRepository: FirebaseProductRepository,
    private val currentUser: StateFlow<User?>
) : ViewModel() {

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _permissionError = MutableStateFlow<String?>(null)
    val permissionError: StateFlow<String?> = _permissionError.asStateFlow()

    private val _operationSuccess = MutableStateFlow<String?>(null)
    val operationSuccess: StateFlow<String?> = _operationSuccess.asStateFlow()

    // Computed properties
    val cartItemCount: StateFlow<Int> = cartItems.map { items ->
        items.sumOf { it.quantity }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val cartTotal: StateFlow<BigDecimal> = cartItems.map { items ->
        // Note: We'll need to get product prices from the product repository
        // For now, return zero until we implement proper cart total calculation
        BigDecimal.ZERO
    }.stateIn(viewModelScope, SharingStarted.Lazily, BigDecimal.ZERO)

    init {
        // Set up real-time cart listener when user is available
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null && (PermissionManager.canUseShoppingCart(user) || PermissionManager.isAdmin(user))) {
                    println("DEBUG: Setting up real-time cart listener for user: ${user.username} (ID: ${user.id})")
                    setupRealTimeCartListener(user.id)
                } else {
                    println("DEBUG: User not available or no cart permissions, clearing cart items")
                    _cartItems.value = emptyList()
                }
            }
        }
    }

    /**
     * Set up real-time cart listener for automatic updates
     */
    private fun setupRealTimeCartListener(userId: Long) {
        viewModelScope.launch {
            try {
                cartRepository.getCartItemsFlowByUserId(userId).collect { cartItems ->
                    println("DEBUG: Real-time cart update received: ${cartItems.size} items")
                    _cartItems.value = cartItems
                }
            } catch (e: Exception) {
                println("DEBUG: Error in real-time cart listener: ${e.message}")
                _errorMessage.value = "Failed to sync cart: ${e.message}"
            }
        }
    }

    /**
     * Load cart items (Customer only for own cart, Admin can view all)
     */
    fun loadCartItems() {
        val user = currentUser.value
        if (!PermissionManager.canUseShoppingCart(user) && !PermissionManager.isAdmin(user)) {
            _permissionError.value = "Access denied: Only customers can access shopping cart"
            return
        }

        if (user == null) {
            _permissionError.value = "Please log in to access your cart"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _permissionError.value = null
            
            try {
                println("DEBUG: Loading cart for user: ${user.username} (ID: ${user.id})")
                val result = cartRepository.getCartItemsByUserId(user.id)
                if (result.isSuccess) {
                    val cartItems = result.getOrNull() ?: emptyList()
                    println("DEBUG: Cart loaded successfully with ${cartItems.size} items for user ${user.id}")
                    _cartItems.value = cartItems
                } else {
                    println("DEBUG: Failed to load cart for user ${user.id}: ${result.exceptionOrNull()?.message}")
                    _errorMessage.value = "Failed to load cart: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                println("DEBUG: Exception loading cart for user ${user.id}: ${e.message}")
                _errorMessage.value = "Failed to load cart: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Add item to cart (Customer only)
     */
    fun addToCart(productId: Long, quantity: Int = 1) {
        val user = currentUser.value
        if (!PermissionManager.canUseShoppingCart(user)) {
            _permissionError.value = "Access denied: Only customers can add items to cart"
            return
        }

        if (user == null) {
            _permissionError.value = "Please log in to add items to cart"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _permissionError.value = null
            _operationSuccess.value = null
            
            try {
                // Check if product exists and has sufficient stock
                val productResult = productRepository.getProductById(productId)
                if (productResult.isSuccess) {
                    val product = productResult.getOrNull()
                    if (product == null) {
                        _errorMessage.value = "Product not found"
                        return@launch
                    }

                    if (product.quantity < quantity) {
                        _errorMessage.value = "Insufficient stock. Available: ${product.quantity}"
                        return@launch
                    }

                    // Check if item already exists in cart
                    val existingItem = _cartItems.value.find { it.productId == productId }
                    
                    if (existingItem != null) {
                        // Update existing cart item
                        val newQuantity = existingItem.quantity + quantity
                        if (product.quantity < newQuantity) {
                            _errorMessage.value = "Cannot add more items. Cart would exceed available stock (${product.quantity})"
                            return@launch
                        }
                        
                        val updatedItem = existingItem.copy(quantity = newQuantity)
                        val result = cartRepository.updateCartItem(updatedItem)
                        if (result.isSuccess) {
                            _operationSuccess.value = "Cart updated successfully"
                        } else {
                            _errorMessage.value = "Failed to update cart: ${result.exceptionOrNull()?.message}"
                        }
                    } else {
                        // Add new cart item
                        val cartItem = CartItem(
                            id = System.currentTimeMillis(),
                            userId = user.id,
                            productId = productId,
                            quantity = quantity,
                            addedAt = System.currentTimeMillis()
                        )
                        
                        val result = cartRepository.addCartItem(cartItem)
                        if (result.isSuccess) {
                            _operationSuccess.value = "Item added to cart"
                        } else {
                            _errorMessage.value = "Failed to add to cart: ${result.exceptionOrNull()?.message}"
                        }
                    }
                } else {
                    _errorMessage.value = "Failed to verify product: ${productResult.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error adding to cart: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Update cart item quantity (Customer only for own cart)
     */
    fun updateCartItemQuantity(cartItemId: Long, newQuantity: Int) {
        val user = currentUser.value
        if (!PermissionManager.canUseShoppingCart(user)) {
            _permissionError.value = "Access denied: Only customers can modify cart items"
            return
        }

        if (newQuantity <= 0) {
            removeFromCart(cartItemId)
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _permissionError.value = null
            _operationSuccess.value = null
            
            try {
                val cartItem = _cartItems.value.find { it.id == cartItemId }
                if (cartItem == null) {
                    _errorMessage.value = "Cart item not found"
                    return@launch
                }

                // Verify stock availability
                val productResult = productRepository.getProductById(cartItem.productId)
                if (productResult.isSuccess) {
                    val product = productResult.getOrNull()
                    if (product != null && product.quantity < newQuantity) {
                        _errorMessage.value = "Insufficient stock. Available: ${product.quantity}"
                        return@launch
                    }
                }

                val updatedItem = cartItem.copy(quantity = newQuantity)
                val result = cartRepository.updateCartItem(updatedItem)
                if (result.isSuccess) {
                    _operationSuccess.value = "Cart updated successfully"
                } else {
                    _errorMessage.value = "Failed to update cart: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error updating cart: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Remove item from cart (Customer only for own cart)
     */
    fun removeFromCart(cartItemId: Long) {
        val user = currentUser.value
        if (!PermissionManager.canUseShoppingCart(user)) {
            _permissionError.value = "Access denied: Only customers can remove cart items"
            return
        }

        val userId = user?.id
        if (userId == null) {
            _errorMessage.value = "User not found"
            return
        }

        println("DEBUG: Removing cart item with ID: $cartItemId for user: $userId")

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _permissionError.value = null
            _operationSuccess.value = null
            
            try {
                // Find the cart item to get the productId
                val cartItem = _cartItems.value.find { it.id == cartItemId }
                if (cartItem == null) {
                    println("DEBUG: Cart item not found with ID: $cartItemId")
                    _errorMessage.value = "Cart item not found"
                    return@launch
                }
                
                println("DEBUG: Calling cartRepository.removeCartItem($userId, ${cartItem.productId})")
                val result = cartRepository.removeCartItem(userId, cartItem.productId)
                if (result.isSuccess) {
                    println("DEBUG: Cart item removed successfully, reloading cart items")
                    _operationSuccess.value = "Item removed from cart"
                    // Reload cart items to reflect the change
                    loadCartItems()
                } else {
                    println("DEBUG: Failed to remove cart item: ${result.exceptionOrNull()?.message}")
                    _errorMessage.value = "Failed to remove item: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                println("DEBUG: Exception removing cart item: ${e.message}")
                _errorMessage.value = "Error removing item: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Clear entire cart (Customer only for own cart)
     */
    fun clearCart() {
        val user = currentUser.value
        if (!PermissionManager.canUseShoppingCart(user)) {
            _permissionError.value = "Access denied: Only customers can clear cart"
            return
        }

        if (user == null) return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _permissionError.value = null
            _operationSuccess.value = null
            
            try {
                val result = cartRepository.clearCart(user.id)
                if (result.isSuccess) {
                    _operationSuccess.value = "Cart cleared successfully"
                } else {
                    _errorMessage.value = "Failed to clear cart: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error clearing cart: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Checkout cart (Customer only)
     */
    fun checkout() {
        val user = currentUser.value
        if (!PermissionManager.canCheckout(user)) {
            _permissionError.value = "Access denied: Only customers can checkout"
            return
        }

        if (user == null) {
            _permissionError.value = "Please log in to checkout"
            return
        }

        if (_cartItems.value.isEmpty()) {
            _errorMessage.value = "Cart is empty"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _permissionError.value = null
            _operationSuccess.value = null
            
            try {
                // Verify all items are still in stock
                for (cartItem in _cartItems.value) {
                    val productResult = productRepository.getProductById(cartItem.productId)
                    if (productResult.isSuccess) {
                        val product = productResult.getOrNull()
                        if (product == null || product.quantity < cartItem.quantity) {
                            _errorMessage.value = "Some items are no longer available. Please review your cart."
                            return@launch
                        }
                    }
                }

                // Process checkout (this would integrate with payment processing)
                // For now, we'll implement a simple checkout that clears the cart
                // In a real app, this would process payment and create orders
                val result = cartRepository.clearCart(user.id)
                if (result.isSuccess) {
                    _operationSuccess.value = "Order placed successfully!"
                    clearCart() // Clear cart after successful checkout
                } else {
                    _errorMessage.value = "Checkout failed: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Checkout error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Check if current user can perform cart operations
     */
    fun canUseShoppingCart(): Boolean = PermissionManager.canUseShoppingCart(currentUser.value)
    fun canCheckout(): Boolean = PermissionManager.canCheckout(currentUser.value)

    /**
     * Clear messages
     */
    fun clearMessages() {
        _errorMessage.value = null
        _permissionError.value = null
        _operationSuccess.value = null
    }
}
