package com.laz.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Rich Admin Dashboard Screen
 * Displays comprehensive statistics, analytics, and management actions
 * UPDATED: Now uses Order-based architecture instead of old Sales logic
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    user: User,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToUserManagement: () -> Unit,
    onNavigateToProductManagement: () -> Unit,
    onNavigateToSalesProcessing: () -> Unit,
    onNavigateToReturnsProcessing: () -> Unit,
    onNavigateToSalesOverview: () -> Unit,
    onNavigateToOrderManagement: () -> Unit,
    productViewModel: SecureFirebaseProductViewModel = viewModel(factory = FirebaseServices.secureViewModelFactory),
    userViewModel: SecureFirebaseUserViewModel = viewModel(factory = FirebaseServices.secureViewModelFactory),
    returnsViewModel: FirebaseReturnsViewModel = viewModel(factory = FirebaseServices.viewModelFactory),
    ordersViewModel: FirebaseOrdersViewModel = viewModel(factory = FirebaseServices.secureViewModelFactory)
) {
    // Collect state from ViewModels
    val userStats by userViewModel.getUserStatistics().collectAsState()
    val products by productViewModel.products.collectAsState()
    val errorMessage by userViewModel.errorMessage.collectAsState()
    
    // Collect real Firebase orders and returns data
    val returns by returnsViewModel.returns.collectAsState()
    val returnsCount by returnsViewModel.returnsCount.collectAsState()
    val returnsErrorMessage by returnsViewModel.errorMessage.collectAsState()
    
    // Collect orders data
    val orders by ordersViewModel.orders.collectAsState()
    val ordersErrorMessage by ordersViewModel.errorMessage.collectAsState()
    
    // Load data on screen start
    LaunchedEffect(Unit) {
        userViewModel.loadUsers()
        productViewModel.loadProducts()
        returnsViewModel.loadAllReturns()
        ordersViewModel.loadOrders()
    }
    
    // Calculate statistics
    val totalProducts = products.size
    val lowStockProducts = products.filter { it.quantity <= 5 }.size
    val totalEmployees = userStats.employeeCount
    
    // Calculate today's sales from real Firebase orders data
    val todaysSales = remember(orders) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        orders.filter { order ->
            order.status == com.laz.models.OrderStatus.DELIVERED && 
            order.orderDate.toString().startsWith(today)
        }.sumOf { order ->
            order.totalAmount.toDouble()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Admin Dashboard",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
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
                WelcomeSection(user)
            }
            
            // Statistics Cards Row
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    item {
                        StatCard(
                            title = "Total Products",
                            value = totalProducts.toString(),
                            icon = Icons.Default.Inventory,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.width(160.dp)
                        ) { onNavigateToProductManagement() }
                    }
                    
                    item {
                        StatCard(
                            title = "Low Stock",
                            value = lowStockProducts.toString(),
                            icon = Icons.Default.Warning,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.width(160.dp)
                        ) { onNavigateToProductManagement() }
                    }
                    
                    item {
                        StatCard(
                            title = "Employees",
                            value = totalEmployees.toString(),
                            icon = Icons.Default.People,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.width(160.dp)
                        ) { onNavigateToUserManagement() }
                    }
                    
                    item {
                        StatCard(
                            title = "Returns",
                            value = returnsCount.toString(),
                            icon = Icons.AutoMirrored.Filled.AssignmentReturn,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.width(160.dp)
                        ) { onNavigateToReturnsProcessing() }
                    }
                }
            }
            
            // Today's Sales Card
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Total Sales",
                        value = "JOD ${String.format("%.2f", todaysSales)}",
                        icon = Icons.Default.AttachMoney,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    ) { onNavigateToSalesOverview() }
                    
                    StatCard(
                        title = "Today's Sales",
                        value = "JOD ${String.format("%.2f", todaysSales)}",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    ) { onNavigateToSalesOverview() }
                }
            }
            
            // Quick Actions Section
            item {
                Text(
                    "Quick Actions",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionCard(
                        title = "Process Sale",
                        description = "Create new sale transaction",
                        icon = Icons.Default.PointOfSale,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToSalesProcessing
                    )
                    
                    ActionCard(
                        title = "Manage Orders",
                        description = "View and manage all orders",
                        icon = Icons.Default.ShoppingCart,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToOrderManagement
                    )
                }
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionCard(
                        title = "Process Returns",
                        description = "Handle product returns",
                        icon = Icons.AutoMirrored.Filled.AssignmentReturn,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToReturnsProcessing
                    )
                    
                    ActionCard(
                        title = "User Management",
                        description = "Manage employees and customers",
                        icon = Icons.Default.People,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToUserManagement
                    )
                }
            }
            
            // Recent Sales List - FIXED: Now uses Order-based logic
            item {
                Text(
                    "Recent Sales",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        val deliveredOrders = orders.filter { it.status == com.laz.models.OrderStatus.DELIVERED }
                        if (deliveredOrders.isEmpty()) {
                            Text(
                                "No sales recorded yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            // Show last 3 delivered orders (sales)
                            deliveredOrders.take(3).forEach { order ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            "Order #${order.id.toString().take(8)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            order.orderDate.toString().take(16).replace("T", " "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        "JOD ${order.totalAmount}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (order != deliveredOrders.take(3).last()) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                                }
                            }
                            
                            if (deliveredOrders.size > 3) {
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = { onNavigateToSalesOverview() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("View All Sales (${deliveredOrders.size})")
                                }
                            }
                        }
                    }
                }
            }
            
            // Recent Orders Activity Section
            item {
                Text(
                    "Recent Orders Activity",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        val recentOrders = orders.take(5)
                        if (recentOrders.isEmpty()) {
                            Text(
                                "No orders recorded yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            // Show last 5 orders
                            recentOrders.forEach { order ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            "Order #${order.id.toString().take(8)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            order.orderDate.toString().take(16).replace("T", " "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            "JOD ${order.totalAmount}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            order.status.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = when (order.status) {
                                                com.laz.models.OrderStatus.DELIVERED -> MaterialTheme.colorScheme.primary
                                                com.laz.models.OrderStatus.PENDING -> MaterialTheme.colorScheme.secondary
                                                com.laz.models.OrderStatus.CANCELLED -> MaterialTheme.colorScheme.error
                                                com.laz.models.OrderStatus.RETURNED -> MaterialTheme.colorScheme.outline
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                }
                                if (order != recentOrders.last()) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                                }
                            }
                            
                            if (orders.size > 5) {
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = { onNavigateToOrderManagement() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("View All Orders (${orders.size})")
                                }
                            }
                        }
                    }
                }
            }
            
            // Returns error handling
            returnsErrorMessage?.let { error ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            "Returns Error: $error",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            
            // Orders error handling
            ordersErrorMessage?.let { error ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            "Orders Error: $error",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            
            // General error handling
            errorMessage?.let { error ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            "Error: $error",
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
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                "Welcome back, ${user.username}!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                "Here's your store overview for today",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
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
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
