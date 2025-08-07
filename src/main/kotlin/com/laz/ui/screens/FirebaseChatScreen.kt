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
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String = "",
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirebaseChatScreen(
    onBack: () -> Unit
) {
    var messages by remember { 
        mutableStateOf(
            listOf(
                ChatMessage(
                    content = "Hi! I'm your LAZ Store assistant. How can I help you today?",
                    isFromUser = false
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
            scope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surface)
                )
            )
    ) {
        // Header
        ChatHeader(onBack = onBack)

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { message ->
                ChatMessageBubble(message = message)
            }
            
            if (isLoading) {
                item {
                    TypingIndicator()
                }
            }
        }

        // Input area
        ChatInputArea(
            userInput = userInput,
            onInputChange = { userInput = it },
            onSendMessage = {
                if (userInput.isNotBlank() && !isLoading) {
                    // Add user message
                    val userMessage = ChatMessage(
                        content = userInput.trim(),
                        isFromUser = true
                    )
                    messages = messages + userMessage
                    
                    val currentInput = userInput.trim()
                    userInput = ""
                    isLoading = true
                    
                    // Simulate bot response (in real implementation, this would call an API)
                    scope.launch {
                        kotlinx.coroutines.delay(1500) // Simulate processing time
                        
                        val botResponse = generateBotResponse(currentInput)
                        val botMessage = ChatMessage(
                            content = botResponse,
                            isFromUser = false
                        )
                        messages = messages + botMessage
                        isLoading = false
                    }
                }
            },
            isLoading = isLoading
        )
    }
}

@Composable
fun ChatHeader(onBack: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "LAZ Store Assistant",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Online • Ready to help",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun ChatMessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isFromUser) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .padding(top = 4.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isFromUser) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isFromUser) 16.dp else 4.dp,
                bottomEnd = if (message.isFromUser) 4.dp else 16.dp
            )
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(12.dp),
                color = if (message.isFromUser) 
                    MaterialTheme.colorScheme.onPrimary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
        
        if (message.isFromUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .padding(top = 4.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun TypingIndicator() {
    Row(
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.SmartToy,
            contentDescription = null,
            modifier = Modifier
                .size(32.dp)
                .padding(top = 4.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    var alpha by remember { mutableStateOf(0.3f) }
                    
                    LaunchedEffect(Unit) {
                        while (true) {
                            kotlinx.coroutines.delay(index * 200L)
                            alpha = 1f
                            kotlinx.coroutines.delay(600)
                            alpha = 0.3f
                            kotlinx.coroutines.delay(600)
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                                RoundedCornerShape(50)
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun ChatInputArea(
    userInput: String,
    onInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = userInput,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type your message...") },
                enabled = !isLoading,
                maxLines = 4,
                shape = RoundedCornerShape(24.dp)
            )
            
            FloatingActionButton(
                onClick = onSendMessage,
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send message"
                    )
                }
            }
        }
    }
}

private fun generateBotResponse(userInput: String): String {
    val input = userInput.lowercase()
    
    return when {
        input.contains("hello") || input.contains("hi") || input.contains("hey") -> 
            "Hello! Welcome to LAZ Store. How can I assist you today?"
        
        input.contains("product") || input.contains("item") -> 
            "I can help you find products in our store. What specific item are you looking for?"
        
        input.contains("price") || input.contains("cost") -> 
            "I can help you check prices. Please let me know which product you're interested in."
        
        input.contains("order") || input.contains("purchase") -> 
            "I can help you with orders and purchases. Would you like to place a new order or check an existing one?"
        
        input.contains("return") || input.contains("refund") -> 
            "I can assist with returns and refunds. Please provide your order details and I'll help you process the return."
        
        input.contains("help") || input.contains("support") -> 
            "I'm here to help! I can assist with:\n• Finding products\n• Checking prices\n• Order information\n• Returns and refunds\n• Store policies\n\nWhat would you like to know?"
        
        input.contains("hours") || input.contains("open") -> 
            "LAZ Store is open Monday-Saturday 9AM-9PM, Sunday 10AM-6PM. Is there anything else I can help you with?"
        
        input.contains("location") || input.contains("address") -> 
            "You can find our store locations in the app. Would you like me to help you find the nearest location?"
        
        input.contains("thank") -> 
            "You're welcome! Is there anything else I can help you with today?"
        
        input.contains("bye") || input.contains("goodbye") -> 
            "Goodbye! Thank you for visiting LAZ Store. Have a great day!"
        
        else -> 
            "I understand you're asking about: \"$userInput\". I'm here to help with products, orders, returns, and store information. Could you please be more specific about what you need assistance with?"
    }
}
