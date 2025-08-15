package com.laz.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laz.models.Return
import com.laz.models.Order
import com.laz.repositories.FirebaseProductRepository
import com.laz.repositories.FirebaseReturnsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Firebase-integrated ReturnsViewModel for managing returns in Firebase Realtime Database
 * Replaces Room DAO-based ReturnsViewModel for pure Firebase architecture
 * 
 * Note: This is a simplified version that focuses on product management
 * Returns data structure would need to be defined in Firebase schema
 */
class FirebaseReturnsViewModel(
    private val firebaseProductRepository: FirebaseProductRepository,
    private val firebaseReturnsRepository: FirebaseReturnsRepository
) : ViewModel() {
    
    private val _returns = MutableStateFlow<List<Return>>(emptyList())
    val returns: StateFlow<List<Return>> = _returns.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _returnsCount = MutableStateFlow(0)
    val returnsCount: StateFlow<Int> = _returnsCount.asStateFlow()
    
    init {
        loadAllReturns()
        // Start collecting returns from repository
        viewModelScope.launch {
            firebaseReturnsRepository.getAllReturns().collect { returnsList ->
                _returns.value = returnsList
                _returnsCount.value = returnsList.size
            }
        }
    }

    /**
     * Load all returns from Firebase
     */
    fun loadAllReturns() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                // Data will be loaded via the Flow in init block
                // This method can be called to refresh or handle errors
                
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load returns: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Process a return (simplified version for Firebase integration)
     * Updates product quantity in Firebase
     */
    suspend fun processReturn(productId: Long, quantity: Int, reason: String): Boolean {
        return try {
            _isLoading.value = true
            
            // Get the product from Firebase
            val productResult = firebaseProductRepository.getProductById(productId)
            val product = productResult.getOrNull()
            if (product == null) {
                _errorMessage.value = "Product not found"
                return false
            }
            
            // Update product quantity (add back returned items)
            val updatedProduct = product.copy(quantity = product.quantity + quantity)
            val updateResult = firebaseProductRepository.updateProduct(updatedProduct)
            
            if (updateResult.isSuccess) {
                // Create return record in Firebase
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val returnRecord = Return(
                    saleId = productId, // Using productId as saleId for now (should be actual saleId)
                    reason = reason,
                    date = dateFormat.format(Date())
                )
                
                val returnResult = firebaseReturnsRepository.createReturn(returnRecord)
                if (returnResult.isSuccess) {
                    println("DEBUG: Return record created successfully")
                    true
                } else {
                    _errorMessage.value = "Failed to save return record: ${returnResult.exceptionOrNull()?.message}"
                    false
                }
            } else {
                _errorMessage.value = "Failed to update product inventory: ${updateResult.exceptionOrNull()?.message}"
                false
            }
        } catch (e: Exception) {
            _errorMessage.value = "Error processing return: ${e.message}"
            false
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Process a return from an order object
     * This is the main method used by the returns processing screen
     */
    suspend fun processReturnFromOrder(order: Order, reason: String): Boolean {
        return try {
            _isLoading.value = true
            println("DEBUG: Processing return for order ID: ${order.id}, customer: ${order.customerUsername}")
            
            // Process return for each item in the order
            order.items.forEach { orderItem ->
                // Get the product from Firebase to update inventory
                val productResult = firebaseProductRepository.getProductById(orderItem.productId)
                val product = productResult.getOrNull()
                if (product == null) {
                    _errorMessage.value = "Product not found for return: ${orderItem.productName}"
                    return@forEach
                }
                
                // Update product quantity (add back returned items)
                val updatedProduct = product.copy(quantity = product.quantity + orderItem.quantity)
                println("DEBUG: Updating product ${product.name} from quantity ${product.quantity} to ${updatedProduct.quantity}")
                val updateResult = firebaseProductRepository.updateProduct(updatedProduct)
                
                if (!updateResult.isSuccess) {
                    _errorMessage.value = "Failed to update product inventory: ${updateResult.exceptionOrNull()?.message}"
                    return@forEach
                }
            }
            
            // Create return record in Firebase
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val returnRecord = Return(
                saleId = order.id,
                reason = reason,
                date = dateFormat.format(Date())
            )
            
            val returnResult = firebaseReturnsRepository.createReturn(returnRecord)
            if (returnResult.isSuccess) {
                println("DEBUG: Return record created successfully for order ID: ${order.id}")
                true
            } else {
                _errorMessage.value = "Failed to save return record: ${returnResult.exceptionOrNull()?.message}"
                false
            }
        } catch (e: Exception) {
            _errorMessage.value = "Error processing return: ${e.message}"
            false
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Get all returns (placeholder for Firebase implementation)
     */
    suspend fun getAllReturns(): List<Return> {
        // In a full Firebase implementation, this would fetch returns from Firebase
        // For now, return empty list as returns structure needs to be defined
        return emptyList()
    }

    /**
     * Get recent returns (placeholder for Firebase implementation)
     */
    suspend fun getRecentReturns(days: Int): List<Return> {
        // In a full Firebase implementation, this would fetch recent returns from Firebase
        // For now, return empty list as returns structure needs to be defined
        return emptyList()
    }

    /**
     * Add return (placeholder for Firebase implementation)
     */
    suspend fun addReturn(returnItem: Return): Boolean {
        // In a full Firebase implementation, this would add return to Firebase
        // For now, return true as placeholder
        return true
    }

    /**
     * Update return (placeholder for Firebase implementation)
     */
    suspend fun updateReturn(returnItem: Return): Boolean {
        // In a full Firebase implementation, this would update return in Firebase
        // For now, return true as placeholder
        return true
    }

    /**
     * Delete return (placeholder for Firebase implementation)
     */
    suspend fun deleteReturn(returnId: Long): Boolean {
        // In a full Firebase implementation, this would delete return from Firebase
        // For now, return true as placeholder
        return true
    }

    /**
     * Get returns by product (placeholder for Firebase implementation)
     */
    suspend fun getReturnsByProduct(productId: Long): List<Return> {
        // In a full Firebase implementation, this would fetch product returns from Firebase
        // For now, return empty list as returns structure needs to be defined
        return emptyList()
    }

    /**
     * Get total returns count (placeholder for Firebase implementation)
     */
    suspend fun getReturnsCount(): Int {
        // In a full Firebase implementation, this would count returns from Firebase
        // For now, return 0 as placeholder
        return 0
    }

    /**
     * Get total returns value (placeholder for Firebase implementation)
     */
    suspend fun getTotalReturnsValue(): Double {
        // In a full Firebase implementation, this would calculate from Firebase data
        // For now, return 0.0 as placeholder
        return 0.0
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Refresh returns data (placeholder for Firebase implementation)
     */
    fun refresh() {
        // In a full Firebase implementation, this would refresh returns from Firebase
        // For now, just clear error
        clearError()
    }
}
