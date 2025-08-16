package com.laz.config

/**
 * AI Configuration - Now uses SecureConfig for sensitive values
 */
object AIConfig {
    // Request Configuration (non-sensitive)
    const val MAX_TOKENS = 4000
    const val TEMPERATURE = 0.7
    const val REQUEST_TIMEOUT_SECONDS = 30L
    
    // Retry Configuration
    const val MAX_RETRIES = 3
    const val RETRY_DELAY_MS = 1000L
    
    // Search Configuration
    const val MAX_SEARCH_RESULTS = 5
    const val SEARCH_TIMEOUT_SECONDS = 30
    const val MAX_SEARCH_TERMS = 2
    
    // Pricing Configuration
    const val DEFAULT_PROFIT_MARGIN = 0.20 // 20%
    const val MIN_PROFIT_MARGIN = 0.10 // 10%
    const val MAX_PROFIT_MARGIN = 0.50 // 50%
}
