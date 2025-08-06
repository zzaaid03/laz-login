package com.laz.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laz.database.SaleDao
import com.laz.database.ProductDao
import com.laz.models.Sale
import com.laz.models.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SalesViewModel(
    private val saleDao: SaleDao,
    private val productDao: ProductDao
) : ViewModel() {
    
    private val _sales = MutableStateFlow<List<Sale>>(emptyList())
    val sales: StateFlow<List<Sale>> = _sales.asStateFlow()
    
    private val _userSales = MutableStateFlow<List<Sale>>(emptyList())
    val userSales: StateFlow<List<Sale>> = _userSales.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    init {
        viewModelScope.launch {
            saleDao.getAllSalesFlow().collect { salesList ->
                _sales.value = salesList
            }
        }
    }
    
    fun fetchSalesByUserId(userId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                // Filter sales by user ID
                val userSalesList = _sales.value.filter { it.userId.toString() == userId }
                _userSales.value = userSalesList
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An error occurred fetching sales"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    suspend fun createSale(
        product: Product,
        quantity: Int,
        userId: Long,
        userName: String
    ): Long? {
        return try {
            // Check if enough stock is available
            if (product.quantity >= quantity) {
                // Create sale record
                val sale = Sale(
                    productId = product.id,
                    productName = product.name,
                    productPrice = product.price.toString(),
                    quantity = quantity,
                    userId = userId,
                    userName = userName,
                    date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date()),
                    isReturned = false
                )
                
                val saleId = saleDao.insertSale(sale)
                
                // Decrease product quantity
                productDao.decreaseQuantity(product.id, quantity)
                
                saleId
            } else {
                null // Not enough stock
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    suspend fun getSaleById(id: Long): Sale? {
        return saleDao.getSaleById(id)
    }
    
    suspend fun getSalesByUserId(userId: Long): List<Sale> {
        return saleDao.getSalesByUserId(userId)
    }
    
    suspend fun getSalesByProductId(productId: Long): List<Sale> {
        return saleDao.getSalesByProductId(productId)
    }
    
    suspend fun getNonReturnedSales(): List<Sale> {
        return saleDao.getNonReturnedSales()
    }
    
    suspend fun getReturnedSales(): List<Sale> {
        return saleDao.getReturnedSales()
    }
    
    suspend fun getSalesByDate(date: String): List<Sale> {
        return saleDao.getSalesByDate(date)
    }
    
    suspend fun markSaleAsReturned(saleId: Long) {
        saleDao.markSaleAsReturned(saleId)
    }
    
    
    suspend fun getTodaysSales(): List<Sale> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return getSalesByDate(today)
    }
    
    suspend fun calculateTodaysSalesTotal(): Double {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return saleDao.getTodaySalesTotal(today) ?: 0.0
    }
    
    suspend fun getTodaySalesCount(): Int {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return saleDao.getTodaySalesCount(today)
    }
    
    suspend fun getSalesCountFromDate(daysAgo: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
        val fromDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(calendar.time)
        return saleDao.getSalesCountFromDate(fromDate)
    }
    
    suspend fun getRecentSales(days: Int): List<Sale> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        val targetDate = calendar.time
        
        return _sales.value.filter { sale ->
            try {
                val saleDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(sale.date)
                saleDate?.after(targetDate) == true
            } catch (e: Exception) {
                false
            }
        }
    }
    
    suspend fun processSale(product: com.laz.models.Product, quantity: Int, user: com.laz.models.User): Sale? {
        return try {
            if (product.quantity >= quantity) {
                val saleId = createSale(product, quantity, user.id, user.username)
                if (saleId != null) {
                    getSaleById(saleId)
                } else null
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    suspend fun getAllSales(): List<Sale> {
        return saleDao.getAllSales()
    }
    
    suspend fun updateSale(sale: Sale) {
        saleDao.updateSale(sale)
    }
    
    suspend fun deleteSale(saleId: Long) {
        saleDao.deleteSale(saleId)
    }
}
