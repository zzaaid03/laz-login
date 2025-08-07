package com.laz.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.laz.models.Sale
import com.laz.viewmodels.*
import com.laz.ui.theme.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirebaseSalesOverviewScreen(
    onBack: () -> Unit,
    salesViewModel: FirebaseSalesViewModel = viewModel(factory = FirebaseServices.secureViewModelFactory)
) {
    val sales by salesViewModel.sales.collectAsState()
    val isLoading by salesViewModel.isLoading.collectAsState()
    val errorMessage by salesViewModel.errorMessage.collectAsState()
    
    var selectedSale by remember { mutableStateOf<Sale?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showReceiptDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
        SalesOverviewHeader(onBack = onBack)

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Sales Analytics",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Sales Statistics Card
            SalesStatisticsCard(sales = sales)

            Text(
                text = "${sales.size} Total Sales",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            // Error handling
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Sales List
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (sales.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "No sales found",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Sales will appear here once processed",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sales) { sale ->
                        SaleItem(
                            sale = sale,
                            onViewReceipt = {
                                selectedSale = sale
                                showReceiptDialog = true
                            },
                            onEdit = {
                                selectedSale = sale
                                showEditDialog = true
                            },
                            onDelete = {
                                selectedSale = sale
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    selectedSale?.let { sale ->
        if (showReceiptDialog) {
            ReceiptDialog(
                sale = sale,
                onDismiss = { 
                    showReceiptDialog = false
                    selectedSale = null
                }
            )
        }
        
        if (showEditDialog) {
            EditSaleDialog(
                sale = sale,
                salesViewModel = salesViewModel,
                onDismiss = { 
                    showEditDialog = false
                    selectedSale = null
                },
                onSaveSuccess = {
                    showEditDialog = false
                    selectedSale = null
                    scope.launch {
                        salesViewModel.refresh()
                    }
                }
            )
        }
        
        if (showDeleteDialog) {
            DeleteSaleDialog(
                sale = sale,
                salesViewModel = salesViewModel,
                onDismiss = { 
                    showDeleteDialog = false
                    selectedSale = null
                },
                onDeleteSuccess = {
                    showDeleteDialog = false
                    selectedSale = null
                    scope.launch {
                        salesViewModel.refresh()
                    }
                }
            )
        }
    }
}

@Composable
fun SalesOverviewHeader(
    onBack: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
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
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Text(
                    text = "Sales Overview",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SalesStatisticsCard(sales: List<Sale>) {
    // Calculate statistics using correct Sale model properties
    val totalRevenue = sales.sumOf { 
        try {
            it.productPrice.toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }
    val averageSale = if (sales.isNotEmpty()) totalRevenue / sales.size else 0.0
    val todaySales = sales.filter { 
        try {
            // Assuming date format is consistent, compare with today
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            it.date.contains(today)
        } catch (e: Exception) {
            false
        }
    }
    
    val formatter = NumberFormat.getCurrencyInstance()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Sales Statistics",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    label = "Total Revenue",
                    value = formatter.format(totalRevenue),
                    icon = Icons.Default.AttachMoney
                )
                StatisticItem(
                    label = "Average Sale",
                    value = formatter.format(averageSale),
                    icon = Icons.AutoMirrored.Filled.TrendingUp
                )
                StatisticItem(
                    label = "Today's Sales",
                    value = "${todaySales.size}",
                    icon = Icons.Default.Today
                )
            }
        }
    }
}

@Composable
fun StatisticItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SaleItem(
    sale: Sale,
    onViewReceipt: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewReceipt() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                Column {
                    Text(
                        text = "Sale #${sale.id}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = sale.date,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${sale.productName} (${sale.quantity}x)",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
                Text(
                    text = NumberFormat.getCurrencyInstance().format(
                        try {
                            sale.productPrice.toDoubleOrNull() ?: 0.0
                        } catch (e: Exception) {
                            0.0
                        }
                    ),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onViewReceipt,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Receipt")
                }
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit")
                }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
fun ReceiptDialog(
    sale: Sale,
    onDismiss: () -> Unit
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
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "LAZ STORE",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "SALES RECEIPT",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Date:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(sale.date, color = MaterialTheme.colorScheme.onSurface)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Sale ID:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(sale.id.toString(), color = MaterialTheme.colorScheme.onSurface)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Customer:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(sale.userName, color = MaterialTheme.colorScheme.onSurface)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Product:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(sale.productName, color = MaterialTheme.colorScheme.onSurface)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Quantity:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(sale.quantity.toString(), color = MaterialTheme.colorScheme.onSurface)
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("TOTAL:", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        NumberFormat.getCurrencyInstance().format(
                            try {
                                sale.productPrice.toDoubleOrNull() ?: 0.0
                            } catch (e: Exception) {
                                0.0
                            }
                        ),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun EditSaleDialog(
    sale: Sale,
    salesViewModel: FirebaseSalesViewModel,
    onDismiss: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    var editedPrice by remember { mutableStateOf(sale.productPrice) }
    var editedQuantity by remember { mutableStateOf(sale.quantity.toString()) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
                    text = "Edit Sale #${sale.id}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                OutlinedTextField(
                    value = editedPrice,
                    onValueChange = { editedPrice = it },
                    label = { Text("Price") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = editedQuantity,
                    onValueChange = { editedQuantity = it },
                    label = { Text("Quantity") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                try {
                                    val updatedSale = sale.copy(
                                        productPrice = editedPrice,
                                        quantity = editedQuantity.toIntOrNull() ?: sale.quantity
                                    )
                                    salesViewModel.updateSale(updatedSale)
                                    onSaveSuccess()
                                } catch (e: Exception) {
                                    // Handle error
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteSaleDialog(
    sale: Sale,
    salesViewModel: FirebaseSalesViewModel,
    onDismiss: () -> Unit,
    onDeleteSuccess: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Sale") },
        text = { 
            Text("Are you sure you want to delete Sale #${sale.id}? This action cannot be undone.")
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        try {
                            salesViewModel.deleteSale(sale.id)
                            onDeleteSuccess()
                        } catch (e: Exception) {
                            // Handle error
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onError
                    )
                } else {
                    Text("Delete")
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
