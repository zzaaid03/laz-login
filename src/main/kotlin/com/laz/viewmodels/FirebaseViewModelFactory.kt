package com.laz.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.laz.repositories.FirebaseCartRepository
import com.laz.repositories.FirebaseProductRepository
import com.laz.repositories.FirebaseUserRepository
import com.laz.services.FirebaseAuthService
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.laz.models.User

/**
 * Secure Firebase ViewModelFactory
 * Creates secure role-based ViewModels with Firebase dependencies
 */
class SecureFirebaseViewModelFactory(
    private val firebaseAuthService: FirebaseAuthService,
    private val firebaseUserRepository: FirebaseUserRepository,
    private val firebaseProductRepository: FirebaseProductRepository,
    private val firebaseCartRepository: FirebaseCartRepository,
    private val currentUser: StateFlow<User?>
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            FirebaseAuthViewModel::class.java -> {
                FirebaseAuthViewModel(
                    firebaseAuthService = firebaseAuthService,
                    firebaseUserRepository = firebaseUserRepository
                ) as T
            }
            SecureFirebaseProductViewModel::class.java -> {
                SecureFirebaseProductViewModel(
                    productRepository = firebaseProductRepository,
                    currentUser = currentUser
                ) as T
            }
            SecureFirebaseCartViewModel::class.java -> {
                SecureFirebaseCartViewModel(
                    cartRepository = firebaseCartRepository,
                    productRepository = firebaseProductRepository,
                    currentUser = currentUser
                ) as T
            }
            SecureFirebaseUserViewModel::class.java -> {
                SecureFirebaseUserViewModel(
                    userRepository = firebaseUserRepository,
                    authService = firebaseAuthService,
                    currentUser = currentUser
                ) as T
            }
            FirebaseSalesViewModel::class.java -> {
                FirebaseSalesViewModel(
                    firebaseProductRepository = firebaseProductRepository
                ) as T
            }
            FirebaseReturnsViewModel::class.java -> {
                FirebaseReturnsViewModel(
                    firebaseProductRepository = firebaseProductRepository
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

/**
 * Singleton object that provides Firebase services and ViewModels
 * Holds all Firebase service instances for dependency injection
 */
object FirebaseServices {
    val authService: FirebaseAuthService by lazy { FirebaseAuthService() }
    val userRepository: FirebaseUserRepository by lazy { FirebaseUserRepository() }
    val productRepository: FirebaseProductRepository by lazy { FirebaseProductRepository() }
    val cartRepository: FirebaseCartRepository by lazy { FirebaseCartRepository() }
    
    // Current user StateFlow that combines Firebase auth with User model data
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    // Initialize current user state from Firebase auth
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    init {
        scope.launch {
            authService.authStateFlow.collect { firebaseUser ->
                println("DEBUG: ===== FIREBASE SERVICES USER UPDATE =====")
                println("DEBUG: Firebase user changed: ${firebaseUser?.email}")
                
                if (firebaseUser != null) {
                    println("DEBUG: Firebase user found: ${firebaseUser.email}")
                    // Look up the User model from Firebase database using email
                    try {
                        val result = userRepository.getAllUsers()
                        if (result.isSuccess) {
                            val users = result.getOrNull() ?: emptyList()
                            println("DEBUG: Found ${users.size} users in database")
                            val user = users.find { it.email == firebaseUser.email }
                            if (user != null) {
                                println("DEBUG: Found matching user: ${user.username} (${user.role})")
                                _currentUser.value = user
                                println("DEBUG: Current user updated successfully")
                            } else {
                                println("DEBUG: No matching user found for email: ${firebaseUser.email}")
                                println("DEBUG: Available users:")
                                users.forEach { u ->
                                    println("DEBUG:   - ${u.email} (${u.username}, ${u.role})")
                                }
                                _currentUser.value = null
                            }
                        } else {
                            println("DEBUG: Error loading user data: ${result.exceptionOrNull()?.message}")
                            _currentUser.value = null
                        }
                    } catch (e: Exception) {
                        println("DEBUG: Exception loading user data: ${e.message}")
                        _currentUser.value = null
                    }
                } else {
                    println("DEBUG: No Firebase user, setting current user to null")
                    _currentUser.value = null
                }
                println("DEBUG: Final current user value: ${_currentUser.value}")
                println("DEBUG: ===== END FIREBASE SERVICES UPDATE =====")
            }
        }
    }
    
    val viewModelFactory: SecureFirebaseViewModelFactory by lazy {
        SecureFirebaseViewModelFactory(
            firebaseAuthService = authService,
            firebaseUserRepository = userRepository,
            firebaseProductRepository = productRepository,
            firebaseCartRepository = cartRepository,
            currentUser = currentUser
        )
    }
    
    // Secure ViewModelFactory with role-based access control
    val secureViewModelFactory: SecureFirebaseViewModelFactory by lazy {
        viewModelFactory // Use the same instance
    }
}
