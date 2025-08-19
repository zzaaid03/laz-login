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
import com.laz.repositories.FirebaseRealtimePotentialOrderRepository
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import com.laz.services.PartIdentificationResult
import java.util.Date

class TeslaPartsAIViewModel(
    private val aiService: OpenRouterAIService = OpenRouterAIService(),
    private val potentialOrderRepository: FirebaseRealtimePotentialOrderRepository = FirebaseRealtimePotentialOrderRepository(),
    private val firebaseStorage: FirebaseStorage = FirebaseStorage.getInstance()
) : ViewModel() {

    private val _chatMessages = MutableStateFlow<List<AIChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<AIChatMessage>> = _chatMessages.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _currentChatSession = MutableStateFlow<String?>(null)
    val currentChatSession: StateFlow<String?> = _currentChatSession.asStateFlow()

    private val _priceEstimate = MutableStateFlow<Double>(0.0)
    val priceEstimate: StateFlow<Double> = _priceEstimate.asStateFlow()

    private val _currentPartIdentification = MutableStateFlow<PartIdentificationResult?>(null)
    val currentPartIdentification: StateFlow<PartIdentificationResult?> = _currentPartIdentification.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun startNewChatSession(customerId: String): String {
        val sessionId = "chat_${System.currentTimeMillis()}_${customerId}"
        _currentChatSession.value = sessionId
        _chatMessages.value = emptyList()
        _priceEstimate.value = 0.0
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
            _priceEstimate.value = partIdentification.globalPriceEstimate
            
            addBotMessage("I've identified this as: ${partIdentification.partName}. Let me provide you with pricing information...")

            // Step 2: Generate customer response with AI-generated pricing
            val aiResponse = aiService.generateCustomerResponse(partIdentification, message)
            addBotMessage(aiResponse)
            
            // Add pricing summary message
            val pricingSummary = buildPricingSummaryMessage(partIdentification)
            addBotMessage(pricingSummary)

        } catch (e: Exception) {
            addBotMessage("I encountered an error while processing your request. Please try again or contact support.")
            _errorMessage.value = e.message
        }
    }

    private fun buildPricingSummaryMessage(partIdentification: PartIdentificationResult): String {
        val builder = StringBuilder()
        builder.append("💰 **Pricing Summary:**\n\n")
        builder.append("📦 **${partIdentification.partName}**\n")
        builder.append("💵 Estimated Price: **${partIdentification.globalPriceEstimate} JOD**\n")
        builder.append("📊 Price Range: ${partIdentification.priceRange}\n")
        builder.append("⭐ Quality Level: ${partIdentification.qualityLevel}\n")
        builder.append("🚚 Delivery Time: ${partIdentification.deliveryEstimate}\n\n")
        builder.append("✅ *Price includes shipping, handling, and service fee*\n\n")
        builder.append("Would you like me to create a potential order for this part? Just confirm and I'll prepare the order details for our team to review!")
        return builder.toString()
    }

    fun createPotentialOrder(customerId: String, customerName: String, quantity: Int = 1) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                
                val partIdentification = _currentPartIdentification.value
                
                if (partIdentification == null) {
                    _errorMessage.value = "No part identified. Please start a new search."
                    return@launch
                }

                val requestedPart = RequestedPart(
                    id = UUID.randomUUID().toString(),
                    partName = partIdentification.partName,
                    description = partIdentification.description,
                    customerImages = _chatMessages.value.flatMap { it.imageUrls },
                    aliexpressLinks = emptyList(), // No longer using AliExpress
                    selectedProduct = null, // Using AI pricing instead
                    quantity = quantity,
                    estimatedCost = partIdentification.globalPriceEstimate * 0.8, // Estimated cost without margin
                    sellingPrice = partIdentification.globalPriceEstimate,
                    priceRange = partIdentification.priceRange,
                    deliveryEstimate = partIdentification.deliveryEstimate,
                    qualityLevel = partIdentification.qualityLevel
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
        private val potentialOrderRepository: FirebaseRealtimePotentialOrderRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TeslaPartsAIViewModel::class.java)) {
                return TeslaPartsAIViewModel(aiService, potentialOrderRepository) as T
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
