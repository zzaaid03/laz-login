package com.laz.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.laz.models.User
import com.laz.viewmodels.*
import kotlinx.coroutines.launch

data class SupportMessage(
    val id: String = "",
    val content: String,
    val isFromCustomer: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isSystemMessage: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerSupportScreen(
    user: User,
    onBack: () -> Unit,
    chatViewModel: FirebaseChatViewModel = viewModel(factory = FirebaseServices.secureViewModelFactory)
) {
    var messages by remember { 
        mutableStateOf(
            listOf(
                SupportMessage(
                    content = "Hello ${user.username}! Welcome to LAZ Store Customer Support. I'm here to help you with:\n\n• Order inquiries and tracking\n• Product information and availability\n• Account and billing questions\n• Returns and exchanges\n• Technical support\n\nHow can I assist you today?",
                    isFromCustomer = false,
                    isSystemMessage = true
                )
            )
        )
    }
    var userInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Auto-scroll to bottom when new messages are added
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Customer Support")
                        Text(
                            "LAZ Store Help Center",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Icon(
                        Icons.Default.SupportAgent,
                        contentDescription = "Support",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = userInput,
                        onValueChange = { userInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Describe your issue or question...") },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(24.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    FloatingActionButton(
                        onClick = {
                            if (userInput.isNotBlank() && !isLoading) {
                                val userMessage = SupportMessage(
                                    content = userInput,
                                    isFromCustomer = true
                                )
                                messages = messages + userMessage
                                
                                val currentInput = userInput
                                userInput = ""
                                isLoading = true
                                
                                // Generate support response
                                scope.launch {
                                    try {
                                        val response = generateSupportResponse(currentInput, user)
                                        val supportResponse = SupportMessage(
                                            content = response,
                                            isFromCustomer = false
                                        )
                                        messages = messages + supportResponse
                                    } catch (e: Exception) {
                                        val errorResponse = SupportMessage(
                                            content = "I apologize, but I'm having trouble processing your request right now. Please try again or contact our support team directly at support@lazstore.com",
                                            isFromCustomer = false
                                        )
                                        messages = messages + errorResponse
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                ),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { message ->
                SupportMessageBubble(message = message, user = user)
            }
        }
    }
}

@Composable
private fun SupportMessageBubble(
    message: SupportMessage,
    user: User
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromCustomer) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isFromCustomer) {
            // Support agent avatar
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.SupportAgent,
                    contentDescription = "Support",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(
                topStart = if (message.isFromCustomer) 16.dp else 4.dp,
                topEnd = if (message.isFromCustomer) 4.dp else 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isFromCustomer) {
                    MaterialTheme.colorScheme.primary
                } else if (message.isSystemMessage) {
                    MaterialTheme.colorScheme.tertiaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                if (!message.isFromCustomer && !message.isSystemMessage) {
                    Text(
                        text = "LAZ Support",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isFromCustomer) {
                        MaterialTheme.colorScheme.onPrimary
                    } else if (message.isSystemMessage) {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
        
        if (message.isFromCustomer) {
            Spacer(modifier = Modifier.width(8.dp))
            // Customer avatar
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "You",
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

private suspend fun generateSupportResponse(userInput: String, user: User): String {
    // Simulate processing time
    kotlinx.coroutines.delay(1000)
    
    val input = userInput.lowercase()
    
    return when {
        input.contains("order") && (input.contains("track") || input.contains("status")) -> {
            "I can help you track your order! To check your order status:\n\n1. Go to 'Order Tracking' from your dashboard\n2. Your active orders will show current status\n3. For completed orders, check 'Order History'\n\nIf you need specific order details, please provide your order number and I'll look it up for you."
        }
        
        input.contains("return") || input.contains("exchange") || input.contains("refund") -> {
            "I'd be happy to help with returns and exchanges!\n\n📋 Return Policy:\n• 30-day return window\n• Items must be unused and in original packaging\n• Original receipt required\n\n🔄 How to return:\n1. Go to Order History\n2. Find your order\n3. Select 'Return Item'\n4. Follow the return process\n\nNeed help with a specific return? Let me know your order details!"
        }
        
        input.contains("product") && (input.contains("available") || input.contains("stock")) -> {
            "To check product availability:\n\n1. Browse our product catalog\n2. Available items show current stock\n3. Out-of-stock items are clearly marked\n\nLooking for something specific? Tell me the product name and I'll check our inventory for you!"
        }
        
        input.contains("account") || input.contains("profile") || input.contains("password") -> {
            "For account-related questions:\n\n👤 Profile Management:\n• Update your profile from the dashboard\n• Change personal information\n• Update contact details\n\n🔐 Password Issues:\n• Use 'Forgot Password' on login screen\n• Ensure strong password (8+ characters)\n\nNeed help with specific account settings? I'm here to guide you!"
        }
        
        input.contains("payment") || input.contains("billing") || input.contains("card") -> {
            "Payment and billing support:\n\n💳 Payment Methods:\n• Credit/Debit cards accepted\n• Cash on Delivery available\n• Secure payment processing\n\n📄 Billing Questions:\n• Order receipts sent via email\n• View payment history in Order History\n• Contact billing@lazstore.com for disputes\n\nHaving payment issues? Describe the problem and I'll help resolve it!"
        }
        
        input.contains("delivery") || input.contains("shipping") -> {
            "Shipping and delivery information:\n\n🚚 Delivery Options:\n• Standard delivery: 3-5 business days\n• Express delivery: 1-2 business days\n• Free shipping on orders over JOD 50\n\n📍 Tracking:\n• Track orders in real-time\n• SMS/email notifications\n• Delivery confirmation required\n\nQuestions about a specific delivery? Share your order details!"
        }
        
        input.contains("cancel") -> {
            "Order cancellation help:\n\n❌ Cancellation Policy:\n• Orders can be cancelled within 1 hour of placement\n• Processing orders may have restrictions\n• Full refund for cancelled orders\n\n📞 To cancel:\n1. Contact us immediately\n2. Provide order number\n3. Cancellation processed within 24 hours\n\nNeed to cancel an order? Give me your order number!"
        }
        
        input.contains("help") || input.contains("support") -> {
            "I'm here to help with all your LAZ Store needs!\n\n🛍️ I can assist with:\n• Order tracking and status\n• Product information\n• Returns and exchanges\n• Account management\n• Payment and billing\n• Shipping questions\n\n📞 Additional Support:\n• Email: support@lazstore.com\n• Phone: +962-123-4567\n• Hours: 9 AM - 6 PM (Sun-Thu)\n\nWhat specific area would you like help with?"
        }
        
        else -> {
            "Thank you for contacting LAZ Store support, ${user.username}!\n\nI understand you're asking about: \"$userInput\"\n\nI'm here to help! Could you provide a bit more detail about your specific question or issue? This will help me give you the most accurate assistance.\n\n🔍 Common topics I can help with:\n• Order tracking and management\n• Product availability and information\n• Returns, exchanges, and refunds\n• Account and billing questions\n• Shipping and delivery\n• Technical support\n\nFeel free to ask about any of these topics or describe your specific situation!"
        }
    }
}
