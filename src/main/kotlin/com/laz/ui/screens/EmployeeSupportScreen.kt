package com.laz.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.laz.models.User
import com.laz.models.SupportMessage
import com.laz.models.SupportChat
import com.laz.viewmodels.SupportChatViewModel
import com.laz.viewmodels.FirebaseServices
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeSupportScreen(
    employee: User,
    onBack: () -> Unit
) {
    val chatViewModel: SupportChatViewModel = viewModel(
        factory = FirebaseServices.secureViewModelFactory
    )
    
    val allChats by chatViewModel.allChats.collectAsState()
    val currentChatId by chatViewModel.currentChatId.collectAsState()
    val messages by chatViewModel.messages.collectAsState()
    val totalUnreadCount by chatViewModel.totalUnreadCount.collectAsState()
    val error by chatViewModel.error.collectAsState()
    
    var userInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Auto-scroll to bottom when new messages are added
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Show error snackbar
    error?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            // Show snackbar or handle error
            chatViewModel.clearError()
        }
    }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val sidebarWidth = when {
        screenWidth < 600.dp -> screenWidth * 0.4f // 40% on small screens
        screenWidth < 900.dp -> 320.dp // Fixed width on medium screens
        else -> 360.dp // Larger width on big screens
    }

    Row(modifier = Modifier.fillMaxSize()) {
        // Chat list sidebar
        Card(
            modifier = Modifier
                .width(sidebarWidth)
                .fillMaxHeight()
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column {
                // Header
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Customer Support",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${employee.username} - ${allChats.size} chats",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }
                        if (totalUnreadCount > 0) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.error
                            ) {
                                Text(
                                    totalUnreadCount.toString(),
                                    color = MaterialTheme.colorScheme.onError
                                )
                            }
                        }
                    }
                }

                // Chat list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(allChats) { chat ->
                        ChatListItem(
                            chat = chat,
                            isSelected = chat.id == currentChatId,
                            onClick = { chatViewModel.selectChat(chat.id) },
                            onClose = { chatViewModel.closeChat(chat.id) }
                        )
                    }
                    
                    if (allChats.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Chat,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "No Active Chats",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "Customer chats will appear here",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Chat area
        if (currentChatId != null) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Scaffold(
                    topBar = {
                        val currentChat = allChats.find { it.id == currentChatId }
                        TopAppBar(
                            title = {
                                Column {
                                    Text(currentChat?.customerName ?: "Customer")
                                    Text(
                                        "Customer ID: ${currentChat?.customerId}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { 
                                        currentChatId?.let { chatViewModel.closeChat(it) }
                                    }
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close Chat")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                                    placeholder = { Text("Type your response...") },
                                    shape = RoundedCornerShape(24.dp)
                                )
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                FloatingActionButton(
                                    onClick = {
                                        if (userInput.isNotBlank()) {
                                            val currentChat = allChats.find { it.id == currentChatId }
                                            currentChat?.let { chat ->
                                                chatViewModel.sendMessage(
                                                    customerId = chat.customerId,
                                                    customerName = chat.customerName,
                                                    message = userInput,
                                                    isFromCustomer = false,
                                                    employeeName = employee.username
                                                )
                                                userInput = ""
                                            }
                                        }
                                    },
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
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
                            EmployeeMessageBubble(message = message)
                        }
                    }
                }
            }
        } else {
            // No chat selected
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Select a Chat",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Choose a customer chat from the sidebar to start helping",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatListItem(
    chat: SupportChat,
    isSelected: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(
                2.dp,
                MaterialTheme.colorScheme.primary
            )
        } else null
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chat.customerName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (chat.unreadByEmployee > 0) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.error
                        ) {
                            Text(
                                chat.unreadByEmployee.toString(),
                                color = MaterialTheme.colorScheme.onError
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close Chat",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = chat.lastMessage,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                }
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = timeFormat.format(Date(chat.lastMessageTime)),
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                }
            )
        }
    }
}

@Composable
private fun EmployeeMessageBubble(message: SupportMessage) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromCustomer) Arrangement.Start else Arrangement.End
    ) {
        if (message.isFromCustomer) {
            // Customer avatar
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Customer",
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Card(
            modifier = Modifier.widthIn(max = (screenWidth * 0.7f).coerceAtMost(320.dp)),
            shape = RoundedCornerShape(
                topStart = if (!message.isFromCustomer) 16.dp else 4.dp,
                topEnd = if (!message.isFromCustomer) 4.dp else 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isFromCustomer) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                if (message.isFromCustomer) {
                    Text(
                        text = message.customerName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                Text(
                    text = message.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isFromCustomer) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    }
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = timeFormat.format(Date(message.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (message.isFromCustomer) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    }
                )
            }
        }
        
        if (!message.isFromCustomer) {
            Spacer(modifier = Modifier.width(8.dp))
            // Employee avatar
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.SupportAgent,
                    contentDescription = "You",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}
