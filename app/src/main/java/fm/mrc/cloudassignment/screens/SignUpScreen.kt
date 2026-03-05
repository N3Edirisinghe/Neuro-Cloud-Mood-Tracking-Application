package fm.mrc.cloudassignment.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import fm.mrc.cloudassignment.R
import fm.mrc.cloudassignment.auth.AuthManager
import fm.mrc.cloudassignment.auth.UserData
import fm.mrc.cloudassignment.navigation.Screen
import fm.mrc.cloudassignment.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import fm.mrc.cloudassignment.auth.GoogleSignInHelper
import fm.mrc.cloudassignment.ui.theme.AppIcons
import fm.mrc.cloudassignment.auth.UserAuthUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(navController: NavController) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Check if user is already logged in
    LaunchedEffect(AuthManager.isLoggedIn.value) {
        if (AuthManager.isLoggedIn.value) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.SignUp.route) { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(0.1f))

        Text(
            text = "Neuro Cloud",
            style = MaterialTheme.typography.headlineLarge,
            color = TextWhite,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        Text(
            text = "Sign Up",
            style = MaterialTheme.typography.headlineSmall,
            color = TextWhite,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Username field
        OutlinedTextField(
            value = username,
            onValueChange = { 
                username = it 
                errorMessage = ""
            },
            label = { Text("Username", color = TextGray) },
            leadingIcon = { Icon(AppIcons.Person, contentDescription = "Username", tint = PrimaryBlue) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = PrimaryBlue,
                cursorColor = PrimaryBlue,
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        // Email field
        OutlinedTextField(
            value = email,
            onValueChange = { 
                email = it 
                errorMessage = ""
            },
            label = { Text("Email", color = TextGray) },
            leadingIcon = { Icon(AppIcons.Email, contentDescription = "Email", tint = PrimaryBlue) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = PrimaryBlue,
                cursorColor = PrimaryBlue,
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = { 
                password = it 
                errorMessage = ""
            },
            label = { Text("Password", color = TextGray) },
            leadingIcon = { Icon(AppIcons.Lock, contentDescription = "Password", tint = PrimaryBlue) },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        imageVector = if (showPassword) AppIcons.VisibilityOff else AppIcons.Visibility,
                        contentDescription = if (showPassword) "Hide password" else "Show password",
                        tint = PrimaryBlue
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = PrimaryBlue.copy(alpha = 0.5f),
                cursorColor = PrimaryBlue,
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        // Confirm Password field
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { 
                confirmPassword = it 
                errorMessage = ""
            },
            label = { Text("Confirm Password", color = TextGray) },
            leadingIcon = { Icon(AppIcons.Lock, contentDescription = "Confirm Password", tint = PrimaryBlue) },
            visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                    Icon(
                        imageVector = if (showConfirmPassword) AppIcons.VisibilityOff else AppIcons.Visibility,
                        contentDescription = if (showConfirmPassword) "Hide password" else "Show password",
                        tint = PrimaryBlue
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = PrimaryBlue.copy(alpha = 0.5f),
                cursorColor = PrimaryBlue,
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        )

        // Error message
        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Sign Up button
        Button(
            onClick = {
                coroutineScope.launch {
                    isLoading = true
                    errorMessage = ""
                    
                    // Validate inputs
                    if (username.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                        errorMessage = "Please fill in all fields"
                        isLoading = false
                        return@launch
                    }
                    
                    if (password != confirmPassword) {
                        errorMessage = "Passwords do not match"
                        isLoading = false
                        return@launch
                    }
                    
                    if (password.length < 6) {
                        errorMessage = "Password must be at least 6 characters"
                        isLoading = false
                        return@launch
                    }
                    
                    try {
                        // Check if email is valid
                        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                            errorMessage = "Please enter a valid email address"
                            isLoading = false
                            return@launch
                        }
                        
                        // Check if Firebase is initialized
                        if (AuthManager.auth == null || AuthManager.firestore == null) {
                            Log.e("SignUpScreen", "Firebase not initialized")
                            // Create a temporary user for testing if Firebase is not initialized
                            val tempUser = UserData(
                                uid = "temp-${System.currentTimeMillis()}",
                                email = email,
                                displayName = username,
                                photoUrl = null
                            )
                            AuthManager.currentUser.value = tempUser
                            AuthManager.isLoggedIn.value = true
                            
                            Toast.makeText(context, "Dev mode: Account created", Toast.LENGTH_SHORT).show()
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.SignUp.route) { inclusive = true }
                            }
                            isLoading = false
                            return@launch
                        }
                        
                        Log.d("SignUpScreen", "Attempting to sign up user: $email")
                        // Attempt to sign up
                        val success = AuthManager.signUpWithEmail(email, password, username)
                        if (success) {
                            Log.d("SignUpScreen", "Sign up successful")
                            Toast.makeText(context, "Account created successfully", Toast.LENGTH_SHORT).show()
                            // Navigate to home screen
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.SignUp.route) { inclusive = true }
                            }
                        } else {
                            Log.e("SignUpScreen", "Sign up failed: Unknown reason")
                            errorMessage = "Failed to create account. Please try again."
                        }
                    } catch (e: Exception) {
                        Log.e("SignUpScreen", "Sign up error: ${e.message}", e)
                        // Handle specific Firebase errors
                        val errorMsg = when {
                            e.message?.contains("email already in use", ignoreCase = true) == true -> 
                                "Email already in use. Please use a different email or sign in."
                            e.message?.contains("network", ignoreCase = true) == true -> 
                                "Network error. Please check your internet connection."
                            e.message?.contains("invalid email", ignoreCase = true) == true -> 
                                "Invalid email format. Please use a valid email."
                            e.message?.contains("weak password", ignoreCase = true) == true -> 
                                "Password is too weak. Please use a stronger password."
                            else -> "Error: ${e.message}"
                        }
                        errorMessage = errorMsg
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(28.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Sign Up")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        // Divider with "OR" text
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Divider(
                modifier = Modifier.weight(1f),
                color = TextGray.copy(alpha = 0.5f)
            )
            Text(
                text = " OR ",
                color = TextGray,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Divider(
                modifier = Modifier.weight(1f),
                color = TextGray.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Google Sign-In button
        Button(
            onClick = { 
                try {
                    // Get the activity context
                    val activity = context as? androidx.activity.ComponentActivity
                    if (activity == null) {
                        Toast.makeText(context, "Could not access activity", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    // Set up the callback for handling the sign-in result
                    (activity as? fm.mrc.cloudassignment.MainActivity)?.setGoogleSignInCallback { data ->
                        // Handle the sign-in result
                        GoogleSignInHelper.handleActivityResult(
                            requestCode = 9001, // RC_SIGN_IN
                            resultCode = Activity.RESULT_OK,
                            data = data,
                            context = context,
                            scope = coroutineScope,
                            onSignInSuccess = { userData ->
                                // Update auth state
                                AuthManager.currentUser.value = userData
                                AuthManager.isLoggedIn.value = true
                                
                                // Navigate to Home
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.SignUp.route) { inclusive = true }
                                }
                            }
                        )
                    }
                    
                    // Start the Google Sign-In process
                    GoogleSignInHelper.signInWithGoogle(
                        context = context,
                        scope = coroutineScope,
                        onSignInSuccess = { userData ->
                            // Update auth state
                            AuthManager.currentUser.value = userData
                            AuthManager.isLoggedIn.value = true
                            
                            // Navigate to Home
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.SignUp.route) { inclusive = true }
                            }
                        }
                    )
                } catch (e: Exception) {
                    Log.e("SignUpScreen", "Google Sign-In error: ${e.message}")
                    Toast.makeText(
                        context,
                        "Error starting Google Sign-In: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_google),
                    contentDescription = "Google",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Sign up with Google",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.1f))

        TextButton(
            onClick = { navController.navigate(Screen.Login.route) },
            colors = ButtonDefaults.textButtonColors(
                contentColor = PrimaryBlue
            )
        ) {
            Text(
                "Already have an account? Log In",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.weight(0.2f))
    }
}

private fun validateInputs(username: String, email: String, password: String, confirmPassword: String): Boolean {
    return username.length >= 3 && 
           email.contains("@") && 
           password.length >= 6 && 
           password == confirmPassword
} 