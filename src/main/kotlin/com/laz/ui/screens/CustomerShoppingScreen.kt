package com.laz.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.laz.models.Product
import com.laz.viewmodels.SecureFirebaseProductViewModel
import com.laz.viewmodels.SecureFirebaseCartViewModel
import com.laz.ui.components.ProductImageDisplay
import com.laz.ui.components.FloatingCartSummary

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

    // Animation states
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isVisible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Custom Header
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(
                    initialOffsetY = { -60 },
                    animationSpec = tween(500)
                ) + fadeIn(animationSpec = tween(500))
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Shop Products",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Browse our Tesla parts collection",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Cart badge
                            BadgedBox(
                                badge = {
                                    if (cartItemCount > 0) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.error
                                        ) {
                                            Text(
                                                text = cartItemCount.toString(),
                                                color = MaterialTheme.colorScheme.onError,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                            ) {
                                IconButton(
                                    onClick = onNavigateToCart,
                                    modifier = Modifier.background(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        CircleShape
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.ShoppingCart,
                                        contentDescription = "Cart",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
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

                // Customer Welcome Message with animation
                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(
                        initialOffsetY = { 40 },
                        animationSpec = tween(600, delayMillis = 200)
                    ) + fadeIn(animationSpec = tween(600, delayMillis = 200))
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.ShoppingBag,
                                    contentDescription = "Shopping",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Welcome to LAZ Store!",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Browse our Tesla parts and add items to your cart.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(
                        initialOffsetY = { 40 },
                        animationSpec = tween(600, delayMillis = 400)
                    ) + fadeIn(animationSpec = tween(600, delayMillis = 400))
                ) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = success,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onTertiary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
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
    
    // Floating Cart Summary
    FloatingCartSummary(
        cartItemCount = cartItemCount,
        cartTotal = cartTotal,
        isVisible = true,
        onCartClick = onNavigateToCart
    )
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
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Product Image - Bigger
            ProductImageDisplay(
                imageUrl = product.imageUrl,
                modifier = Modifier
                    .size(160.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        RoundedCornerShape(12.dp)
                    )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Product Name - Under image
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Price - Under name
            Text(
                text = "JOD ${product.price}",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Stock Status (customer-friendly)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = when {
                        product.quantity > 10 -> Icons.Default.CheckCircle
                        product.quantity > 0 -> Icons.Default.Warning
                        else -> Icons.Default.Cancel
                    },
                    contentDescription = "Stock Status",
                    tint = when {
                        product.quantity > 10 -> MaterialTheme.colorScheme.tertiary
                        product.quantity > 0 -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        product.quantity > 10 -> "In Stock"
                        product.quantity > 0 -> "Limited Stock"
                        else -> "Out of Stock"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        product.quantity > 10 -> MaterialTheme.colorScheme.tertiary
                        product.quantity > 0 -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.error
                    },
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Add to Cart Section - Full width button
            if (canAddToCart && product.quantity > 0) {
                Button(
                    onClick = { showQuantityDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.AddShoppingCart,
                        contentDescription = "Add to Cart",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Add to Cart",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (!canAddToCart) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Login to Purchase",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    )
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Out of Stock",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
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
