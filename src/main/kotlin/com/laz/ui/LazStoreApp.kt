package com.laz.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.laz.models.User
import com.laz.models.UserRole
import com.laz.ui.screens.*
import com.laz.ui.theme.*
import com.laz.viewmodels.*

// Android version of the main app - uses ViewModels instead of Spring services

@Composable
fun LazStoreApp(
    userViewModel: UserViewModel,
    productViewModel: ProductViewModel,
    salesViewModel: SalesViewModel,
    returnsViewModel: ReturnsViewModel
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }
    var currentUser by remember { mutableStateOf<User?>(null) }

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

                        onLogout = {
                            currentUser = null
                            currentScreen = Screen.Login
                        },
                        onNavigate = { screen: Screen -> currentScreen = screen }
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
                    // Replace with your actual ProductScreen composable and its parameters
                    // For example:
                    // ProductScreen(
                    //     productViewModel = productViewModel,
                    //     onBack = {
                    //         currentScreen = when (currentUser?.role) { // Use safe call here
                    //             UserRole.ADMIN -> Screen.AdminDashboard
                    //             UserRole.EMPLOYEE -> Screen.EmployeeDashboard
                    //             UserRole.CUSTOMER -> Screen.CustomerDashboard
                    //             null -> Screen.Login // Or some other default if user is null
                    //         }
                    //     }
                    // )

                    // Placeholder if you haven't created the UI yet:
                    Text("Product Screen WIP") // TODO: Implement ProductScreen
                }
                is Screen.ChatScreen -> {
                    // Replace with your actual ProductScreen composable and its parameters
                    // For example:
                    // ProductScreen(
                    //     productViewModel = productViewModel,
                    //     onBack = {
                    //         currentScreen = when (currentUser?.role) { // Use safe call here
                    //             UserRole.ADMIN -> Screen.AdminDashboard
                    //             UserRole.EMPLOYEE -> Screen.EmployeeDashboard
                    //             UserRole.CUSTOMER -> Screen.CustomerDashboard
                    //             null -> Screen.Login // Or some other default if user is null
                    //         }
                    //     }
                    // )

                    // Placeholder if you haven't created the UI yet:
                    Text("Product Screen WIP") // TODO: Implement ProductScreen
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
    companion object {

    }
}
