package com.laz.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.laz.models.*
import com.laz.services.OpenRouterAIService
import com.laz.services.AliExpressSearchService
import com.laz.repositories.FirebaseRealtimePotentialOrderRepository
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import com.laz.services.PartIdentificationResult
import java.util.Date

class TeslaPartsAIViewModel(
    private val aiService: OpenRouterAIService = OpenRouterAIService(),
    private val aliExpressService: AliExpressSearchService = AliExpressSearchService(),
    private val potentialOrderRepository: FirebaseRealtimePotentialOrderRepository = FirebaseRealtimePotentialOrderRepository(),
    private val firebaseStorage: FirebaseStorage = FirebaseStorage.getInstance()
) : ViewModel() {

    private val _chatMessages = MutableStateFlow<List<AIChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<AIChatMessage>> = _chatMessages.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _currentChatSession = MutableStateFlow<String?>(null)
    val currentChatSession: StateFlow<String?> = _currentChatSession.asStateFlow()

    private val _foundProducts = MutableStateFlow<List<AliexpressProduct>>(emptyList())
    val foundProducts: StateFlow<List<AliexpressProduct>> = _foundProducts.asStateFlow()

    private val _currentPartIdentification = MutableStateFlow<PartIdentificationResult?>(null)
    val currentPartIdentification: StateFlow<PartIdentificationResult?> = _currentPartIdentification.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun startNewChatSession(customerId: String): String {
        val sessionId = "chat_${System.currentTimeMillis()}_${customerId}"
        _currentChatSession.value = sessionId
        _chatMessages.value = emptyList()
        _foundProducts.value = emptyList()
        _currentPartIdentification.value = null
        _errorMessage.value = null
        
        // Send welcome message
        addBotMessage("Hello! I'm your Tesla parts specialist. I can help you find Tesla parts and accessories. Please describe what you're looking for or upload a photo of the part you need.")
        
        return sessionId
    }

    fun sendMessage(customerId: String, message: String, imageUris: List<Uri> = emptyList()) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                _errorMessage.value = null

                // Add customer message to chat
                val customerMessage = AIChatMessage(
                    id = UUID.randomUUID().toString(),
                    senderId = customerId,
                    senderType = MessageSenderType.CUSTOMER,
                    message = message,
                    timestamp = System.currentTimeMillis()
                )
                addMessageToChat(customerMessage)

                // Upload images if any
                val imageUrls = mutableListOf<String>()
                imageUris.forEach { uri ->
                    try {
                        val imageUrl = uploadImageToFirebase(uri, customerId)
                        imageUrls.add(imageUrl)
                    } catch (e: Exception) {
                        Log.e("TeslaPartsAI", "Failed to upload image", e)
                        // Continue with other images
                    }
                }

                // Update customer message with image URLs
                if (imageUrls.isNotEmpty()) {
                    val updatedMessage = customerMessage.copy(imageUrls = imageUrls)
                    updateMessageInChat(updatedMessage)
                }

                // Process with AI
                processCustomerRequest(customerId, message, imageUrls)

            } catch (e: Exception) {
                _errorMessage.value = "Error processing message: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    private suspend fun processCustomerRequest(customerId: String, message: String, imageUrls: List<String>) {
        try {
            // Step 1: Identify the Tesla part using AI
            addBotMessage("Let me analyze your request and identify the Tesla part...")
            
            val partIdentification = aiService.identifyTeslaPart(message, imageUrls)
            
            if (!partIdentification.success) {
                addBotMessage("I'm having trouble identifying the part you're looking for. Could you provide more details or a clearer description?")
                return
            }

            _currentPartIdentification.value = partIdentification
            addBotMessage("I've identified this as: ${partIdentification.partName}. Let me search for available options...")

            // Step 2: Search AliExpress for products using proper search terms
            val searchTerms = if (partIdentification.searchTerms.isNotEmpty()) {
                partIdentification.searchTerms
            } else {
                // Fallback: use part name and category
                listOf(partIdentification.partName, partIdentification.category).filter { it.isNotBlank() }
            }
            
            val products = aliExpressService.searchProducts(searchTerms, maxResults = 5)
            
            if (products.isEmpty()) {
                addBotMessage("I couldn't find any matching products right now. Please try a different description or contact our support team.")
                return
            }

            _foundProducts.value = products
            
            // Step 3: Generate customer response with AI
            val aiResponse = aiService.generateCustomerResponse(partIdentification, products, message)
            addBotMessage(aiResponse)
            
            // Add product options message
            val productOptionsMessage = buildProductOptionsMessage(products)
            addBotMessage(productOptionsMessage)

        } catch (e: Exception) {
            addBotMessage("I encountered an error while processing your request. Please try again or contact support.")
            _errorMessage.value = e.message
        }
    }

    private fun buildProductOptionsMessage(products: List<AliexpressProduct>): String {
        val builder = StringBuilder()
        builder.append("Here are the best options I found:\n\n")
        
        products.take(3).forEachIndexed { index, product ->
            builder.append("${index + 1}. ${product.title}\n")
            builder.append("   Price: ${product.totalCost} JOD (includes shipping & service fee)\n")
            builder.append("   Rating: ${product.rating}/5 ⭐\n")
            builder.append("   Delivery: ${product.deliveryTime}\n\n")
        }
        
        builder.append("Would you like me to create a potential order for any of these options? Just let me know which one interests you!")
        return builder.toString()
    }

    fun createPotentialOrder(customerId: String, customerName: String, selectedProductIndex: Int, quantity: Int = 1) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                
                val selectedProduct = _foundProducts.value.getOrNull(selectedProductIndex)
                val partIdentification = _currentPartIdentification.value
                
                if (selectedProduct == null || partIdentification == null) {
                    _errorMessage.value = "Please select a valid product option"
                    return@launch
                }

                val requestedPart = RequestedPart(
                    id = UUID.randomUUID().toString(),
                    partName = partIdentification.partName,
                    description = partIdentification.description,
                    customerImages = _chatMessages.value.flatMap { it.imageUrls },
                    aliexpressLinks = _foundProducts.value,
                    selectedProduct = selectedProduct,
                    quantity = quantity,
                    estimatedCost = selectedProduct.price + selectedProduct.shippingCost,
                    sellingPrice = selectedProduct.totalCost
                )

                val potentialOrder = PotentialOrder(
                    customerId = customerId,
                    customerName = customerName,
                    chatSessionId = _currentChatSession.value ?: "",
                    requestedParts = listOf(requestedPart),
                    chatHistory = _chatMessages.value,
                    totalEstimatedCost = requestedPart.estimatedCost,
                    totalSellingPrice = requestedPart.sellingPrice,
                    createdAt = Date()
                )

                val result = potentialOrderRepository.createPotentialOrder(potentialOrder)
                result.onSuccess { orderId ->
                    addBotMessage("Perfect! I've created a potential order for you. Our team will review it and get back to you soon with availability and final details. Order reference: #${orderId.takeLast(8)}")
                }.onFailure { error ->
                    _errorMessage.value = "Failed to create order: ${error.message}"
                    addBotMessage("I'm sorry, there was an issue creating your order. Please try again or contact our support team.")
                }

            } catch (e: Exception) {
                _errorMessage.value = "Error creating potential order: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    private fun addBotMessage(message: String) {
        val botMessage = AIChatMessage(
            id = UUID.randomUUID().toString(),
            senderId = "ai_bot",
            senderType = MessageSenderType.AI_BOT,
            message = message,
            timestamp = System.currentTimeMillis(),
            aiProcessed = true
        )
        addMessageToChat(botMessage)
    }

    private fun addMessageToChat(message: AIChatMessage) {
        _chatMessages.value = _chatMessages.value + message
    }

    private fun updateMessageInChat(updatedMessage: AIChatMessage) {
        _chatMessages.value = _chatMessages.value.map { 
            if (it.id == updatedMessage.id) updatedMessage else it 
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    class Factory(
        private val aiService: OpenRouterAIService,
        private val aliExpressService: AliExpressSearchService,
        private val potentialOrderRepository: FirebaseRealtimePotentialOrderRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TeslaPartsAIViewModel::class.java)) {
                return TeslaPartsAIViewModel(aiService, aliExpressService, potentialOrderRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
    
    private suspend fun uploadImageToFirebase(imageUri: Uri, customerId: String): String {
        return try {
            val fileName = "ai_chat_images/${customerId}/${UUID.randomUUID()}.jpg"
            val storageRef = firebaseStorage.reference.child(fileName)
            
            val uploadTask = storageRef.putFile(imageUri).await()
            val downloadUrl = storageRef.downloadUrl.await()
            
            Log.d("TeslaPartsAI", "Image uploaded successfully: $downloadUrl")
            downloadUrl.toString()
        } catch (e: Exception) {
            Log.e("TeslaPartsAI", "Failed to upload image to Firebase", e)
            throw e
        }
    }
}
