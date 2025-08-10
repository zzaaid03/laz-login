package com.laz.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.laz.models.Sale
import com.laz.viewmodels.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirebaseReturnsProcessingScreen(
    onBack: () -> Unit,
    salesViewModel: FirebaseSalesViewModel = viewModel(factory = FirebaseServices.secureViewModelFactory),
    returnsViewModel: FirebaseReturnsViewModel = viewModel(factory = FirebaseServices.secureViewModelFactory)
) {
    val sales by salesViewModel.sales.collectAsState()
    val isLoading by salesViewModel.isLoading.collectAsState()
    val errorMessage by salesViewModel.errorMessage.collectAsState()
    
    var selectedSale by remember { mutableStateOf<Sale?>(null) }
    var returnReason by remember { mutableStateOf("") }
    var showReturnDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var processedReturn by remember { mutableStateOf<Sale?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    // Filter non-returned sales for returns processing
    val returnableSales = remember(sales) {
        sales.filter { !it.isReturned }
    }

    // Load sales when screen opens
    LaunchedEffect(Unit) {
        scope.launch {
            salesViewModel.refresh()
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
        ReturnsProcessingHeader(onBack = onBack)

        // Content
        Box(modifier = Modifier.weight(1f)) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (errorMessage != null) {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = "Error: $errorMessage",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else if (returnableSales.isEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AssignmentReturn,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "No returnable sales found",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Only non-returned sales can be processed for returns",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(returnableSales) { sale ->
                        ReturnableSaleCard(
                            sale = sale,
                            isSelected = selectedSale?.id == sale.id,
                            onSelect = {
                                selectedSale = sale
                                showReturnDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Return Processing Dialog
    selectedSale?.let { sale ->
        if (showReturnDialog) {
            ReturnProcessingDialog(
                sale = sale,
                returnReason = returnReason,
                onReasonChange = { returnReason = it },
                isProcessing = isProcessing,
                onDismiss = {
                    showReturnDialog = false
                    selectedSale = null
                    returnReason = ""
                },
                onConfirm = {
                    scope.launch {
                        isProcessing = true
                        try {
                            // Process return using the returns ViewModel
                            // This will: 1) Create return record, 2) Update product inventory, 3) Mark sale as returned
                            val returnSuccess = returnsViewModel.processReturnFromSale(sale, returnReason)
                            
                            if (returnSuccess) {
                                // Update sale as returned
                                val updatedSale = sale.copy(isReturned = true)
                                val saleUpdateSuccess = salesViewModel.updateSale(updatedSale)
                                
                                if (saleUpdateSuccess) {
                                    processedReturn = sale
                                    showReturnDialog = false
                                    showSuccessDialog = true
                                    salesViewModel.refresh() // Refresh the sales list
                                } else {
                                    showReturnDialog = false
                                    showErrorDialog = true
                                }
                            } else {
                                showReturnDialog = false
                                showErrorDialog = true
                            }
                        } catch (e: Exception) {
                            println("DEBUG: Return processing error: ${e.message}")
                            showReturnDialog = false
                            showErrorDialog = true
                        } finally {
                            isProcessing = false
                            selectedSale = null
                            returnReason = ""
                        }
                    }
                }
            )
        }
    }

    // Success Dialog
    processedReturn?.let { sale ->
        if (showSuccessDialog) {
            ReturnSuccessDialog(
                sale = sale,
                onDismiss = {
                    showSuccessDialog = false
                    processedReturn = null
                }
            )
        }
    }

    // Error Dialog
    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("Return Processing Failed") },
            text = { Text("Failed to process the return. Please try again.") },
            confirmButton = {
                Button(onClick = { showErrorDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun ReturnsProcessingHeader(onBack: () -> Unit) {
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
            Text(
                text = "Returns Processing",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ReturnableSaleCard(
    sale: Sale,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp),
        onClick = onSelect
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sale.productName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Sold by: ${sale.userName}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatCurrency(sale.productPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Qty: ${sale.quantity}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sale Date: ${sale.date}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "Returnable",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun ReturnProcessingDialog(
    sale: Sale,
    returnReason: String,
    onReasonChange: (String) -> Unit,
    isProcessing: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Process Return",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Sale details
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = sale.productName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Price: ${formatCurrency(sale.productPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO)}",
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Quantity: ${sale.quantity}",
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Total: ${formatCurrency((sale.productPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO) * sale.quantity.toBigDecimal())}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Return reason
                OutlinedTextField(
                    value = returnReason,
                    onValueChange = onReasonChange,
                    label = { Text("Return Reason") },
                    placeholder = { Text("Enter reason for return...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isProcessing
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        enabled = !isProcessing && returnReason.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onError
                            )
                        } else {
                            Text("Process Return")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReturnSuccessDialog(
    sale: Sale,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Return Processed Successfully")
            }
        },
        text = { 
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("The following item has been successfully returned:")
                Text(
                    text = "• ${sale.productName}",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "• Quantity: ${sale.quantity}",
                    fontSize = 14.sp
                )
                Text(
                    text = "• Refund Amount: ${formatCurrency((sale.productPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO) * sale.quantity.toBigDecimal())}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

private fun formatCurrency(amount: BigDecimal): String {
    return "${amount.setScale(2)} JOD"
}
