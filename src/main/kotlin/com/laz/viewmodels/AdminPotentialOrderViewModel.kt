package com.laz.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.laz.models.PotentialOrder
import com.laz.models.PotentialOrderStatus
import com.laz.repositories.FirebaseRealtimePotentialOrderRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AdminPotentialOrderViewModel(
    private val repository: FirebaseRealtimePotentialOrderRepository
) : ViewModel() {

    private val _potentialOrders = MutableStateFlow<List<PotentialOrder>>(emptyList())
    val potentialOrders: StateFlow<List<PotentialOrder>> = _potentialOrders.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadPendingOrders()
    }

    fun loadPendingOrders() {
        currentViewType = ViewType.PENDING
        currentStatus = null
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            repository.getPendingPotentialOrders()
                .catch { error ->
                    _errorMessage.value = "Failed to load pending orders: ${error.message}"
                    _isLoading.value = false
                }
                .collect { orders ->
                    _potentialOrders.value = orders
                    _isLoading.value = false
                }
        }
    }

    fun loadAllOrders() {
        currentViewType = ViewType.ALL
        currentStatus = null
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            repository.getAllPotentialOrders()
                .catch { error ->
                    _errorMessage.value = "Failed to load all orders: ${error.message}"
                    _isLoading.value = false
                }
                .collect { orders ->
                    _potentialOrders.value = orders
                    _isLoading.value = false
                }
        }
    }

    fun loadOrdersByStatus(status: PotentialOrderStatus) {
        currentViewType = ViewType.BY_STATUS
        currentStatus = status
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val result = repository.getOrdersByStatus(status)
                result.onSuccess { orders ->
                    _potentialOrders.value = orders
                }.onFailure { error ->
                    _errorMessage.value = "Failed to load orders: ${error.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error loading orders: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun approveOrder(orderId: String, adminNotes: String = "Approved for ordering") {
        viewModelScope.launch {
            try {
                val result = repository.updateOrderStatus(orderId, PotentialOrderStatus.APPROVED, adminNotes)
                result.onSuccess {
                    // Refresh the current view after successful approval
                    refreshCurrentView()
                }.onFailure { error ->
                    _errorMessage.value = "Failed to approve order: ${error.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error approving order: ${e.message}"
            }
        }
    }

    fun rejectOrder(orderId: String, adminNotes: String = "Order rejected") {
        viewModelScope.launch {
            try {
                val result = repository.updateOrderStatus(orderId, PotentialOrderStatus.REJECTED, adminNotes)
                result.onSuccess {
                    // Refresh the current view after successful rejection
                    refreshCurrentView()
                }.onFailure { error ->
                    _errorMessage.value = "Failed to reject order: ${error.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error rejecting order: ${e.message}"
            }
        }
    }

    private var currentViewType: ViewType = ViewType.PENDING
    private var currentStatus: PotentialOrderStatus? = null

    private enum class ViewType {
        PENDING, ALL, BY_STATUS
    }

    private fun refreshCurrentView() {
        when (currentViewType) {
            ViewType.PENDING -> loadPendingOrders()
            ViewType.ALL -> loadAllOrders()
            ViewType.BY_STATUS -> currentStatus?.let { loadOrdersByStatus(it) }
        }
    }

    fun updateOrderStatus(orderId: String, status: PotentialOrderStatus, notes: String = "") {
        viewModelScope.launch {
            try {
                val result = repository.updateOrderStatus(orderId, status, notes)
                result.onFailure { error ->
                    _errorMessage.value = "Failed to update order status: ${error.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error updating order: ${e.message}"
            }
        }
    }

    fun deleteOrder(orderId: String) {
        viewModelScope.launch {
            try {
                val result = repository.deletePotentialOrder(orderId)
                result.onFailure { error ->
                    _errorMessage.value = "Failed to delete order: ${error.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error deleting order: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    class Factory(
        private val repository: FirebaseRealtimePotentialOrderRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AdminPotentialOrderViewModel::class.java)) {
                return AdminPotentialOrderViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
