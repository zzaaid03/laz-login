package com.laz.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.laz.R
import com.laz.models.User
import com.laz.ui.Screen
import com.laz.ui.components.ActionCard
import com.laz.ui.components.StatCard
import com.laz.ui.theme.*
import com.laz.viewmodels.ChatViewModel
import com.laz.viewmodels.ProductViewModel
import com.laz.viewmodels.SalesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CustomerDashboardScreen(
    user: User,
    userViewModel: UserViewModel,
    productViewModel: ProductViewModel,
    cartViewModel: CartViewModel,
    salesViewModel: SalesViewModel = viewModel(),
    chatViewModel: ChatViewModel = viewModel(),
    onLogout: () -> Unit,
    onNavigate: (Screen) -> Unit
) {
    // Customer statistics
    var recentPurchases by remember { mutableStateOf(0) }
    var availableProducts by remember { mutableStateOf(0) }
    var chatResponse by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var userInput by remember { mutableStateOf("") }
    var showChatDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Collect chat responses
    val botReply by chatViewModel.chatReply.collectAsState()

    // Load customer-specific data
    LaunchedEffect(Unit) {
        try {
            withContext(Dispatchers.IO) {
                // Get user's recent purchases (last 30 days)
                val userSales = salesViewModel.getSalesByUserId(user.id)
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                val thirtyDaysAgo = calendar.time
                
                recentPurchases = userSales.count { sale ->
                    try {
                        val saleDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(sale.date)
                        saleDate?.after(thirtyDaysAgo) == true
                    } catch (e: Exception) {
                        false
                    }
                }
                
                // Get available products count
                availableProducts = productViewModel.getAllProducts().count { it.quantity > 0 }
            }
        } catch (e: Exception) {
            println("Error loading customer dashboard data: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    // Chat dialog
    if (showChatDialog) {
        AlertDialog(
            onDismissRequest = { showChatDialog = false },
            title = { Text("Chat with LAZ Assistant") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    // Chat messages area
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        colors = CardDefaults.cardColors(containerColor = LazDarkCard)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Bot: $botReply",
                                color = LazWhite,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Input field
                    OutlinedTextField(
                        value = userInput,
                        onValueChange = { userInput = it },
                        label = { Text("Your message") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LazRed,
                            unfocusedBorderColor = LazLightGray,
                            focusedLabelColor = LazRed
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (userInput.isNotBlank()) {
                            scope.launch {
                                chatViewModel.sendMessageToBot(userInput)
                                userInput = ""
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LazRed
                    )
                ) {
                    Text("Send")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showChatDialog = false },
                    border = BorderStroke(1.dp, LazRed)
                ) {
                    Text("Close", color = LazRed)
                }
            }
        )
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
        // Collect cart count
        val cartItemCount by cartViewModel.cartItemCount.collectAsState(0)
        
        // Header
        CustomerHeader(
            user = user,
            onLogout = onLogout,
            onCartClick = { onNavigate(Screen.Cart) },
            cartItemCount = cartItemCount
        )

        // Dashboard Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Welcome message
            Text(
                text = "Welcome, ${user.username}!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = LazWhite
            )

            // Customer Statistics
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
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height(100.dp)
                ) {
                    items(
                        listOf(
                            StatCard("Recent Purchases", recentPurchases.toString(), Icons.Default.ShoppingBag, LazRedGlow, onClick = {}),
                            StatCard("Available Products", availableProducts.toString(), Icons.Default.Inventory, LazRed, onClick = {})
                        )
                    ) { stat ->
                        CustomerStatCard(stat)
                    }
                }
            }

            // Customer Actions
            Text(
                text = "Quick Actions",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = LazWhite,
                modifier = Modifier.padding(top = 16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.height(240.dp)
            ) {
                items(
                    listOf(
                        ActionCard("Browse Products", "View available products", Icons.Default.ShoppingCart) {
                            onNavigate(Screen.ProductScreen)
                        },
                        ActionCard("Order History", "View your past orders", Icons.Default.Receipt) {
                            onNavigate(Screen.OrderHistory)
                        },
                        ActionCard("Chat Assistant", "Ask about products", Icons.Default.Chat) {
                            showChatDialog = true
                        },
                        ActionCard("My Profile", "View and edit profile", Icons.Default.Person) {
                            onNavigate(Screen.Profile)
                        }
                    )
                ) { action ->
                    CustomerActionCard(action)
                }
            }

            // Special Offers
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LazDarkCard)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Special Offers",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = LazRedGlow
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• 10% off on all Tesla accessories this week",
                        fontSize = 12.sp,
                        color = LazWhite
                    )
                    Text(
                        text = "• Free shipping on orders over JOD 50",
                        fontSize = 12.sp,
                        color = LazWhite
                    )
                    Text(
                        text = "• Loyalty points for every purchase",
                        fontSize = 12.sp,
                        color = LazWhite
                    )
                }
            }
        }
    }
}

@Composable
fun CustomerHeader(
    user: User,
    onLogout: () -> Unit,
    onCartClick: () -> Unit = {},
    cartItemCount: Int = 0
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
                    text = "Customer Dashboard",
                    fontSize = 18.sp,
                    color = LazWhite
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(contentAlignment = Alignment.TopEnd) {
                    IconButton(
                        onClick = onCartClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Cart",
                            tint = LazRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (cartItemCount > 0) {
                        Badge(
                            containerColor = LazRedGlow,
                            contentColor = LazWhite,
                            modifier = Modifier
                                .offset(x = (-2).dp, y = 2.dp)
                                .size(16.dp)
                        ) {
                            Text(
                                text = if (cartItemCount > 9) "9+" else cartItemCount.toString(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Customer",
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
fun CustomerStatCard(stat: StatCard) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp),
        colors = CardDefaults.cardColors(
            containerColor = LazGray
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = BorderStroke(1.dp, LazRed.copy(alpha = 0.3f))
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
                    color = LazRedGlow,
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
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = LazWhite,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun CustomerActionCard(action: ActionCard) {
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


// ActionCard moved to CommonComponents.kt