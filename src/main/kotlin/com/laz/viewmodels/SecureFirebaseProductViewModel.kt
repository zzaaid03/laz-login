package com.laz.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laz.models.Product
import com.laz.models.User
import com.laz.repositories.FirebaseProductRepository
import com.laz.security.PermissionManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Secure Firebase Product ViewModel
 * Enforces role-based permissions for product operations
 */
class SecureFirebaseProductViewModel(
    private val productRepository: FirebaseProductRepository,
    private val currentUser: StateFlow<User?>
) : ViewModel() {

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _permissionError = MutableStateFlow<String?>(null)
    val permissionError: StateFlow<String?> = _permissionError.asStateFlow()

    init {
        // Load products when user authentication state is available
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null) {
                    loadProducts()
                }
            }
        }
    }

    /**
     * Load products (all authenticated users can view)
     */
    fun loadProducts() {
        val user = currentUser.value
        if (!PermissionManager.canViewProducts(user)) {
            _permissionError.value = "Access denied: You don't have permission to view products"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _permissionError.value = null
            
            try {
                val result = productRepository.getAllProducts()
                if (result.isSuccess) {
                    _products.value = result.getOrNull() ?: emptyList()
                } else {
                    _errorMessage.value = "Failed to load products: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load products: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Add new product (Admin and Employee only)
     */
    fun addProduct(product: Product) {
        val user = currentUser.value
        if (!PermissionManager.canAddProducts(user)) {
            _permissionError.value = "Access denied: Only administrators and employees can add products"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _permissionError.value = null
            
            try {
                val result = productRepository.createProduct(product)
                if (result.isSuccess) {
                    loadProducts() // Refresh the list
                } else {
                    _errorMessage.value = "Failed to add product: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error adding product: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Update existing product (Admin and Employee only)
     */
    fun updateProduct(product: Product) {
        val user = currentUser.value
        if (!PermissionManager.canEditProducts(user)) {
            _permissionError.value = "Access denied: Only administrators and employees can edit products"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _permissionError.value = null
            
            try {
                val result = productRepository.updateProduct(product)
                if (result.isSuccess) {
                    loadProducts() // Refresh the list
                } else {
                    _errorMessage.value = "Failed to update product: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error updating product: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Delete product (Admin only)
     */
    fun deleteProduct(productId: Long) {
        val user = currentUser.value
        if (!PermissionManager.canDeleteProducts(user)) {
            _permissionError.value = "Access denied: Only administrators can delete products"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _permissionError.value = null
            
            try {
                val result = productRepository.deleteProduct(productId)
                if (result.isSuccess) {
                    loadProducts() // Refresh the list
                } else {
                    _errorMessage.value = "Failed to delete product: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error deleting product: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Update inventory/stock (Admin and Employee only)
     */
    fun updateInventory(productId: Long, newQuantity: Int) {
        val user = currentUser.value
        if (!PermissionManager.canManageInventory(user)) {
            _permissionError.value = "Access denied: Only administrators and employees can manage inventory"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _permissionError.value = null
            
            try {
                val result = productRepository.updateProductQuantity(productId, newQuantity)
                if (result.isSuccess) {
                    loadProducts() // Refresh the list
                } else {
                    _errorMessage.value = "Failed to update inventory: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error updating inventory: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Get products with low stock (Admin and Employee only)
     */
    fun getLowStockProducts(threshold: Int = 5): StateFlow<List<Product>> {
        val user = currentUser.value
        if (!PermissionManager.canManageInventory(user)) {
            _permissionError.value = "Access denied: Only administrators and employees can view inventory reports"
            return flowOf(emptyList<Product>()).stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        }

        return products.map { productList ->
            productList.filter { it.quantity <= threshold }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList<Product>())
    }

    /**
     * Check if current user can perform specific actions
     */
    fun canAddProducts(): Boolean = PermissionManager.canAddProducts(currentUser.value)
    fun canEditProducts(): Boolean = PermissionManager.canEditProducts(currentUser.value)
    fun canDeleteProducts(): Boolean = PermissionManager.canDeleteProducts(currentUser.value)
    fun canManageInventory(): Boolean = PermissionManager.canManageInventory(currentUser.value)

    /**
     * Clear error messages
     */
    fun clearErrors() {
        _errorMessage.value = null
        _permissionError.value = null
    }

    /**
     * Get filtered products for customers (hide admin-specific details)
     */
    fun getCustomerProducts(): StateFlow<List<Product>> {
        val user = currentUser.value
        return if (PermissionManager.isCustomer(user)) {
            // For customers, only show products that are in stock
            products.map { productList ->
                productList.filter { it.quantity > 0 }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        } else {
            products // Admin and Employee see all products
        }
    }
}
