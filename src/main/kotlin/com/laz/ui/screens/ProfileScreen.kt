package com.laz.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laz.models.User
import com.laz.ui.theme.LazRed
import com.laz.viewmodels.UserViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userViewModel: UserViewModel,
    onNavigateBack: () -> Unit
) {
    val user = userViewModel.currentUser.collectAsState().value
    
    var isEditing by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf(user?.username ?: "") }
    var password by remember { mutableStateOf("") } // Empty for security
    
    // Create coroutine scope at the composable level
    val scope = rememberCoroutineScope()
    
    // Since the User model doesn't have these fields, we'll store them in local state
    // In a real app, these would come from a user profile model linked to the User
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    
    val scrollState = rememberScrollState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        if (isEditing) {
                            // Save changes
                            user?.let { currentUser ->
                                // In a real app, we would update all fields
                                // For now, we can only update username and password
                                val updatedUser = currentUser.copy(
                                    username = username
                                )
                                // Use the scope defined at the composable level
                                scope.launch {
                                    userViewModel.updateUser(updatedUser, if (password.isNotEmpty()) password else null)
                                }
                            }
                        }
                        isEditing = !isEditing
                    }) {
                        Icon(
                            if (isEditing) Icons.Filled.Save else Icons.Filled.Edit,
                            contentDescription = if (isEditing) "Save" else "Edit"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Personal Information",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = LazRed
                    )
                    
                    ProfileField(
                        label = "Username",
                        value = username,
                        onValueChange = { username = it },
                        isEditable = isEditing
                    )
                    
                    ProfileField(
                        label = "Password",
                        value = password,
                        onValueChange = { password = it },
                        isEditable = isEditing,
                        isPassword = true
                    )
                    
                    // These fields would be in a real profile system but aren't in our User model
                    // Keeping them for UI demonstration purposes
                    ProfileField(
                        label = "First Name",
                        value = firstName,
                        onValueChange = { firstName = it },
                        isEditable = isEditing
                    )
                    
                    ProfileField(
                        label = "Last Name",
                        value = lastName,
                        onValueChange = { lastName = it },
                        isEditable = isEditing
                    )
                    
                    ProfileField(
                        label = "Email",
                        value = email,
                        onValueChange = { email = it },
                        isEditable = isEditing
                    )
                    
                    ProfileField(
                        label = "Phone",
                        value = phone,
                        onValueChange = { phone = it },
                        isEditable = isEditing
                    )
                    
                    ProfileField(
                        label = "Address",
                        value = address,
                        onValueChange = { address = it },
                        isEditable = isEditing
                    )
                }
            }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Account Information",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = LazRed
                    )
                    
                    user?.let { currentUser ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "User ID",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = currentUser.id.toString(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Role",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = currentUser.role.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            if (isEditing) {
                Button(
                    onClick = {
                        user?.let { currentUser ->
                            // In a real app, we would update all fields
                            // For now, we can only update username and password
                            val updatedUser = currentUser.copy(
                                username = username
                            )
                            // Use the scope defined at the composable level
                            scope.launch {
                                userViewModel.updateUser(updatedUser, if (password.isNotEmpty()) password else null)
                            }
                        }
                        isEditing = false
                    },
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = LazRed)
                ) {
                    Text("Save Changes")
                }
            }
        }
    }
}

@Composable
fun ProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isEditable: Boolean,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        if (isEditable) {
            var passwordVisible by remember { mutableStateOf(false) }
            
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = LazRed,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                ),
                visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = if (isPassword) KeyboardOptions(keyboardType = KeyboardType.Password) else KeyboardOptions.Default,
                trailingIcon = if (isPassword) {
                    @Composable
                    fun PasswordVisibilityIcon() {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    }
                    PasswordVisibilityIcon()
                } else null,
            )
        } else {
            Text(
                text = if (isPassword && value.isNotEmpty()) "********" else value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
