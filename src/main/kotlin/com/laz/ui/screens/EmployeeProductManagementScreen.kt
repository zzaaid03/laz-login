package com.laz.ui.screens

import android.annotation.SuppressLint
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import com.laz.ui.components.ProductImageDisplay
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.laz.models.Product
import com.laz.ui.components.ImagePicker
import com.laz.ui.components.ProductImageDisplay
import com.laz.viewmodels.SecureFirebaseProductViewModel
import com.laz.services.FirebaseStorageService
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

// Default spacing values
private object DefaultSpacing {
    val small = 8.dp
    val medium = 16.dp
    val large = 24.dp
}

// Create a LocalSpacing CompositionLocal
private val LocalSpacing = staticCompositionLocalOf { DefaultSpacing }

// Constants
private const val LOW_STOCK_THRESHOLD = 10

// Sealed class for tab state
sealed class ProductTab(val title: String) {
    object All : ProductTab("All Products")
    object LowStock : ProductTab("Low Stock")
    object OutOfStock : ProductTab("Out of Stock")
}

// Data class for product state
data class ProductUiState(
    val products: List<Product> = emptyList(),
    val lowStockProducts: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// Define a CompositionLocal for the current user role
val LocalUserRole = staticCompositionLocalOf<String?> { null }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeProductManagementScreen(
    productViewModel: SecureFirebaseProductViewModel,
    userRole: String?,
    onBackClick: () -> Unit
) {
    val products by productViewModel.products.collectAsState()
    val isLoading by productViewModel.isLoading.collectAsState()
    val errorMessage by productViewModel.errorMessage.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        productViewModel.loadProducts()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Product Management") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Only show Add button for Admin
                    if (userRole == "ADMIN") {
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Product"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (products.isEmpty() && !isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "No products available",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            if (userRole == "ADMIN") {
                                Text(
                                    text = "Add your first product to get started",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            } else {
                items(products) { product ->
                    ProductItemCard(
                        product = product,
                        onEditClick = if (userRole == "ADMIN") {
                            {
                                selectedProduct = product
                                showEditDialog = true
                            }
                        } else null,
                        onDeleteClick = if (userRole == "ADMIN") {
                            {
                                selectedProduct = product
                                showDeleteDialog = true
                            }
                        } else null
                    )
                }
            }
            
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }

    // Add Product Dialog
    if (showAddDialog) {
        ProductFormDialogWithImage(
            onDismiss = { showAddDialog = false },
            onSave = { newProduct ->
                coroutineScope.launch {
                    productViewModel.addProduct(newProduct)
                    showAddDialog = false
                }
            }
        )
    }

    // Edit Product Dialog
    if (showEditDialog && selectedProduct != null) {
        ProductFormDialogWithImage(
            product = selectedProduct,
            isEditing = true,
            onDismiss = { 
                showEditDialog = false
                selectedProduct = null
            },
            onSave = { updatedProduct ->
                coroutineScope.launch {
                    productViewModel.updateProduct(updatedProduct)
                    showEditDialog = false
                    selectedProduct = null
                }
            }
        )
    }

    // Delete Product Confirmation Dialog
    if (showDeleteDialog && selectedProduct != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                selectedProduct = null
            },
            title = {
                Text("Delete Product")
            },
            text = {
                Text("Are you sure you want to delete \"${selectedProduct!!.name}\"? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            productViewModel.deleteProduct(selectedProduct!!.id)
                            showDeleteDialog = false
                            selectedProduct = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showDeleteDialog = false
                        selectedProduct = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Error handling
    errorMessage?.let { error ->
        LaunchedEffect(error) {
            // Handle error display - could show a snackbar or dialog
        }
    }
}

// Simple tab indicator - removing complex offset function
@Composable
private fun SimpleTabIndicator() {
    // This will be handled by TabRowDefaults.Indicator
}

@Composable
private fun ProductItem(
    product: Product,
    userRole: String? = null,
    onEditClick: (Product) -> Unit = {},
    onDeleteClick: (Product) -> Unit = {},
    onInventoryClick: (Product) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp)
        ) {
            // Product Image
            ProductImageDisplay(
                imageUrl = product.imageUrl,
                modifier = Modifier.padding(end = 16.dp),
                size = 80
            )
            
            // Product Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Stock status indicator
                    val stockStatusColor = when {
                        product.quantity <= 0 -> MaterialTheme.colorScheme.error
                        product.quantity <= LOW_STOCK_THRESHOLD -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    }
                    
                    Text(
                        text = if (product.quantity > 0) "${product.quantity} in stock" else "Out of stock",
                        color = stockStatusColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp)
        ) {
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Price and shelf location
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${NumberFormat.getCurrencyInstance(Locale.US).format(product.price)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "Shelf: ${product.shelfLocation}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Role-based action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (userRole == "ADMIN") {
                    // Admin can edit, delete, and update inventory
                    IconButton(onClick = { onInventoryClick(product) }) {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = "Inventory"
                        )
                    }
                    
                    IconButton(onClick = { onEditClick(product) }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Product"
                        )
                    }
                    
                    IconButton(onClick = { onDeleteClick(product) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Product"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProductItemCard(
    product: Product,
    onEditClick: (() -> Unit)?,
    onDeleteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product Image
            ProductImageDisplay(
                imageUrl = product.imageUrl,
                modifier = Modifier.size(60.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Product Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$${product.price}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Qty: ${product.quantity} | ${product.shelfLocation ?: "No location"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Admin Action Buttons
            if (onEditClick != null) {
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Product"
                    )
                }
            }
            
            if (onDeleteClick != null) {
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Product",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormDialogWithImage(
    product: Product? = null,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit,
    isEditing: Boolean = false
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var price by remember { mutableStateOf(product?.price?.toString() ?: "") }
    var cost by remember { mutableStateOf(product?.cost?.toString() ?: "") }
    var quantity by remember { mutableStateOf(product?.quantity?.toString() ?: "") }
    var shelfLocation by remember { mutableStateOf(product?.shelfLocation ?: "") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }
    
    var showError by remember { mutableStateOf(false) }
    var dialogErrorMessage by remember { mutableStateOf("") }
    
    // Firebase Storage Service
    val storageService = remember { FirebaseStorageService() }
    val coroutineScope = rememberCoroutineScope()
    
    // Form validation
    val isFormValid = name.isNotBlank() && 
                     price.toDoubleOrNull() != null && 
                     cost.toDoubleOrNull() != null &&
                     quantity.toIntOrNull() != null &&
                     shelfLocation.isNotBlank()
    
    // Update form when product changes
    LaunchedEffect(product) {
        name = product?.name ?: ""
        price = product?.price?.toString() ?: ""
        cost = product?.cost?.toString() ?: ""
        quantity = product?.quantity?.toString() ?: ""
        shelfLocation = product?.shelfLocation ?: ""
    }
    
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        androidx.compose.material3.Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f)
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Title with icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isEditing) "Edit Product" else "Add Product",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Scrollable form content
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        // Form fields
                        Column {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    
                    item {
                        OutlinedTextField(
                            value = price,
                            onValueChange = { price = it },
                            label = { Text("Price") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    item {
                        OutlinedTextField(
                            value = cost,
                            onValueChange = { cost = it },
                            label = { Text("Cost") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    item {
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { quantity = it },
                            label = { Text("Quantity") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    item {
                        OutlinedTextField(
                            value = shelfLocation,
                            onValueChange = { shelfLocation = it },
                            label = { Text("Shelf Location") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    item {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Image Picker Section
                            Text(
                                text = "Product Image",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            ImagePicker(
                                currentImageUrl = product?.imageUrl,
                                selectedImageUri = selectedImageUri,
                                onImageSelected = { uri -> selectedImageUri = uri },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp) // Limit height to make it more compact
                            )
                        }
                    }
                    
                    item {
                        // Error message
                        if (showError) {
                            Text(
                                text = dialogErrorMessage,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
                
                // Buttons - Fixed at bottom
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Button(
                        onClick = {
                            if (isFormValid && !isUploading) {
                                coroutineScope.launch {
                                    try {
                                        println("DEBUG: Save button clicked - starting product save process")
                                        isUploading = true
                                        showError = false
                                        
                                        // Handle image upload if a new image is selected
                                        var finalImageUrl = product?.imageUrl
                                        println("DEBUG: Initial imageUrl: $finalImageUrl")
                                        println("DEBUG: Selected image URI: $selectedImageUri")
                                        
                                        if (selectedImageUri != null) {
                                            println("DEBUG: Image selected, starting upload...")
                                            uploadProgress = 0.3f
                                            val uploadResult = storageService.uploadProductImage(
                                                selectedImageUri!!, 
                                                product?.id ?: System.currentTimeMillis()
                                            )
                                            
                                            uploadResult.fold(
                                                onSuccess = { downloadUrl ->
                                                    println("DEBUG: Image upload successful: $downloadUrl")
                                                    finalImageUrl = downloadUrl
                                                    uploadProgress = 1.0f
                                                },
                                                onFailure = { exception ->
                                                    println("DEBUG: Image upload failed: ${exception.message}")
                                                    showError = true
                                                    dialogErrorMessage = "Failed to upload image: ${exception.message}"
                                                    isUploading = false
                                                    return@launch
                                                }
                                            )
                                        } else {
                                            println("DEBUG: No image selected, proceeding with existing imageUrl")
                                        }
                                        
                                        // Create and save the product
                                        val productToSave = Product(
                                            id = product?.id ?: 0L,
                                            name = name,
                                            price = BigDecimal(price),
                                            cost = BigDecimal(cost),
                                            quantity = quantity.toInt(),
                                            shelfLocation = shelfLocation,
                                            imageUrl = finalImageUrl
                                        )
                                        
                                        println("DEBUG: Product to save: $productToSave")
                                        println("DEBUG: Calling onSave...")
                                        onSave(productToSave)
                                        println("DEBUG: onSave completed")
                                        isUploading = false
                                        onDismiss()
                                        
                                    } catch (e: Exception) {
                                        println("DEBUG: Exception in save process: ${e.message}")
                                        e.printStackTrace()
                                        isUploading = false
                                        showError = true
                                        dialogErrorMessage = "Error saving product: ${e.message}"
                                    }
                                }
                            } else if (!isFormValid) {
                                showError = true
                                dialogErrorMessage = "Please fill out all fields correctly"
                            }
                        },
                        enabled = isFormValid && !isUploading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    ) {
                        if (isUploading) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Text("Uploading...")
                            }
                        } else {
                            Text(if (isEditing) "Save Changes" else "Add Product")
                        }
                    }
                }
            }
        }
    }
}
