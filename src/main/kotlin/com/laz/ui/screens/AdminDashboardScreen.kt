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
    salesViewModel: FirebaseSalesViewModel = viewModel(factory = FirebaseServices.viewModelFactory),
<<<<<<< Updated upstream
    returnsViewModel: FirebaseReturnsViewModel = viewModel(factory = FirebaseServices.viewModelFactory),
    ordersViewModel: FirebaseOrdersViewModel = viewModel(factory = FirebaseServices.secureViewModelFactory)
=======
    returnsViewModel: FirebaseReturnsViewModel = viewModel(factory = FirebaseServices.viewModelFactory)
>>>>>>> Stashed changes
) {
    // Collect state from ViewModels
    val userStats by userViewModel.getUserStatistics().collectAsState()
    val products by productViewModel.products.collectAsState()
    val errorMessage by userViewModel.errorMessage.collectAsState()
    
    // Collect real Firebase sales and returns data
    val sales by salesViewModel.sales.collectAsState()
    val totalSalesAmount by salesViewModel.totalSalesAmount.collectAsState()
    val salesCount by salesViewModel.salesCount.collectAsState()
    val returns by returnsViewModel.returns.collectAsState()
    val returnsCount by returnsViewModel.returnsCount.collectAsState()
    val salesErrorMessage by salesViewModel.errorMessage.collectAsState()
    val returnsErrorMessage by returnsViewModel.errorMessage.collectAsState()
    
<<<<<<< Updated upstream
    // Collect orders data
    val orders by ordersViewModel.orders.collectAsState()
    val ordersCount by ordersViewModel.ordersCount.collectAsState()
    val totalOrdersAmount by ordersViewModel.totalOrdersAmount.collectAsState()
    val ordersErrorMessage by ordersViewModel.errorMessage.collectAsState()
    
=======
>>>>>>> Stashed changes
    // Load data on screen start
    LaunchedEffect(Unit) {
        userViewModel.loadUsers()
        productViewModel.loadProducts()
        salesViewModel.loadAllSales()
        returnsViewModel.loadAllReturns()
<<<<<<< Updated upstream
        ordersViewModel.loadOrders()
=======
>>>>>>> Stashed changes
    }
    
    // Calculate statistics from loaded data
    val totalUsers = userStats.totalUsers
    val totalProducts = products.size
    val lowStockProducts = products.filter { it.quantity <= 5 }.size
    val totalEmployees = userStats.employeeCount
    
    // Calculate today's sales from real Firebase data
    val todaysSales = remember(sales) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        sales.filter { sale ->
            sale.date.startsWith(today)
        }.sumOf { sale ->
            // Parse price from "JOD XX.XX" format
            sale.productPrice.replace("JOD ", "").toDoubleOrNull() ?: 0.0
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
                        Icon(
                            Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin",
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
            
            // Statistics Overview
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Real-time Dashboard",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    // Live indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    Color.Green,
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                        )
                        Text(
                            "Live",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Green,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // Top Row Statistics
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "Total Sales",
                        value = "JOD ${String.format("%.2f", totalSalesAmount)}",
                        icon = Icons.Default.AttachMoney,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    ) { onNavigateToSalesOverview() }
                    
                    StatCard(
                        title = "Products",
                        value = totalProducts.toString(),
                        icon = Icons.Default.Inventory,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    ) { onNavigateToProductManagement() }
                }
            }
            
            // Bottom Row Statistics
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "Low Stock",
                        value = lowStockProducts.toString(),
                        icon = Icons.Default.Warning,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    ) { onNavigateToProductManagement() }
                    
                    StatCard(
                        title = "Employees",
                        value = totalEmployees.toString(),
                        icon = Icons.Default.People,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    ) { onNavigateToUserManagement() }
                }
            }
            
            // Second Row Statistics
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "Today's Sales",
                        value = "JOD ${String.format("%.2f", todaysSales)}",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    ) { onNavigateToSalesOverview() }
                    
                    StatCard(
                        title = "Returns",
                        value = returnsCount.toString(),
                        icon = Icons.AutoMirrored.Filled.AssignmentReturn,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    ) { onNavigateToReturnsProcessing() }
                }
            }
            
            // Third Row Statistics - Orders
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "Total Orders",
                        value = ordersCount.toString(),
                        icon = Icons.Default.Assignment,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    ) { onNavigateToOrderManagement() }
                    
                    StatCard(
                        title = "Orders Value",
                        value = "JOD ${String.format("%.2f", totalOrdersAmount)}",
                        icon = Icons.Default.MonetizationOn,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    ) { onNavigateToOrderManagement() }
                }
            }
            
            // Admin Actions Section
            item {
                Text(
                    "Admin Actions",
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
                            title = "Product Management",
                            description = "Manage inventory, add/edit products",
                            icon = Icons.Default.Inventory,
                            modifier = Modifier.weight(1f)
                        ) { onNavigateToProductManagement() }
                        
                        ActionCard(
                            title = "User Management",
                            description = "Manage user accounts and roles",
                            icon = Icons.Default.People,
                            modifier = Modifier.weight(1f)
                        ) { onNavigateToUserManagement() }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionCard(
                            title = "Sales Processing",
                            description = "Process new sales",
                            icon = Icons.Default.PointOfSale,
                            modifier = Modifier.weight(1f)
                        ) { onNavigateToSalesProcessing() }
                        
                        ActionCard(
                            title = "Returns Processing",
                            description = "Process customer returns",
                            icon = Icons.Filled.AssignmentReturn,
                            modifier = Modifier.weight(1f)
                        ) { onNavigateToReturnsProcessing() }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionCard(
                            title = "Sales Overview",
                            description = "View sales analytics and reports",
                            icon = Icons.Default.Analytics,
                            modifier = Modifier.weight(1f)
                        ) { onNavigateToSalesOverview() }
                        
                        ActionCard(
                            title = "Order Management",
                            description = "Manage customer orders and fulfillment",
                            icon = Icons.Default.Assignment,
                            modifier = Modifier.weight(1f)
                        ) { onNavigateToOrderManagement() }
                    }
                }
            }
            
            // Recent Sales Activity Section
            item {
                Text(
                    "Recent Sales Activity",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            // Recent Sales List
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
                        if (sales.isEmpty()) {
                            Text(
                                "No sales recorded yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            // Show last 3 sales
                            sales.take(3).forEach { sale ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            "Sale #${sale.id.toString().take(8)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            sale.date.take(16).replace("T", " "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        sale.productPrice,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (sale != sales.take(3).last()) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                                }
                            }
                            
                            if (sales.size > 3) {
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = { onNavigateToSalesOverview() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("View All Sales (${sales.size})")
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
            
            // Recent Orders List
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
                        if (orders.isEmpty()) {
                            Text(
                                "No orders recorded yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            // Show last 3 orders
                            orders.take(3).forEach { order ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            "Order #${order.id}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            "${order.customerUsername} • ${order.status.name}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        "JOD ${String.format("%.2f", order.totalAmount)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (order != orders.take(3).last()) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                                }
                            }
                            
                            if (orders.size > 3) {
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
            
            // Recent Sales Activity Section
            item {
                Text(
                    "Recent Sales Activity",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            // Recent Sales List
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
                        if (sales.isEmpty()) {
                            Text(
                                "No sales recorded yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            // Show last 3 sales
                            sales.take(3).forEach { sale ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            "Sale #${sale.id.toString().take(8)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            sale.date.take(16).replace("T", " "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        sale.productPrice,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (sale != sales.take(3).last()) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                                }
                            }
                            
                            if (sales.size > 3) {
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = { onNavigateToSalesOverview() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("View All Sales (${sales.size})")
                                }
                            }
                        }
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
            
            // Sales and Returns error handling
            salesErrorMessage?.let { error ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = "Sales Error: $error",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            
            returnsErrorMessage?.let { error ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = "Returns Error: $error",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
<<<<<<< Updated upstream
            
            ordersErrorMessage?.let { error ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = "Orders Error: $error",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
=======
>>>>>>> Stashed changes
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
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Welcome back, ${user.username}!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "Role: ADMIN",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
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
