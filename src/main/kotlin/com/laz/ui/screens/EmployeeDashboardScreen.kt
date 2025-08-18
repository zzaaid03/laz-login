package com.laz.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.RemoveShoppingCart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.laz.models.User
import com.laz.viewmodels.FirebaseServices
import com.laz.viewmodels.SecureFirebaseProductViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeDashboardScreen(
    user: User,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToProductManagement: () -> Unit = {},
    onNavigateToPointOfSale: () -> Unit = {},
    onNavigateToReturnsProcessing: () -> Unit = {},
    onNavigateToOrderManagement: () -> Unit = {},
    onNavigateToChatManagement: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    productViewModel: SecureFirebaseProductViewModel = viewModel(factory = FirebaseServices.secureViewModelFactory)
) {
    val products by productViewModel.products.collectAsState()
    val errorMessage by productViewModel.errorMessage.collectAsState()

    val totalProducts = products.size
    val lowStockProducts = products.count { it.quantity <= 5 && it.quantity > 0 }
    val outOfStockProducts = products.count { it.quantity == 0 }

    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        productViewModel.loadProducts()
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
                                "Employee Dashboard",
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
                                Icons.Filled.Logout,
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

                item {
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = slideInVertically(
                            initialOffsetY = { 40 },
                            animationSpec = tween(600, delayMillis = 200)
                        ) + fadeIn(animationSpec = tween(600, delayMillis = 200))
                    ) {
                        Column {
                            Text(
                                "Inventory Overview",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 16.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )

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
                                        title = "Out of Stock",
                                        value = outOfStockProducts.toString(),
                                        icon = Icons.Filled.RemoveShoppingCart,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.width(160.dp)
                                    ) { onNavigateToProductManagement() }
                                }

                                item {
                                    StatCard(
                                        title = "Today's Revenue",
                                        value = "JOD 0.00",
                                        icon = Icons.Filled.TrendingUp,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.width(160.dp)
                                    ) { }
                                }
                            }
                        }
                    }
                }

                item {
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = slideInVertically(
                            initialOffsetY = { 40 },
                            animationSpec = tween(600, delayMillis = 400)
                        ) + fadeIn(animationSpec = tween(600, delayMillis = 400))
                    ) {
                        Column {
                            Text(
                                "Employee Actions",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 16.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Column(
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    ActionCard(
                                        title = "Product Management",
                                        description = "Manage inventory, update stock",
                                        icon = Icons.Filled.Inventory,
                                        modifier = Modifier.weight(1f)
                                    ) { onNavigateToProductManagement() }

                                    ActionCard(
                                        title = "Point of Sale",
                                        description = "Process in-store orders",
                                        icon = Icons.Filled.PointOfSale,
                                        modifier = Modifier.weight(1f)
                                    ) { onNavigateToPointOfSale() }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    ActionCard(
                                        title = "Returns Processing",
                                        description = "Handle customer returns",
                                        icon = Icons.Filled.AssignmentReturn,
                                        modifier = Modifier.weight(1f)
                                    ) { onNavigateToReturnsProcessing() }

                                    ActionCard(
                                        title = "Order Management",
                                        description = "Manage customer orders",
                                        icon = Icons.Filled.Assignment,
                                        modifier = Modifier.weight(1f)
                                    ) { onNavigateToOrderManagement() }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    ActionCard(
                                        title = "Customer Support",
                                        description = "Manage customer chats",
                                        icon = Icons.Filled.SupportAgent,
                                        modifier = Modifier.weight(1f)
                                    ) { onNavigateToChatManagement() }

                                    ActionCard(
                                        title = "Settings",
                                        description = "System settings",
                                        icon = Icons.Filled.Settings,
                                        modifier = Modifier.weight(1f)
                                    ) { onNavigateToProfile() }
                                }
                            }
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
                        Column {
                            Text(
                                "Quick Actions",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 16.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Button(
                                    onClick = onNavigateToPointOfSale,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.PointOfSale,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Quick Sale",
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }

                                Button(
                                    onClick = onNavigateToProductManagement,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Add Product",
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                if (lowStockProducts > 0) {
                    item {
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = slideInVertically(
                                initialOffsetY = { 40 },
                                animationSpec = tween(600, delayMillis = 800)
                            ) + fadeIn(animationSpec = tween(600, delayMillis = 800))
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Warning,
                                        contentDescription = "Warning",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Low Stock Alert",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Text(
                                            "$lowStockProducts products are running low on stock",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                    TextButton(
                                        onClick = onNavigateToProductManagement
                                    ) {
                                        Text(
                                            "View",
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                errorMessage?.let { error ->
                    item {
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = slideInVertically(
                                initialOffsetY = { 40 },
                                animationSpec = tween(600, delayMillis = 1000)
                            ) + fadeIn(animationSpec = tween(600, delayMillis = 1000))
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}