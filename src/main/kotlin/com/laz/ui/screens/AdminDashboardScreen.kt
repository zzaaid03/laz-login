package com.laz.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import com.laz.R
import com.laz.models.User
import com.laz.ui.components.ActionCard
import com.laz.ui.components.StatCard
import com.laz.viewmodels.ProductViewModel
import com.laz.viewmodels.SalesViewModel
import com.laz.viewmodels.UserViewModel
import com.laz.viewmodels.ReturnsViewModel
import com.laz.ui.Screen
import com.laz.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal

// Admin dashboard for managing the store

@Composable
fun AdminDashboardScreen(
    user: User,
    productViewModel: ProductViewModel,
    salesViewModel: SalesViewModel,
    userViewModel: UserViewModel,
    returnsViewModel: ReturnsViewModel,
    onLogout: () -> Unit,
    onNavigate: (Screen) -> Unit
) {
    // Real statistics from database
    var totalSales by remember { mutableStateOf(0.0) }
    var totalProducts by remember { mutableStateOf(0) }
    var lowStockItems by remember { mutableStateOf(0) }
    var totalUsers by remember { mutableStateOf(0) }
    var todaySales by remember { mutableStateOf(0.0) }
    var totalReturns by remember { mutableStateOf(0L) }
    var isLoading by remember { mutableStateOf(true) }

    // Load statistics from database efficiently - refresh each time we navigate to dashboard
    LaunchedEffect(salesViewModel, returnsViewModel) {
        try {
            withContext(Dispatchers.IO) {
                // Get product statistics
                val products = productViewModel.getAllProducts()
                totalProducts = products.size
                lowStockItems = productViewModel.getLowStockProducts(5).size

                // Get user count from StateFlow
                val users = userViewModel.users.value
                totalUsers = users.size

                // Use new efficient methods for sales statistics
                val nonReturnedSales = salesViewModel.getNonReturnedSales()
                totalSales = nonReturnedSales.sumOf { sale ->
                    sale.productPrice.toDouble() * sale.quantity
                }

                // Get today's sales total efficiently
                todaySales = salesViewModel.calculateTodaysSalesTotal()

                // Get total returns count for the past year
                totalReturns = returnsViewModel.getReturnCountFromDate(365)
            }
        } catch (e: Exception) {
            // Handle error - keep default values
            println("Error loading dashboard statistics: ${e.message}")
        } finally {
            isLoading = false
        }
    }

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
        AdminHeader(
            user = user,
            onLogout = onLogout
        )

        // Dashboard Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp), // Reduced padding
            verticalArrangement = Arrangement.spacedBy(16.dp) // Reduced spacing
        ) {
            // Welcome message
            Text(
                text = "Welcome back, ${user.username}!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = LazWhite
            )

            // Statistics Grid
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = LazRed)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height(210.dp) // Increased to accommodate taller cards
                ) {
                    items(
                        listOf(
                            StatCard("Total Sales", "JOD ${String.format(java.util.Locale.US, "%.2f", totalSales)}", Icons.Default.AttachMoney, LazRedGlow, onClick = {}),
                            StatCard("Products", totalProducts.toString(), Icons.Default.Inventory, LazRed, onClick = {}),
                            StatCard("Low Stock", lowStockItems.toString(), Icons.Default.Warning, LazRedGlow, onClick = {}),
                            StatCard("Users", totalUsers.toString(), Icons.Default.People, LazRed, onClick = {}),
                            StatCard("Today's Sales", "JOD ${String.format(java.util.Locale.US, "%.2f", todaySales)}", Icons.AutoMirrored.Filled.TrendingUp, LazRedGlow, onClick = {}),
                            StatCard("Returns", totalReturns.toString(), Icons.AutoMirrored.Filled.AssignmentReturn, LazRed, onClick = {})
                        )
                    ) { stat ->
                        StatisticCard(stat)
                    }
                }
            }

            // Admin Actions
            Text(
                text = "Admin Actions",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = LazWhite,
                modifier = Modifier.padding(top = 16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.height(320.dp) // Increased height to accommodate 5 items (3 rows)
            ) {
                items(
                    listOf(
                        ActionCard("Product Management", "Manage inventory, add/edit products", Icons.Default.Inventory) {
                            onNavigate(Screen.ProductManagement)
                        },
                        ActionCard("User Management", "Add users, manage roles", Icons.Default.People) {
                            onNavigate(Screen.UserManagement)
                        },
                        ActionCard("Sales Processing", "Process new sales", Icons.Default.PointOfSale) {
                            onNavigate(Screen.SalesProcessing)
                        },
                        ActionCard("Returns Processing", "Handle customer returns", Icons.AutoMirrored.Filled.AssignmentReturn) {
                            onNavigate(Screen.ReturnsProcessing)
                        },
                        ActionCard("Sales Overview", "View and manage all sales", Icons.Default.Receipt) {
                            onNavigate(Screen.SalesOverview)
                        }
                    )
                ) { action ->
                    AdminActionCard(action)
                }
            }
        }
    }
}

@Composable
fun AdminHeader(
    user: User,
    onLogout: () -> Unit
) {
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
                Image(
                    painter = painterResource(id = R.drawable.laz_logo),
                    contentDescription = "LAZ Logo",
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Admin Dashboard",
                    fontSize = 18.sp,
                    color = LazWhite
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = "Admin",
                    tint = LazRedGlow
                )
                Text(
                    text = user.username,
                    color = LazWhite,
                    fontWeight = FontWeight.Medium
                )
                IconButton(
                    onClick = onLogout,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Logout",
                        tint = LazRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatisticCard(stat: StatCard) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp), // Increased height for better visibility
        colors = CardDefaults.cardColors(
            containerColor = LazGray // Changed to lighter gray for better contrast
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = BorderStroke(1.dp, LazRed.copy(alpha = 0.3f)) // Add subtle border
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stat.title,
                    fontSize = 10.sp,
                    color = LazRedGlow, // Changed to red glow for better visibility
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = stat.icon,
                    contentDescription = stat.title,
                    tint = stat.color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = stat.value,
                fontSize = 16.sp, // Slightly smaller to fit better
                fontWeight = FontWeight.Bold,
                color = LazWhite,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun AdminActionCard(action: ActionCard) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        colors = CardDefaults.cardColors(containerColor = LazDarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Button(
            onClick = action.onClick,
            modifier = Modifier.fillMaxSize(),
            colors = ButtonDefaults.buttonColors(
                containerColor = LazDarkCard,
                contentColor = LazWhite
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.title,
                    tint = LazRed,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = action.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = action.description,
                    fontSize = 10.sp,
                    color = LazLightGray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

data class statCard(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)
