package com.laz.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.laz.models.PotentialOrder
import com.laz.models.PotentialOrderStatus
import com.laz.repositories.FirebaseRealtimePotentialOrderRepository
import com.laz.viewmodels.AdminPotentialOrderViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPotentialOrdersScreen(
    onNavigateBack: () -> Unit
) {
    val viewModel: AdminPotentialOrderViewModel = viewModel(
        factory = AdminPotentialOrderViewModel.Factory(
            FirebaseRealtimePotentialOrderRepository()
        )
    )
    
    val potentialOrders by viewModel.potentialOrders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    var selectedOrder by remember { mutableStateOf<PotentialOrder?>(null) }
    var showOrderDetails by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    
    val tabs = listOf("Pending", "All Orders", "Approved", "Rejected")
    
    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            0 -> viewModel.loadPendingOrders()
            1 -> viewModel.loadAllOrders()
            2 -> viewModel.loadOrdersByStatus(PotentialOrderStatus.APPROVED)
            3 -> viewModel.loadOrdersByStatus(PotentialOrderStatus.REJECTED)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Parts Orders") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            // Content
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = errorMessage!!,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                potentialOrders.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (selectedTab) {
                                0 -> "No pending orders"
                                1 -> "No orders found"
                                2 -> "No approved orders"
                                3 -> "No rejected orders"
                                else -> "No orders found"
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(potentialOrders) { order ->
                            PotentialOrderCard(
                                order = order,
                                onViewDetails = {
                                    selectedOrder = order
                                    showOrderDetails = true
                                },
                                onApprove = { viewModel.approveOrder(order.id) },
                                onReject = { viewModel.rejectOrder(order.id) }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Order Details Dialog
    if (showOrderDetails && selectedOrder != null) {
        OrderDetailsDialog(
            order = selectedOrder!!,
            onDismiss = { 
                showOrderDetails = false
                selectedOrder = null
            },
            onApprove = { 
                viewModel.approveOrder(selectedOrder!!.id)
                showOrderDetails = false
                selectedOrder = null
            },
            onReject = { 
                viewModel.rejectOrder(selectedOrder!!.id)
                showOrderDetails = false
                selectedOrder = null
            }
        )
    }
}

@Composable
private fun PotentialOrderCard(
    order: PotentialOrder,
    onViewDetails: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "JO"))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.customerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Order #${order.id.takeLast(8)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dateFormat.format(order.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Status badge
                AssistChip(
                    onClick = { },
                    label = { Text(order.status.name) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = when (order.status) {
                            PotentialOrderStatus.PENDING -> MaterialTheme.colorScheme.secondaryContainer
                            PotentialOrderStatus.APPROVED -> MaterialTheme.colorScheme.primaryContainer
                            PotentialOrderStatus.REJECTED -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Parts summary
            if (order.requestedParts.isNotEmpty()) {
                val firstPart = order.requestedParts.first()
                Text(
                    text = "Part: ${firstPart.partName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (order.requestedParts.size > 1) {
                    Text(
                        text = "+${order.requestedParts.size - 1} more parts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Pricing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Cost: ${currencyFormat.format(order.totalEstimatedCost)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Price: ${currencyFormat.format(order.totalSellingPrice)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Details")
                }
                
                if (order.status == PotentialOrderStatus.PENDING) {
                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Approve")
                    }
                    
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reject")
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderDetailsDialog(
    order: PotentialOrder,
    onDismiss: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "JO"))
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Order Details") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Customer info
                    Card {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Customer Information",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Name: ${order.customerName}")
                            Text("Order ID: ${order.id}")
                            Text("Date: ${dateFormat.format(order.createdAt)}")
                            Text("Status: ${order.status.name}")
                        }
                    }
                }
                
                // Requested parts
                items(order.requestedParts) { part ->
                    Card {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = part.partName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (part.description.isNotEmpty()) {
                                Text(
                                    text = part.description,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Quantity: ${part.quantity}")
                            Text("Estimated Cost: ${currencyFormat.format(part.estimatedCost)}")
                            Text("Selling Price: ${currencyFormat.format(part.sellingPrice)}")
                            
                            // Selected product info
                            part.selectedProduct?.let { product ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Selected Product:",
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = product.title,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "AliExpress Price: ${currencyFormat.format(product.price)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
                
                item {
                    // Chat history preview
                    Card {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Chat History (${order.chatHistory.size} messages)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            order.chatHistory.take(3).forEach { message ->
                                Text(
                                    text = "${message.senderType.name}: ${message.message.take(100)}${if (message.message.length > 100) "..." else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                            if (order.chatHistory.size > 3) {
                                Text(
                                    text = "... and ${order.chatHistory.size - 3} more messages",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                item {
                    // Total pricing
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Pricing Summary",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Cost:")
                                Text(currencyFormat.format(order.totalEstimatedCost))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Selling Price:")
                                Text(
                                    text = currencyFormat.format(order.totalSellingPrice),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Profit Margin:")
                                Text("${(order.profitMargin * 100).toInt()}%")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (order.status == PotentialOrderStatus.PENDING) {
                Row {
                    TextButton(onClick = onReject) {
                        Text("Reject")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onApprove) {
                        Text("Approve")
                    }
                }
            } else {
                Button(onClick = onDismiss) {
                    Text("Close")
                }
            }
        },
        dismissButton = {
            if (order.status == PotentialOrderStatus.PENDING) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
