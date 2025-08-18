package com.laz.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.laz.R
import com.laz.models.User
import com.laz.viewmodels.FirebaseOrdersViewModel
import com.laz.viewmodels.FirebaseReturnsViewModel
import com.laz.viewmodels.FirebaseServices
import com.laz.viewmodels.SecureFirebaseProductViewModel
import com.laz.viewmodels.SecureFirebaseUserViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    user: User,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToUserManagement: () -> Unit,
    onNavigateToProductManagement: () -> Unit,
    onNavigateToPointOfSale: () -> Unit,
    onNavigateToReturnsProcessing: () -> Unit,
    onNavigateToOrderAnalytics: () -> Unit,
    onNavigateToOrderManagement: () -> Unit,
    onNavigateToAIPotentialOrders: () -> Unit = {},
    productViewModel: SecureFirebaseProductViewModel = viewModel(factory = FirebaseServices.secureViewModelFactory),
    userViewModel: SecureFirebaseUserViewModel = viewModel(factory = FirebaseServices.secureViewModelFactory),
    returnsViewModel: FirebaseReturnsViewModel = viewModel(factory = FirebaseServices.viewModelFactory),
    ordersViewModel: FirebaseOrdersViewModel = viewModel(factory = FirebaseServices.secureViewModelFactory)
) {
    // Collect state from ViewModels
    val userStats by userViewModel.getUserStatistics().collectAsState()
    val products by productViewModel.products.collectAsState()
    val errorMessage by userViewModel.errorMessage.collectAsState()

    // Collect returns data
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
    val lowStockProducts = products.count { it.quantity <= 5 }
    val totalEmployees = userStats.employeeCount

    // Calculate today's revenue from orders
    val todaysRevenue = remember(orders) {
        val todayPrefix = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        orders
            .filter {
                it.status == com.laz.models.OrderStatus.DELIVERED &&
                        it.orderDate.toString().startsWith(todayPrefix)
            }
            .sumOf { it.totalAmount.toDouble() }
    }

    // Animation state
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(
                    initialOffsetY = { -60 },
                    animationSpec = tween(500)
                ) + fadeIn(animationSpec = tween(500))
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
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
                                "Admin Dashboard",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Welcome back, ${user.username}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = onLogout,
                            modifier = Modifier.background(
                                MaterialTheme.colorScheme.secondaryContainer,
                                CircleShape
                            )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Logout",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                // Statistics Cards Row
                item {
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = slideInVertically(
                            initialOffsetY = { 40 },
                            animationSpec = tween(600, delayMillis = 200)
                        ) + fadeIn(animationSpec = tween(600, delayMillis = 200))
                    ) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            item {
                                StatCard(
                                    title = "Total Products",
                                    value = totalProducts.toString(),
                                    icon = Icons.Filled.Inventory,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.width(160.dp)
                                ) { onNavigateToProductManagement() }
                            }
                            item {
                                StatCard(
                                    title = "Low Stock",
                                    value = lowStockProducts.toString(),
                                    icon = Icons.Filled.Warning,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.width(160.dp)
                                ) { onNavigateToProductManagement() }
                            }
                            item {
                                StatCard(
                                    title = "Employees",
                                    value = totalEmployees.toString(),
                                    icon = Icons.Filled.People,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.width(160.dp)
                                ) { onNavigateToUserManagement() }
                            }
                            item {
                                StatCard(
                                    title = "Returns",
                                    value = returnsCount.toString(),
                                    icon = Icons.AutoMirrored.Filled.AssignmentReturn,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.width(160.dp)
                                ) { onNavigateToReturnsProcessing() }
                            }
                        }
                    }
                }

                // Today's Revenue
                item {
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = slideInVertically(
                            initialOffsetY = { 40 },
                            animationSpec = tween(600, delayMillis = 300)
                        ) + fadeIn(animationSpec = tween(600, delayMillis = 300))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            StatCard(
                                title = "Total Revenue",
                                value = "JOD ${String.format("%.2f", todaysRevenue)}",
                                icon = Icons.Filled.AttachMoney,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            ) { onNavigateToOrderAnalytics() }
                        }
                    }
                }

                // Quick Actions header
                item {
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = slideInVertically(
                            initialOffsetY = { 40 },
                            animationSpec = tween(600, delayMillis = 400)
                        ) + fadeIn(animationSpec = tween(600, delayMillis = 400))
                    ) {
                        Text(
                            "Quick Actions",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                }

                // Quick Actions rows
                item {
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = slideInVertically(
                            initialOffsetY = { 40 },
                            animationSpec = tween(600, delayMillis = 500)
                        ) + fadeIn(animationSpec = tween(600, delayMillis = 500))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ActionCard(
                                title = "Point of Sale",
                                description = "Create new in-store order",
                                icon = Icons.Filled.PointOfSale,
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToPointOfSale
                            )
                            ActionCard(
                                title = "Manage Orders",
                                description = "View and manage all orders",
                                icon = Icons.Filled.ShoppingCart,
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToOrderManagement
                            )
                        }
                    }
                }

                item {
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = slideInVertically(
                            initialOffsetY = { 40 },
                            animationSpec = tween(600, delayMillis = 600)
                        ) + fadeIn(animationSpec = tween(600, delayMillis = 600))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ActionCard(
                                title = "Process Returns",
                                description = "Handle product returns",
                                icon = Icons.AutoMirrored.Filled.AssignmentReturn,
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToReturnsProcessing
                            )
                            ActionCard(
                                title = "Product Management",
                                description = "Manage inventory and products",
                                icon = Icons.Filled.Inventory,
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToProductManagement
                            )
                        }
                    }
                }

                item {
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = slideInVertically(
                            initialOffsetY = { 40 },
                            animationSpec = tween(600, delayMillis = 700)
                        ) + fadeIn(animationSpec = tween(600, delayMillis = 700))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ActionCard(
                                title = "User Management",
                                description = "Manage employees and customers",
                                icon = Icons.Filled.People,
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToUserManagement
                            )
                            ActionCard(
                                title = "AI Parts Orders",
                                description = "Review customer AI requests",
                                icon = Icons.Filled.SmartToy,
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToAIPotentialOrders
                            )
                        }
                    }
                }

                // Recent Delivered Orders
                item {
                    Text(
                        "Recent Orders",
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
                        Column(modifier = Modifier.padding(16.dp)) {
                            val deliveredOrders =
                                orders.filter { it.status == com.laz.models.OrderStatus.DELIVERED }
                            if (deliveredOrders.isEmpty()) {
                                Text(
                                    "No orders recorded yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                val subset = deliveredOrders.take(3)
                                subset.forEach { order ->
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
                                                order.orderDate.toString()
                                                    .take(16)
                                                    .replace("T", " "),
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
                                    if (order != subset.last()) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        )
                                    }
                                }
                                if (deliveredOrders.size > 3) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(
                                        onClick = onNavigateToOrderAnalytics,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("View All Orders (${deliveredOrders.size})")
                                    }
                                }
                            }
                        }
                    }
                }

                // Recent Orders Activity
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
                        Column(modifier = Modifier.padding(16.dp)) {
                            val recentOrders = orders.take(5)
                            if (recentOrders.isEmpty()) {
                                Text(
                                    "No orders recorded yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
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
                                                order.orderDate.toString()
                                                    .take(16)
                                                    .replace("T", " "),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
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
                                                    com.laz.models.OrderStatus.DELIVERED ->
                                                        MaterialTheme.colorScheme.primary
                                                    com.laz.models.OrderStatus.PENDING ->
                                                        MaterialTheme.colorScheme.secondary
                                                    com.laz.models.OrderStatus.CANCELLED ->
                                                        MaterialTheme.colorScheme.error
                                                    com.laz.models.OrderStatus.RETURNED ->
                                                        MaterialTheme.colorScheme.outline
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
                                        onClick = onNavigateToOrderManagement,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("View All Orders (${orders.size})")
                                    }
                                }
                            }
                        }
                    }
                }

                // Returns error
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

                // Orders error
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

                // General error
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
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(color.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
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
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.secondaryContainer,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}