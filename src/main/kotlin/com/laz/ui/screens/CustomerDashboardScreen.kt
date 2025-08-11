package com.laz.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.laz.models.User
import com.laz.viewmodels.*
import java.text.NumberFormat
import java.util.*

/**
 * Rich Customer Dashboard Screen
 * Displays customer shopping overview and available actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDashboardScreen(
    user: User,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToShopping: () -> Unit = {},
    onNavigateToCart: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToOrderHistory: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    productViewModel: SecureFirebaseProductViewModel = viewModel(factory = FirebaseServices.secureViewModelFactory),
    cartViewModel: SecureFirebaseCartViewModel = viewModel(factory = FirebaseServices.secureViewModelFactory)
) {
    // Collect state from ViewModels
    val products by productViewModel.getCustomerProducts().collectAsState()
    val cartItemCount by cartViewModel.cartItemCount.collectAsState()
    val cartTotal by cartViewModel.cartTotal.collectAsState()
    val isLoading by productViewModel.isLoading.collectAsState()
    val errorMessage by productViewModel.errorMessage.collectAsState()
    
    // Calculate statistics
    val availableProducts = products.filter { it.quantity > 0 }.size
    val totalProducts = products.size
    val newProducts = products.filter { it.id > 0 }.take(5).size // Placeholder logic for "new" products
    
    // Load data on screen start
    LaunchedEffect(Unit) {
        productViewModel.loadProducts()
        cartViewModel.loadCartItems()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Customer Dashboard",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Cart badge (only show badge if items > 0)
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
                            IconButton(onClick = onNavigateToCart) {
                                Icon(
                                    Icons.Default.ShoppingCart,
                                    contentDescription = if (cartItemCount > 0) "Cart ($cartItemCount items)" else "Cart (empty)",
                                    tint = if (cartItemCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Customer",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            user.username,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Button(
                            onClick = onLogout,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Logout")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Welcome Section
            item {
                WelcomeSection(user = user)
            }
            
            // Shopping Overview
            item {
                Text(
                    "Shopping Overview",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            // Top Row Statistics
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "Available Products",
                        value = availableProducts.toString(),
                        icon = Icons.Default.Storefront,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    ) { onNavigateToShopping() }
                    
                    StatCard(
                        title = "Total Products",
                        value = totalProducts.toString(),
                        icon = Icons.Default.Inventory,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    ) { onNavigateToShopping() }
                }
            }
            
            // Bottom Row Statistics
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "Cart Total",
                        value = "JOD %.2f".format(cartTotal),
                        icon = Icons.Default.AttachMoney,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    ) { onNavigateToCart() }
                    
                    StatCard(
                        title = "New Products",
                        value = newProducts.toString(),
                        icon = Icons.Default.NewReleases,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    ) { onNavigateToShopping() }
                }
            }
            
            // Customer Actions Section
            item {
                Text(
                    "Customer Actions",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            // Action Cards Grid
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionCard(
                            title = "Browse Products",
                            description = "Explore our product catalog",
                            icon = Icons.Default.Storefront,
                            modifier = Modifier.weight(1f)
                        ) { onNavigateToShopping() }
                        
                        ActionCard(
                            title = "Order Tracking",
                            description = "Track your order status",
                            icon = Icons.Default.LocalShipping,
                            modifier = Modifier.weight(1f)
                        ) { onNavigateToOrderHistory() }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionCard(
                            title = "My Profile",
                            description = "Manage account settings",
                            icon = Icons.Default.AccountCircle,
                            modifier = Modifier.weight(1f)
                        ) { onNavigateToProfile() }
                        
                        ActionCard(
                            title = "Order History",
                            description = "View your past orders",
                            icon = Icons.Default.History,
                            modifier = Modifier.weight(1f)
                        ) { onNavigateToOrderHistory() }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionCard(
                            title = "Chat Support",
                            description = "Get help from our assistant",
                            icon = Icons.AutoMirrored.Filled.Chat,
                            modifier = Modifier.weight(1f)
                        ) { onNavigateToChat() }
                        
                        ActionCard(
                            title = "Customer Support",
                            description = "Get help and assistance",
                            icon = Icons.Default.SupportAgent,
                            modifier = Modifier.weight(1f)
                        ) { onNavigateToChat() }
                    }
                }
            }
            

            
            // Cart Summary (if items in cart)
            if (cartItemCount > 0) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Cart Summary",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "$cartItemCount items • ${NumberFormat.getCurrencyInstance(Locale.US).format(cartTotal)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Button(
                                    onClick = onNavigateToCart,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text("View Cart")
                                }
                            }
                        }
                    }
                }
            }
            
            // Promotional Section
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.LocalOffer,
                                contentDescription = "Offers",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Special Offers",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Check out our latest products and seasonal discounts!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
            
            // Error handling
            errorMessage?.let { error ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeSection(user: User) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Welcome, ${user.username}!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Discover amazing products and enjoy shopping!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
