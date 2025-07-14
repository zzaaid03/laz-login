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
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import com.laz.R
import com.laz.models.Sale
import com.laz.models.User
import com.laz.viewmodels.ProductViewModel
import com.laz.viewmodels.SalesViewModel
import com.laz.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SalesOverviewScreen(
    salesViewModel: SalesViewModel,
    onBack: () -> Unit
) {
    var allSales by remember { mutableStateOf<List<Sale>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedSale by remember { mutableStateOf<Sale?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showReceiptDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Load all sales
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                allSales = salesViewModel.getAllSales()
            } catch (e: Exception) {
                println("Error loading sales: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(LazDarkBackground, LazDarkSurface)
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
                text = "Sales Overview",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = LazWhite
            )

            Text(
                text = "${allSales.size} Total Sales",
                fontSize = 16.sp,
                color = LazLightGray
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = LazRed)
                }
            } else if (allSales.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No sales found",
                        color = LazLightGray,
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allSales) { sale ->
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
    if (showReceiptDialog && selectedSale != null) {
        ReceiptDialog(
            sale = selectedSale!!,
            onDismiss = { showReceiptDialog = false }
        )
    }

    if (showEditDialog && selectedSale != null) {
        EditSaleDialog(
            sale = selectedSale!!,
            salesViewModel = salesViewModel,
            onDismiss = { showEditDialog = false },
            onSaveSuccess = {
                scope.launch {
                    allSales = salesViewModel.getAllSales()
                }
                showEditDialog = false
            }
        )
    }

    if (showDeleteDialog && selectedSale != null) {
        DeleteSaleDialog(
            sale = selectedSale!!,
            salesViewModel = salesViewModel,
            onDismiss = { showDeleteDialog = false },
            onDeleteSuccess = {
                scope.launch {
                    allSales = salesViewModel.getAllSales()
                }
                showDeleteDialog = false
            }
        )
    }
}

@Composable
fun SalesOverviewHeader(
    onBack: () -> Unit
) {
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
                        tint = LazWhite
                    )
                }
                Image(
                    painter = painterResource(id = R.drawable.laz_logo),
                    contentDescription = "LAZ Logo",
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Sales Overview",
                    fontSize = 18.sp,
                    color = LazWhite
                )
            }
        }
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
        colors = CardDefaults.cardColors(containerColor = LazDarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sale.productName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = LazWhite
                    )
                    Text(
                        text = "Qty: ${sale.quantity} • Price: ${sale.productPrice}",
                        fontSize = 12.sp,
                        color = LazLightGray
                    )
                    Text(
                        text = "Total: JOD ${String.format("%.2f", sale.productPrice.toDouble() * sale.quantity)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = LazRedGlow
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = sale.date,
                        fontSize = 10.sp,
                        color = LazLightGray
                    )
                    Text(
                        text = "By: ${sale.userName}",
                        fontSize = 10.sp,
                        color = LazLightGray
                    )
                    if (sale.isReturned) {
                        Text(
                            text = "RETURNED",
                            fontSize = 10.sp,
                            color = LazRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                Button(
                    onClick = onViewReceipt,
                    colors = ButtonDefaults.buttonColors(containerColor = LazRedGlow),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Receipt", fontSize = 10.sp)
                }
                Button(
                    onClick = onEdit,
                    colors = ButtonDefaults.buttonColors(containerColor = LazRed),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Edit", fontSize = 10.sp)
                }
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = LazRed.copy(alpha = 0.7f)),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Delete", fontSize = 10.sp)
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
            colors = CardDefaults.cardColors(containerColor = LazWhite)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "LAZ STORE",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = LazDarkBackground
                )
                Text(
                    text = "SALES RECEIPT",
                    fontSize = 14.sp,
                    color = LazGray
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Date:", fontWeight = FontWeight.Bold, color = LazDarkBackground)
                    Text(sale.date, color = LazDarkBackground)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Sale ID:", fontWeight = FontWeight.Bold, color = LazDarkBackground)
                    Text(sale.id.toString(), color = LazDarkBackground)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Cashier:", fontWeight = FontWeight.Bold, color = LazDarkBackground)
                    Text(sale.userName, color = LazDarkBackground)
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Product:", fontWeight = FontWeight.Bold, color = LazDarkBackground)
                    Text(sale.productName, color = LazDarkBackground)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Unit Price:", fontWeight = FontWeight.Bold, color = LazDarkBackground)
                    Text("JOD ${sale.productPrice}", color = LazDarkBackground)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Quantity:", fontWeight = FontWeight.Bold, color = LazDarkBackground)
                    Text(sale.quantity.toString(), color = LazDarkBackground)
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("TOTAL:", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = LazDarkBackground)
                    Text(
                        "JOD ${String.format("%.2f", sale.productPrice.toDouble() * sale.quantity)}", 
                        fontSize = 18.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = LazRed
                    )
                }
                
                if (sale.isReturned) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "*** RETURNED ***",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = LazRed,
                        textAlign = TextAlign.Center
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = LazRed)
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
    salesViewModel: SalesViewModel,
    onDismiss: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    var newQuantity by remember { mutableStateOf(sale.quantity.toString()) }
    var newPrice by remember { mutableStateOf(sale.productPrice) }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = LazDarkCard)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Edit Sale",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = LazWhite
                )
                
                Text(
                    text = sale.productName,
                    fontSize = 16.sp,
                    color = LazLightGray
                )
                
                OutlinedTextField(
                    value = newQuantity,
                    onValueChange = { newQuantity = it },
                    label = { Text("Quantity", color = LazLightGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LazWhite,
                        unfocusedTextColor = LazWhite,
                        focusedBorderColor = LazRed,
                        unfocusedBorderColor = LazLightGray
                    )
                )
                
                OutlinedTextField(
                    value = newPrice,
                    onValueChange = { newPrice = it },
                    label = { Text("Price", color = LazLightGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LazWhite,
                        unfocusedTextColor = LazWhite,
                        focusedBorderColor = LazRed,
                        unfocusedBorderColor = LazLightGray
                    )
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = LazGray),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    val updatedSale = sale.copy(
                                        quantity = newQuantity.toIntOrNull() ?: sale.quantity,
                                        productPrice = newPrice
                                    )
                                    salesViewModel.updateSale(updatedSale)
                                    onSaveSuccess()
                                } catch (e: Exception) {
                                    println("Error updating sale: ${e.message}")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LazRed),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteSaleDialog(
    sale: Sale,
    salesViewModel: SalesViewModel,
    onDismiss: () -> Unit,
    onDeleteSuccess: () -> Unit
) {
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = LazDarkCard)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Delete Sale",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = LazRed
                )
                
                Text(
                    text = "Are you sure you want to delete this sale?",
                    fontSize = 16.sp,
                    color = LazWhite
                )
                
                Text(
                    text = "${sale.productName} - JOD ${String.format("%.2f", sale.productPrice.toDouble() * sale.quantity)}",
                    fontSize = 14.sp,
                    color = LazLightGray
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = LazGray),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    salesViewModel.deleteSale(sale.id)
                                    onDeleteSuccess()
                                } catch (e: Exception) {
                                    println("Error deleting sale: ${e.message}")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LazRed),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}
