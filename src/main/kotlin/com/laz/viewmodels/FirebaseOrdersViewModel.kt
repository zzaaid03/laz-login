package com.laz.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laz.models.Order
import com.laz.models.OrderStatus
import com.laz.models.User
import com.laz.repositories.FirebaseOrdersRepository
import com.laz.repositories.FirebaseProductRepository
import com.laz.security.PermissionManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal

/**
 * Firebase Orders ViewModel
 * Manages customer orders with role-based permissions
 */
class FirebaseOrdersViewModel(
    private val ordersRepository: FirebaseOrdersRepository,
    private val productRepository: FirebaseProductRepository,
    private val currentUser: StateFlow<User?>
) : ViewModel() {

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _permissionError = MutableStateFlow<String?>(null)
    val permissionError: StateFlow<String?> = _permissionError.asStateFlow()

    private val _operationSuccess = MutableStateFlow<String?>(null)
    val operationSuccess: StateFlow<String?> = _operationSuccess.asStateFlow()

    private val _lastCreatedOrder = MutableStateFlow<Order?>(null)
    val lastCreatedOrder: StateFlow<Order?> = _lastCreatedOrder.asStateFlow()

    // Statistics
    private val _ordersCount = MutableStateFlow(0)
    val ordersCount: StateFlow<Int> = _ordersCount.asStateFlow()

    private val _totalOrdersAmount = MutableStateFlow(BigDecimal.ZERO)
    val totalOrdersAmount: StateFlow<BigDecimal> = _totalOrdersAmount.asStateFlow()

    init {
        // Set up real-time orders listening when user authentication state is available
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null) {
                    startRealTimeOrdersListening(user)
                }
            }
        }
    }

    /**
     * Start real-time orders listening based on user role
     */
    private fun startRealTimeOrdersListening(user: User) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _permissionError.value = null
            
            try {
                if (PermissionManager.isCustomer(user)) {
                    // Customers see only their orders with real-time updates
                    ordersRepository.getOrdersByCustomerIdFlow(user.id).collect { orders ->
                        _orders.value = orders
                        updateStatistics()
                        _isLoading.value = false
                    }
                } else {
                    // Admin/Employee see all orders with real-time updates
                    ordersRepository.getAllOrdersFlow().collect { orders ->
                        _orders.value = orders
                        updateStatistics()
                        _isLoading.value = false
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load orders: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * Load orders based on user role (fallback method for manual refresh)
     */
    fun loadOrders() {
        val user = currentUser.value
        if (user != null) {
            startRealTimeOrdersListening(user)
        } else {
            _permissionError.value = "Authentication required"
        }
    }

    /**
     * Create new order (Customer, Admin, or Employee for Point of Sale)
     */
    fun createOrder(order: Order) {
        val user = currentUser.value
        if (user == null) {
            _permissionError.value = "Authentication required"
            return
        }
        
        // Allow customers, admins, and employees to create orders
        if (!PermissionManager.isCustomer(user) && !PermissionManager.isAdmin(user) && !PermissionManager.isEmployee(user)) {
            _permissionError.value = "Insufficient permissions to create orders"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _permissionError.value = null
            
            try {
                // First, reduce stock for all ordered items
                var stockReductionSuccess = true
                val stockReductionErrors = mutableListOf<String>()
                
                for (item in order.items) {
                    val stockResult = productRepository.reduceProductStock(item.productId, item.quantity)
                    if (stockResult.isFailure) {
                        stockReductionSuccess = false
                        stockReductionErrors.add("Failed to reduce stock for ${item.productName}: ${stockResult.exceptionOrNull()?.message}")
                    }
                }
                
                if (!stockReductionSuccess) {
                    _errorMessage.value = "Order failed due to stock issues: ${stockReductionErrors.joinToString(", ")}"
                    return@launch
                }
                
                // If stock reduction successful, create the order
                android.util.Log.d("OrdersViewModel", "Creating order for ${order.items.size} items, total: ${order.totalAmount}")
                val result = ordersRepository.createOrder(order)
                if (result.isSuccess) {
                    val createdOrder = result.getOrNull()
                    android.util.Log.d("OrdersViewModel", "Order created successfully with ID: ${createdOrder?.id}")
                    _lastCreatedOrder.value = createdOrder
                    _operationSuccess.value = "Order created successfully! Order ID: ${createdOrder?.id}"
                    loadOrders() // Refresh the list
                } else {
                    android.util.Log.e("OrdersViewModel", "Failed to create order: ${result.exceptionOrNull()?.message}")
                    // If order creation fails, we should restore the stock
                    // For now, we'll just show the error - in production, implement stock restoration
                    _errorMessage.value = "Failed to create order: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error creating order: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Update order status (Admin/Employee only)
     */
    fun updateOrderStatus(orderId: Long, status: OrderStatus, trackingNumber: String? = null) {
        val user = currentUser.value
        if (!PermissionManager.canManageOrders(user)) {
            _permissionError.value = "Access denied: Only administrators and employees can update order status"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _permissionError.value = null
            
            try {
                val result = ordersRepository.updateOrderStatus(orderId, status, trackingNumber)
                if (result.isSuccess) {
                    _operationSuccess.value = "Order status updated to ${status.displayName}"
                    loadOrders() // Refresh the list
                } else {
                    _errorMessage.value = "Failed to update order status: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error updating order status: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Get orders by status (Admin/Employee only)
     */
    fun getOrdersByStatus(status: OrderStatus) {
        val user = currentUser.value
        if (!PermissionManager.canManageOrders(user)) {
            _permissionError.value = "Access denied: Only administrators and employees can filter orders"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _permissionError.value = null
            
            try {
                val result = ordersRepository.getOrdersByStatus(status)
                if (result.isSuccess) {
                    val orders = result.getOrNull() ?: emptyList()
                    _orders.value = orders
                } else {
                    _errorMessage.value = "Failed to filter orders: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error filtering orders: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Get customer orders (for customer view)
     */
    fun getCustomerOrders(): StateFlow<List<Order>> {
        val user = currentUser.value
        return if (PermissionManager.isCustomer(user)) {
            orders
        } else {
            MutableStateFlow<List<Order>>(emptyList()).asStateFlow()
        }
    }

    /**
     * Get recent orders (Admin/Employee dashboard)
     */
    fun loadRecentOrders(limit: Int = 5) {
        val user = currentUser.value
        if (!PermissionManager.canManageOrders(user)) {
            return
        }

        viewModelScope.launch {
            try {
                val result = ordersRepository.getRecentOrders(limit)
                if (result.isSuccess) {
                    val orders = result.getOrNull() ?: emptyList()
                    _orders.value = orders
                }
            } catch (e: Exception) {
                // Silent fail for dashboard widgets
            }
        }
    }

    /**
     * Update statistics
     */
    private suspend fun updateStatistics() {
        try {
            val countResult = ordersRepository.getOrdersCount()
            if (countResult.isSuccess) {
                _ordersCount.value = countResult.getOrNull() ?: 0
            }

            val totalResult = ordersRepository.getTotalOrdersAmount()
            if (totalResult.isSuccess) {
                _totalOrdersAmount.value = totalResult.getOrNull() ?: BigDecimal.ZERO
            }
        } catch (e: Exception) {
            // Silent fail for statistics
        }
    }

    /**
     * Clear error and success messages
     */
    fun clearMessages() {
        _errorMessage.value = null
        _permissionError.value = null
        _operationSuccess.value = null
    }

    /**
     * Clear errors only
     */
    fun clearErrors() {
        _errorMessage.value = null
        _permissionError.value = null
    }
}
