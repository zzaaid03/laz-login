package com.laz.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laz.models.UserRole
import com.laz.viewmodels.UserViewModel
import com.laz.ui.theme.LazRed // Ensure you have this color
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    userViewModel: UserViewModel,
    onSignupSuccess: () -> Unit, // Callback for successful signup
    onNavigateToLogin: () -> Unit // Callback to go back to Login
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    fun handleSignup() {
        if (password != confirmPassword) {
            errorMessage = "Passwords do not match."
            Log.d("SignupScreen", "Passwords do not match")
            return
        }
        if (username.isBlank() || password.isBlank()) { // confirmPassword is implicitly checked by the first condition if password is not blank
            errorMessage = "All fields are required."
            Log.d("SignupScreen", "A field is blank")
            return
        }

        isLoading = true
        errorMessage = ""
        Log.d("SignupScreen", "Attempting signup for username: $username")

        coroutineScope.launch {
            try {
                // In a real app, also check if username or email already exists
                // This now assumes userViewModel.createUser returns a Boolean
                val createdUser = userViewModel.createUser(username, password, UserRole.CUSTOMER)

                isLoading = false

                if (createdUser != null) { // Check if the user object is not null
                    Log.d(
                        "SignupScreen",
                        "Signup successful for $username. Calling onSignupSuccess()."
                    )
                    onSignupSuccess()
                } else {
                    errorMessage = "Signup failed. Username might already exist or server error."
                    Log.d("SignupScreen", "Signup failed for $username. createUser returned null.")
                } // This closing brace for 'else' was missing or misplaced

            } catch (e: Exception) { // Line 72
                isLoading = false // Ensure loading is stopped in case of an unexpected error
                errorMessage = "An unexpected error occurred: ${e.message}"
                Log.e("SignupScreen", "Exception during signup for $username: ${e.message}", e)
            }
        }
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(400.dp)
                .padding(32.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Create Account",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LazRed,
                        focusedLabelColor = LazRed,
                        cursorColor = LazRed
                    )
                )


                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LazRed,
                        focusedLabelColor = LazRed,
                        cursorColor = LazRed
                    )
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LazRed,
                        focusedLabelColor = LazRed,
                        cursorColor = LazRed
                    )
                )

                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Button(
                    onClick = { handleSignup() },
                    enabled = !isLoading && username.isNotBlank() && password.isNotBlank()  && confirmPassword.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("SIGN UP", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = { onNavigateToLogin() }) {
                    Text(
                        "Already have an account? Login",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}