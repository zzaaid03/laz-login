package com.laz.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.laz.database.*

class ViewModelFactory(private val database: LazDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserViewModel(database.userDao()) as T
        }
        if (modelClass.isAssignableFrom(ProductViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProductViewModel(database.productDao()) as T
        }
        if (modelClass.isAssignableFrom(SalesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SalesViewModel(database.saleDao(), database.productDao(), database.userDao()) as T
        }
        if (modelClass.isAssignableFrom(ReturnsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReturnsViewModel(database.returnDao(), database.saleDao()) as T
        }
        if (modelClass.isAssignableFrom(CartViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CartViewModel(database.cartDao(), database.productDao()) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
