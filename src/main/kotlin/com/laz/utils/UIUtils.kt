package com.laz.utils

import java.text.NumberFormat
import java.util.*

/**
 * Common UI utility functions for the LAZ Store app
 */
object UIUtils {
    
    /**
     * Formats a double value as Jordanian Dinar currency
     */
    fun formatCurrency(amount: Double): String {
        return "JOD ${String.format("%.2f", amount)}"
    }
    
    /**
     * Formats a BigDecimal value as Jordanian Dinar currency
     */
    fun formatCurrency(amount: java.math.BigDecimal): String {
        return "JOD ${String.format("%.2f", amount.toDouble())}"
    }
    
    /**
     * Formats a date timestamp to readable string
     */
    fun formatDate(timestamp: Long): String {
        val date = Date(timestamp)
        val formatter = java.text.SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return formatter.format(date)
    }
    
    /**
     * Formats a date timestamp to readable date and time string
     */
    fun formatDateTime(timestamp: Long): String {
        val date = Date(timestamp)
        val formatter = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return formatter.format(date)
    }
    
    /**
     * Truncates text to specified length with ellipsis
     */
    fun truncateText(text: String, maxLength: Int): String {
        return if (text.length <= maxLength) {
            text
        } else {
            "${text.substring(0, maxLength - 3)}..."
        }
    }
    
    /**
     * Capitalizes first letter of each word
     */
    fun capitalizeWords(text: String): String {
        return text.split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { 
                if (it.isLowerCase()) it.titlecase() else it.toString() 
            }
        }
    }
}
