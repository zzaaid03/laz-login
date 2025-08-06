package com.laz.services

import android.util.Log
import com.laz.models.User
import com.laz.models.UserRole
import com.laz.repositories.FirebaseUserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Default User Service
 * Creates default admin and employee users if they don't exist
 */
class DefaultUserService(
    private val firebaseAuthService: FirebaseAuthService,
    private val firebaseUserRepository: FirebaseUserRepository
) {
    
    private val TAG = "DefaultUserService"
    
    /**
     * Create default admin and employee users
     */
    fun createDefaultUsers() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Create default admin user
                createDefaultAdmin()
                
                // Create default employee user
                createDefaultEmployee()
                
                Log.d(TAG, "Default users creation process completed")
            } catch (e: Exception) {
                Log.e(TAG, "Error creating default users: ${e.message}", e)
            }
        }
    }
    
    private suspend fun createDefaultAdmin() {
        val adminEmail = "admin@laz.com"
        val adminPassword = "admin123"
        
        try {
            // Check if admin already exists
            val existingAdmin = firebaseUserRepository.getUserByEmail(adminEmail)
            if (existingAdmin.isSuccess && existingAdmin.getOrNull() != null) {
                Log.d(TAG, "Default admin user already exists")
                return
            }
            
            // Create Firebase Auth user for admin
            val authResult = firebaseAuthService.signUp(adminEmail, adminPassword, "Admin")
            if (authResult.isSuccess) {
                val firebaseUser = authResult.getOrNull()
                if (firebaseUser != null) {
                    // Generate user ID and create admin user in database
                    val userId = firebaseUserRepository.getNextUserId()
                    val adminUser = User(
                        id = userId,
                        username = "admin",
                        password = "", // Not stored in Firebase
                        email = adminEmail,
                        phoneNumber = "1234567890",
                        address = "Admin Office",
                        role = UserRole.ADMIN,
                        createdAt = System.currentTimeMillis()
                    )
                    
                    // Save admin user to Firebase Database
                    val userResult = firebaseUserRepository.createUser(adminUser, firebaseUser.uid)
                    if (userResult.isSuccess) {
                        Log.d(TAG, "Default admin user created successfully")
                    } else {
                        Log.e(TAG, "Failed to create admin user profile: ${userResult.exceptionOrNull()?.message}")
                    }
                } else {
                    Log.e(TAG, "Firebase user creation succeeded but user is null")
                }
            } else {
                Log.e(TAG, "Failed to create admin Firebase Auth user: ${authResult.exceptionOrNull()?.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating default admin: ${e.message}", e)
        }
    }
    
    private suspend fun createDefaultEmployee() {
        val employeeEmail = "emp@laz.com"
        val employeePassword = "emp123"
        
        try {
            // Check if employee already exists
            val existingEmployee = firebaseUserRepository.getUserByEmail(employeeEmail)
            if (existingEmployee.isSuccess && existingEmployee.getOrNull() != null) {
                Log.d(TAG, "Default employee user already exists")
                return
            }
            
            // Create Firebase Auth user for employee
            val authResult = firebaseAuthService.signUp(employeeEmail, employeePassword, "Employee")
            if (authResult.isSuccess) {
                val firebaseUser = authResult.getOrNull()
                if (firebaseUser != null) {
                    // Generate user ID and create employee user in database
                    val userId = firebaseUserRepository.getNextUserId()
                    val employeeUser = User(
                        id = userId,
                        username = "employee",
                        password = "", // Not stored in Firebase
                        email = employeeEmail,
                        phoneNumber = "0987654321",
                        address = "Employee Office",
                        role = UserRole.EMPLOYEE,
                        createdAt = System.currentTimeMillis()
                    )
                    
                    // Save employee user to Firebase Database
                    val userResult = firebaseUserRepository.createUser(employeeUser, firebaseUser.uid)
                    if (userResult.isSuccess) {
                        Log.d(TAG, "Default employee user created successfully")
                    } else {
                        Log.e(TAG, "Failed to create employee user profile: ${userResult.exceptionOrNull()?.message}")
                    }
                } else {
                    Log.e(TAG, "Firebase user creation succeeded but user is null")
                }
            } else {
                Log.e(TAG, "Failed to create employee Firebase Auth user: ${authResult.exceptionOrNull()?.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating default employee: ${e.message}", e)
        }
    }
}
