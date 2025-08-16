package com.laz.config

import android.content.Context
import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.tasks.await

/**
 * Secure configuration manager using Firebase Remote Config
 * Stores sensitive API keys server-side instead of hardcoding in app
 */
class SecureConfig private constructor() {
    
    private var remoteConfig: FirebaseRemoteConfig? = null
    private var isInitialized = false
    
    companion object {
        @Volatile
        private var INSTANCE: SecureConfig? = null
        
        fun getInstance(): SecureConfig {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecureConfig().also { INSTANCE = it }
            }
        }
        
        private const val TAG = "SecureConfig"
        
        // Remote Config Keys
        private const val OPENROUTER_API_KEY = "openrouter_api_key"
        private const val PRIMARY_AI_MODEL = "primary_ai_model"
        private const val FALLBACK_AI_MODEL = "fallback_ai_model"
        private const val DEFAULT_PROFIT_MARGIN = "default_profit_margin"
        
        // Default values (fallbacks)
        private const val DEFAULT_OPENROUTER_KEY = ""
        private const val DEFAULT_PRIMARY_MODEL = "anthropic/claude-3-haiku"
        private const val DEFAULT_FALLBACK_MODEL = "openai/gpt-4o-mini"
        private const val DEFAULT_MARGIN = "0.20"
    }
    
    /**
     * Initialize Firebase Remote Config
     * Call this once in Application.onCreate()
     */
    suspend fun initialize(context: Context): Boolean {
        return try {
            remoteConfig = FirebaseRemoteConfig.getInstance().apply {
                val configSettings = FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(3600) // 1 hour
                    .build()
                setConfigSettingsAsync(configSettings)
                
                // Set default values
                setDefaultsAsync(mapOf(
                    OPENROUTER_API_KEY to DEFAULT_OPENROUTER_KEY,
                    PRIMARY_AI_MODEL to DEFAULT_PRIMARY_MODEL,
                    FALLBACK_AI_MODEL to DEFAULT_FALLBACK_MODEL,
                    DEFAULT_PROFIT_MARGIN to DEFAULT_MARGIN
                ))
            }
            
            // Fetch and activate remote config
            fetchAndActivate()
            isInitialized = true
            
            Log.d(TAG, "✅ SecureConfig initialized successfully")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize SecureConfig", e)
            false
        }
    }
    
    /**
     * Fetch latest config from Firebase
     */
    private suspend fun fetchAndActivate(): Boolean {
        return try {
            remoteConfig?.let { config ->
                config.fetchAndActivate().await()
                Log.d(TAG, "🔄 Remote config fetched and activated")
                true
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to fetch remote config", e)
            false
        }
    }
    
    /**
     * Get OpenRouter API Key
     */
    fun getOpenRouterApiKey(): String {
        val key = remoteConfig?.getString(OPENROUTER_API_KEY) ?: DEFAULT_OPENROUTER_KEY
        
        if (key.isBlank()) {
            Log.w(TAG, "⚠️ OpenRouter API key is empty - check Firebase Remote Config")
            return DEFAULT_OPENROUTER_KEY
        }
        
        Log.d(TAG, "✅ OpenRouter API key loaded from Remote Config")
        Log.d(TAG, "🔑 Key length: ${key.length}")
        Log.d(TAG, "🔑 Key starts with: ${key.take(10)}...")
        
        return key
    }
    
    
    /**
     * Get Primary AI Model
     */
    fun getPrimaryAiModel(): String {
        return remoteConfig?.getString(PRIMARY_AI_MODEL) ?: DEFAULT_PRIMARY_MODEL
    }
    
    /**
     * Get Fallback AI Model
     */
    fun getFallbackAiModel(): String {
        return remoteConfig?.getString(FALLBACK_AI_MODEL) ?: DEFAULT_FALLBACK_MODEL
    }
    
    /**
     * Get Default Profit Margin
     */
    fun getDefaultProfitMargin(): Double {
        val marginStr = remoteConfig?.getString(DEFAULT_PROFIT_MARGIN) ?: DEFAULT_MARGIN
        return marginStr.toDoubleOrNull() ?: 0.20
    }
    
    /**
     * Check if config is properly initialized
     */
    fun isConfigReady(): Boolean {
        return isInitialized && remoteConfig != null
    }
    
    /**
     * Refresh config manually (for testing or admin features)
     */
    suspend fun refreshConfig(): Boolean {
        return if (isInitialized) {
            fetchAndActivate()
        } else {
            Log.w(TAG, "⚠️ Cannot refresh - SecureConfig not initialized")
            false
        }
    }
}
