package com.laz.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laz.database.ProductDao
import com.laz.database.UserDao
import com.laz.models.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

class ProductViewModel(private val ProductDao : ProductDao ) : ViewModel() {

    private val _allProducts = MutableStateFlow<List<Product>>(emptyList())
    val allProducts: StateFlow<List<Product>> = _allProducts.asStateFlow()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _isLoading = MutableStateFlow(false) // Default to false
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null) // Default to null
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()


    fun fetchAllProducts() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

        }
    }

    init {
        loadProducts()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            // Replace with your actual data fetching logic (e.g., from a repository)
            
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            // Check if we have any products directly from database, if not create sample data
            val existingProductsCount = ProductDao.getProductCount()
            if (existingProductsCount == 0) {
                createProduct(
                    Product(
                        name = "Tesla Model S Door Handle",
                        quantity = 50,
                        cost = BigDecimal("25.00"),
                        price = BigDecimal("45.00"),
                        shelfLocation = "A1"
                    )
                )
                createProduct(
                    Product(
                        name = "Tesla Model 3 Screen Protector",
                        quantity = 30,
                        cost = BigDecimal("5.00"),
                        price = BigDecimal("15.00"),
                        shelfLocation = "B2"
                    )
                )
                createProduct(
                    Product(
                        name = "Tesla Charging Cable",
                        quantity = 3,
                        cost = BigDecimal("80.00"),
                        price = BigDecimal("120.00"),
                        shelfLocation = "C3"
                    )
                )
            }
        }
    }

    suspend fun createProduct(product: Product): Long {
        return ProductDao.insertProduct(product)
    }

    suspend fun updateProduct(product: Product) {
        ProductDao.updateProduct(product)
    }

    suspend fun deleteProduct(product: Product) {
        ProductDao.deleteProduct(product)
    }

    suspend fun deleteProductById(id: Long) {
        ProductDao.deleteProductById(id)
    }

    suspend fun getProductById(id: Long): Product? {
        return ProductDao.getProductById(id)
    }

    suspend fun searchProductsByName(name: String): List<Product> {
        return ProductDao.searchProductsByName(name)
    }

    suspend fun getLowStockProducts(threshold: Int = 5): List<Product> {
        return ProductDao.getLowStockProducts(threshold)
    }

    suspend fun decreaseProductQuantity(id: Long, quantity: Int) {
        ProductDao.decreaseQuantity(id, quantity)
    }

    suspend fun increaseProductQuantity(id: Long, quantity: Int) {
        ProductDao.increaseQuantity(id, quantity)
    }

    fun getAllProducts(): List<Product> {
        return _products.value
    }

    // Convenience methods with individual parameters
    suspend fun createProduct(
        name: String,
        quantity: Int,
        cost: java.math.BigDecimal,
        price: java.math.BigDecimal,
        shelfLocation: String?
    ) {
        val product = Product(
            name = name,
            quantity = quantity,
            cost = cost,
            price = price,
            shelfLocation = shelfLocation
        )
        createProduct(product)
    }


    suspend fun deleteProduct(id: Long) {
        deleteProductById(id)
    }
}
