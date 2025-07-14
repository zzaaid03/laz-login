package com.laz.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laz.models.User
import com.laz.ui.Screen // Ensure your Screen sealed class is imported
import com.laz.ui.ChatScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDashboardScreen(
    user: User,
    onLogout: () -> Unit,
    onNavigate: (Screen) -> Unit // For navigating to other customer-specific screens
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Welcome, ${user.username}") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.Filled.ExitToApp,
                            contentDescription = "Logout"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Customer Dashboard",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Example Navigation Buttons
            DashboardButton(
                text = "View Products",
                icon = Icons.Filled.ShoppingCart,
                onClick = {
                    // TODO: Navigate to a ProductListScreen or similar
                     onNavigate(Screen.ProductScreen)
                }
            )

            DashboardButton(
                text = "Order History",
                icon = Icons.Filled.History,
                onClick = {
                    // TODO: Navigate to an OrderHistoryScreen
                    // onNavigate(Screen.OrderHistory) // Assuming you'll create this screen
                }
            )

            DashboardButton(
                text = "My Profile",
                icon = Icons.Filled.Person,
                onClick = {
                    // TODO: Navigate to a ProfileScreen
                    // onNavigate(Screen.UserProfile) // Assuming you'll create this screen
                }
            )

            DashboardButton(
                text = "Ask For New Product",
                icon = Icons.Filled.Person,
                onClick = {
                    // TODO: Navigate to a ProfileScreen
                     onNavigate(Screen.ChatScreen)  // Assuming you'll create this screen
                }
            )
        }
    }
}

@Composable
fun DashboardButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = text)
            Text(text = text, fontSize = 16.sp)
        }
    }
}