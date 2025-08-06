package com.laz.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laz.models.User
import com.laz.models.UserRole
import com.laz.repositories.FirebaseUserRepository
import com.laz.security.PermissionManager
import com.laz.services.FirebaseAuthService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Secure Firebase User ViewModel
 * Enforces admin-only permissions for user management operations
 */
class SecureFirebaseUserViewModel(
    private val userRepository: FirebaseUserRepository,
    private val authService: FirebaseAuthService,
    private val currentUser: StateFlow<User?>
) : ViewModel() {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _permissionError = MutableStateFlow<String?>(null)
    val permissionError: StateFlow<String?> = _permissionError.asStateFlow()

    private val _operationSuccess = MutableStateFlow<String?>(null)
    val operationSuccess: StateFlow<String?> = _operationSuccess.asStateFlow()

    init {
        loadUsers()
    }

    /**
     * Load all users (Admin only)
     */
    fun loadUsers() {
        val user = currentUser.value
        if (!PermissionManager.canViewAllUsers(user)) {
            _permissionError.value = "Access denied: Only administrators can view all users"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _permissionError.value = null
            
            try {
                val result = userRepository.getAllUsers()
                if (result.isSuccess) {
                    _users.value = result.getOrNull() ?: emptyList()
                } else {
                    _errorMessage.value = "Failed to load users: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error loading users: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Create new employee account (Admin only)
     */
    fun createEmployee(
        username: String,
        email: String,
        password: String,
        phoneNumber: String = "",
        address: String = ""
    ) {
        val user = currentUser.value
        if (!PermissionManager.canCreateEmployees(user)) {
            _permissionError.value = "Access denied: Only administrators can create employee accounts"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _permissionError.value = null
            _operationSuccess.value = null
            
            try {
                // Create Firebase Auth account
                val authResult = authService.signUp(email, password, username)
                if (authResult.isSuccess) {
                    val firebaseUser = authResult.getOrNull()
                    if (firebaseUser != null) {
                        // Create user profile in database
                        val newEmployee = User(
                            id = System.currentTimeMillis(), // Generate unique ID
                            username = username,
                            password = password, // Add missing password parameter
                            email = email,
                            phoneNumber = phoneNumber,
                            address = address,
                            role = UserRole.EMPLOYEE,
                            createdAt = System.currentTimeMillis()
                        )
                        
                        val createResult = userRepository.createUser(newEmployee, firebaseUser.uid)
                        if (createResult.isSuccess) {
                            _operationSuccess.value = "Employee account created successfully"
                            loadUsers() // Refresh the list
                        } else {
                            _errorMessage.value = "Failed to create employee profile: ${createResult.exceptionOrNull()?.message}"
                        }
                    }
                } else {
                    _errorMessage.value = "Failed to create employee account: ${authResult.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error creating employee: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Update user role (Admin only)
     */
    fun updateUserRole(userId: Long, newRole: UserRole) {
        val user = currentUser.value
        if (!PermissionManager.canEditUserRoles(user)) {
            _permissionError.value = "Access denied: Only administrators can change user roles"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _permissionError.value = null
            _operationSuccess.value = null
            
            try {
                val targetUser = _users.value.find { it.id == userId }
                if (targetUser != null) {
                    val updatedUser = targetUser.copy(role = newRole)
                    val result = userRepository.updateUser(updatedUser)
                    if (result.isSuccess) {
                        _operationSuccess.value = "User role updated successfully"
                        loadUsers() // Refresh the list
                    } else {
                        _errorMessage.value = "Failed to update user role: ${result.exceptionOrNull()?.message}"
                    }
                } else {
                    _errorMessage.value = "User not found"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error updating user role: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Delete user account (Admin only)
     */
    fun deleteUser(userId: Long) {
        val user = currentUser.value
        if (!PermissionManager.canDeleteUsers(user)) {
            _permissionError.value = "Access denied: Only administrators can delete user accounts"
            return
        }

        // Prevent admin from deleting their own account
        if (user?.id == userId) {
            _errorMessage.value = "Cannot delete your own account"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _permissionError.value = null
            _operationSuccess.value = null
            
            try {
                val result = userRepository.deleteUser(userId)
                if (result.isSuccess) {
                    _operationSuccess.value = "User account deleted successfully"
                    loadUsers() // Refresh the list
                } else {
                    _errorMessage.value = "Failed to delete user: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error deleting user: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Get users by role
     */
    fun getUsersByRole(role: UserRole): StateFlow<List<User>> {
        val user = currentUser.value
        if (!PermissionManager.canViewAllUsers(user)) {
            return flowOf(emptyList<User>()).stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        }

        return users.map { userList ->
            userList.filter { it.role == role }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList<User>())
    }

    /**
     * Get user statistics (Admin only)
     */
    fun getUserStatistics(): StateFlow<UserStatistics> {
        val user = currentUser.value
        if (!PermissionManager.canViewAllUsers(user)) {
            return flowOf(UserStatistics()).stateIn(viewModelScope, SharingStarted.Lazily, UserStatistics())
        }

        return users.map { userList ->
            UserStatistics(
                totalUsers = userList.size,
                adminCount = userList.count { it.role == UserRole.ADMIN },
                employeeCount = userList.count { it.role == UserRole.EMPLOYEE },
                customerCount = userList.count { it.role == UserRole.CUSTOMER }
            )
        }.stateIn(viewModelScope, SharingStarted.Lazily, UserStatistics())
    }

    /**
     * Search users by username or email (Admin only)
     */
    fun searchUsers(query: String): StateFlow<List<User>> {
        val user = currentUser.value
        if (!PermissionManager.canViewAllUsers(user)) {
            return flowOf(emptyList<User>()).stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        }

        return users.map { userList ->
            if (query.isBlank()) {
                userList
            } else {
                userList.filter { 
                    it.username.contains(query, ignoreCase = true) || 
                    it.email?.contains(query, ignoreCase = true) == true
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList<User>())
    }

    /**
     * Check if current user can perform specific actions
     */
    fun canViewAllUsers(): Boolean = PermissionManager.canViewAllUsers(currentUser.value)
    fun canCreateEmployees(): Boolean = PermissionManager.canCreateEmployees(currentUser.value)
    fun canDeleteUsers(): Boolean = PermissionManager.canDeleteUsers(currentUser.value)
    fun canEditUserRoles(): Boolean = PermissionManager.canEditUserRoles(currentUser.value)

    /**
     * Clear messages
     */
    fun clearMessages() {
        _errorMessage.value = null
        _permissionError.value = null
        _operationSuccess.value = null
    }
}

/**
 * Data class for user statistics
 */
data class UserStatistics(
    val totalUsers: Int = 0,
    val adminCount: Int = 0,
    val employeeCount: Int = 0,
    val customerCount: Int = 0
)
