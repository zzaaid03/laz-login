package com.laz.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.laz.models.Product
import com.laz.viewmodels.SecureFirebaseProductViewModel
import com.laz.viewmodels.SecureFirebaseCartViewModel
import com.laz.ui.components.FloatingCartSummary
import com.laz.ui.components.ProductImageDisplay

/**
 * Customer Shopping Screen
 * Customer-only screen for browsing and purchasing products
 * - Can view products that are in stock
 * - Can add items to cart
 * - CANNOT see admin details (cost, shelf location, etc.)
 * - CANNOT manage inventory or edit products
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerShoppingScreen(
    productViewModel: SecureFirebaseProductViewModel,
    cartViewModel: SecureFirebaseCartViewModel,
    onNavigateToCart: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val products by productViewModel.getCustomerProducts().collectAsState()
    val cartItemCount by cartViewModel.cartItemCount.collectAsState()
    val cartTotal by cartViewModel.cartTotal.collectAsState()
    val isLoading by productViewModel.isLoading.collectAsState()
    val errorMessage by productViewModel.errorMessage.collectAsState()
    val permissionError by productViewModel.permissionError.collectAsState()
    val cartOperationSuccess by cartViewModel.operationSuccess.collectAsState()
    val cartError by cartViewModel.errorMessage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shop Products") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Cart Icon with Badge
                    BadgedBox(
                        badge = {
                            if (cartItemCount > 0) {
                                Badge {
                                    Text(cartItemCount.toString())
                                }
                            }
                        }
                    ) {
                        IconButton(onClick = onNavigateToCart) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    titleContentColor = MaterialTheme.colorScheme.onTertiary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onTertiary,
                    actionIconContentColor = MaterialTheme.colorScheme.onTertiary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Permission Error Display
            permissionError?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = "Permission Error",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Customer Welcome Message
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ShoppingBag,
                        contentDescription = "Shopping",
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "Welcome to LAZ Store!",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "Browse our products and add items to your cart.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Products List
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (products.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Inventory,
                            contentDescription = "No Products",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Products Available",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Check back later for new products!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(products) { product ->
                        CustomerProductCard(
                            product = product,
                            onAddToCart = { productId, quantity ->
                                cartViewModel.addToCart(productId, quantity)
                            },
                            canAddToCart = cartViewModel.canUseShoppingCart()
                        )
                    }
                }
            }
            
            // Error Messages
             errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(8.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            cartError?.let { error ->
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(8.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            // Success Messages
            cartOperationSuccess?.let { success ->
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text(
                        text = success,
                        modifier = Modifier.padding(8.dp),
                        color = Color.White
                    )
                }
            }
        }
    }
    
    // Clear messages after showing them
    LaunchedEffect(errorMessage, cartError, cartOperationSuccess) {
        if (errorMessage != null) {
            kotlinx.coroutines.delay(3000)
            productViewModel.clearErrors()
        }
        if (cartError != null || cartOperationSuccess != null) {
            kotlinx.coroutines.delay(3000)
            cartViewModel.clearMessages()
        }
    }
    
    // Floating Cart Summary - Show only if cart has items
    if (cartItemCount > 0) {
        FloatingCartSummary(
            cartItemCount = cartItemCount,
            cartTotal = cartTotal,
            isVisible = true,
            onCartClick = onNavigateToCart
        )
    }
}

@Composable
private fun CustomerProductCard(
    product: Product,
    onAddToCart: (Long, Int) -> Unit,
    canAddToCart: Boolean
) {
    var quantity by remember { mutableStateOf(1) }
    var showQuantityDialog by remember { mutableStateOf(false) }
    
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
                verticalAlignment = Alignment.Top
            ) {
                // Product Image
                ProductImageDisplay(
                    imageUrl = product.imageUrl,
                    modifier = Modifier.size(80.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "$${product.price}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Stock Status (customer-friendly)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when {
                                product.quantity > 10 -> Icons.Default.CheckCircle
                                product.quantity > 0 -> Icons.Default.Warning
                                else -> Icons.Default.Cancel
                            },
                            contentDescription = "Stock Status",
                            tint = when {
                                product.quantity > 10 -> Color(0xFF4CAF50)
                                product.quantity > 0 -> Color(0xFFFF9800)
                                else -> MaterialTheme.colorScheme.error
                            },
                            modifier = Modifier.size(8.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = when {
                                product.quantity > 10 -> "In Stock"
                                product.quantity > 0 -> "Limited Stock"
                                else -> "Out of Stock"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                product.quantity > 10 -> Color(0xFF4CAF50)
                                product.quantity > 0 -> Color(0xFFFF9800)
                                else -> MaterialTheme.colorScheme.error
                            },
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Add to Cart Section
                if (canAddToCart && product.quantity > 0) {
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Button(
                            onClick = { showQuantityDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Icon(
                                Icons.Default.AddShoppingCart,
                                contentDescription = "Add to Cart",
                                modifier = Modifier.size(9.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Add to Cart")
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Quick Add Button
                        OutlinedButton(
                            onClick = { onAddToCart(product.id, 1) },
                            modifier = Modifier.size(width = 60.dp, height = 18.dp),
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Text(
                                "Quick Add",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                } else if (!canAddToCart) {
                    Text(
                        text = "Login to Purchase",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Out of Stock",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
    
    // Quantity Selection Dialog
    if (showQuantityDialog) {
        AlertDialog(
            onDismissRequest = { showQuantityDialog = false },
            title = { Text("Select Quantity") },
            text = {
                Column {
                    Text("Product: ${product.name}")
                    Text("Price: $${product.price} each")
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { if (quantity > 1) quantity-- },
                            enabled = quantity > 1
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease")
                        }
                        
                        Text(
                            text = quantity.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.width(20.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        
                        IconButton(
                            onClick = { if (quantity < product.quantity) quantity++ },
                            enabled = quantity < product.quantity
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase")
                        }
                    }
                    
                    Text(
                        text = "Total: $${product.price * quantity.toBigDecimal()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddToCart(product.id, quantity)
                        showQuantityDialog = false
                        quantity = 1 // Reset for next time
                    }
                ) {
                    Text("Add to Cart")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuantityDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
