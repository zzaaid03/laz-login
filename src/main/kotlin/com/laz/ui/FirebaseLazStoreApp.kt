package com.laz.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.laz.models.UserRole
import com.laz.ui.screens.*
import com.laz.viewmodels.FirebaseAuthViewModel
import com.laz.viewmodels.SecureFirebaseProductViewModel
import com.laz.viewmodels.FirebaseServices

/**
 * Firebase-integrated LAZ Store App
 * Main UI container that works with both Firebase and local services
 */
@Composable
fun FirebaseLazStoreApp(
    firebaseAuthViewModel: FirebaseAuthViewModel
) {
    val navController = rememberNavController()
    
    // Observe Firebase auth state
    val authState by firebaseAuthViewModel.authState.collectAsState()
    val isLoading by firebaseAuthViewModel.isLoading.collectAsState()
    
    // Simple role-based navigation without complex ViewModels
    // The secure ViewModels are available in the role-specific screens when needed

    // Set current user for cart if logged in
    LaunchedEffect(authState.user) {
        authState.user?.let { user ->
            // Removed reference to deleted cart view model
        }
    }

    // Handle authentication state changes and navigation
    LaunchedEffect(authState.isLoggedIn, authState.userRole) {
        if (authState.isLoggedIn && authState.user != null) {
            val destination = when (authState.userRole) {
                UserRole.ADMIN -> {
                    android.util.Log.d("Navigation", "Navigating ADMIN user to Admin Dashboard")
                    Screen.AdminDashboard.route
                }
                UserRole.EMPLOYEE -> {
                    android.util.Log.d("Navigation", "Navigating EMPLOYEE user to Employee Dashboard")
                    Screen.EmployeeDashboard.route
                }
                UserRole.CUSTOMER -> {
                    android.util.Log.d("Navigation", "Navigating CUSTOMER user to Customer Dashboard")
                    Screen.CustomerDashboard.route
                }
            }
            android.util.Log.d("Navigation", "User role: ${authState.userRole}, navigating to: $destination")
            navController.navigate(destination) {
                popUpTo(Screen.Login.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    if (isLoading) {
        // Show loading screen while checking authentication
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Column(
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Initializing LAZ Store...")
            }
        }
    } else {
        NavHost(
            navController = navController,
            startDestination = if (authState.isLoggedIn) {
                when (authState.userRole) {
                    UserRole.ADMIN -> Screen.AdminDashboard.route
                    UserRole.EMPLOYEE -> Screen.EmployeeDashboard.route
                    UserRole.CUSTOMER -> Screen.CustomerDashboard.route
                }
            } else {
                Screen.Login.route
            }
        ) {
            // Authentication Screens
            composable(Screen.Login.route) {
                FirebaseLoginScreen(
                    authViewModel = firebaseAuthViewModel,
                    onNavigateToSignup = { navController.navigate(Screen.Signup.route) },
                    onLoginSuccess = { userRole ->
                        val destination = when (userRole) {
                            UserRole.ADMIN -> Screen.AdminDashboard.route
                            UserRole.EMPLOYEE -> Screen.EmployeeDashboard.route
                            UserRole.CUSTOMER -> Screen.CustomerDashboard.route
                        }
                        navController.navigate(destination) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Signup.route) {
                FirebaseSignupScreen(
                    authViewModel = firebaseAuthViewModel,
                    onNavigateToLogin = { navController.navigate(Screen.Login.route) },
                    onSignupSuccess = {
                        navController.navigate(Screen.CustomerDashboard.route) {
                            popUpTo(Screen.Signup.route) { inclusive = true }
                        }
                    }
                )
            }

            // Admin Dashboard
            composable(Screen.AdminDashboard.route) {
                authState.user?.let { user ->
                    // For now, show a simple admin dashboard placeholder
                    AdminDashboardPlaceholder(
                        user = user,
                        onNavigateBack = { navController.popBackStack() },
                        onLogout = {
                            firebaseAuthViewModel.signOut()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
            }

            // Employee Dashboard
            composable(Screen.EmployeeDashboard.route) {
                authState.user?.let { user ->
                    val productViewModel: SecureFirebaseProductViewModel = viewModel(
                        factory = FirebaseServices.secureViewModelFactory
                    )
                    
                    EmployeeDashboardPlaceholder(
                        user = user,
                        onNavigateBack = { navController.popBackStack() },
                        onLogout = {
                            firebaseAuthViewModel.signOut()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onNavigateToProductManagement = {
                            navController.navigate(Screen.ProductManagement.route)
                        }
                    )
                }
            }
            
            // Product Management Screen
            composable(Screen.ProductManagement.route) {
                val productViewModel: SecureFirebaseProductViewModel = viewModel(
                    factory = FirebaseServices.secureViewModelFactory
                )

                EmployeeProductManagementScreen(
                    productViewModel = productViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            // Customer Dashboard
            composable(Screen.CustomerDashboard.route) {
                authState.user?.let { user ->
                    // For now, show a simple customer dashboard placeholder
                    CustomerDashboardPlaceholder(
                        user = user,
                        onNavigateBack = { navController.popBackStack() },
                        onLogout = {
                            firebaseAuthViewModel.signOut()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
            }

            // Profile Screen - Simple profile management
            composable(Screen.ProfileScreen.route) {
                FirebaseProfileScreen(
                    authViewModel = firebaseAuthViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

/**
 * Screen definitions for navigation
 */
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Signup : Screen("signup")
    object AdminDashboard : Screen("admin_dashboard")
    object EmployeeDashboard : Screen("employee_dashboard")
    object CustomerDashboard : Screen("customer_dashboard")
    object ProductManagement : Screen("product_management")
    object ProductScreen : Screen("product_screen")
    object CartScreen : Screen("cart_screen")
    object ProfileScreen : Screen("profile_screen")
    object SalesProcessing : Screen("sales_processing")
    object ReturnsProcessing : Screen("returns_processing")
    object SalesOverview : Screen("sales_overview")
    object UserManagement : Screen("user_management")
}

/**
 * Simple placeholder screens for role-based dashboards
 */
@Composable
fun AdminDashboardPlaceholder(
    user: com.laz.models.User,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Admin Dashboard",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Welcome, ${user.username}!",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "Role: ${user.role}",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Admin features:",
            style = MaterialTheme.typography.titleMedium
        )
        Text("• User Management")
        Text("• Full System Access")
        Text("• Analytics & Reports")
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onLogout) {
            Text("Logout")
        }
    }
}

@Composable
fun EmployeeDashboardPlaceholder(
    user: com.laz.models.User,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToProductManagement: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Employee Dashboard",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Welcome, ${user.username}!",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "Role: ${user.role}",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        // Product Management Button
        Button(
            onClick = onNavigateToProductManagement,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Icon(
                imageVector = Icons.Default.Inventory,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Manage Products")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Other actions can be added here
        Text(
            text = "Quick Actions:",
            style = MaterialTheme.typography.titleMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Logout Button
        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ),
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text("Logout")
        }
    }
}

@Composable
fun CustomerDashboardPlaceholder(
    user: com.laz.models.User,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Customer Dashboard",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Welcome, ${user.username}!",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "Role: ${user.role}",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Customer features:",
            style = MaterialTheme.typography.titleMedium
        )
        Text("• Browse Products")
        Text("• Shopping Cart")
        Text("• Profile Management")
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onLogout) {
            Text("Logout")
        }
    }
}
