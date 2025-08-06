package com.laz.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laz.models.User
import com.laz.models.UserRole
import com.laz.repositories.FirebaseUserRepository
import com.laz.services.FirebaseAuthService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Firebase Authentication ViewModel
 * Manages authentication state using Firebase Auth and Realtime Database
 */
class FirebaseAuthViewModel(
    private val firebaseAuthService: FirebaseAuthService,
    private val firebaseUserRepository: FirebaseUserRepository
) : ViewModel() {

    // Authentication state
    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error messages
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        // Check if user is already logged in
        checkCurrentUser()
    }

    /**
     * Check if user is currently logged in
     */
    private fun checkCurrentUser() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentFirebaseUser = firebaseAuthService.currentUser
                if (currentFirebaseUser != null) {
                    // Get user data from Firebase Database
                    val userResult = firebaseUserRepository.getUserByFirebaseUid(currentFirebaseUser.uid)
                    if (userResult.isSuccess) {
                        val user = userResult.getOrNull()
                        if (user != null) {
                            _authState.value = AuthState(
                                isLoggedIn = true,
                                user = user,
                                userRole = user.role
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error checking authentication: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Sign up new user
     */
    fun signUp(
        username: String,
        email: String,
        password: String,
        phoneNumber: String? = null,
        address: String? = null,
        role: UserRole = UserRole.CUSTOMER
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                // Check if email is already taken (only if the query succeeds)
                val existingUserResult = firebaseUserRepository.getUserByEmail(email)
                if (existingUserResult.isSuccess) {
                    val existingUser = existingUserResult.getOrNull()
                    if (existingUser != null) {
                        _errorMessage.value = "Email already exists"
                        return@launch
                    }
                    // If result is success but user is null, email is available
                } else {
                    // If query failed, log the error but continue with signup
                    android.util.Log.w("FirebaseAuth", "Email check failed: ${existingUserResult.exceptionOrNull()?.message}")
                }

                // Create Firebase Auth user
                val authResult = firebaseAuthService.signUp(email, password, username)
                if (authResult.isSuccess) {
                    val firebaseUser = authResult.getOrNull()
                    if (firebaseUser != null) {
                        // Generate user ID and create user in database
                        val userId = firebaseUserRepository.getNextUserId()
                        val user = User(
                            id = userId,
                            username = username,
                            password = "", // Not stored in Firebase
                            email = email,
                            phoneNumber = phoneNumber,
                            address = address,
                            role = role,
                            createdAt = System.currentTimeMillis()
                        )

                        // Save user to Firebase Database
                        val userResult = firebaseUserRepository.createUser(user, firebaseUser.uid)
                        if (userResult.isSuccess) {
                            _authState.value = AuthState(
                                isLoggedIn = true,
                                user = user,
                                userRole = role
                            )
                        } else {
                            _errorMessage.value = "Failed to create user profile: ${userResult.exceptionOrNull()?.message}"
                        }
                    }
                } else {
                    _errorMessage.value = authResult.exceptionOrNull()?.message ?: "Sign up failed"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Sign up error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Sign in user
     */
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                // Sign in with Firebase Auth
                val authResult = firebaseAuthService.signIn(email, password)
                if (authResult.isSuccess) {
                    val firebaseUser = authResult.getOrNull()
                    if (firebaseUser != null) {
                        // Get user data from Firebase Database
                        val userResult = firebaseUserRepository.getUserByFirebaseUid(firebaseUser.uid)
                        if (userResult.isSuccess) {
                            val user = userResult.getOrNull()
                            if (user != null) {
                                _authState.value = AuthState(
                                    isLoggedIn = true,
                                    user = user,
                                    userRole = user.role
                                )
                            } else {
                                _errorMessage.value = "User profile not found"
                            }
                        } else {
                            _errorMessage.value = "Failed to load user profile: ${userResult.exceptionOrNull()?.message}"
                        }
                    }
                } else {
                    _errorMessage.value = authResult.exceptionOrNull()?.message ?: "Sign in failed"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Sign in error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Sign out user
     */
    fun signOut() {
        viewModelScope.launch {
            try {
                firebaseAuthService.signOut()
                _authState.value = AuthState()
            } catch (e: Exception) {
                _errorMessage.value = "Sign out error: ${e.message}"
            }
        }
    }

    /**
     * Update user profile
     */
    fun updateProfile(
        username: String? = null,
        email: String? = null,
        phoneNumber: String? = null,
        address: String? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val currentUser = _authState.value.user
                if (currentUser != null) {
                    val updatedUser = currentUser.copy(
                        username = username ?: currentUser.username,
                        email = email ?: currentUser.email,
                        phoneNumber = phoneNumber ?: currentUser.phoneNumber,
                        address = address ?: currentUser.address
                    )

                    // Update in Firebase Database
                    val result = firebaseUserRepository.updateUser(updatedUser)
                    if (result.isSuccess) {
                        _authState.value = _authState.value.copy(user = updatedUser)
                        
                        // Note: Email updates would require Firebase Auth profile updates
                        // This functionality can be added later if needed
                    } else {
                        _errorMessage.value = "Failed to update profile: ${result.exceptionOrNull()?.message}"
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Profile update error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Reset password
     */
    fun resetPassword(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                // Note: Password reset functionality would require Firebase Auth implementation
                // This functionality can be added later if needed
                _errorMessage.value = "Password reset functionality not implemented yet"
            } catch (e: Exception) {
                _errorMessage.value = "Password reset error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Delete user account
     */
    fun deleteAccount() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val currentUser = _authState.value.user
                if (currentUser != null) {
                    // Delete from Firebase Database
                    val dbResult = firebaseUserRepository.deleteUser(currentUser.id)
                    if (dbResult.isSuccess) {
                        // Delete from Firebase Auth
                        val authResult = firebaseAuthService.deleteAccount()
                        if (authResult.isSuccess) {
                            _authState.value = AuthState()
                        } else {
                            _errorMessage.value = "Failed to delete auth account: ${authResult.exceptionOrNull()?.message}"
                        }
                    } else {
                        _errorMessage.value = "Failed to delete user data: ${dbResult.exceptionOrNull()?.message}"
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Account deletion error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Get current user ID
     */
    fun getCurrentUserId(): Long? {
        return _authState.value.user?.id
    }

    /**
     * Check if user has specific role
     */
    fun hasRole(role: UserRole): Boolean {
        return _authState.value.userRole == role
    }

    /**
     * Check if user is admin
     */
    fun isAdmin(): Boolean {
        return hasRole(UserRole.ADMIN)
    }

    /**
     * Check if user is employee
     */
    fun isEmployee(): Boolean {
        return hasRole(UserRole.EMPLOYEE)
    }

    /**
     * Check if user is customer
     */
    fun isCustomer(): Boolean {
        return hasRole(UserRole.CUSTOMER)
    }
}

/**
 * Authentication State Data Class
 */
data class AuthState(
    val isLoggedIn: Boolean = false,
    val user: User? = null,
    val userRole: UserRole = UserRole.CUSTOMER,
    val loading: Boolean = false,
    val error: String? = null
)
