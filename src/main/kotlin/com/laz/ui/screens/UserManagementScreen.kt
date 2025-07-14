package com.laz.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laz.models.User
import com.laz.models.UserRole
import com.laz.ui.theme.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch

// In Python, this would be like a user management interface
// class UserManager:
//     def __init__(self):
//         self.users = []
//         self.setup_user_form()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    userViewModel: com.laz.viewmodels.UserViewModel,
    onBack: () -> Unit
) {
    // Load users from database using UserService
    val users = remember { mutableStateListOf<User>() }
    
    // Load users on composition
    LaunchedEffect(Unit) {
        users.clear()
        users.addAll(userViewModel.getAllUsers())
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingUser by remember { mutableStateOf<User?>(null) }
    var newUsername by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var newRole by remember { mutableStateOf(UserRole.EMPLOYEE) }
    var editUsername by remember { mutableStateOf("") }
    var editPassword by remember { mutableStateOf("") } // For new password input
    var editRole by remember { mutableStateOf(UserRole.EMPLOYEE) }

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(LazDarkBackground, LazDarkSurface)
                )
            )
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = LazDarkCard)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "User Management",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = LazWhite
                    )
                }

                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LazRed,
                        contentColor = LazWhite
                    )
                ) {
                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add User")
                }
            }
        }

        // User List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(users) { user ->
                UserCard(
                    user = user,
                    onEdit = { 
                        if (user.username != "admin") {
                            editingUser = user
                            newUsername = user.username
                            newPassword = ""
                            newRole = user.role
                            showEditDialog = true
                        }
                    },
                    onDelete = {
                        if (user.username != "admin") {
                            coroutineScope.launch {
                                userViewModel.deleteUser(user)
                                users.remove(user)
                            }
                        }
                    }
                )
            }
        }
    }

    // Add User Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add New User", color = LazWhite) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = newUsername,
                        onValueChange = { newUsername = it },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LazRed,
                            focusedLabelColor = LazRed
                        )
                    )
                    
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LazRed,
                            focusedLabelColor = LazRed
                        )
                    )

                    // Role Selection
                    Text("Role:", color = LazWhite)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = newRole == UserRole.EMPLOYEE,
                                onClick = { newRole = UserRole.EMPLOYEE },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = LazRed,
                                    unselectedColor = LazLightGray
                                )
                            )
                            Text("Employee", color = LazWhite)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = newRole == UserRole.ADMIN,
                                onClick = { newRole = UserRole.ADMIN },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = LazRed,
                                    unselectedColor = LazLightGray
                                )
                            )
                            Text("Admin", color = LazWhite)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newUsername.isNotBlank() && newPassword.isNotBlank()) {
                            coroutineScope.launch {
                                val createdUser = userViewModel.createUser(
                                    username = newUsername,
                                    password = newPassword,
                                    role = newRole
                                )
                                if (createdUser != null) {
                                    users.add(createdUser)
                                    showAddDialog = false
                                    // Clear form
                                    newUsername = ""
                                    newPassword = ""
                                    newRole = UserRole.EMPLOYEE
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LazRed,
                        contentColor = LazWhite
                    )
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showAddDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LazGray,
                        contentColor = LazWhite
                    )
                ) {
                    Text("Cancel")
                }
            },
            containerColor = LazDarkCard
        )
    }

    // Edit User Dialog
    if (showEditDialog && editingUser != null) {
        AlertDialog(
            onDismissRequest = {
                showEditDialog = false
                editingUser = null // Clear the user being edited
            },
            title = { Text("Edit User: ${editingUser?.username ?: ""}", color = LazWhite) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = editUsername,
                        onValueChange = { editUsername = it },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LazRed,
                            focusedLabelColor = LazRed,
                            // Add other necessary text field colors for your theme
                        )
                    )

                    OutlinedTextField(
                        value = editPassword, // This will be for the NEW password
                        onValueChange = { editPassword = it },
                        label = { Text("New Password (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        // Add visualTransformation = PasswordVisualTransformation() if desired
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LazRed,
                            focusedLabelColor = LazRed,
                            // Add other necessary text field colors for your theme
                        )
                    )

                    // Role Selection for Edit
                    Text("Role:", color = LazWhite)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = editRole == UserRole.EMPLOYEE,
                                onClick = { editRole = UserRole.EMPLOYEE },
                                colors = RadioButtonDefaults.colors(selectedColor = LazRed, unselectedColor = LazLightGray)
                            )
                            Text("Employee", color = LazWhite)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = editRole == UserRole.ADMIN,
                                onClick = { editRole = UserRole.ADMIN },
                                colors = RadioButtonDefaults.colors(selectedColor = LazRed, unselectedColor = LazLightGray)
                            )
                            Text("Admin", color = LazWhite)
                        }
                        // Add CUSTOMER role if applicable
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = editRole == UserRole.CUSTOMER,
                                onClick = { editRole = UserRole.CUSTOMER },
                                colors = RadioButtonDefaults.colors(selectedColor = LazRed, unselectedColor = LazLightGray)
                            )
                            Text("Customer", color = LazWhite)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        editingUser?.let { currentUser -> // Ensure editingUser is not null
                            if (editUsername.isNotBlank()) { // Basic validation
                                coroutineScope.launch {
                                    val userWithUpdates = currentUser.copy(
                                        username = editUsername,
                                        role = editRole
                                        // The password in this 'userWithUpdates' object is still the old one.
                                        // The 'updateUser' ViewModel function will handle creating a new copy
                                        // with the hashed new password if 'editPassword' is provided.
                                    )

                                    val successfullyUpdatedUser = userViewModel.updateUser(
                                        user = userWithUpdates,
                                        newPasswordPlain = editPassword.takeIf { it.isNotBlank() } // Pass new password if not blank
                                    )

                                    if (successfullyUpdatedUser != null) {
                                        // Update the user in the local 'users' list
                                        val index = users.indexOfFirst { it.id == successfullyUpdatedUser.id }
                                        if (index != -1) {
                                            users[index] = successfullyUpdatedUser
                                        }
                                        showEditDialog = false
                                        editingUser = null // Clear the user being edited
                                    } else {
                                        // Handle update failure (e.g., show a Snackbar or Toast)
                                        // For now, just log it or set an error message state
                                        println("Error: Failed to update user ${currentUser.username}")
                                    }
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LazRed, contentColor = LazWhite)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showEditDialog = false
                        editingUser = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LazGray, contentColor = LazWhite)
                ) {
                    Text("Cancel")
                }
            },
            containerColor = LazDarkCard
        )
    }
}

@Composable
fun UserCard(
    user: User,
    onEdit: (User) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LazDarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (user.role == UserRole.ADMIN) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                    contentDescription = user.role.name,
                    tint = if (user.role == UserRole.ADMIN) LazRedGlow else LazRed,
                    modifier = Modifier.size(32.dp)
                )
                
                Column {
                    Text(
                        text = user.username,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = LazWhite
                    )
                    Text(
                        text = user.role.name,
                        fontSize = 12.sp,
                        color = if (user.role == UserRole.ADMIN) LazRedGlow else LazLightGray
                    )
                }
            }

            if (user.username.lowercase() != "admin") {
                IconButton(onClick = { onEdit(user) }) { // Call onEdit with the user
                    Icon(Icons.Default.Edit, contentDescription = "Edit User", tint = LazLightGray)
                }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = LazRedGlow
                        )
                    }
                }
            }
        }
    }

