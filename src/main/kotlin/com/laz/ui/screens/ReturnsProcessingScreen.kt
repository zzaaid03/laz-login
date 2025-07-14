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
import com.laz.models.Product
import com.laz.models.Sale
import com.laz.models.User
import com.laz.viewmodels.SalesViewModel
import com.laz.viewmodels.ReturnsViewModel
import com.laz.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

// Returns processing screen for handling customer returns

data class SaleForReturn(
    val id: Long,
    val productName: String,
    val quantity: Int,
    val price: BigDecimal,
    val date: String,
    val cashier: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReturnsProcessingScreen(
    salesViewModel: SalesViewModel,
    returnsViewModel: ReturnsViewModel,
    onBack: () -> Unit
) {
    // Load recent non-returned sales from database
    var recentSales by remember { mutableStateOf<List<Sale>>(emptyList()) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            // Get only non-returned sales from the last 7 days
            recentSales = salesViewModel.getRecentSales(7).filter { !it.isReturned }
        }
    }

    var selectedSale by remember { mutableStateOf<Sale?>(null) }
    var returnReason by remember { mutableStateOf("") }
    var showReturnDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var processedReturn by remember { mutableStateOf<Sale?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = LazDarkCard)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Returns Processing",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = LazWhite
                    )
                }

                Text(
                    text = "${recentSales.size} Available for Return",
                    fontSize = 14.sp,
                    color = LazLightGray
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Recent Sales List (Left Panel)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = LazDarkCard)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Available for Return (Last 7 Days)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(recentSales) { sale ->
                            SaleCard(
                                sale = sale,
                                isSelected = selectedSale == sale,
                                onSelect = { selectedSale = sale }
                            )
                        }
                    }
                }
            }

            // Return Processing (Right Panel)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = LazDarkCard)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Process Return",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = LazWhite
                    )

                    if (selectedSale != null) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Sale Details:",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Medium
                            )
                            
                            Text(
                                text = "Product: ${selectedSale!!.productName}",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            Text(
                                text = "Quantity: ${selectedSale!!.quantity}",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            Text(
                                text = "Unit Price: JOD ${selectedSale!!.productPrice}",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            val total = BigDecimal(selectedSale!!.productPrice).multiply(BigDecimal.valueOf(selectedSale!!.quantity.toLong()))
                            Text(
                                text = "Total Refund: JOD ${total}",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Text(
                                text = "Date: ${selectedSale!!.date}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            
                            Text(
                                text = "Cashier: ${selectedSale!!.userName}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )

                            OutlinedTextField(
                                value = returnReason,
                                onValueChange = { returnReason = it },
                                label = { Text("Return Reason") },
                                placeholder = { Text("e.g., Defective product, Wrong item, Customer changed mind") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LazRed,
                                    focusedLabelColor = LazRed
                                ),
                                maxLines = 3
                            )

                            Button(
                                onClick = { showReturnDialog = true },
                                enabled = returnReason.isNotBlank(),
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LazRed,
                                    contentColor = LazWhite
                                )
                            ) {
                                Text("Process Return", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Text(
                            text = "Please select a sale to process return",
                            fontSize = 14.sp,
                            color = LazLightGray
                        )
                    }
                }
            }
        }
    }

    // Confirmation Dialog
    if (showReturnDialog && selectedSale != null) {
        val total = BigDecimal(selectedSale!!.productPrice).multiply(BigDecimal.valueOf(selectedSale!!.quantity.toLong()))
        
        AlertDialog(
            onDismissRequest = { showReturnDialog = false },
            title = { Text("Confirm Return", color = LazWhite) },
            text = {
                Column {
                    Text("Are you sure you want to process this return?", color = LazWhite)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Product: ${selectedSale!!.productName}", color = LazWhite)
                    Text("Refund Amount: JOD ${total}", color = LazRed, fontWeight = FontWeight.Bold)
                    Text("Reason: $returnReason", color = LazLightGray)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedSale?.let { saleToReturn ->
                            isProcessing = true
                            coroutineScope.launch {
                                try {
                                    withContext(Dispatchers.IO) {
                                        returnsViewModel.processReturn(saleToReturn, returnReason)
                                    }
                                    processedReturn = saleToReturn
                                    showSuccessDialog = true
                                    // Refresh sales list - only show non-returned sales
                                    recentSales = withContext(Dispatchers.IO) {
                                        salesViewModel.getRecentSales(7).filter { !it.isReturned }
                                    }
                                    selectedSale = null
                                } catch (e: Exception) {
                                    errorMessage = "Error processing return: ${e.message}"
                                    showErrorDialog = true
                                } finally {
                                    isProcessing = false
                                }
                            }
                        }
                        showReturnDialog = false
                        returnReason = ""
                    },
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LazRed,
                        contentColor = LazWhite
                    )
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = LazWhite
                        )
                    } else {
                        Text("Confirm Return")
                    }
                }
            },
            dismissButton = {
                Button(
                    onClick = { showReturnDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.outline,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Cancel")
                }
            },
            containerColor = LazDarkCard
        )
    }

    // Success Dialog
    if (showSuccessDialog && processedReturn != null) {
        val total = BigDecimal(processedReturn!!.productPrice).multiply(BigDecimal.valueOf(processedReturn!!.quantity.toLong()))
        
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("Return Processed", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Return has been successfully processed!", color = MaterialTheme.colorScheme.onSurface)
                    Text("Refund Amount: JOD ${total}", color = LazRed, fontWeight = FontWeight.Bold)
                    Text("Product has been added back to inventory.", color = MaterialTheme.colorScheme.outlineVariant)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        processedReturn = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LazRed,
                        contentColor = LazWhite
                    )
                ) {
                    Text("Close")
                }
            },
            containerColor = LazDarkCard
        )
    }

    // Error Dialog
    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("Error", color = LazRed) },
            text = {
                Column {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        tint = LazRed,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage, color = MaterialTheme.colorScheme.onSurface)
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        showErrorDialog = false 
                        errorMessage = ""
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LazRed,
                        contentColor = LazWhite
                    )
                ) {
                    Text("OK")
                }
            },
            containerColor = LazDarkCard
        )
    }
}

@Composable
fun SaleCard(
    sale: Sale,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp)
    ) {
        Button(
            onClick = onSelect,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = sale.productName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Qty: ${sale.quantity} × JOD ${sale.productPrice}",
                    fontSize = 12.sp,
                    color = if (isSelected) LazWhite else LazLightGray
                )
                Text(
                    text = sale.date,
                    fontSize = 10.sp,
                    color = if (isSelected) LazWhite else LazLightGray
                )
                Text(
                    text = "By: ${sale.userName}",
                    fontSize = 10.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
