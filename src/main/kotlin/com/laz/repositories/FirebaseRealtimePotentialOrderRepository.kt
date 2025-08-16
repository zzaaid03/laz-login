package com.laz.repositories

import android.util.Log
import com.google.firebase.database.*
import com.laz.models.PotentialOrder
import com.laz.models.PotentialOrderStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirebaseRealtimePotentialOrderRepository {
    private val database = FirebaseDatabase.getInstance()
    private val potentialOrdersRef = database.getReference("potential_orders")
    
    suspend fun createPotentialOrder(potentialOrder: PotentialOrder): Result<String> {
        return try {
            val orderId = getNextOrderId()
            val orderWithId = potentialOrder.copy(
                id = orderId,
                createdAt = Date(),
                updatedAt = Date()
            )
            
            val orderMap = mapOf(
                "id" to orderWithId.id,
                "customerId" to orderWithId.customerId,
                "customerName" to orderWithId.customerName,
                "chatSessionId" to orderWithId.chatSessionId,
                "status" to orderWithId.status.name,
                "requestedParts" to orderWithId.requestedParts.map { part ->
                    mapOf(
                        "id" to part.id,
                        "partName" to part.partName,
                        "description" to part.description,
                        "customerImages" to part.customerImages,
                        "quantity" to part.quantity,
                        "estimatedCost" to part.estimatedCost,
                        "sellingPrice" to part.sellingPrice,
                        "selectedProduct" to part.selectedProduct?.let { product ->
                            mapOf(
                                "productId" to product.productId,
                                "title" to product.title,
                                "price" to product.price,
                                "shippingCost" to product.shippingCost,
                                "totalCost" to product.totalCost,
                                "productUrl" to product.productUrl,
                                "imageUrl" to product.imageUrl,
                                "rating" to product.rating,
                                "soldCount" to product.soldCount,
                                "deliveryTime" to product.deliveryTime
                            )
                        }
                    )
                },
                "chatHistory" to orderWithId.chatHistory.map { message ->
                    mapOf(
                        "id" to message.id,
                        "senderId" to message.senderId,
                        "senderType" to message.senderType.name,
                        "message" to message.message,
                        "imageUrls" to message.imageUrls,
                        "timestamp" to message.timestamp,
                        "aiProcessed" to message.aiProcessed,
                        "aiResponse" to message.aiResponse
                    )
                },
                "totalEstimatedCost" to orderWithId.totalEstimatedCost,
                "totalSellingPrice" to orderWithId.totalSellingPrice,
                "profitMargin" to orderWithId.profitMargin,
                "adminNotes" to orderWithId.adminNotes,
                "createdAt" to orderWithId.createdAt.time,
                "updatedAt" to orderWithId.updatedAt.time
            )
            
            potentialOrdersRef.child(orderId).setValue(orderMap).await()
            Log.d("PotentialOrderRepo", "Created potential order: $orderId")
            Result.success(orderId)
        } catch (e: Exception) {
            Log.e("PotentialOrderRepo", "Error creating potential order", e)
            Result.failure(e)
        }
    }
    
    suspend fun updatePotentialOrder(potentialOrder: PotentialOrder): Result<Unit> {
        return try {
            val updatedOrder = potentialOrder.copy(updatedAt = Date())
            val orderMap = mapOf(
                "id" to updatedOrder.id,
                "customerId" to updatedOrder.customerId,
                "customerName" to updatedOrder.customerName,
                "chatSessionId" to updatedOrder.chatSessionId,
                "status" to updatedOrder.status.name,
                "requestedParts" to updatedOrder.requestedParts.map { part ->
                    mapOf(
                        "id" to part.id,
                        "partName" to part.partName,
                        "description" to part.description,
                        "customerImages" to part.customerImages,
                        "quantity" to part.quantity,
                        "estimatedCost" to part.estimatedCost,
                        "sellingPrice" to part.sellingPrice,
                        "selectedProduct" to part.selectedProduct?.let { product ->
                            mapOf(
                                "productId" to product.productId,
                                "title" to product.title,
                                "price" to product.price,
                                "shippingCost" to product.shippingCost,
                                "totalCost" to product.totalCost,
                                "productUrl" to product.productUrl,
                                "imageUrl" to product.imageUrl,
                                "rating" to product.rating,
                                "soldCount" to product.soldCount,
                                "deliveryTime" to product.deliveryTime
                            )
                        }
                    )
                },
                "chatHistory" to updatedOrder.chatHistory.map { message ->
                    mapOf(
                        "id" to message.id,
                        "senderId" to message.senderId,
                        "senderType" to message.senderType.name,
                        "message" to message.message,
                        "imageUrls" to message.imageUrls,
                        "timestamp" to message.timestamp,
                        "aiProcessed" to message.aiProcessed,
                        "aiResponse" to message.aiResponse
                    )
                },
                "totalEstimatedCost" to updatedOrder.totalEstimatedCost,
                "totalSellingPrice" to updatedOrder.totalSellingPrice,
                "profitMargin" to updatedOrder.profitMargin,
                "adminNotes" to updatedOrder.adminNotes,
                "createdAt" to updatedOrder.createdAt.time,
                "updatedAt" to updatedOrder.updatedAt.time
            )
            
            potentialOrdersRef.child(potentialOrder.id).setValue(orderMap).await()
            Log.d("PotentialOrderRepo", "Updated potential order: ${potentialOrder.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PotentialOrderRepo", "Error updating potential order", e)
            Result.failure(e)
        }
    }
    
    suspend fun getPotentialOrder(orderId: String): Result<PotentialOrder?> {
        return try {
            val snapshot = potentialOrdersRef.child(orderId).get().await()
            val order = snapshot.toPotentialOrder()
            Result.success(order)
        } catch (e: Exception) {
            Log.e("PotentialOrderRepo", "Error getting potential order", e)
            Result.failure(e)
        }
    }
    
    fun getPotentialOrdersByCustomer(customerId: String): Flow<List<PotentialOrder>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val orders = snapshot.children.mapNotNull { it.toPotentialOrder() }
                    .filter { it.customerId == customerId }
                    .sortedByDescending { it.createdAt }
                trySend(orders)
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("PotentialOrderRepo", "Error listening to customer orders: ${error.message}")
                close(Exception(error.message))
            }
        }
        
        potentialOrdersRef.addValueEventListener(listener)
        awaitClose { potentialOrdersRef.removeEventListener(listener) }
    }
    
    fun getPendingPotentialOrders(): Flow<List<PotentialOrder>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val orders = snapshot.children.mapNotNull { it.toPotentialOrder() }
                    .filter { it.status == PotentialOrderStatus.PENDING }
                    .sortedByDescending { it.createdAt }
                trySend(orders)
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("PotentialOrderRepo", "Error listening to pending orders: ${error.message}")
                close(Exception(error.message))
            }
        }
        
        potentialOrdersRef.addValueEventListener(listener)
        awaitClose { potentialOrdersRef.removeEventListener(listener) }
    }
    
    fun getAllPotentialOrders(): Flow<List<PotentialOrder>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val orders = snapshot.children.mapNotNull { it.toPotentialOrder() }
                    .sortedByDescending { it.createdAt }
                trySend(orders)
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("PotentialOrderRepo", "Error listening to all orders: ${error.message}")
                close(Exception(error.message))
            }
        }
        
        potentialOrdersRef.addValueEventListener(listener)
        awaitClose { potentialOrdersRef.removeEventListener(listener) }
    }
    
    suspend fun updateOrderStatus(orderId: String, status: PotentialOrderStatus, adminNotes: String = ""): Result<Unit> {
        return try {
            val updates = mapOf(
                "status" to status.name,
                "adminNotes" to adminNotes,
                "updatedAt" to Date().time
            )
            
            potentialOrdersRef.child(orderId).updateChildren(updates).await()
            Log.d("PotentialOrderRepo", "Updated order status: $orderId -> $status")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PotentialOrderRepo", "Error updating order status", e)
            Result.failure(e)
        }
    }
    
    suspend fun deletePotentialOrder(orderId: String): Result<Unit> {
        return try {
            potentialOrdersRef.child(orderId).removeValue().await()
            Log.d("PotentialOrderRepo", "Deleted potential order: $orderId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PotentialOrderRepo", "Error deleting potential order", e)
            Result.failure(e)
        }
    }
    
    suspend fun getOrdersByStatus(status: PotentialOrderStatus): Result<List<PotentialOrder>> {
        return try {
            val snapshot = potentialOrdersRef.get().await()
            val orders = snapshot.children.mapNotNull { it.toPotentialOrder() }
                .filter { it.status == status }
                .sortedByDescending { it.createdAt }
            Result.success(orders)
        } catch (e: Exception) {
            Log.e("PotentialOrderRepo", "Error getting orders by status", e)
            Result.failure(e)
        }
    }
    
    private suspend fun getNextOrderId(): String {
        return try {
            val snapshot = potentialOrdersRef.get().await()
            val maxId = snapshot.children.mapNotNull { 
                it.child("id").getValue(String::class.java)?.removePrefix("PO")?.toIntOrNull()
            }.maxOrNull() ?: 0
            "PO${String.format("%06d", maxId + 1)}"
        } catch (e: Exception) {
            "PO${String.format("%06d", System.currentTimeMillis() % 1000000)}"
        }
    }
    
    private fun DataSnapshot.toPotentialOrder(): PotentialOrder? {
        return try {
            val id = child("id").getValue(String::class.java) ?: return null
            val customerId = child("customerId").getValue(String::class.java) ?: return null
            val customerName = child("customerName").getValue(String::class.java) ?: return null
            val chatSessionId = child("chatSessionId").getValue(String::class.java) ?: ""
            
            val requestedParts = child("requestedParts").children.mapNotNull { partSnapshot ->
                try {
                    val partId = partSnapshot.child("id").getValue(String::class.java) ?: ""
                    val partName = partSnapshot.child("partName").getValue(String::class.java) ?: return@mapNotNull null
                    val description = partSnapshot.child("description").getValue(String::class.java) ?: ""
                    val customerImages = partSnapshot.child("customerImages").children.mapNotNull { it.getValue(String::class.java) }
                    val quantity = partSnapshot.child("quantity").getValue(Int::class.java) ?: return@mapNotNull null
                    val estimatedCost = partSnapshot.child("estimatedCost").getValue(Double::class.java) ?: return@mapNotNull null
                    val sellingPrice = partSnapshot.child("sellingPrice").getValue(Double::class.java) ?: return@mapNotNull null
                    
                    val selectedProduct = partSnapshot.child("selectedProduct").takeIf { it.exists() }?.let { productSnapshot ->
                        try {
                            com.laz.models.AliexpressProduct(
                                productId = productSnapshot.child("productId").getValue(String::class.java) ?: "",
                                title = productSnapshot.child("title").getValue(String::class.java) ?: "",
                                price = productSnapshot.child("price").getValue(Double::class.java) ?: 0.0,
                                shippingCost = productSnapshot.child("shippingCost").getValue(Double::class.java) ?: 0.0,
                                totalCost = productSnapshot.child("totalCost").getValue(Double::class.java) ?: 0.0,
                                productUrl = productSnapshot.child("productUrl").getValue(String::class.java) ?: "",
                                imageUrl = productSnapshot.child("imageUrl").getValue(String::class.java) ?: "",
                                rating = productSnapshot.child("rating").getValue(Double::class.java) ?: 0.0,
                                soldCount = productSnapshot.child("soldCount").getValue(Int::class.java) ?: 0,
                                deliveryTime = productSnapshot.child("deliveryTime").getValue(String::class.java) ?: ""
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    
                    com.laz.models.RequestedPart(
                        id = partId,
                        partName = partName,
                        description = description,
                        customerImages = customerImages,
                        aliexpressLinks = emptyList(), // Not stored in DB
                        selectedProduct = selectedProduct,
                        quantity = quantity,
                        estimatedCost = estimatedCost,
                        sellingPrice = sellingPrice
                    )
                } catch (e: Exception) {
                    null
                }
            }
            
            val chatHistory = child("chatHistory").children.mapNotNull { messageSnapshot ->
                try {
                    val messageId = messageSnapshot.child("id").getValue(String::class.java) ?: return@mapNotNull null
                    val senderId = messageSnapshot.child("senderId").getValue(String::class.java) ?: return@mapNotNull null
                    val message = messageSnapshot.child("message").getValue(String::class.java) ?: return@mapNotNull null
                    val senderType = messageSnapshot.child("senderType").getValue(String::class.java)?.let { 
                        com.laz.models.MessageSenderType.valueOf(it) 
                    } ?: return@mapNotNull null
                    val timestamp = messageSnapshot.child("timestamp").getValue(Long::class.java) ?: return@mapNotNull null
                    val imageUrls = messageSnapshot.child("imageUrls").children.mapNotNull { it.getValue(String::class.java) }
                    val aiProcessed = messageSnapshot.child("aiProcessed").getValue(Boolean::class.java) ?: false
                    val aiResponse = messageSnapshot.child("aiResponse").getValue(String::class.java) ?: ""
                    
                    com.laz.models.AIChatMessage(
                        id = messageId,
                        senderId = senderId,
                        senderType = senderType,
                        message = message,
                        imageUrls = imageUrls,
                        timestamp = timestamp,
                        aiProcessed = aiProcessed,
                        aiResponse = aiResponse
                    )
                } catch (e: Exception) {
                    null
                }
            }
            
            val totalEstimatedCost = child("totalEstimatedCost").getValue(Double::class.java) ?: 0.0
            val totalSellingPrice = child("totalSellingPrice").getValue(Double::class.java) ?: 0.0
            val profitMargin = child("profitMargin").getValue(Double::class.java) ?: 0.2
            val status = child("status").getValue(String::class.java)?.let { 
                PotentialOrderStatus.valueOf(it) 
            } ?: PotentialOrderStatus.PENDING
            val adminNotes = child("adminNotes").getValue(String::class.java) ?: ""
            val createdAt = child("createdAt").getValue(Long::class.java)?.let { Date(it) } ?: Date()
            val updatedAt = child("updatedAt").getValue(Long::class.java)?.let { Date(it) } ?: Date()
            
            PotentialOrder(
                id = id,
                customerId = customerId,
                customerName = customerName,
                chatSessionId = chatSessionId,
                status = status,
                requestedParts = requestedParts,
                chatHistory = chatHistory,
                totalEstimatedCost = totalEstimatedCost,
                totalSellingPrice = totalSellingPrice,
                profitMargin = profitMargin,
                adminNotes = adminNotes,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        } catch (e: Exception) {
            Log.e("PotentialOrderRepo", "Error parsing potential order", e)
            null
        }
    }
}
