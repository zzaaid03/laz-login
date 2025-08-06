package com.laz.firebase

import com.google.firebase.database.*
import com.laz.models.User
import com.laz.models.Product
import com.laz.models.CartItem
import com.laz.models.Sale
import com.laz.models.Return
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseDatabaseService {
    private val database = FirebaseDatabase.getInstance()
    private val usersRef = database.getReference("users")
    private val productsRef = database.getReference("products")
    private val cartItemsRef = database.getReference("cartItems")
    private val salesRef = database.getReference("sales")
    private val returnsRef = database.getReference("returns")
    
    // User operations
    suspend fun createUser(user: User): String {
        val userRef = usersRef.push()
        userRef.setValue(user).await()
        return userRef.key ?: ""
    }
    
    suspend fun getUserByUsername(username: String): User? {
        val snapshot = usersRef.orderByChild("username").equalTo(username).get().await()
        return snapshot.children.firstOrNull()?.getValue(User::class.java)
    }
    
    suspend fun getUserById(userId: String): User? {
        val snapshot = usersRef.child(userId).get().await()
        return snapshot.getValue(User::class.java)
    }
    
    fun getAllUsers(): Flow<List<User>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val users = snapshot.children.mapNotNull { 
                    it.getValue(User::class.java)?.copy(id = it.key?.toLongOrNull() ?: 0)
                }
                trySend(users)
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        usersRef.addValueEventListener(listener)
        awaitClose { usersRef.removeEventListener(listener) }
    }
    
    // Product operations
    suspend fun createProduct(product: Product): String {
        val productRef = productsRef.push()
        productRef.setValue(product).await()
        return productRef.key ?: ""
    }
    
    suspend fun updateProduct(productId: String, product: Product) {
        productsRef.child(productId).setValue(product).await()
    }
    
    suspend fun deleteProduct(productId: String) {
        productsRef.child(productId).removeValue().await()
    }
    
    fun getAllProducts(): Flow<List<Product>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val products = snapshot.children.mapNotNull { 
                    it.getValue(Product::class.java)?.copy(id = it.key?.toLongOrNull() ?: 0)
                }
                trySend(products)
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        productsRef.addValueEventListener(listener)
        awaitClose { productsRef.removeEventListener(listener) }
    }
    
    // Cart operations
    suspend fun addToCart(userId: String, cartItem: CartItem): String {
        val cartRef = cartItemsRef.child(userId).push()
        cartRef.setValue(cartItem).await()
        return cartRef.key ?: ""
    }
    
    suspend fun updateCartItem(userId: String, cartItemId: String, cartItem: CartItem) {
        cartItemsRef.child(userId).child(cartItemId).setValue(cartItem).await()
    }
    
    suspend fun removeFromCart(userId: String, cartItemId: String) {
        cartItemsRef.child(userId).child(cartItemId).removeValue().await()
    }
    
    suspend fun clearCart(userId: String) {
        cartItemsRef.child(userId).removeValue().await()
    }
    
    fun getCartItems(userId: String): Flow<List<CartItem>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val cartItems = snapshot.children.mapNotNull { 
                    it.getValue(CartItem::class.java)
                }
                trySend(cartItems)
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        cartItemsRef.child(userId).addValueEventListener(listener)
        awaitClose { cartItemsRef.child(userId).removeEventListener(listener) }
    }
    
    // Sales operations
    suspend fun createSale(sale: Sale): String {
        val saleRef = salesRef.push()
        saleRef.setValue(sale).await()
        return saleRef.key ?: ""
    }
    
    fun getAllSales(): Flow<List<Sale>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sales = snapshot.children.mapNotNull { 
                    it.getValue(Sale::class.java)?.copy(id = it.key?.toLongOrNull() ?: 0)
                }
                trySend(sales)
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        salesRef.addValueEventListener(listener)
        awaitClose { salesRef.removeEventListener(listener) }
    }
    
    // Returns operations
    suspend fun createReturn(returnItem: Return): String {
        val returnRef = returnsRef.push()
        returnRef.setValue(returnItem).await()
        return returnRef.key ?: ""
    }
    
    fun getAllReturns(): Flow<List<Return>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val returns = snapshot.children.mapNotNull { 
                    it.getValue(Return::class.java)?.copy(id = it.key?.toLongOrNull() ?: 0)
                }
                trySend(returns)
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        returnsRef.addValueEventListener(listener)
        awaitClose { returnsRef.removeEventListener(listener) }
    }
}
