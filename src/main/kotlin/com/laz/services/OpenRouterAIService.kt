package com.laz.services

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerialName
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

@Serializable
data class OpenRouterRequest(
    val model: String,
    val messages: List<OpenRouterRequestMessage>,
    val max_tokens: Int = 1000,
    val temperature: Double = 0.3
)

@Serializable
data class OpenRouterRequestMessage(
    val role: String,
    val content: List<OpenRouterContent>
)

@Serializable
data class OpenRouterResponseMessage(
    val role: String,
    val content: String
)

@Serializable
data class OpenRouterContent(
    val type: String,
    val text: String? = null,
    val image_url: OpenRouterImageUrl? = null
)

@Serializable
data class OpenRouterImageUrl(
    val url: String
)

@Serializable
data class OpenRouterResponse(
    val choices: List<OpenRouterChoice>
)

@Serializable
data class OpenRouterChoice(
    val message: OpenRouterResponseMessage
)

@Serializable
data class SpecificationsObject(
    @SerialName("modelYear") val modelYear: String = "",
    @SerialName("side") val side: String = "",
    @SerialName("finishPreference") val finishPreference: String = "",
    @SerialName("installationType") val installationType: String = ""
)

@Serializable
data class ColorFinishObject(
    @SerialName("material") val material: String = "",
    @SerialName("pattern") val pattern: String = "",
    @SerialName("finish") val finish: String = ""
)

@Serializable
data class AIPartIdentification(
    @SerialName("partName") val partName: String,
    @SerialName("category") val category: String,
    @SerialName("teslaModels") val teslaModels: List<String>,
    @SerialName("confidence") val confidence: Double,
    @SerialName("description") val description: String,
    @SerialName("specifications") val specifications: List<String> = emptyList(),
    @SerialName("language") val language: String = "en",
    @SerialName("photoAnalysis") val photoAnalysis: String = "",
    @SerialName("damageAssessment") val damageAssessment: String = "",
    @SerialName("colorFinish") val colorFinish: ColorFinishObject = ColorFinishObject(),
    @SerialName("estimatedSize") val estimatedSize: String = "",
    @SerialName("globalPriceEstimate") val globalPriceEstimate: Double,
    @SerialName("priceRange") val priceRange: String = "",
    @SerialName("deliveryEstimate") val deliveryEstimate: String = "",
    @SerialName("qualityLevel") val qualityLevel: String = ""
)

class OpenRouterAIService {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    
    private val baseUrl = "https://openrouter.ai/api/v1/chat/completions"
    private val secureConfig = com.laz.config.SecureConfig.getInstance()
    private val defaultModel = "anthropic/claude-3-5-sonnet"
    
    init {
        val apiKey = secureConfig.getOpenRouterApiKey()
        android.util.Log.d("OpenRouterAI", "API Key loaded: ${if (apiKey.isNotBlank()) "✅ Valid" else "❌ Empty"}")
        android.util.Log.d("OpenRouterAI", "API Key length: ${apiKey.length}")
        android.util.Log.d("OpenRouterAI", "API Key starts with: ${apiKey.take(10)}...")
    }
    
    suspend fun identifyTeslaPart(
        customerMessage: String,
        imageUrls: List<String> = emptyList()
    ): PartIdentificationResult = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = buildTeslaPartsSystemPrompt()
            val userPrompt = buildPartIdentificationPrompt(customerMessage, imageUrls)
            
            val response = callOpenRouter(systemPrompt, userPrompt, imageUrls)
            parsePartIdentificationResponse(response)
        } catch (e: Exception) {
            Log.e("OpenRouterAI", "Error identifying Tesla part", e)
            PartIdentificationResult(
                success = false,
                error = "Failed to identify part: ${e.message}",
                partName = "",
                confidence = 0.0
            )
        }
    }
    
    suspend fun generateCustomerResponse(
        partIdentification: PartIdentificationResult,
        customerMessage: String
    ): String = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = buildCustomerResponseSystemPrompt()
            val userPrompt = buildCustomerResponsePrompt(partIdentification, customerMessage)
            
            callOpenRouter(systemPrompt, userPrompt)
        } catch (e: Exception) {
            Log.e("OpenRouterAI", "Error generating customer response", e)
            "I apologize, but I'm having trouble processing your request right now. Please try again or contact our support team."
        }
    }
    
    private suspend fun callOpenRouter(systemPrompt: String, userPrompt: String, imageUrls: List<String> = emptyList()): String {
        // Use vision model if images are provided
        val modelToUse = if (imageUrls.isNotEmpty()) "anthropic/claude-3-5-sonnet" else defaultModel
        
        val messages = mutableListOf<OpenRouterRequestMessage>()
        
        // System message
        messages.add(OpenRouterRequestMessage(
            role = "system",
            content = listOf(OpenRouterContent(type = "text", text = systemPrompt))
        ))
        
        // User message with text and images
        val userContent = mutableListOf<OpenRouterContent>()
        userContent.add(OpenRouterContent(type = "text", text = userPrompt))
        
        // Add images if provided
        imageUrls.forEach { imageUrl ->
            userContent.add(OpenRouterContent(
                type = "image_url",
                image_url = OpenRouterImageUrl(url = imageUrl)
            ))
        }
        
        messages.add(OpenRouterRequestMessage(
            role = "user",
            content = userContent
        ))
        
        val request = OpenRouterRequest(
            model = modelToUse,
            messages = messages,
            max_tokens = 1000,
            temperature = 0.3
        )
        
        val requestBodyString = json.encodeToString(OpenRouterRequest.serializer(), request)
        val requestBody = requestBodyString.toRequestBody("application/json".toMediaType())
        
        android.util.Log.d("OpenRouterAI", "Request model: $modelToUse")
        android.util.Log.d("OpenRouterAI", "Request body: $requestBodyString")
        android.util.Log.d("OpenRouterAI", "Image URLs count: ${imageUrls.size}")
        android.util.Log.d("OpenRouterAI", "Image URLs: ${imageUrls.joinToString()}")
        
        val httpRequest = Request.Builder()
            .url(baseUrl)
            .addHeader("Authorization", "Bearer ${secureConfig.getOpenRouterApiKey()}")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()
        
        val response = client.newCall(httpRequest).execute()
        val responseBody = response.body?.string() ?: throw IOException("Empty response body")
        
        android.util.Log.d("OpenRouterAI", "Response code: ${response.code}")
        android.util.Log.d("OpenRouterAI", "Response body: $responseBody")
        
        if (!response.isSuccessful) {
            throw IOException("OpenRouter API call failed: ${response.code} - $responseBody")
        }
        
        val openRouterResponse = json.decodeFromString(OpenRouterResponse.serializer(), responseBody)
        
        return openRouterResponse.choices.firstOrNull()?.message?.content 
            ?: throw IOException("No response content")
    }
    
    private fun buildTeslaPartsSystemPrompt(): String = """
        You are an expert Tesla and electric vehicle parts specialist who speaks both Arabic and English fluently.
        
        LANGUAGE SUPPORT: Respond in the same language the customer uses (Arabic or English). If mixed languages, prioritize Arabic.
        
        TESLA MODELS: Model S, Model 3, Model X, Model Y, Cybertruck, Roadster
        
        COMMON TESLA PARTS TO RECOGNIZE:
        
        EXTERIOR PARTS:
        - Side mirror caps (often carbon fiber or painted)
        - Door handles (flush or traditional)
        - Front/rear bumpers and splitters
        - Spoilers and wings
        - Trim pieces and emblems
        - Grilles and air intakes
        - Wheel covers and rims
        
        INTERIOR PARTS:
        - Center console pieces
        - Dashboard trim
        - Door panels and armrests
        - Steering wheel components
        - Roof console trim (map lights area)
        - Seat components
        - Floor mats and pedals
        
        CARBON FIBER PARTS (very common):
        - Mirror caps
        - Interior trim pieces
        - Spoilers and diffusers
        - Door sill plates
        - Steering wheel trim
        
        LIGHTING:
        - Headlights and taillights
        - LED strips and accent lighting
        - Interior ambient lighting
        
        PHOTO ANALYSIS EXPERTISE:
        When analyzing images, look for these specific details:
        
        1. PART IDENTIFICATION:
        - Look at shape, mounting points, and design
        - Check for Tesla-specific design elements
        - Identify if it's OEM or aftermarket
        
        2. MATERIAL & FINISH:
        - Carbon fiber (woven pattern, glossy finish)
        - Painted plastic (smooth, colored)
        - Chrome or metallic finishes
        - Matte or textured surfaces
        
        3. LOCATION CLUES:
        - Side mirrors (aerodynamic, camera integration)
        - Interior roof console (map lights, sunroof controls)
        - Dashboard areas (air vents, trim strips)
        - Door panels (window controls, armrests)
        
        4. CONDITION ASSESSMENT:
        - Scratches, cracks, or chips
        - Fading or discoloration
        - Missing pieces or broken clips
        - Wear patterns
        
        GLOBAL PRICING EXPERTISE:
        Provide accurate global market price estimates based on:
        - Part complexity and material (carbon fiber, painted plastic, OEM vs aftermarket)
        - Tesla model compatibility and rarity
        - Current global market trends for Tesla parts
        - Quality levels: Budget ($15-40), Mid-range ($40-80), Premium ($80-150+)
        - Include shipping and handling in estimates
        - Factor in 20% service margin for final customer price
        
        DELIVERY ESTIMATES:
        - Standard parts: 10-20 days
        - Custom/rare parts: 15-30 days
        - OEM parts: 20-35 days
        - Express shipping available: +$15-25
        
        CRITICAL: Respond with EXACTLY this JSON structure - no deviations:
{
    "partName": "string",
    "category": "string", 
    "teslaModels": ["Model 3", "Model Y"],
    "confidence": 0.95,
    "description": "string",
    "specifications": ["spec1", "spec2", "spec3"],
    "language": "en",
    "photoAnalysis": "string",
    "damageAssessment": "string",
    "colorFinish": {
        "material": "string",
        "pattern": "string", 
        "finish": "string"
    },
    "estimatedSize": "string",
    "globalPriceEstimate": 65.50,
    "priceRange": "$45-85 depending on quality",
    "deliveryEstimate": "15-25 days standard shipping",
    "qualityLevel": "Premium aftermarket quality"
}

MANDATORY RULES:
- colorFinish MUST be an object with material, pattern, finish fields
- specifications MUST be an array of strings
- globalPriceEstimate MUST be a number (final price including shipping + 20% margin)
- priceRange MUST show the range of available options
- deliveryEstimate MUST include realistic timeframes
- qualityLevel MUST indicate expected quality tier
- Do NOT add extra text after the JSON
- Do NOT use different field types than specified
    """.trimIndent()
    
    private fun buildPartIdentificationPrompt(
        customerMessage: String,
        imageUrls: List<String>
    ): String {
        val prompt = StringBuilder()
        prompt.append("Customer message: $customerMessage\n\n")
        
        if (imageUrls.isNotEmpty()) {
            prompt.append("PHOTO ANALYSIS REQUIRED:\n")
            prompt.append("I have uploaded ${imageUrls.size} image(s) showing Tesla parts. Please analyze these images carefully.\n")
            prompt.append("\nFor each image, identify:\n")
            prompt.append("- Exact part name and location\n")
            prompt.append("- Visible damage or wear\n")
            prompt.append("- Color and material\n")
            prompt.append("- Tesla model (if identifiable)\n")
            prompt.append("- Size/dimensions estimate\n")
            prompt.append("- Replacement urgency\n\n")
        }
        
        prompt.append("Detect the language used (Arabic or English) and identify the Tesla part.\n")
        prompt.append("Provide structured response in JSON format with all required fields.")
        return prompt.toString()
    }
    
    private fun buildCustomerResponseSystemPrompt(): String = """
        You are a helpful Tesla parts specialist chatbot who speaks both Arabic and English fluently.
        
        LANGUAGE RULES:
        - Respond in the same language the customer uses
        - If customer uses Arabic, respond in Arabic
        - If customer uses English, respond in English
        - If mixed languages, prioritize Arabic
        
        Your role is to:
        1. Acknowledge the customer's part request in their language
        2. If you identified a part from photo, describe what you see and ask for specifications:
           - Tesla model and year (أي موديل تسلا وأي سنة؟ / What Tesla model and year?)
           - Color preference (أي لون تفضل؟ / What color do you prefer?)
           - Size/dimensions if relevant (أي مقاس؟ / What size?)
           - Quality level (جودة أصلية أم بديلة؟ / OEM or aftermarket quality?)
        3. Present the global price estimate with delivery information
        4. Explain pricing includes shipping, handling, and service fee
        5. Mention available quality options and price ranges
        6. Ask for confirmation to create potential order
        7. Be friendly, professional, and knowledgeable
        
        PHOTO ANALYSIS RESPONSES:
        When customer sends photos, always:
        1. Describe what you see in the image
        2. Identify the specific part and any damage
        3. Ask clarifying questions about specifications
        4. Provide realistic price estimates and delivery times
        
        PRICING COMMUNICATION:
        - Always present the final price estimate clearly
        - Mention the price range for different quality levels
        - Explain delivery timeframes realistically
        - Be transparent about what's included in the price
        
        Keep responses helpful and conversational. Use emojis appropriately for friendliness.
    """.trimIndent()
    
    private fun buildCustomerResponsePrompt(
        partIdentification: PartIdentificationResult,
        customerMessage: String
    ): String = """
        Customer message: $customerMessage
        Detected language: ${partIdentification.language ?: "en"}
        
        PART IDENTIFICATION:
        - Part: ${partIdentification.partName}
        - Category: ${partIdentification.category}
        - Tesla models: ${partIdentification.teslaModels.joinToString(", ")}
        - Confidence: ${partIdentification.confidence}
        - Description: ${partIdentification.description}
        
        SPECIFICATIONS NEEDED:
        ${partIdentification.specifications.joinToString("\n") { "- $it" }}
        
        PRICING INFORMATION:
        - Estimated Price: ${partIdentification.globalPriceEstimate} JOD (شامل الشحن والخدمة / including shipping and service fee)
        - Price Range: ${partIdentification.priceRange}
        - Quality Level: ${partIdentification.qualityLevel}
        - Delivery Time: ${partIdentification.deliveryEstimate}
        
        PHOTO ANALYSIS:
        ${if (partIdentification.photoAnalysis.isNotEmpty()) "- Analysis: ${partIdentification.photoAnalysis}" else ""}
        ${if (partIdentification.damageAssessment.isNotEmpty()) "- Condition: ${partIdentification.damageAssessment}" else ""}
        
        INSTRUCTIONS:
        1. Respond in the detected language (Arabic if "ar", English if "en")
        2. If photo was analyzed, describe what you identified from the image
        3. Ask for the missing specifications if any are needed
        4. Present the price estimate with clear explanation of what's included
        5. Mention the price range for different quality options
        6. Explain delivery timeframe
        7. Ask for confirmation to create potential order
        8. Be helpful and professional
    """.trimIndent()
    
    private fun parsePartIdentificationResponse(response: String): PartIdentificationResult {
        return try {
            Log.d("OpenRouterAI", "Parsing AI response: $response")
            
            // Extract JSON from the response
            val jsonStart = response.indexOf("{")
            val jsonEnd = response.lastIndexOf("}") + 1
            val jsonResponse = response.substring(jsonStart, jsonEnd)
            
            // First try to parse with flexible JSON that ignores unknown keys
            val flexibleJson = Json { 
                ignoreUnknownKeys = true
                isLenient = true
            }
            
            // Try to parse directly first
            try {
                val aiResult = flexibleJson.decodeFromString(AIPartIdentification.serializer(), jsonResponse)
                return PartIdentificationResult(
                    success = true,
                    partName = aiResult.partName,
                    category = aiResult.category,
                    teslaModels = aiResult.teslaModels,
                    confidence = aiResult.confidence,
                    description = aiResult.description,
                    specifications = aiResult.specifications,
                    language = aiResult.language,
                    photoAnalysis = aiResult.photoAnalysis,
                    damageAssessment = aiResult.damageAssessment,
                    colorFinish = aiResult.colorFinish,
                    estimatedSize = aiResult.estimatedSize,
                    globalPriceEstimate = aiResult.globalPriceEstimate,
                    priceRange = aiResult.priceRange,
                    deliveryEstimate = aiResult.deliveryEstimate,
                    qualityLevel = aiResult.qualityLevel
                )
            } catch (e: Exception) {
                Log.w("OpenRouterAI", "Direct parsing failed, trying fallback: ${e.message}")
                // Fall through to fallback parsing
            }
            
            // Fallback: try to extract basic info from text response
            val fallbackResult = extractBasicPartInfo(response)
            if (fallbackResult != null) {
                fallbackResult
            } else {
                PartIdentificationResult(
                    success = false,
                    error = "Failed to parse AI response",
                    partName = "",
                    confidence = 0.0
                )
            }
        } catch (e: Exception) {
            Log.e("OpenRouterAI", "Failed to parse AI response", e)
            PartIdentificationResult(
                success = false,
                error = "Failed to parse AI response: ${e.message}",
                partName = "",
                confidence = 0.0
            )
        }
    }
    
    private fun extractBasicPartInfo(response: String): PartIdentificationResult? {
        return try {
            // Try to extract key fields from the JSON even if parsing fails
            val partNameMatch = Regex("\"partName\"\\s*:\\s*\"([^\"]+)\"").find(response)
            val searchTermsMatch = Regex("\"searchTerms\"\\s*:\\s*\\[([^\\]]+)\\]").find(response)
            val categoryMatch = Regex("\"category\"\\s*:\\s*\"([^\"]+)\"").find(response)
            val confidenceMatch = Regex("\"confidence\"\\s*:\\s*([0-9.]+)").find(response)
            
            val partName = partNameMatch?.groupValues?.get(1) ?: ""
            val category = categoryMatch?.groupValues?.get(1) ?: ""
            val confidence = confidenceMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.7
            
            val searchTerms = if (searchTermsMatch != null) {
                val termsString = searchTermsMatch.groupValues[1]
                Regex("\"([^\"]+)\"").findAll(termsString).map { it.groupValues[1] }.toList()
            } else {
                listOf(partName, "Tesla $partName").filter { it.isNotBlank() }
            }
            
            if (partName.isNotEmpty()) {
                PartIdentificationResult(
                    success = true,
                    partName = partName,
                    category = category,
                    confidence = confidence,
                    description = "Part identified from fallback parsing",
                    language = if (response.contains("[ا-ي]".toRegex())) "ar" else "en",
                    globalPriceEstimate = 50.0, // Default fallback price
                    priceRange = "$30-70 estimated range",
                    deliveryEstimate = "15-25 days",
                    qualityLevel = "Standard quality"
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

data class PartIdentificationResult(
    val success: Boolean,
    val error: String = "",
    val partName: String,
    val category: String = "",
    val teslaModels: List<String> = emptyList(),
    val confidence: Double,
    val description: String = "",
    val specifications: List<String> = emptyList(),
    val language: String = "en",
    val photoAnalysis: String = "",
    val damageAssessment: String = "",
    val colorFinish: ColorFinishObject = ColorFinishObject(),
    val estimatedSize: String = "",
    val globalPriceEstimate: Double = 0.0,
    val priceRange: String = "",
    val deliveryEstimate: String = "",
    val qualityLevel: String = ""
)
