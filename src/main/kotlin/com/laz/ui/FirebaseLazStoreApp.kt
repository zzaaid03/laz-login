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
import com.laz.models.User
import com.laz.models.UserRole
import com.laz.models.Order
import com.laz.ui.screens.*
import com.laz.viewmodels.*
import java.math.BigDecimal

/**
 * Firebase-integrated LAZ Store App
 * Main UI container that works with both Firebase and local services
 */
@Composable
fun FirebaseLazStoreApp(
    user: User,
    authViewModel: FirebaseAuthViewModel
) {
    val navController = rememberNavController()
    
    // Observe Firebase auth state
    val authState by authViewModel.authState.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()
    
    // Simple role-based navigation without complex ViewModels
    // The secure ViewModels are available in the role-specific screens when needed

    // Set current user for cart if logged in
    LaunchedEffect(authState.user) {
        authState.user?.let { user ->
            // Removed reference to deleted cart view model
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
                Text("Loading...")
            }
        }
    } else {
        // Main navigation - simple approach
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
                    authViewModel = authViewModel,
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
                    authViewModel = authViewModel,
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
                val currentUser = authState.user
                if (currentUser != null) {
                    AdminDashboardScreen(
                        user = currentUser,
                        onNavigateBack = { navController.popBackStack() },
                        onLogout = { 
                            try {
                                authViewModel.signOut()
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("Navigation", "Logout navigation error: ${e.message}")
                            }
                        },
                        onNavigateToUserManagement = { navController.navigate(Screen.UserManagement.route) },
                        onNavigateToProductManagement = { navController.navigate(Screen.ProductManagement.route) },
                        onNavigateToPointOfSale = { navController.navigate(Screen.PointOfSale.route) },
                        onNavigateToReturnsProcessing = { navController.navigate(Screen.ReturnsProcessing.route) },
                        onNavigateToOrderAnalytics = { navController.navigate(Screen.OrderAnalytics.route) },
                        onNavigateToOrderManagement = { navController.navigate(Screen.OrderManagement.route) }
                    )
                } else {
                    // User is null, navigate to login
                    LaunchedEffect(Unit) {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            }
            
            // Employee Dashboard
            composable(Screen.EmployeeDashboard.route) {
                val currentUser = authState.user
                if (currentUser != null) {
                    EmployeeDashboardScreen(
                        user = currentUser,
                        onNavigateBack = { navController.popBackStack() },
                        onLogout = { 
                            try {
                                authViewModel.signOut()
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("Navigation", "Logout navigation error: ${e.message}")
                            }
                        },
                        onNavigateToProductManagement = { navController.navigate(Screen.ProductManagement.route) },
                        onNavigateToPointOfSale = { navController.navigate(Screen.PointOfSale.route) },
                        onNavigateToReturnsProcessing = { navController.navigate(Screen.ReturnsProcessing.route) },
                        onNavigateToOrderManagement = { navController.navigate(Screen.OrderManagement.route) },
                        onNavigateToChatManagement = { navController.navigate(Screen.EmployeeChatManagement.route) },
                        onNavigateToProfile = { navController.navigate(Screen.ProfileScreen.route) }
                    )
                } else {
                    // User is null, navigate to login
                    LaunchedEffect(Unit) {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            }
            
            // Customer Dashboard
            composable("customer_dashboard") {
                CustomerDashboardScreen(
                    user = user,
                    onNavigateBack = { navController.popBackStack() },
                    onLogout = { 
                        authViewModel.signOut()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true } 
                        }
                    },
                    onNavigateToShopping = { navController.navigate(Screen.CustomerShopping.route) },
                    onNavigateToCart = { navController.navigate(Screen.EnhancedCart.route) },
                    onNavigateToProfile = { navController.navigate(Screen.ProfileScreen.route) },
                    onNavigateToOrderHistory = { navController.navigate(Screen.OrderHistory.route) },
                    onNavigateToOrderTracking = { navController.navigate(Screen.OrderTracking.route) },
                    onNavigateToChat = { navController.navigate(Screen.Chat.route) },
                    onNavigateToCustomerSupport = { navController.navigate(Screen.CustomerSupport.route) }
                )
            }
            
            // Product Management Screen
            composable(Screen.ProductManagement.route) {
                val productViewModel: SecureFirebaseProductViewModel = viewModel(
                    factory = FirebaseServices.secureViewModelFactory
                )

                EmployeeProductManagementScreen(
                    productViewModel = productViewModel,
                    onBackClick = { navController.popBackStack() },
                    userRole = authState.user?.role?.name
                )
            }

            // Profile Screen - Simple profile management
            composable(Screen.ProfileScreen.route) {
                FirebaseProfileScreen(
                    authViewModel = authViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            // Order Analytics Screen - Analytics dashboard (now using orders)
            composable(Screen.OrderAnalytics.route) {
                // Navigate back since dedicated analytics screen is not implemented yet
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
            
            // Point of Sale Screen - In-store order processing
            composable(Screen.PointOfSale.route) {
                FirebaseSalesProcessingScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable(Screen.UserManagement.route) {
                FirebaseUserManagementScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable(Screen.OrderHistory.route) {
                FirebaseOrderHistoryScreen(
                    currentUser = authState.user!!,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            composable(Screen.EnhancedCart.route) {
                FirebaseEnhancedCartScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            composable(Screen.ReturnsProcessing.route) {
                FirebaseReturnsProcessingScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable(Screen.Chat.route) {
                FirebaseChatScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            
            // Customer Shopping Screen - Product browsing for customers
            composable(Screen.CustomerShopping.route) {
                val productViewModel: SecureFirebaseProductViewModel = viewModel(
                    factory = FirebaseServices.secureViewModelFactory
                )
                val cartViewModel: SecureFirebaseCartViewModel = viewModel(
                    factory = FirebaseServices.secureViewModelFactory
                )
                
                CustomerShoppingScreen(
                    productViewModel = productViewModel,
                    cartViewModel = cartViewModel,
                    onNavigateToCart = { navController.navigate(Screen.EnhancedCart.route) },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            // Enhanced Cart Screen - Shopping cart with checkout
            composable(Screen.EnhancedCart.route) {
                FirebaseEnhancedCartScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPayment = { cartItems, total ->
                        // Navigate to payment with cart total
                        navController.navigate("${Screen.Payment.route}/${total}")
                    }
                )
            }
            
            // Payment Screen - Checkout and order creation
            composable("${Screen.Payment.route}/{total}") { backStackEntry ->
                val total = backStackEntry.arguments?.getString("total")?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                val currentUser = authState.user
                
                if (currentUser != null) {
                    PaymentScreen(
                        user = currentUser,
                        cartTotal = total,
                        onNavigateBack = { navController.popBackStack() },
                        onPaymentSuccess = { order ->
                            // Navigate to order tracking after successful payment
                            navController.navigate(Screen.OrderTracking.route) {
                                popUpTo(Screen.CustomerDashboard.route)
                            }
                        }
                    )
                } else {
                    LaunchedEffect(Unit) {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            }
            
            // Order Tracking Screen - Customer order tracking
            composable(Screen.OrderTracking.route) {
                val currentUser = authState.user
                if (currentUser != null) {
                    OrderTrackingScreen(
                        user = currentUser,
                        onNavigateBack = { navController.popBackStack() }
                    )
                } else {
                    LaunchedEffect(Unit) {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            }
            
            // Order History Screen - Customer order history
            composable(Screen.OrderHistory.route) {
                val currentUser = authState.user
                if (currentUser != null) {
                    FirebaseOrderHistoryScreen(
                        currentUser = currentUser,
                        onNavigateBack = { navController.popBackStack() }
                    )
                } else {
                    LaunchedEffect(Unit) {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            }
            
            // Order Management Screen - Admin order management
            composable(Screen.OrderManagement.route) {
                val currentUser = authState.user
                if (currentUser != null) {
                    OrderManagementScreen(
                        user = currentUser,
                        onNavigateBack = { navController.popBackStack() }
                    )
                } else {
                    LaunchedEffect(Unit) {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            }
            
            // Customer Support Chat Screen
            composable(Screen.CustomerSupport.route) {
                val currentUser = authState.user
                if (currentUser != null) {
                    CustomerSupportScreen(
                        user = currentUser,
                        onBack = { navController.popBackStack() }
                    )
                } else {
                    LaunchedEffect(Unit) {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            }
            
            // Employee Chat Management Screen
            composable(Screen.EmployeeChatManagement.route) {
                val currentUser = authState.user
                if (currentUser != null) {
                    EmployeeChatManagementScreen(
                        employee = currentUser,
                        onNavigateBack = { navController.popBackStack() }
                    )
                } else {
                    LaunchedEffect(Unit) {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
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
    object CustomerShopping : Screen("customer_shopping")
    object CartScreen : Screen("cart_screen")
    object EnhancedCart : Screen("enhanced_cart")
    object Payment : Screen("payment")
    object OrderTracking : Screen("order_tracking")
    object ProfileScreen : Screen("profile_screen")
    object PointOfSale : Screen("point_of_sale")
    object ReturnsProcessing : Screen("returns_processing")
    object OrderAnalytics : Screen("order_analytics")
    object UserManagement : Screen("user_management")
    object OrderManagement : Screen("order_management")
    object OrderHistory : Screen("order_history")
    object Chat : Screen("chat")
    object CustomerSupport : Screen("customer_support")
    object EmployeeChatManagement : Screen("employee_chat_management")
}
