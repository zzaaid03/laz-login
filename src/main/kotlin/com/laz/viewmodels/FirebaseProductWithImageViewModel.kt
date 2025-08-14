package com.laz.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laz.models.Product
import com.laz.models.User
import com.laz.repositories.FirebaseProductRepository
import com.laz.security.PermissionManager
import com.laz.services.FirebaseStorageService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal

/**
 * Enhanced Firebase Product ViewModel with Image Upload Support
 * Extends the product management functionality to include Firebase Storage integration
 */
class FirebaseProductWithImageViewModel(
    private val productRepository: FirebaseProductRepository,
    private val storageService: FirebaseStorageService,
    private val currentUser: StateFlow<User?>
) : ViewModel() {

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _operationSuccess = MutableStateFlow<String?>(null)
    val operationSuccess: StateFlow<String?> = _operationSuccess.asStateFlow()

    private val _permissionError = MutableStateFlow<String?>(null)
    val permissionError: StateFlow<String?> = _permissionError.asStateFlow()

    private val _uploadProgress = MutableStateFlow<Float?>(null)
    val uploadProgress: StateFlow<Float?> = _uploadProgress.asStateFlow()

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
     * Load all products
     */
    fun loadProducts() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val result = productRepository.getAllProducts()
                if (result.isSuccess) {
                    _products.value = result.getOrNull() ?: emptyList()
                } else {
                    _errorMessage.value = "Failed to load products: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error loading products: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Create product with image upload
     */
    fun createProductWithImage(
        name: String,
        quantity: Int,
        cost: BigDecimal,
        price: BigDecimal,
        shelfLocation: String?,
        imageUri: Uri?
    ) {
        val user = currentUser.value
        if (!PermissionManager.canAddProducts(user)) {
            _permissionError.value = "Access denied: Only administrators can add products"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _permissionError.value = null
            _uploadProgress.value = null
            
            try {
                var imageUrl: String? = null
                
                // Upload image if provided
                if (imageUri != null) {
                    _uploadProgress.value = 0.5f
                    val uploadResult = storageService.uploadProductImageWithName(
                        imageUri, 
                        storageService.generateImageFileName(0L, "product_image.jpg")
                    )
                    
                    if (uploadResult.isSuccess) {
                        imageUrl = uploadResult.getOrNull()
                        _uploadProgress.value = 1.0f
                    } else {
                        _errorMessage.value = "Failed to upload image: ${uploadResult.exceptionOrNull()?.message}"
                        return@launch
                    }
                }
                
                // Create product with image URL
                val product = Product(
                    name = name,
                    quantity = quantity,
                    cost = cost,
                    price = price,
                    shelfLocation = shelfLocation,
                    imageUrl = imageUrl
                )
                
                val result = productRepository.createProduct(product)
                if (result.isSuccess) {
                    _operationSuccess.value = "Product created successfully!"
                    loadProducts() // Refresh the list
                } else {
                    // If product creation fails but image was uploaded, clean up the image
                    if (imageUrl != null) {
                        storageService.deleteProductImage(imageUrl)
                    }
                    _errorMessage.value = "Failed to create product: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error creating product: ${e.message}"
            } finally {
                _isLoading.value = false
                _uploadProgress.value = null
            }
        }
    }

    /**
     * Update product with optional image upload
     */
    fun updateProductWithImage(
        product: Product,
        newImageUri: Uri? = null,
        removeExistingImage: Boolean = false
    ) {
        val user = currentUser.value
        if (!PermissionManager.canEditProducts(user)) {
            _permissionError.value = "Access denied: Only administrators can edit products"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _permissionError.value = null
            _uploadProgress.value = null
            
            try {
                var updatedImageUrl = product.imageUrl
                
                // Handle image removal
                if (removeExistingImage && product.imageUrl != null) {
                    storageService.deleteProductImage(product.imageUrl)
                    updatedImageUrl = null
                }
                
                // Handle new image upload
                if (newImageUri != null) {
                    _uploadProgress.value = 0.5f
                    
                    // Delete old image if exists
                    if (product.imageUrl != null) {
                        storageService.deleteProductImage(product.imageUrl)
                    }
                    
                    val uploadResult = storageService.uploadProductImage(newImageUri, product.id)
                    if (uploadResult.isSuccess) {
                        updatedImageUrl = uploadResult.getOrNull()
                        _uploadProgress.value = 1.0f
                    } else {
                        _errorMessage.value = "Failed to upload new image: ${uploadResult.exceptionOrNull()?.message}"
                        return@launch
                    }
                }
                
                // Update product with new image URL
                val updatedProduct = product.copy(imageUrl = updatedImageUrl)
                val result = productRepository.updateProduct(updatedProduct)
                
                if (result.isSuccess) {
                    _operationSuccess.value = "Product updated successfully!"
                    loadProducts() // Refresh the list
                } else {
                    _errorMessage.value = "Failed to update product: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error updating product: ${e.message}"
            } finally {
                _isLoading.value = false
                _uploadProgress.value = null
            }
        }
    }

    /**
     * Delete product and its image
     */
    fun deleteProductWithImage(productId: Long) {
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
                // Get product to access image URL
                val product = _products.value.find { it.id == productId }
                
                // Delete product from database
                val result = productRepository.deleteProduct(productId)
                if (result.isSuccess) {
                    // Delete associated image if exists
                    if (product?.imageUrl != null) {
                        storageService.deleteProductImage(product.imageUrl)
                    }
                    
                    _operationSuccess.value = "Product deleted successfully!"
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
     * Clear error messages
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
        _permissionError.value = null
    }

    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        _operationSuccess.value = null
    }

    /**
     * Permission check methods
     */
    fun canAddProducts(): Boolean = PermissionManager.canAddProducts(currentUser.value)
    fun canEditProducts(): Boolean = PermissionManager.canEditProducts(currentUser.value)
    fun canDeleteProducts(): Boolean = PermissionManager.canDeleteProducts(currentUser.value)
}
