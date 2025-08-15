package com.laz.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.laz.models.Product
import com.laz.viewmodels.FirebaseOrdersViewModel
import com.laz.viewmodels.SecureFirebaseProductViewModel
import com.laz.viewmodels.FirebaseServices
import com.laz.ui.components.OrderReceiptDialog
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*
import java.math.BigDecimal

/**
 * Format currency in Jordanian Dinar (JOD)
 */
private fun formatJOD(amount: BigDecimal): String {
    return "${amount.setScale(2)} JOD"
}

/**
 * Firebase Point of Sale Screen
 * In-store order processing interface for admin/employee transactions
 */
@OptIn(ExperimentalMaterial3Api::class) 
@Composable
fun FirebaseSalesProcessingScreen(
    onBack: () -> Unit
) {
    val ordersViewModel: FirebaseOrdersViewModel = viewModel(
        factory = FirebaseServices.secureViewModelFactory
    )
    val productViewModel: SecureFirebaseProductViewModel = viewModel(
        factory = FirebaseServices.secureViewModelFactory
    )
    
    val products by productViewModel.products.collectAsState()
    val isLoading by ordersViewModel.isLoading.collectAsState()
    val errorMessage by ordersViewModel.errorMessage.collectAsState()
    val permissionError by ordersViewModel.permissionError.collectAsState()
    val operationSuccess by ordersViewModel.operationSuccess.collectAsState()
    val lastCreatedOrder by ordersViewModel.lastCreatedOrder.collectAsState()
    
    var selectedProducts by remember { mutableStateOf<Map<Long, Int>>(emptyMap()) }
    var searchQuery by remember { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var showReceiptDialog by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var lastSaleResult by remember { mutableStateOf<String?>(null) }
    var selectedOrder by remember { mutableStateOf<com.laz.models.Order?>(null) }
    
    val scope = rememberCoroutineScope()
    
    // Load products when screen opens
    LaunchedEffect(Unit) {
        productViewModel.loadProducts()
    }
    
    // Handle order creation success
    LaunchedEffect(operationSuccess) {
        operationSuccess?.let {
            selectedOrder = lastCreatedOrder
            showReceiptDialog = true
            lastSaleResult = it
        }
    }
    
    // Handle order creation errors
    LaunchedEffect(errorMessage, permissionError) {
        val error = errorMessage ?: permissionError
        error?.let {
            lastSaleResult = "Error: $it"
            showErrorDialog = true
        }
    }
    
    // Filter products based on search
    val filteredProducts = products.filter { product ->
        product.name.contains(searchQuery, ignoreCase = true) ||
        product.id.toString().contains(searchQuery, ignoreCase = true)
    }
    
    // Calculate total
    val total = selectedProducts.entries.sumOf { (productId, quantity) ->
        val product = products.find { it.id == productId }
        (product?.price?.toDouble() ?: 0.0) * quantity
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Point of Sale") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectedProducts.isNotEmpty()) {
                        TextButton(
                            onClick = { showConfirmDialog = true }
                        ) {
                            Text("Process Order")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Products") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            
            // Selected Items Summary
            if (selectedProducts.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Current Order",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        selectedProducts.forEach { (productId, quantity) ->
                            val product = products.find { it.id == productId }
                            if (product != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${product.name} x$quantity")
                                    Text(
                                        NumberFormat.getCurrencyInstance(Locale.US)
                                            .format(product.price.multiply(BigDecimal(quantity)))
                                    )
                                }
                            }
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = NumberFormat.getCurrencyInstance(Locale.US).format(total),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Products List
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredProducts) { product ->
                        ProductSaleItem(
                            product = product,
                            selectedQuantity = selectedProducts[product.id] ?: 0,
                            onQuantityChange = { quantity ->
                                selectedProducts = if (quantity > 0) {
                                    selectedProducts + (product.id to quantity)
                                } else {
                                    selectedProducts - product.id
                                }
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Confirmation Dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Order") },
            text = {
                Column {
                    Text("Process order for:")
                    Spacer(modifier = Modifier.height(8.dp))
                    selectedProducts.forEach { (productId, quantity) ->
                        val product = products.find { it.id == productId }
                        if (product != null) {
                            Text("• ${product.name} x$quantity")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Total: ${formatJOD(total.toBigDecimal())}",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (!isProcessing) {
                            isProcessing = true
                            scope.launch {
                                try {
                                    // Create order items from selected products
                                    val orderItems = selectedProducts.mapNotNull { (productId, quantity) ->
                                        val product = products.find { it.id == productId }
                                        product?.let {
                                            val itemTotal = it.price.multiply(BigDecimal(quantity))
                                            com.laz.models.OrderItem(
                                                productId = it.id,
                                                productName = it.name,
                                                quantity = quantity,
                                                unitPrice = it.price,
                                                totalPrice = itemTotal
                                            )
                                        }
                                    }
                                    
                                    // Create order for in-store purchase (admin/employee processing)
                                    val order = com.laz.models.Order(
                                        customerId = 1L, // In-store customer placeholder
                                        customerUsername = "in-store-customer",
                                        items = orderItems,
                                        totalAmount = total.toBigDecimal(),
                                        status = com.laz.models.OrderStatus.DELIVERED, // Immediate delivery for in-store sales
                                        shippingAddress = "In-Store Pickup",
                                        paymentMethod = "Cash/Card",
                                        orderDate = System.currentTimeMillis()
                                    )
                                    
                                    // Use order creation instead of direct sales processing
                                    android.util.Log.d("PointOfSale", "Creating order with ${order.items.size} items, total: ${order.totalAmount}")
                                    android.util.Log.d("PointOfSale", "Order items: ${order.items.map { "${it.productName} x${it.quantity}" }}")
                                    ordersViewModel.createOrder(order)
                                            
                                    // Show receipt instead of generic success message
                                    selectedOrder = order
                                    lastSaleResult = "Order completed successfully! Receipt generated."
                                    selectedProducts = emptyMap()
                                    showConfirmDialog = false
                                    showReceiptDialog = true
                                } catch (e: Exception) {
                                    lastSaleResult = "Error processing order: ${e.message}"
                                    showErrorDialog = true
                                } finally {
                                    isProcessing = false
                                }
                            }
                        }
                    },
                    enabled = !isProcessing
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Confirm")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Success Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("Order Completed") },
            text = {
                Column {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(lastSaleResult ?: "Order processed successfully!")
                }
            },
            confirmButton = {
                TextButton(onClick = { showSuccessDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
    
    // Error Dialog
    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("Order Error") },
            text = {
                Column {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(lastSaleResult ?: errorMessage ?: "An error occurred processing the order")
                }
            },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
    
    // Receipt Dialog
    if (showReceiptDialog && selectedOrder != null) {
        OrderReceiptDialog(
            order = selectedOrder!!,
            onDismiss = { 
                showReceiptDialog = false
                selectedOrder = null
            }
        )
    }
    
    // Error Message from ViewModel
    errorMessage?.let { error ->
        LaunchedEffect(error) {
            lastSaleResult = error
            showErrorDialog = true
        }
    }
}

@Composable
private fun ProductSaleItem(
    product: Product,
    selectedQuantity: Int,
    onQuantityChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = NumberFormat.getCurrencyInstance(Locale.US).format(product.price),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Stock: ${product.quantity}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (product.quantity > 0) 
                            MaterialTheme.colorScheme.onSurface 
                        else 
                            MaterialTheme.colorScheme.error
                    )
                }
                
                // Quantity Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { 
                            if (selectedQuantity > 0) {
                                onQuantityChange(selectedQuantity - 1)
                            }
                        },
                        enabled = selectedQuantity > 0
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
                    }
                    
                    Text(
                        text = selectedQuantity.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.widthIn(min = 24.dp),
                        textAlign = TextAlign.Center
                    )
                    
                    IconButton(
                        onClick = { 
                            if (selectedQuantity < product.quantity) {
                                onQuantityChange(selectedQuantity + 1)
                            }
                        },
                        enabled = selectedQuantity < product.quantity && product.quantity > 0
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase")
                    }
                }
            }
        }
    }
}
