package com.laz.repositories

import com.google.firebase.database.*
import com.laz.models.Product
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.math.BigDecimal

/**
 * Firebase Product Repository
 * Manages product data in Firebase Realtime Database
 */
class FirebaseProductRepository {
    private val database = FirebaseDatabase.getInstance()
    private val productsRef = database.getReference("products")
    
    /**
     * Create new product
     */
    suspend fun createProduct(product: Product): Result<Product> {
        return try {
            println("DEBUG: Creating product: ${product.name}")
            val productId = getNextProductId()
            println("DEBUG: Generated product ID: $productId")
            val productWithId = product.copy(id = productId)
            
            val productMap = mapOf(
                "id" to productWithId.id,
                "name" to productWithId.name,
                "price" to productWithId.price.toString(),
                "quantity" to productWithId.quantity,
                "cost" to productWithId.cost.toString(),
                "shelfLocation" to (productWithId.shelfLocation ?: ""),
                "createdAt" to System.currentTimeMillis()
            )
            
            println("DEBUG: Product data to save: $productMap")
            println("DEBUG: Saving to Firebase path: ${productsRef.child(productId.toString())}")
            
            productsRef.child(productId.toString()).setValue(productMap).await()
            println("DEBUG: Successfully saved product: ${productWithId.name} with ID: $productId")
            Result.success(productWithId)
        } catch (e: Exception) {
            println("DEBUG: Exception creating product: ${e.message}")
            println("DEBUG: Exception stack trace: ${e.stackTrace.joinToString("\n")}")
            Result.failure(e)
        }
    }

    /**
     * Get product by ID
     */
    suspend fun getProductById(productId: Long): Result<Product?> {
        return try {
            val snapshot = productsRef.child(productId.toString()).get().await()
            val product = snapshot.toProduct()
            Result.success(product)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all products
     */
    suspend fun getAllProducts(): Result<List<Product>> {
        return try {
            println("DEBUG: Fetching products from Firebase...")
            println("DEBUG: Firebase database URL: ${database.reference.toString()}")
            println("DEBUG: Products reference path: ${productsRef.toString()}")
            
            val snapshot = productsRef.get().await()
            println("DEBUG: Firebase snapshot exists: ${snapshot.exists()}")
            println("DEBUG: Firebase snapshot children count: ${snapshot.childrenCount}")
            println("DEBUG: Firebase snapshot value: ${snapshot.value}")
            
            if (!snapshot.exists()) {
                println("DEBUG: No products collection found in Firebase - this is normal for first run")
                return Result.success(emptyList())
            }
            
            val products = snapshot.children.mapNotNull { childSnapshot ->
                println("DEBUG: Processing child snapshot: ${childSnapshot.key}")
                println("DEBUG: Child snapshot value: ${childSnapshot.value}")
                val product = childSnapshot.toProduct()
                if (product != null) {
                    println("DEBUG: Successfully loaded product: ${product.name} (ID: ${product.id})")
                } else {
                    println("DEBUG: Failed to parse product from snapshot: ${childSnapshot.key}")
                    println("DEBUG: Raw child data: ${childSnapshot.value}")
                }
                product
            }
            
            println("DEBUG: Successfully loaded ${products.size} products from Firebase")
            Result.success(products)
        } catch (e: Exception) {
            println("DEBUG: Error loading products: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Update product
     */
    suspend fun updateProduct(product: Product): Result<Product> {
        return try {
            val updates = mapOf(
                "name" to product.name,
                "price" to product.price.toString(),
                "quantity" to product.quantity,
                "cost" to product.cost.toString(),
                "shelfLocation" to product.shelfLocation,
                "updatedAt" to System.currentTimeMillis()
            )
            
            productsRef.child(product.id.toString()).updateChildren(updates).await()
            Result.success(product)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update product quantity (for stock management)
     */
    suspend fun updateProductQuantity(productId: Long, newQuantity: Int): Result<Unit> {
        return try {
            val updates = mapOf(
                "quantity" to newQuantity,
                "updatedAt" to System.currentTimeMillis()
            )
            
            productsRef.child(productId.toString()).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete product
     */
    suspend fun deleteProduct(productId: Long): Result<Unit> {
        return try {
            productsRef.child(productId.toString()).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Search products by name or shelf location
     */
    suspend fun searchProducts(query: String): Result<List<Product>> {
        return try {
            val snapshot = productsRef.get().await()
            val products = snapshot.children.mapNotNull { it.toProduct() }
                .filter { product ->
                    product.name.contains(query, ignoreCase = true) ||
                    (product.shelfLocation?.contains(query, ignoreCase = true) == true)
                }
            Result.success(products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // getProductsByCategory method removed - Product model doesn't have category field

    /**
     * Get products with low stock (quantity <= threshold)
     */
    suspend fun getLowStockProducts(threshold: Int = 5): Result<List<Product>> {
        return try {
            val snapshot = productsRef.get().await()
            val products = snapshot.children.mapNotNull { it.toProduct() }
                .filter { it.quantity <= threshold }
            Result.success(products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observe all products in real-time
     */
    fun observeAllProducts(): Flow<List<Product>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val products = snapshot.children.mapNotNull { it.toProduct() }
                trySend(products)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        productsRef.addValueEventListener(listener)
        awaitClose { productsRef.removeEventListener(listener) }
    }

    /**
     * Observe specific product in real-time
     */
    fun observeProduct(productId: Long): Flow<Product?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val product = snapshot.toProduct()
                trySend(product)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        productsRef.child(productId.toString()).addValueEventListener(listener)
        awaitClose { productsRef.child(productId.toString()).removeEventListener(listener) }
    }

    // observeProductsByCategory method removed - Product model doesn't have category field

    /**
     * Initialize sample products (for first-time setup)
     */
    suspend fun initializeSampleProducts(): Result<Unit> {
        return try {
            val existingProducts = getAllProducts()
            if (existingProducts.isSuccess && existingProducts.getOrNull()?.isEmpty() == true) {
                val sampleProducts = listOf(
                    Product(
                        id = 0L,
                        name = "Tesla Model S Door Handle",
                        quantity = 5,
                        cost = BigDecimal("30.00"),
                        price = BigDecimal("45.00"),
                        shelfLocation = "A1"
                    ),
                    Product(
                        id = 0L,
                        name = "Tesla Model 3 Screen Protector",
                        quantity = 10,
                        cost = BigDecimal("8.00"),
                        price = BigDecimal("15.00"),
                        shelfLocation = "B2"
                    ),
                    Product(
                        id = 0L,
                        name = "Tesla Charging Cable",
                        quantity = 3,
                        cost = BigDecimal("80.00"),
                        price = BigDecimal("120.00"),
                        shelfLocation = "C3"
                    ),
                    Product(
                        id = 0L,
                        name = "Tesla Model Y Floor Mats",
                        quantity = 8,
                        cost = BigDecimal("40.00"),
                        price = BigDecimal("65.00"),
                        shelfLocation = "D4"
                    ),
                    Product(
                        id = 0L,
                        name = "Tesla Wireless Charger",
                        quantity = 6,
                        cost = BigDecimal("55.00"),
                        price = BigDecimal("85.00"),
                        shelfLocation = "E5"
                    )
                )
                
                sampleProducts.forEach { product ->
                    createProduct(product)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generate next product ID
     */
    private suspend fun getNextProductId(): Long {
        return try {
            val snapshot = productsRef.get().await()
            val maxId = snapshot.children.mapNotNull { 
                it.child("id").getValue(Long::class.java) 
            }.maxOrNull() ?: 0L
            maxId + 1
        } catch (e: Exception) {
            System.currentTimeMillis() // Fallback to timestamp
        }
    }

    /**
     * Reduce product stock when order is placed
     */
    suspend fun reduceProductStock(productId: Long, quantity: Int): Result<Unit> {
        return try {
            println("DEBUG: Reducing stock for product $productId by $quantity")
            
            // Get current product data
            val snapshot = productsRef.child(productId.toString()).get().await()
            val product = snapshot.toProduct()
            
            if (product == null) {
                println("DEBUG: Product $productId not found")
                return Result.failure(Exception("Product not found"))
            }
            
            // Check if sufficient stock available
            if (product.quantity < quantity) {
                println("DEBUG: Insufficient stock for product $productId. Available: ${product.quantity}, Requested: $quantity")
                return Result.failure(Exception("Insufficient stock. Available: ${product.quantity}, Requested: $quantity"))
            }
            
            // Calculate new quantity
            val newQuantity = product.quantity - quantity
            println("DEBUG: Updating product $productId stock from ${product.quantity} to $newQuantity")
            
            // Update the quantity in Firebase
            productsRef.child(productId.toString()).child("quantity").setValue(newQuantity).await()
            
            println("DEBUG: Successfully reduced stock for product $productId")
            Result.success(Unit)
        } catch (e: Exception) {
            println("DEBUG: Exception reducing stock for product $productId: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Extension function to convert DataSnapshot to Product
     */
    private fun DataSnapshot.toProduct(): Product? {
        return try {
            val id = child("id").getValue(Long::class.java) ?: return null
            val name = child("name").getValue(String::class.java) ?: return null
            val priceString = child("price").getValue(String::class.java) ?: return null
            val quantity = child("quantity").getValue(Int::class.java) ?: 0
            val costString = child("cost").getValue(String::class.java) ?: "0.00"
            val shelfLocation = child("shelfLocation").getValue(String::class.java)

            Product(
                id = id,
                name = name,
                quantity = quantity,
                cost = BigDecimal(costString),
                price = BigDecimal(priceString),
                shelfLocation = shelfLocation
            )
        } catch (e: Exception) {
            null
        }
    }
}
