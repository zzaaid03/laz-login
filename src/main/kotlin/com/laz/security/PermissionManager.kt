package com.laz.security

import com.laz.models.User
import com.laz.models.UserRole

/**
 * Permission Manager
 * Centralized role-based access control for LAZ Store
 */
object PermissionManager {
    
    // ========================================
    // PRODUCT MANAGEMENT PERMISSIONS
    // ========================================
    
    /**
     * Can view products (all users can browse)
     */
    fun canViewProducts(user: User?): Boolean {
        return user != null // All authenticated users can view products
    }
    
    /**
     * Can add new products (Admin and Employee only)
     */
    fun canAddProducts(user: User?): Boolean {
        return user?.role in listOf(UserRole.ADMIN, UserRole.EMPLOYEE)
    }
    
    /**
     * Can edit existing products (Admin and Employee only)
     */
    fun canEditProducts(user: User?): Boolean {
        return user?.role in listOf(UserRole.ADMIN, UserRole.EMPLOYEE)
    }
    
    /**
     * Can delete products (Admin only)
     */
    fun canDeleteProducts(user: User?): Boolean {
        return user?.role == UserRole.ADMIN
    }
    
    /**
     * Can manage inventory/stock levels (Admin and Employee only)
     */
    fun canManageInventory(user: User?): Boolean {
        return user?.role in listOf(UserRole.ADMIN, UserRole.EMPLOYEE)
    }
    
    // ========================================
    // ORDER MANAGEMENT PERMISSIONS
    // ========================================
    
    /**
     * Can create orders (Customer only)
     */
    fun canCreateOrders(user: User?): Boolean {
        return user?.role == UserRole.CUSTOMER
    }
    
    /**
     * Can manage orders (Admin and Employee only)
     */
    fun canManageOrders(user: User?): Boolean {
        return user?.role in listOf(UserRole.ADMIN, UserRole.EMPLOYEE)
    }
    
    /**
     * Can view all orders (Admin and Employee only)
     */
    fun canViewAllOrders(user: User?): Boolean {
        return user?.role in listOf(UserRole.ADMIN, UserRole.EMPLOYEE)
    }
    
    /**
     * Can update order status (Admin and Employee only)
     */
    fun canUpdateOrderStatus(user: User?): Boolean {
        return user?.role in listOf(UserRole.ADMIN, UserRole.EMPLOYEE)
    }
    
    // ========================================
    // USER MANAGEMENT PERMISSIONS
    // ========================================
    
    /**
     * Can view all users (Admin only)
     */
    fun canViewAllUsers(user: User?): Boolean {
        return user?.role == UserRole.ADMIN
    }
    
    /**
     * Can create employee accounts (Admin only)
     */
    fun canCreateEmployees(user: User?): Boolean {
        return user?.role == UserRole.ADMIN
    }
    
    /**
     * Can delete user accounts (Admin only)
     */
    fun canDeleteUsers(user: User?): Boolean {
        return user?.role == UserRole.ADMIN
    }
    
    /**
     * Can edit user roles (Admin only)
     */
    fun canEditUserRoles(user: User?): Boolean {
        return user?.role == UserRole.ADMIN
    }
    
    /**
     * Can view user details (Admin can see all, others can see own profile)
     */
    fun canViewUserDetails(currentUser: User?, targetUserId: Long): Boolean {
        return when (currentUser?.role) {
            UserRole.ADMIN -> true // Admin can see all users
            UserRole.EMPLOYEE, UserRole.CUSTOMER -> currentUser.id == targetUserId // Can only see own profile
            null -> false
        }
    }
    
    // ========================================
    // SALES & TRANSACTIONS PERMISSIONS
    // ========================================
    
    /**
     * Can process sales/transactions (Admin and Employee only)
     */
    fun canProcessSales(user: User?): Boolean {
        return user?.role in listOf(UserRole.ADMIN, UserRole.EMPLOYEE)
    }
    
    /**
     * Can view sales reports (Admin only)
     */
    fun canViewSalesReports(user: User?): Boolean {
        return user?.role == UserRole.ADMIN
    }
    
    /**
     * Can view comprehensive analytics (Admin only)
     */
    fun canViewAnalytics(user: User?): Boolean {
        return user?.role == UserRole.ADMIN
    }
    
    /**
     * Can modify sales records (Admin only)
     */
    fun canModifySales(user: User?): Boolean {
        return user?.role == UserRole.ADMIN
    }
    
    // ========================================
    // RETURNS & REFUNDS PERMISSIONS
    // ========================================
    
    /**
     * Can process returns (Admin and Employee only)
     */
    fun canProcessReturns(user: User?): Boolean {
        return user?.role in listOf(UserRole.ADMIN, UserRole.EMPLOYEE)
    }
    
    /**
     * Can approve refunds (Admin only)
     */
    fun canApproveRefunds(user: User?): Boolean {
        return user?.role == UserRole.ADMIN
    }
    
    /**
     * Can view return history (Admin and Employee only)
     */
    fun canViewReturnHistory(user: User?): Boolean {
        return user?.role in listOf(UserRole.ADMIN, UserRole.EMPLOYEE)
    }
    
    // ========================================
    // SHOPPING CART PERMISSIONS
    // ========================================
    
    /**
     * Can use shopping cart (Customers only)
     */
    fun canUseShoppingCart(user: User?): Boolean {
        return user?.role == UserRole.CUSTOMER
    }
    
    /**
     * Can checkout/place orders (Customers only)
     */
    fun canCheckout(user: User?): Boolean {
        return user?.role == UserRole.CUSTOMER
    }
    
    /**
     * Can view own cart (Customer can see own, Admin can see all)
     */
    fun canViewCart(currentUser: User?, cartOwnerId: Long): Boolean {
        return when (currentUser?.role) {
            UserRole.ADMIN -> true // Admin can see all carts
            UserRole.CUSTOMER -> currentUser.id == cartOwnerId // Customer can only see own cart
            UserRole.EMPLOYEE -> false // Employees cannot access customer carts
            null -> false
        }
    }
    
    // ========================================
    // SYSTEM ADMINISTRATION PERMISSIONS
    // ========================================
    
    /**
     * Can access system settings (Admin only)
     */
    fun canAccessSystemSettings(user: User?): Boolean {
        return user?.role == UserRole.ADMIN
    }
    
    /**
     * Can backup/restore data (Admin only)
     */
    fun canBackupData(user: User?): Boolean {
        return user?.role == UserRole.ADMIN
    }
    
    /**
     * Can view system logs (Admin only)
     */
    fun canViewSystemLogs(user: User?): Boolean {
        return user?.role == UserRole.ADMIN
    }
    
    // ========================================
    // NAVIGATION PERMISSIONS
    // ========================================
    
    /**
     * Get allowed navigation destinations based on user role
     */
    fun getAllowedNavigationRoutes(user: User?): List<String> {
        return when (user?.role) {
            UserRole.ADMIN -> listOf(
                "AdminDashboard",
                "ProductManagement", 
                "UserManagement",
                "SalesOverview",
                "ReturnsProcessing",
                "Analytics",
                "Profile"
            )
            UserRole.EMPLOYEE -> listOf(
                "EmployeeDashboard",
                "ProductManagement",
                "SalesProcessing", 
                "ReturnsProcessing",
                "Profile"
            )
            UserRole.CUSTOMER -> listOf(
                "CustomerDashboard",
                "ProductScreen",
                "CartScreen",
                "Profile",
                "OrderHistory"
            )
            null -> listOf("Login", "Signup")
        }
    }
    
    /**
     * Check if user can navigate to specific route
     */
    fun canNavigateTo(user: User?, route: String): Boolean {
        return getAllowedNavigationRoutes(user).contains(route)
    }
    
    // ========================================
    // UTILITY METHODS
    // ========================================
    
    /**
     * Check if user has admin privileges
     */
    fun isAdmin(user: User?): Boolean {
        return user?.role == UserRole.ADMIN
    }
    
    /**
     * Check if user has employee privileges (Admin or Employee)
     */
    fun isEmployee(user: User?): Boolean {
        return user?.role in listOf(UserRole.ADMIN, UserRole.EMPLOYEE)
    }
    
    /**
     * Check if user is a customer
     */
    fun isCustomer(user: User?): Boolean {
        return user?.role == UserRole.CUSTOMER
    }
    
    /**
     * Get user's permission level as string
     */
    fun getPermissionLevel(user: User?): String {
        return when (user?.role) {
            UserRole.ADMIN -> "Full System Access"
            UserRole.EMPLOYEE -> "Operations & Sales Access"
            UserRole.CUSTOMER -> "Shopping Access"
            null -> "No Access"
        }
    }
}
