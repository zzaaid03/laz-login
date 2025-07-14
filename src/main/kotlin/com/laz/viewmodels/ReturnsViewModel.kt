package com.laz.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laz.database.ReturnDao
import com.laz.database.SaleDao
import com.laz.models.Return
import com.laz.models.Sale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ReturnsViewModel(
    private val returnDao: ReturnDao,
    private val saleDao: SaleDao,
    private val productDao: com.laz.database.ProductDao
) : ViewModel() {
    
    private val _returns = MutableStateFlow<List<Return>>(emptyList())
    val returns: StateFlow<List<Return>> = _returns.asStateFlow()
    
    init {
        viewModelScope.launch {
            returnDao.getAllReturns().collect { returnsList ->
                _returns.value = returnsList
            }
        }
    }
    
    suspend fun processReturn(sale: Sale, reason: String): Long? {
        return try {
            // Create return record
            val returnItem = Return(
                saleId = sale.id,
                reason = reason,
                date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
            )
            
            val returnId = returnDao.insertReturn(returnItem)
            
            // Mark sale as returned
            saleDao.markSaleAsReturned(sale.id)
            
            // CRITICAL FIX: Restore product stock when processing return
            productDao.increaseQuantity(sale.productId, sale.quantity)
            
            returnId
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    suspend fun getReturnById(id: Long): Return? {
        return returnDao.getReturnById(id)
    }
    
    suspend fun getReturnsBySaleId(saleId: Long): List<Return> {
        return returnDao.getReturnsBySaleId(saleId)
    }
    
    suspend fun getReturnCountFromDate(daysAgo: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
        val fromDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(calendar.time)
        return returnDao.getReturnCountFromDate(fromDate)
    }
    
    suspend fun deleteReturn(returnItem: Return) {
        returnDao.deleteReturn(returnItem)
    }
    
    fun getAllReturns(): List<Return> {
        return _returns.value
    }
}
