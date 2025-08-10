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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.laz.models.Product
import com.laz.viewmodels.FirebaseSalesViewModel
import com.laz.viewmodels.SecureFirebaseProductViewModel
import com.laz.viewmodels.FirebaseServices
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
 * Firebase Sales Processing Screen
 * Point of Sale interface for processing customer purchases
 */
@OptIn(ExperimentalMaterial3Api::class) 
@Composable
fun FirebaseSalesProcessingScreen(
    onBack: () -> Unit
) {
    val salesViewModel: FirebaseSalesViewModel = viewModel(
        factory = FirebaseServices.viewModelFactory
    )
    val productViewModel: SecureFirebaseProductViewModel = viewModel(
        factory = FirebaseServices.secureViewModelFactory
    )
    
    val products by productViewModel.products.collectAsState()
    val isLoading by salesViewModel.isLoading.collectAsState()
    val errorMessage by salesViewModel.errorMessage.collectAsState()
    
    var selectedProducts by remember { mutableStateOf<Map<Long, Int>>(emptyMap()) }
    var searchQuery by remember { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var lastSaleResult by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    
    // Load products when screen opens
    LaunchedEffect(Unit) {
        productViewModel.loadProducts()
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
                title = { Text("Sales Processing") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectedProducts.isNotEmpty()) {
                        TextButton(
                            onClick = { showConfirmDialog = true }
                        ) {
                            Text("Process Sale")
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
                            text = "Current Sale",
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
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
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
            title = { Text("Confirm Sale") },
            text = {
                Column {
                    Text("Process sale for:")
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
                                    var successCount = 0
                                    var totalItems = selectedProducts.size
                                    
                                    // Process each selected product
                                    selectedProducts.forEach { (productId, quantity) ->
                                        val product = products.find { it.id == productId }
                                        if (product != null) {
                                            salesViewModel.processSale(product, quantity, "Current User")
                                            successCount++
                                        }
                                    }
                                    
                                    // Show success feedback
                                    lastSaleResult = "Successfully processed $successCount of $totalItems items"
                                    selectedProducts = emptyMap()
                                    showConfirmDialog = false
                                    showSuccessDialog = true
                                } catch (e: Exception) {
                                    lastSaleResult = "Error processing sale: ${e.message}"
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
            title = { Text("Sale Completed") },
            text = {
                Column {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(lastSaleResult ?: "Sale processed successfully!")
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
            title = { Text("Sale Error") },
            text = {
                Column {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(lastSaleResult ?: errorMessage ?: "An error occurred processing the sale")
                }
            },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text("OK")
                }
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
