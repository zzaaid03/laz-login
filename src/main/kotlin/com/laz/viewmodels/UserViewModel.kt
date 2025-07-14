package com.laz.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laz.database.UserDao
import com.laz.models.User
import com.laz.models.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class UserViewModel(private val userDao: UserDao) : ViewModel() {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        viewModelScope.launch {
            userDao.getAllUsers().collect { userList ->
                _users.value = userList
            }
        }
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            // Check if we have any users, if not create default admin
            val existingUsers = userDao.getAllUsers()
            existingUsers.collect { userList ->
                if (userList.isEmpty()) {
                    createUser(
                        User(
                            username = "admin",
                            password = "admin",
                            role = UserRole.ADMIN
                        )
                    )
                    createUser(
                        User(
                            username = "employee",
                            password = "employee",
                            role = UserRole.EMPLOYEE
                        )
                    )
                }
            }
        }
    }

    suspend fun authenticateUser(username: String, password: String): User? {
        return userDao.authenticateUser(username, password)?.also { user ->
            _currentUser.value = user
        }
    }

    suspend fun createUser(user: User): Long {
        return userDao.insertUser(user)
    }

//    suspend fun updateUser(user: User): User? {
//        return try {
//            // It's good practice to run database operations on a background thread
//            withContext(Dispatchers.IO) {
//                userDao.updateUser(user) // Assuming your UserDao has an updateUser method
//                Log.d("UserViewModel", "User updated successfully: ${user.username}")
//                // After updating, you might want to return the updated user object.
//                // If userDao.updateUser doesn't return the updated object,
//                // you can fetch it again, or just return the passed 'user' object
//                // if it accurately reflects the state post-update.
//                // For simplicity, we'll return the input 'user' assuming it's correct.
//                // Alternatively, you could fetch it: userDao.getUserById(user.id)
//                user
//            }
//        } catch (e: Exception) {
//            Log.e("UserViewModel", "Error updating user ${user.username}: ${e.message}", e)
//            null // Return null to indicate failure
//        }
//    }
    suspend fun updateUser(user: User, newPasswordPlain: String? = null): User? {
        return try {
            withContext(Dispatchers.IO) {
                val userToUpdate = if (newPasswordPlain != null && newPasswordPlain.isNotBlank()) {
                    // IMPORTANT: Hash the new password before saving!
                    // Replace 'encodePassword' with your actual password hashing logic
                    user.copy(password = encodePassword(newPasswordPlain))
                } else {
                    user // No password change, or new password is blank
                }
                userDao.updateUser(userToUpdate)
                Log.d("UserViewModel", "User updated successfully: ${userToUpdate.username}")
                userToUpdate // Return the (potentially) modified user object
            }
        } catch (e: Exception) {
            Log.e("UserViewModel", "Error updating user ${user.username}: ${e.message}", e)
            null
        }
    }


    suspend fun deleteUser(user: User) {
        userDao.deleteUser(user)
    }

    suspend fun deleteUserById(id: Long) {
        userDao.deleteUserById(id)
    }

    suspend fun getUserById(id: Long): User? {
        return userDao.getUserById(id)
    }

    suspend fun getUserByUsername(username: String): User? {
        return userDao.getUserByUsername(username)
    }

    fun logout() {
        _currentUser.value = null
    }

    // Convenience methods
    fun getAllUsers(): List<User> {
        return _users.value
    }
    suspend fun createUser(username: String, password: String, role: UserRole): User? {
        // ... logic to create user ...
        // You'll need to decide how to handle password hashing here.
        // For now, I'll use your existing encodePassword, but remember this is not secure for production.
        val hashedPassword = encodePassword(password)
        val newUser = User(username = username, password = hashedPassword, role = role)

        // It's crucial to also assign a unique ID to the newUser if it's generated by the database upon insertion.
        // Your existing `createUser(user: User): Long` function already handles this.
        // Let's call that function.

        val insertedId = userDao.insertUser(newUser) // Use the injected userDao

        return if (insertedId > 0) {
            // Fetch the user by the new ID to ensure you have the complete User object
            // as it is stored in the database (including any auto-generated fields like ID).
            userDao.getUserById(insertedId)
        } else {
            Log.e("UserViewModel", "Failed to insert user: $username")
            null // Indicate creation failed
        }
    }
   /* suspend fun createUser(username: String, password: String, role: UserRole): Boolean {
        if (userDao.getUserByUsername(username) != null) {
            Log.w(
                "UserViewModel",
                "Attempt to create user failed: Username '$username' already exists."
            )
            return false // Username exists
        }
        val user = User(
            username = username,
            password = password,
            role = role
        )

        try {
            createUser(user)
            Log.d("UserViewModel", "User created successfully: $user")
            return true
        } catch (e: Exception) {
            Log.e("UserViewModel", "Error creating user: ${e.message}", e)
            return false
        }
    }*/

    fun encodePassword(password: String): String {
        // Simple encoding for now - in production use proper hashing
        return password
    }
}
