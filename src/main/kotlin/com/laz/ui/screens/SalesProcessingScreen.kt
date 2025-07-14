package com.laz.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laz.models.*
import com.laz.viewmodels.ProductViewModel
import com.laz.viewmodels.SalesViewModel
import com.laz.ui.theme.*
import java.math.BigDecimal
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

// Sales processing screen for handling transactions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesProcessingScreen(
    user: User,
    productViewModel: ProductViewModel,
    salesViewModel: SalesViewModel,
    onBack: () -> Unit
) {
    // Load products from the database
    var availableProducts by remember { mutableStateOf<List<Product>>(emptyList()) }
    LaunchedEffect(Unit) {
        availableProducts = productViewModel.getAllProducts()
    }

    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var saleQuantity by remember { mutableStateOf("") }
    var showReceipt by remember { mutableStateOf(false) }
    var lastSale by remember { mutableStateOf<Pair<Product, Int>?>(null) }
    
    val coroutineScope = rememberCoroutineScope()

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
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                        text = "Sales Processing",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "Cashier: ${user.username}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Product Selection (Left Panel)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Select Product",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableProducts) { product ->
                            ProductSelectionCard(
                                product = product,
                                isSelected = selectedProduct == product,
                                onSelect = { selectedProduct = product }
                            )
                        }
                    }
                }
            }

            // Sale Processing (Right Panel)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Process Sale",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (selectedProduct != null) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Selected: ${selectedProduct!!.name}",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                            
                            Text(
                                text = "Available: ${selectedProduct!!.quantity}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                            
                            Text(
                                text = "Price: JOD ${selectedProduct!!.price}",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedTextField(
                                value = saleQuantity,
                                onValueChange = { saleQuantity = it },
                                label = { Text("1") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )

                            val quantity = saleQuantity.toIntOrNull() ?: 0
                            if (quantity > 0) {
                                val total = selectedProduct!!.price.multiply(BigDecimal.valueOf(quantity.toLong()))
                                Text(
                                    text = "Total: JOD ${total}",
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = {
                                    val quantityToSell = saleQuantity.toIntOrNull() ?: 0
                                    if (quantityToSell > 0 && quantityToSell <= selectedProduct!!.quantity) {
                                        coroutineScope.launch {
                                            // Process the sale using SalesViewModel
                                            val sale = salesViewModel.processSale(selectedProduct!!, quantityToSell, user)
                                            if (sale != null) {
                                                lastSale = Pair(selectedProduct!!, quantityToSell)
                                                showReceipt = true
                                                // Refresh product list to show updated stock
                                                availableProducts = productViewModel.getAllProducts()
                                                // Clear form
                                                selectedProduct = null
                                                saleQuantity = ""
                                            }
                                        }
                                    }
                                },
                                enabled = selectedProduct != null && 
                                        saleQuantity.toIntOrNull()?.let { it > 0 && it <= selectedProduct!!.quantity } == true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text("Process Sale", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Text(
                            text = "Please select a product to continue",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }

    // Receipt Dialog
    if (showReceipt && lastSale != null) {
        val (product, quantity) = lastSale!!
        val total = product.price.multiply(BigDecimal.valueOf(quantity.toLong()))
        
        androidx.compose.ui.window.Dialog(onDismissRequest = { showReceipt = false }) {
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
                        Text(java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()), color = LazDarkBackground)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Cashier:", fontWeight = FontWeight.Bold, color = LazDarkBackground)
                        Text(user.username, color = LazDarkBackground)
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Product:", fontWeight = FontWeight.Bold, color = LazDarkBackground)
                        Text(product.name, color = LazDarkBackground)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Unit Price:", fontWeight = FontWeight.Bold, color = LazDarkBackground)
                        Text("JOD ${product.price}", color = LazDarkBackground)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Quantity:", fontWeight = FontWeight.Bold, color = LazDarkBackground)
                        Text(quantity.toString(), color = LazDarkBackground)
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("TOTAL:", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = LazDarkBackground)
                        Text(
                            "JOD ${total}", 
                            fontSize = 18.sp, 
                            fontWeight = FontWeight.Bold, 
                            color = LazRed
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Thank you for shopping with LAZ!",
                        fontSize = 12.sp,
                        color = LazGray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Tesla Parts & Accessories",
                        fontSize = 10.sp,
                        color = LazGray
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = { showReceipt = false },
                        colors = ButtonDefaults.buttonColors(containerColor = LazRed)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
fun ProductSelectionCard(
    product: Product,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) LazRed else LazDarkSurface
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
                    text = product.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Stock: ${product.quantity}",
                    fontSize = 12.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outlineVariant
                )
                Text(
                    text = "JOD ${product.price}",
                    fontSize = 12.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
