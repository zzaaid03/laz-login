package com.laz.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.laz.models.User
import com.laz.models.UserRole
import com.laz.viewmodels.SecureFirebaseUserViewModel
import com.laz.viewmodels.UserStatistics

/**
 * Admin User Management Screen
 * Admin-only screen for managing users, creating employees, and viewing user statistics
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserManagementScreen(
    userViewModel: SecureFirebaseUserViewModel,
    onNavigateBack: () -> Unit
) {
    // Collect state from ViewModel
    val users by userViewModel.users.collectAsState()
    val userStats by userViewModel.getUserStatistics().collectAsState()
    val isLoading by userViewModel.isLoading.collectAsState()
    val errorMessage by userViewModel.errorMessage.collectAsState()
    val permissionError by userViewModel.permissionError.collectAsState()
    val operationSuccess by userViewModel.operationSuccess.collectAsState()
    
    // Local UI state
    var showCreateEmployeeDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    var lastDismissedError by remember { mutableStateOf<String?>(null) }
    
    // Filter users based on search query and selected tab
    val filteredUsers by userViewModel.searchUsers(searchQuery).collectAsState()
    
    // Load users when screen is first displayed
    LaunchedEffect(Unit) {
        if (users.isEmpty()) {
            userViewModel.loadUsers()
        }
    }
    
    // Auto-dismiss success/error messages after 3 seconds
    LaunchedEffect(errorMessage, operationSuccess) {
        if (errorMessage != null || operationSuccess != null) {
            kotlinx.coroutines.delay(3000)
            userViewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Management") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateEmployeeDialog = true }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add Employee")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Permission Error Display
            permissionError?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = "Permission Error",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Only show content if user has permission
            if (userViewModel.canViewAllUsers()) {
                // User Statistics Cards
                UserStatisticsSection(userStats)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search users...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Tab Row for filtering by role
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("All Users") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Admins") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Employees") }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("Customers") }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Users List
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    val displayUsers = when (selectedTab) {
                        1 -> filteredUsers.filter { it.role == UserRole.ADMIN }
                        2 -> filteredUsers.filter { it.role == UserRole.EMPLOYEE }
                        3 -> filteredUsers.filter { it.role == UserRole.CUSTOMER }
                        else -> filteredUsers
                    }
                    
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displayUsers) { user ->
                            UserCard(
                                user = user,
                                onDeleteUser = { userViewModel.deleteUser(it) },
                                onUpdateRole = { userId, newRole -> 
                                    userViewModel.updateUserRole(userId, newRole) 
                                },
                                canDelete = userViewModel.canDeleteUsers(),
                                canEditRoles = userViewModel.canEditUserRoles()
                            )
                        }
                    }
                }
            }
            
            // Error and Success Messages
            errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            operationSuccess?.let { success ->
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text(
                        text = success,
                        modifier = Modifier.padding(16.dp),
                        color = Color.White
                    )
                }
            }
        }
    }
    
    // Create Employee Dialog
    if (showCreateEmployeeDialog) {
        CreateEmployeeDialog(
            onDismiss = { showCreateEmployeeDialog = false },
            onCreateEmployee = { username, email, password, phone, address, role ->
                userViewModel.createEmployee(username, email, password, phone, address)
                showCreateEmployeeDialog = false
            }
        )
    }
    
    // Clear messages after showing them
    LaunchedEffect(errorMessage, operationSuccess) {
        if ((errorMessage != null && errorMessage != lastDismissedError) || operationSuccess != null) {
            kotlinx.coroutines.delay(3000)
            lastDismissedError = errorMessage
            userViewModel.clearMessages()
        }
    }
}

@Composable
private fun UserStatisticsSection(stats: UserStatistics) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            title = "Total Users",
            value = stats.totalUsers.toString(),
            icon = Icons.Default.People,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Admins",
            value = stats.adminCount.toString(),
            icon = Icons.Default.AdminPanelSettings,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Employees",
            value = stats.employeeCount.toString(),
            icon = Icons.Default.Work,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Customers",
            value = stats.customerCount.toString(),
            icon = Icons.Default.ShoppingCart,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun UserCard(
    user: User,
    onDeleteUser: (Long) -> Unit,
    onUpdateRole: (Long, UserRole) -> Unit,
    canDelete: Boolean,
    canEditRoles: Boolean
) {
    var showRoleDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }
    
    // Format user creation date
    val formattedDate = remember(user.createdAt) {
        user.createdAt?.let { 
            java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(it))
        } ?: "Unknown"
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (user.role) {
                UserRole.ADMIN -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                UserRole.EMPLOYEE -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                UserRole.CUSTOMER -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .animateContentSize()
        ) {
            // Header row with user info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User info
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.username,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        
                        // Role badge
                        Surface(
                            color = when (user.role) {
                                UserRole.ADMIN -> MaterialTheme.colorScheme.primary
                                UserRole.EMPLOYEE -> MaterialTheme.colorScheme.secondary
                                UserRole.CUSTOMER -> MaterialTheme.colorScheme.tertiary
                            }.copy(alpha = 0.2f),
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(
                                1.dp,
                                when (user.role) {
                                    UserRole.ADMIN -> MaterialTheme.colorScheme.primary
                                    UserRole.EMPLOYEE -> MaterialTheme.colorScheme.secondary
                                    UserRole.CUSTOMER -> MaterialTheme.colorScheme.tertiary
                                }
                            )
                        ) {
                            Text(
                                text = user.role.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                color = when (user.role) {
                                    UserRole.ADMIN -> MaterialTheme.colorScheme.onPrimaryContainer
                                    UserRole.EMPLOYEE -> MaterialTheme.colorScheme.onSecondaryContainer
                                    UserRole.CUSTOMER -> MaterialTheme.colorScheme.onTertiaryContainer
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Email and phone
                    Text(
                        text = user.email ?: "No email",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (user.phoneNumber?.isNotBlank() == true) {
                        Text(
                            text = user.phoneNumber,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    
                    // Created at
                    Text(
                        text = "Created: $formattedDate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                
                // Action buttons
                Row {
                    // Toggle details button
                    IconButton(onClick = { showDetails = !showDetails }) {
                        Icon(
                            if (showDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (showDetails) "Hide details" else "Show details"
                        )
                    }
                    
                    // Edit role button (if allowed)
                    if (canEditRoles) {
                        IconButton(
                            onClick = { showRoleDialog = true },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit Role",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    // Delete button (if allowed and not admin)
                    if (canDelete && user.role != UserRole.ADMIN) {
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete User",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            
            // Expanded details section
            if (showDetails) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Additional user details
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    // Address
                    user.address?.takeIf { it.isNotBlank() }?.let { address ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = address,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                            )
                        }
                    }
                    
                    // User ID
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "ID: ${user.id}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
    
    // Role Change Dialog
    if (showRoleDialog) {
        RoleChangeDialog(
            currentRole = user.role,
            onDismiss = { showRoleDialog = false },
            onRoleChange = { newRole ->
                onUpdateRole(user.id, newRole)
                showRoleDialog = false
            }
        )
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirm Deletion") },
            text = { 
                Column {
                    Text("Are you sure you want to delete this user?")
                    Text(
                        "This will permanently delete ${user.username}'s account and all associated data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteUser(user.id)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CreateEmployeeDialog(
    onDismiss: () -> Unit,
    onCreateEmployee: (String, String, String, String, String, UserRole) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.EMPLOYEE) }
    var showRoleDropdown by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Create Employee Account",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPassword) "Hide password" else "Show password"
                            )
                        }
                    },
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Role Selection
                ExposedDropdownMenuBox(
                    expanded = showRoleDropdown,
                    onExpandedChange = { showRoleDropdown = !showRoleDropdown }
                ) {
                    OutlinedTextField(
                        value = when (selectedRole) {
                            UserRole.ADMIN -> "Admin"
                            UserRole.EMPLOYEE -> "Employee"
                            else -> "Employee"
                        },
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Role") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = showRoleDropdown
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = showRoleDropdown,
                        onDismissRequest = { showRoleDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Admin") },
                            onClick = {
                                selectedRole = UserRole.ADMIN
                                showRoleDropdown = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Employee") },
                            onClick = {
                                selectedRole = UserRole.EMPLOYEE
                                showRoleDropdown = false
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (username.isNotBlank() && email.isNotBlank() && password.isNotBlank()) {
                                onCreateEmployee(username, email, password, phoneNumber, address, selectedRole)
                            }
                        },
                        enabled = username.isNotBlank() && email.isNotBlank() && password.isNotBlank()
                    ) {
                        Text(when (selectedRole) {
                            UserRole.ADMIN -> "Create Admin"
                            UserRole.EMPLOYEE -> "Create Employee"
                            else -> "Create Employee"
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleChangeDialog(
    currentRole: UserRole,
    onDismiss: () -> Unit,
    onRoleChange: (UserRole) -> Unit
) {
    var selectedRole by remember { mutableStateOf(currentRole) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change User Role") },
        text = {
            Column {
                Text("Select new role:")
                Spacer(modifier = Modifier.height(8.dp))
                
                UserRole.values().forEach { role ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = selectedRole == role,
                            onClick = { selectedRole = role }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(role.name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onRoleChange(selectedRole) },
                enabled = selectedRole != currentRole
            ) {
                Text("Change Role")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
