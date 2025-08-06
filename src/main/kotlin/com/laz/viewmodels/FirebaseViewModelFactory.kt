package com.laz.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.laz.repositories.FirebaseCartRepository
import com.laz.repositories.FirebaseProductRepository
import com.laz.repositories.FirebaseUserRepository
import com.laz.services.FirebaseAuthService
import kotlinx.coroutines.flow.StateFlow
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
 * Firebase Services Container
 * Holds all Firebase service instances for dependency injection
 */
object FirebaseServices {
    val authService: FirebaseAuthService by lazy { FirebaseAuthService() }
    val userRepository: FirebaseUserRepository by lazy { FirebaseUserRepository() }
    val productRepository: FirebaseProductRepository by lazy { FirebaseProductRepository() }
    val cartRepository: FirebaseCartRepository by lazy { FirebaseCartRepository() }
    
    // For now, keep the old ViewModelFactory for compatibility
    // TODO: Migrate to secure ViewModels with proper currentUser StateFlow
    val viewModelFactory: SecureFirebaseViewModelFactory by lazy {
        // We'll create a simple StateFlow for currentUser
        val currentUserFlow = kotlinx.coroutines.flow.MutableStateFlow<com.laz.models.User?>(null)
        SecureFirebaseViewModelFactory(
            firebaseAuthService = authService,
            firebaseUserRepository = userRepository,
            firebaseProductRepository = productRepository,
            firebaseCartRepository = cartRepository,
            currentUser = currentUserFlow
        )
    }
    
    // Secure ViewModelFactory with role-based access control
    val secureViewModelFactory: SecureFirebaseViewModelFactory by lazy {
        viewModelFactory // Use the same instance for now
    }
}
