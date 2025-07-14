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
import com.laz.viewmodels.ProductViewModel
import com.laz.ui.theme.*
import java.math.BigDecimal
import kotlinx.coroutines.launch

// In Python, this would be like a product management interface
// class ProductManager:
//     def __init__(self):
//         self.products = []
//         self.setup_product_form()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductManagementScreen(
    user: com.laz.models.User,
    productViewModel: ProductViewModel,
    onBack: () -> Unit
) {
    // Use StateFlow from ViewModel for automatic updates
    val products by productViewModel.products.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var newProductName by remember { mutableStateOf("") }
    var newProductQuantity by remember { mutableStateOf("") }
    var newProductCost by remember { mutableStateOf("") }
    var newProductPrice by remember { mutableStateOf("") }
    var newProductShelf by remember { mutableStateOf("") }

    // State for Edit Dialog
    var showEditDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var editProductName by remember { mutableStateOf("") }
    var editProductQuantity by remember { mutableStateOf("") }
    var editProductCost by remember { mutableStateOf("") }
    var editProductPrice by remember { mutableStateOf("") }
    var editProductShelf by remember { mutableStateOf("") }

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
                        text = if (user.role == com.laz.models.UserRole.ADMIN) "Product Management" else "Inventory View",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = LazWhite
                    )
                }

                // Only show Add Product button for ADMIN users
                if (user.role == com.laz.models.UserRole.ADMIN) {
                    FloatingActionButton(
                        onClick = { showAddDialog = true },
                        containerColor = LazRed,
                        contentColor = LazWhite,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add, 
                            contentDescription = "Add Product",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Product List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(products) { product ->
                ProductCard(
                    product = product,
                    canEdit = user.role == com.laz.models.UserRole.ADMIN,
                    onEdit = {
                        if (user.role == com.laz.models.UserRole.ADMIN) {
                            productToEdit = product
                            editProductName = product.name
                            editProductQuantity = product.quantity.toString()
                            editProductCost = product.cost.toPlainString()
                            editProductPrice = product.price.toPlainString()
                            editProductShelf = product.shelfLocation ?: ""
                            showEditDialog = true
                        }
                    },
                    onDelete = {
                        if (user.role == com.laz.models.UserRole.ADMIN) {
                            // Use coroutine scope for suspend function
                            coroutineScope.launch {
                                productViewModel.deleteProduct(product.id)
                            }
                        }
                    }
                )
            }
        }
    }

    // Add Product Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add New Product", color = LazWhite) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = newProductName,
                        onValueChange = { newProductName = it },
                        label = { Text("Product Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LazRed,
                            focusedLabelColor = LazRed
                        )
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newProductQuantity,
                            onValueChange = { newProductQuantity = it },
                            label = { Text("Quantity") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LazRed,
                                focusedLabelColor = LazRed
                            )
                        )
                        OutlinedTextField(
                            value = newProductShelf,
                            onValueChange = { newProductShelf = it },
                            label = { Text("Shelf") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LazRed,
                                focusedLabelColor = LazRed
                            )
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newProductCost,
                            onValueChange = { newProductCost = it },
                            label = { Text("Cost (JOD)") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LazRed,
                                focusedLabelColor = LazRed
                            )
                        )
                        OutlinedTextField(
                            value = newProductPrice,
                            onValueChange = { newProductPrice = it },
                            label = { Text("Price (JOD)") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LazRed,
                                focusedLabelColor = LazRed
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val cleanCost = newProductCost.trim().ifBlank { "0" }
                            val cleanPrice = newProductPrice.trim().ifBlank { "0" }

                            coroutineScope.launch {
                                productViewModel.createProduct(
                                    name = newProductName.trim(),
                                    quantity = newProductQuantity.toIntOrNull() ?: 0,
                                    cost = BigDecimal(cleanCost),
                                    price = BigDecimal(cleanPrice),
                                    shelfLocation = newProductShelf.trim().ifBlank { null }
                                )
                                showAddDialog = false

                                // Clear fields
                                newProductName = ""
                                newProductQuantity = ""
                                newProductCost = ""
                                newProductPrice = ""
                                newProductShelf = ""
                            }
                        } catch (e: NumberFormatException) {
                            // Invalid number format - just ignore the action
                            // In a real app, you'd show an error message
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LazRed,
                        contentColor = LazWhite
                    )
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showAddDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LazGray,
                        contentColor = LazWhite
                    )
                ) {
                    Text("Cancel")
                }
            },
            containerColor = LazDarkCard
        )
    }

    // Edit Product Dialog
    if (showEditDialog && productToEdit != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Product", color = LazWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editProductName,
                        onValueChange = { editProductName = it },
                        label = { Text("Product Name", color = LazLightGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = LazWhite,
                            unfocusedTextColor = LazWhite,
                            cursorColor = LazRed,
                            focusedBorderColor = LazRed,
                            unfocusedBorderColor = LazGray
                        )
                    )
                    OutlinedTextField(
                        value = editProductQuantity,
                        onValueChange = { editProductQuantity = it },
                        label = { Text("Quantity", color = LazLightGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = LazWhite,
                            unfocusedTextColor = LazWhite,
                            cursorColor = LazRed,
                            focusedBorderColor = LazRed,
                            unfocusedBorderColor = LazGray
                        )
                    )
                    OutlinedTextField(
                        value = editProductCost,
                        onValueChange = { editProductCost = it },
                        label = { Text("Cost (JOD)", color = LazLightGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = LazWhite,
                            unfocusedTextColor = LazWhite,
                            cursorColor = LazRed,
                            focusedBorderColor = LazRed,
                            unfocusedBorderColor = LazGray
                        )
                    )
                    OutlinedTextField(
                        value = editProductPrice,
                        onValueChange = { editProductPrice = it },
                        label = { Text("Price (JOD)", color = LazLightGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = LazWhite,
                            unfocusedTextColor = LazWhite,
                            cursorColor = LazRed,
                            focusedBorderColor = LazRed,
                            unfocusedBorderColor = LazGray
                        )
                    )
                    OutlinedTextField(
                        value = editProductShelf,
                        onValueChange = { editProductShelf = it },
                        label = { Text("Shelf Location", color = LazLightGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = LazWhite,
                            unfocusedTextColor = LazWhite,
                            cursorColor = LazRed,
                            focusedBorderColor = LazRed,
                            unfocusedBorderColor = LazGray
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        productToEdit?.let { currentProduct ->
                            try {
                                val cleanCost = editProductCost.trim().ifBlank { "0" }
                                val cleanPrice = editProductPrice.trim().ifBlank { "0" }

                                coroutineScope.launch {
                                    val updatedProductData = currentProduct.copy(
                                        name = editProductName.trim(),
                                        quantity = editProductQuantity.toIntOrNull() ?: 0,
                                        cost = BigDecimal(cleanCost),
                                        price = BigDecimal(cleanPrice),
                                        shelfLocation = editProductShelf.trim().ifBlank { null }
                                    )
                                    productViewModel.updateProduct(updatedProductData)
                                    showEditDialog = false
                                }
                            } catch (e: NumberFormatException) {
                                // Handle error
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LazRed,
                        contentColor = LazWhite
                    )
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showEditDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LazGray,
                        contentColor = LazWhite
                    )
                ) {
                    Text("Cancel")
                }
            },
            containerColor = LazDarkCard
        )
    }
}

@Composable
fun ProductCard(
    product: Product,
    canEdit: Boolean = true,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LazDarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = LazWhite
                )
                Text(
                    text = "Qty: ${product.quantity} | Shelf: ${product.shelfLocation ?: "N/A"}",
                    fontSize = 12.sp,
                    color = LazLightGray
                )
                Text(
                    text = "Cost: JOD ${product.cost} | Price: JOD ${product.price}",
                    fontSize = 12.sp,
                    color = LazRedGlow
                )
            }

            // Only show edit/delete buttons for admins
            if (canEdit) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = LazRed
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = LazRedGlow
                        )
                    }
                }
            }
        }
    }
}
