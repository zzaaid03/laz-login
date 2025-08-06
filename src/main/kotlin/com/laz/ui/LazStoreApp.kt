package com.laz.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Modifier
import com.laz.models.User
import com.laz.models.UserRole
import com.laz.ui.screens.*
import com.laz.ui.theme.*
import com.laz.viewmodels.*
import kotlinx.coroutines.launch

// Android version of the main app - uses ViewModels instead of Spring services

@Composable
fun LazStoreApp(
    userViewModel: UserViewModel,
    productViewModel: ProductViewModel,
    salesViewModel: SalesViewModel,
    returnsViewModel: ReturnsViewModel,
    cartViewModel: CartViewModel
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }
    var currentUser by remember { mutableStateOf<User?>(null) }
    val scope = rememberCoroutineScope()

    LazTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentScreen) {
                is Screen.Login -> {
                    LoginScreen(
                        userViewModel = userViewModel,
                        onLoginSuccess = { user ->
                            currentUser = user
                            currentScreen = when (user.role) {
                                UserRole.ADMIN -> Screen.AdminDashboard
                                UserRole.EMPLOYEE -> Screen.EmployeeDashboard
                                UserRole.CUSTOMER -> Screen.CustomerDashboard // Or whatever screen is appropriate
                            }
                        },
                        onNavigateToSignup = {
                            currentScreen = Screen.Signup
                        }
                    )
                }
                is Screen.Signup -> {
                    SignupScreen(
                        userViewModel = userViewModel,
                        onSignupSuccess = {
                            currentScreen = Screen.Login // This should navigate
                        },
                        onNavigateToLogin = {
                            currentScreen = Screen.Login
                        }
                    )
                }
                is Screen.AdminDashboard -> {
                    AdminDashboardScreen(
                        user = currentUser!!,
                        productViewModel = productViewModel,
                        salesViewModel = salesViewModel,
                        userViewModel = userViewModel,
                        returnsViewModel = returnsViewModel,
                        onLogout = {
                            currentUser = null
                            currentScreen = Screen.Login
                        },
                        onNavigate = { screen: Screen -> currentScreen = screen }
                    )
                }
                is Screen.EmployeeDashboard -> {
                    EmployeeDashboardScreen(
                        user = currentUser!!,
                        productViewModel = productViewModel,
                        salesViewModel = salesViewModel,
                        returnsViewModel = returnsViewModel,
                        onLogout = {
                            currentUser = null
                            currentScreen = Screen.Login
                        },
                        onNavigate = { screen: Screen -> currentScreen = screen }
                    )
                }
                is Screen.CustomerDashboard -> {
                    CustomerDashboardScreen(
                        user = currentUser!!,
                        userViewModel = userViewModel,
                        productViewModel = productViewModel,
                        cartViewModel = cartViewModel,
                        onLogout = {
                            currentUser = null
                            currentScreen = Screen.Login
                        },
                        onNavigate = { screen ->
                            currentScreen = screen
                        }
                    )
                }
                is Screen.ProductManagement -> {
                    ProductManagementScreen(
                        user = currentUser!!,
                        productViewModel = productViewModel,
                        onBack = {
                            currentScreen = when (currentUser!!.role) {
                                UserRole.ADMIN -> Screen.AdminDashboard
                                UserRole.EMPLOYEE -> Screen.EmployeeDashboard
                                UserRole.CUSTOMER -> Screen.CustomerDashboard
                            }
                        }
                    )
                }
                is Screen.SalesProcessing -> {
                    SalesProcessingScreen(
                        user = currentUser!!,
                        productViewModel = productViewModel,
                        salesViewModel = salesViewModel,
                        onBack = { 
                            currentScreen = when (currentUser!!.role) {
                                UserRole.ADMIN -> Screen.AdminDashboard
                                UserRole.EMPLOYEE -> Screen.EmployeeDashboard
                                UserRole.CUSTOMER -> Screen.CustomerDashboard
                            }
                        }
                    )
                }
                is Screen.UserManagement -> {
                    // Only allow ADMIN users to access User Management
                    if (currentUser?.role == UserRole.ADMIN) {
                        UserManagementScreen(
                            userViewModel = userViewModel,
                            onBack = { currentScreen = Screen.AdminDashboard }
                        )
                    } else {
                        // Redirect employees back to their dashboard
                        currentScreen = Screen.EmployeeDashboard
                    }
                }
                is Screen.ReturnsProcessing -> {
                    ReturnsProcessingScreen(
                        salesViewModel = salesViewModel,
                        returnsViewModel = returnsViewModel,
                        onBack = { 
                            currentScreen = when (currentUser!!.role) {
                                UserRole.ADMIN -> Screen.AdminDashboard
                                UserRole.EMPLOYEE -> Screen.EmployeeDashboard
                                UserRole.CUSTOMER -> Screen.CustomerDashboard
                            }
                        }
                    )
                }
                is Screen.SalesOverview -> {
                    SalesOverviewScreen(
                        salesViewModel = salesViewModel,
                        onBack = { 
                            currentScreen = when (currentUser!!.role) {
                                UserRole.ADMIN -> Screen.AdminDashboard
                                UserRole.EMPLOYEE -> Screen.EmployeeDashboard
                                UserRole.CUSTOMER -> Screen.CustomerDashboard
                            }
                        }
                    )
                }
                is Screen.ProductScreen -> {
                    ProductScreen(
                        productViewModel = productViewModel,
                        onNavigateBack = {
                            currentScreen = when (currentUser?.role) { // Use safe call here
                                UserRole.ADMIN -> Screen.AdminDashboard
                                UserRole.EMPLOYEE -> Screen.EmployeeDashboard
                                UserRole.CUSTOMER -> Screen.CustomerDashboard
                                null -> Screen.Login // Or some other default if user is null
                            }
                        },
                        onAddToCart = { product ->
                            // Add product to cart and navigate to cart screen
                            scope.launch {
                                cartViewModel.addToCart(currentUser!!.id, product)
                                currentScreen = Screen.Cart
                            }
                        }
                    )
                }
                is Screen.ChatScreen -> {
                    ChatScreen(
                        chatViewModel = viewModel()
                    )
                }
                is Screen.OrderHistory -> {
                    OrderHistoryScreen(
                        salesViewModel = salesViewModel,
                        currentUser = currentUser!!,
                        onNavigateBack = {
                            currentScreen = Screen.CustomerDashboard
                        }
                    )
                }
                is Screen.Profile -> {
                    ProfileScreen(
                        userViewModel = userViewModel,
                        onNavigateBack = {
                            currentScreen = Screen.CustomerDashboard
                        }
                    )
                }
                is Screen.Cart -> {
                    CartScreen(
                        user = currentUser!!,
                        cartViewModel = cartViewModel,
                        onNavigateBack = {
                            currentScreen = Screen.CustomerDashboard
                        }
                    )
                }
            }
        }
    }
}

sealed class Screen {
    object Login : Screen()
    object AdminDashboard : Screen()
    object EmployeeDashboard : Screen()
    object CustomerDashboard : Screen()
    object ProductManagement : Screen()
    object SalesProcessing : Screen()
    object UserManagement : Screen()
    object ReturnsProcessing : Screen()
    object SalesOverview : Screen()
    object Signup : Screen()
    object ProductScreen : Screen()
    object ChatScreen : Screen()
    object OrderHistory : Screen()
    object Profile : Screen()
    object Cart : Screen()
    companion object {

    }
}
