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
import com.laz.viewmodels.ReturnsViewModel
import com.laz.ui.Screen
import com.laz.ui.theme.*

// In Python, this would be like the employee interface
// class EmployeeDashboard:
//     def __init__(self, user):
//         self.user = user
//         self.setup_limited_interface()  # Limited compared to admin

@Composable
fun EmployeeDashboardScreen(
    user: User,
    productViewModel: ProductViewModel,
    salesViewModel: SalesViewModel,
    returnsViewModel: ReturnsViewModel,
    onLogout: () -> Unit,
    onNavigate: (Screen) -> Unit
) {
    // Real statistics from database - employee focused
    var todaySales by remember { mutableStateOf(0.0) }
    var lowStockCount by remember { mutableStateOf(0) }
    var totalReturns by remember { mutableStateOf(0L) }
    var isLoading by remember { mutableStateOf(true) }

    // Load statistics from database
    LaunchedEffect(Unit) {
        try {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                // Calculate today's sales (excluding returned sales)
                todaySales = salesViewModel.calculateTodaysSalesTotal()

                // Get low stock count
                lowStockCount = productViewModel.getLowStockProducts(5).size

                // Get recent returns count
                totalReturns = returnsViewModel.getReturnCountFromDate(30) // Returns in last 30 days
            }
        } catch (e: Exception) {
            println("Error loading employee dashboard statistics: ${e.message}")
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
        EmployeeHeader(
            user = user,
            onLogout = onLogout
        )

        // Dashboard Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp), // Reduced padding to match admin
            verticalArrangement = Arrangement.spacedBy(16.dp) // Reduced spacing to match admin
        ) {
            // Welcome message
            Text(
                text = "Welcome, ${user.username}!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = LazWhite
            )

            // Employee Statistics
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = LazRed)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height(100.dp) // Adjusted for employee dashboard
                ) {
                    items(
                        listOf(
                            StatCard("Today's Sales", "JOD ${String.format(java.util.Locale.US, "%.2f", todaySales)}", Icons.AutoMirrored.Filled.TrendingUp, LazRedGlow, onClick = {}),
                            StatCard("Low Stock Items", lowStockCount.toString(), Icons.Default.Warning, LazRed, onClick = {}),
                            StatCard("Recent Returns", totalReturns.toString(), Icons.AutoMirrored.Filled.AssignmentReturn, LazRedGlow, onClick = {})
                        )
                    ) { stat ->
                        // Use the StatCard directly as we don't have a StatisticCard component
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            colors = CardDefaults.cardColors(containerColor = LazDarkCard)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = stat.icon,
                                    contentDescription = stat.title,
                                    tint = LazRed,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = stat.title,
                                    fontSize = 12.sp,
                                    color = LazWhite,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = stat.value,
                                    fontSize = 16.sp,
                                    color = LazWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Employee Actions
            Text(
                text = "Employee Actions",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = LazWhite,
                modifier = Modifier.padding(top = 16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.height(200.dp) // Fixed height for consistency
            ) {
                items(
                    listOf(
                        ActionCard("Process Sale", "Sell products to customers", Icons.Default.PointOfSale) {
                            onNavigate(Screen.SalesProcessing)
                        },
                        ActionCard("Handle Returns", "Process customer returns", Icons.AutoMirrored.Filled.AssignmentReturn) {
                            onNavigate(Screen.ReturnsProcessing)
                        },
                        ActionCard("View Inventory", "Check product stock levels", Icons.Default.Inventory) {
                            onNavigate(Screen.ProductManagement)
                        },
                        ActionCard("Sales Overview", "View and manage all sales", Icons.Default.Receipt) {
                            onNavigate(Screen.SalesOverview)
                        }
                    )
                ) { action ->
                    EmployeeActionCard(action)
                }
            }

            // Quick Tips
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LazDarkCard)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Quick Tips",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = LazRedGlow
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Always check shelf locations when adding products",
                        fontSize = 12.sp,
                        color = LazWhite
                    )
                    Text(
                        text = "• Verify customer details before processing returns",
                        fontSize = 12.sp,
                        color = LazWhite
                    )
                    Text(
                        text = "• Update product quantities after sales",
                        fontSize = 12.sp,
                        color = LazWhite
                    )
                }
            }
        }
    }
}

@Composable
fun EmployeeHeader(
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
                    text = "Employee Dashboard",
                    fontSize = 18.sp,
                    color = LazWhite
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Employee",
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
fun EmployeeActionCard(action: ActionCard) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
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
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = action.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = action.description,
                    fontSize = 9.sp,
                    color = LazLightGray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
