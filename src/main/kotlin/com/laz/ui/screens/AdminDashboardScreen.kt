package com.laz.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
    productViewModel: SecureFirebaseProductViewModel = viewModel(factory = FirebaseServices.secureViewModelFactory),
    userViewModel: SecureFirebaseUserViewModel = viewModel(factory = FirebaseServices.secureViewModelFactory)
) {
    // Collect state from ViewModels
    val userStats by userViewModel.getUserStatistics().collectAsState()
    val products by productViewModel.products.collectAsState()
    val errorMessage by userViewModel.errorMessage.collectAsState()
    
    // Load data on screen start
    LaunchedEffect(Unit) {
        userViewModel.loadUsers()
        productViewModel.loadProducts()
    }
    
    // Calculate statistics from loaded data
    val totalUsers = userStats.totalUsers
    val totalProducts = products.size
    val lowStockProducts = products.filter { it.quantity <= 5 }.size
    val totalEmployees = userStats.employeeCount
    
    // Mock data for sales and returns (since Firebase sales/returns aren't fully implemented)
    val todaysSales = remember { mutableStateOf(BigDecimal("1250.75")) }
    val totalReturns = remember { mutableStateOf(3) }
    
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
                Text(
                    "Overview",
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
                        title = "Total Sales",
                        value = NumberFormat.getCurrencyInstance(Locale.US).format(todaysSales.value),
                        icon = Icons.Default.AttachMoney,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    ) { onNavigateToSalesOverview() }
                    
                    StatCard(
                        title = "Products",
                        value = totalProducts.toString(),
                        icon = Icons.Default.Inventory,
                        color = Color(0xFF2196F3),
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
                        color = Color(0xFFFF9800),
                        modifier = Modifier.weight(1f)
                    ) { onNavigateToProductManagement() }
                    
                    StatCard(
                        title = "Employees",
                        value = totalEmployees.toString(),
                        icon = Icons.Default.People,
                        color = Color(0xFF9C27B0),
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
                        value = NumberFormat.getCurrencyInstance(Locale.US).format(todaysSales.value),
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    ) { /* Navigate to today's sales */ }
                    
                    StatCard(
                        title = "Returns",
                        value = totalReturns.value.toString(),
                        icon = Icons.AutoMirrored.Filled.AssignmentReturn,
                        color = Color(0xFFF44336),
                        modifier = Modifier.weight(1f)
                    ) { onNavigateToReturnsProcessing() }
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
