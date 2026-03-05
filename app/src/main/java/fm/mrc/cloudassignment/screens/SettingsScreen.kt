package fm.mrc.cloudassignment.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import fm.mrc.cloudassignment.components.BottomNavBar
import fm.mrc.cloudassignment.navigation.Screen
import fm.mrc.cloudassignment.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import fm.mrc.cloudassignment.auth.AuthManager
import android.util.Log
import fm.mrc.cloudassignment.ui.theme.AppIcons

// Create a composable to hold a dark mode state that can be observed across the app
object ThemeManager {
    val isDarkMode = mutableStateOf(true)
}

// User data model to store and update profile information
object UserDataManager {
    var username = mutableStateOf("User")
    var email = mutableStateOf("")
    var phoneNumber = mutableStateOf("")
    var profilePhotoUri = mutableStateOf<Uri?>(null)
    var biometricEnabled = mutableStateOf(false)
    
    private const val TAG = "UserDataManager"
    
    // Initialize with data from AuthManager if available
    init {
        // Check if AuthManager has user data and update UserDataManager
        updateFromAuthManager()
    }
    
    // Update user data from AuthManager
    fun updateFromAuthManager() {
        AuthManager.currentUser.value?.let { userData ->
            Log.d(TAG, "Updating from AuthManager: ${userData.displayName}")
            if (userData.displayName.isNotBlank()) {
                username.value = userData.displayName
                Log.d(TAG, "Username updated to: ${username.value}")
            }
            email.value = userData.email
            
            // Only update profile photo if it's not null
            userData.photoUrl?.let { photoUri ->
                if (profilePhotoUri.value != photoUri) {
                    Log.d(TAG, "Updating profile photo from AuthManager: $photoUri")
                    profilePhotoUri.value = photoUri
                }
            }
        }
    }

    // Special method to refresh profile photo specifically
    fun refreshProfilePhoto() {
        Log.d(TAG, "Refreshing profile photo")
        AuthManager.auth?.currentUser?.let { user ->
            // Force refresh the photoUrl from Firebase Auth
            AuthManager.currentUser.value?.photoUrl?.let { photoUri ->
                Log.d(TAG, "Setting profile photo to: $photoUri")
                profilePhotoUri.value = photoUri
            }
        }
    }
}

// Class to check and manage biometric authentication
object BiometricHelper {
    fun canAuthenticate(context: FragmentActivity): Boolean {
        val biometricManager = androidx.biometric.BiometricManager.from(context)
        // Try with a combination of biometric options - more permissive
        val authTypes = androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or 
                        androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
                        
        val result = biometricManager.canAuthenticate(authTypes)
        
        // Log the result for debugging
        Toast.makeText(
            context,
            "Biometric status: ${getBiometricStatusString(result)}",
            Toast.LENGTH_LONG
        ).show()
        
        return result == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
    }
    
    private fun getBiometricStatusString(status: Int): String {
        return when (status) {
            androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS -> 
                "BIOMETRIC_SUCCESS"
            androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> 
                "BIOMETRIC_ERROR_NO_HARDWARE"
            androidx.biometric.BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> 
                "BIOMETRIC_ERROR_HW_UNAVAILABLE"
            androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> 
                "BIOMETRIC_ERROR_NONE_ENROLLED"
            androidx.biometric.BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> 
                "BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED"
            androidx.biometric.BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> 
                "BIOMETRIC_ERROR_UNSUPPORTED"
            androidx.biometric.BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> 
                "BIOMETRIC_STATUS_UNKNOWN"
            else -> "Unknown status: $status"
        }
    }
    
    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String,
        description: String,
        negativeButtonText: String,
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errorMessage: String) -> Unit,
        onFailed: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }
            
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errorCode, errString.toString())
            }
            
            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onFailed()
            }
        }
        
        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        
        try {
            // First try with device credential support
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setDescription(description)
                .setAllowedAuthenticators(
                    androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()
            
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            // Fallback to just biometric with negative button
            Toast.makeText(
                activity,
                "Falling back to biometric-only mode",
                Toast.LENGTH_SHORT
            ).show()
            
            try {
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setDescription(description)
                    .setNegativeButtonText(negativeButtonText)
                    .setAllowedAuthenticators(
                        androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
                    )
                    .build()
                
                biometricPrompt.authenticate(promptInfo)
            } catch (e: Exception) {
                onError(-1, "Failed to set up biometric authentication: ${e.message}")
            }
        }
    }
}

data class SettingsSection(
    val title: String,
    val items: List<SettingsItem>
)

sealed class SettingsItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String
) {
    class ClickableItem(
        icon: ImageVector,
        title: String,
        subtitle: String,
        val onClick: () -> Unit
    ) : SettingsItem(icon, title, subtitle)

    class SwitchItem(
        icon: ImageVector,
        title: String,
        subtitle: String,
        val initialValue: Boolean,
        val onCheckedChange: (Boolean) -> Unit
    ) : SettingsItem(icon, title, subtitle)
}

// Data class for settings item
data class SettingsItemData(
    val icon: ImageVector,
    val iconTint: Color = Color.Black,
    val title: String,
    val subtitle: String = "",
    val hasToggle: Boolean = false,
    val isToggled: Boolean = false,
    val onToggle: (Boolean) -> Unit = {},
    val onClick: () -> Unit = {}
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDarkMode = remember { ThemeManager.isDarkMode }
    
    // Update user data from AuthManager when SettingsScreen is displayed
    LaunchedEffect(Unit) {
        UserDataManager.updateFromAuthManager()
    }
    
    var notificationsEnabled by remember { mutableStateOf(true) }
    var darkModeEnabled by remember { mutableStateOf(ThemeManager.isDarkMode.value) }
    var dataBackupEnabled by remember { mutableStateOf(false) }
    var biometricEnabled by remember { mutableStateOf(UserDataManager.biometricEnabled.value) }
    val scrollState = rememberScrollState()
    val activity = context as? FragmentActivity
    
    // Dialog states
    var showProfileDialog by remember { mutableStateOf(false) }
    var showUsernameDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showBiometricEnableDialog by remember { mutableStateOf(false) }
    
    // Success message state
    var showSuccessMessage by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    
    // For profile photo
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Update the profile photo URI in UserDataManager
            UserDataManager.profilePhotoUri.value = it
            Log.d("ProfilePhoto", "Profile photo updated to: $it")
            
            // Show success message
            successMessage = "Profile photo updated successfully"
            showSuccessMessage = true
            
            // Update the user's profile photo in Firebase if available
            AuthManager.auth?.currentUser?.let { user ->
                // Create a profile update request with the photo URI
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setPhotoUri(it)
                    .build()
                
                // Update the user's profile
                user.updateProfile(profileUpdates)
                    .addOnSuccessListener {
                        Log.d("ProfilePhoto", "Firebase profile photo updated successfully")
                        // Update AuthManager's copy of the user data
                        AuthManager.currentUser.value = AuthManager.currentUser.value?.copy(photoUrl = uri)
                    }
                    .addOnFailureListener { e ->
                        Log.e("ProfilePhoto", "Failed to update Firebase profile photo: ${e.message}")
                    }
            }
        }
    }
    
    // For password change
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    
    // For username change
    var newUsername by remember { mutableStateOf(UserDataManager.username.value) }
    var usernameError by remember { mutableStateOf("") }

    // Create an animated color for background transition
    val backgroundColorTransition = animateColorAsState(
        targetValue = if (darkModeEnabled) 
            DarkBackground 
        else 
            Color(0xFFF0F2F5),
        animationSpec = tween(500),
        label = "backgroundColorTransition"
    )
    
    // Create an animated color for surface transition
    val surfaceColorTransition = animateColorAsState(
        targetValue = if (darkModeEnabled) 
            DarkSurface 
        else 
            Color.White,
        animationSpec = tween(500),
        label = "surfaceColorTransition"
    )
    
    // Create an animated color for text transition
    val textColorTransition = animateColorAsState(
        targetValue = if (darkModeEnabled) 
            TextWhite 
        else 
            Color(0xFF303030),
        animationSpec = tween(500),
        label = "textColorTransition"
    )
    
    // Create an animated color for subtext transition
    val subtextColorTransition = animateColorAsState(
        targetValue = if (darkModeEnabled) 
            TextGray 
        else 
            Color(0xFF707070),
        animationSpec = tween(500),
        label = "subtextColorTransition"
    )
    
    // Show a toast message when theme changes
    LaunchedEffect(darkModeEnabled) {
        if (darkModeEnabled != ThemeManager.isDarkMode.value) {
            ThemeManager.isDarkMode.value = darkModeEnabled
        }
    }
    
    // Auto-dismiss success message
    LaunchedEffect(showSuccessMessage) {
        if (showSuccessMessage) {
            delay(2000)
            showSuccessMessage = false
        }
    }
    
    // Function to handle biometric authentication
    fun handleBiometricToggle(newValue: Boolean) {
        if (activity == null) {
            Toast.makeText(context, "Cannot access biometric features", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (newValue) {
            // Check if biometric is available on this device
            if (BiometricHelper.canAuthenticate(activity)) {
                showBiometricEnableDialog = true
            } else {
                Toast.makeText(
                    context,
                    "Biometric authentication is not available on this device",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            // Disabling biometric authentication
            biometricEnabled = false
            UserDataManager.biometricEnabled.value = false
            successMessage = "Biometric authentication disabled"
            showSuccessMessage = true
        }
    }
    
    val sections = listOf(
        SettingsSection(
            title = "Account",
            items = listOf(
                SettingsItem.ClickableItem(
                    icon = AppIcons.Person,
                    title = "Profile",
                    subtitle = "Edit your profile information",
                    onClick = { showProfileDialog = true }
                ),
                SettingsItem.ClickableItem(
                    icon = AppIcons.Lock,
                    title = "Security",
                    subtitle = "Change password and security settings",
                    onClick = { showPasswordDialog = true }
                )
            )
        ),
        SettingsSection(
            title = "Preferences",
            items = listOf(
                SettingsItem.SwitchItem(
                    icon = AppIcons.NotificationsOutlined,
                    title = "Notifications",
                    subtitle = "Receive mood tracking reminders",
                    initialValue = notificationsEnabled,
                    onCheckedChange = { 
                        notificationsEnabled = it
                        if (it) {
                            successMessage = "Notifications enabled"
                        } else {
                            successMessage = "Notifications disabled"
                        }
                        showSuccessMessage = true
                    }
                ),
                SettingsItem.SwitchItem(
                    icon = AppIcons.DarkMode,
                    title = "Dark Mode",
                    subtitle = "Use dark theme",
                    initialValue = darkModeEnabled,
                    onCheckedChange = { 
                        darkModeEnabled = it
                        scope.launch {
                            // Add short delay for visual feedback before theme switch
                            delay(200)
                            ThemeManager.isDarkMode.value = it
                            successMessage = if (it) "Dark mode enabled" else "Light mode enabled"
                            showSuccessMessage = true
                        }
                    }
                ),
                SettingsItem.SwitchItem(
                    icon = AppIcons.Backup,
                    title = "Data Backup",
                    subtitle = "Automatically back up your data",
                    initialValue = dataBackupEnabled,
                    onCheckedChange = { 
                        dataBackupEnabled = it 
                        successMessage = if (it) "Data backup enabled" else "Data backup disabled"
                        showSuccessMessage = true
                    }
                ),
                SettingsItem.SwitchItem(
                    icon = AppIcons.Fingerprint,
                    title = "Biometric Authentication",
                    subtitle = "Use fingerprint to unlock app",
                    initialValue = biometricEnabled,
                    onCheckedChange = {
                        handleBiometricToggle(it)
                    }
                )
            )
        ),
        SettingsSection(
            title = "Data & Privacy",
            items = listOf(
                SettingsItem.ClickableItem(
                    icon = AppIcons.CloudDownload,
                    title = "Export Data",
                    subtitle = "Download all your mood data",
                    onClick = { showExportDialog = true }
                ),
                SettingsItem.ClickableItem(
                    icon = AppIcons.DeleteForever,
                    title = "Delete Account",
                    subtitle = "Permanently delete your account and data",
                    onClick = { showDeleteAccountDialog = true }
                )
            )
        ),
        SettingsSection(
            title = "About",
            items = listOf(
                SettingsItem.ClickableItem(
                    icon = AppIcons.Info,
                    title = "App Info",
                    subtitle = "Version 1.0.0",
                    onClick = { 
                        successMessage = "Neuro Cloud - Version 1.0.0"
                        showSuccessMessage = true 
                    }
                ),
                SettingsItem.ClickableItem(
                    icon = AppIcons.Policy,
                    title = "Privacy Policy",
                    subtitle = "Read our privacy policy",
                    onClick = { 
                        successMessage = "Privacy policy opened"
                        showSuccessMessage = true 
                    }
                ),
                SettingsItem.ClickableItem(
                    icon = AppIcons.HelpOutline,
                    title = "Help & Support",
                    subtitle = "Get assistance with using the app",
                    onClick = { 
                        successMessage = "Help & support opened"
                        showSuccessMessage = true 
                    }
                )
            )
        )
    )

    // Status bar color change animation
    var showThemeChangeEffect by remember { mutableStateOf(false) }
    LaunchedEffect(darkModeEnabled) {
        showThemeChangeEffect = true
        delay(500)
        showThemeChangeEffect = false
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = textColorTransition.value
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = surfaceColorTransition.value
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            AppIcons.Back,
                            contentDescription = "Go back",
                            tint = textColorTransition.value
                        )
                    }
                }
            )
        },
        bottomBar = { BottomNavBar(navController) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (darkModeEnabled)
                        Modifier.background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    backgroundColorTransition.value,
                                    backgroundColorTransition.value.copy(alpha = 0.95f)
                                )
                            )
                        )
                    else
                        Modifier.background(backgroundColorTransition.value)
                )
                .padding(padding)
        ) {
            // Theme change visual effect overlay
            AnimatedVisibility(
                visible = showThemeChangeEffect,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(300)),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (darkModeEnabled) 
                                DarkBackground.copy(alpha = 0.3f)
                            else 
                                Color.White.copy(alpha = 0.3f)
                        )
                )
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                sections.forEachIndexed { index, section ->
                    SettingsSectionCard(
                        section = section,
                        surfaceColor = surfaceColorTransition.value,
                        textColor = textColorTransition.value,
                        subtextColor = subtextColorTransition.value,
                        dividerColor = if (darkModeEnabled) 
                            DarkBackground.copy(alpha = 0.5f) 
                        else 
                            Color.LightGray.copy(alpha = 0.5f)
                    )
                    
                    if (index < sections.size - 1) {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Logout button
                Button(
                    onClick = { 
                        // First reset the local states immediately
                        // This ensures the UI reacts immediately
                        AuthManager.currentUser.value = null
                        AuthManager.isLoggedIn.value = false
                        
                        // Show a toast to confirm logout
                        Toast.makeText(context, "Signing out...", Toast.LENGTH_SHORT).show()
                        
                        // Then call AuthManager to sign out properly
                        try {
                            AuthManager.signOut(context)
                        } catch (e: Exception) {
                            Log.e("SettingsScreen", "Error signing out: ${e.message}")
                        }
                        
                        // Navigate to login screen immediately
                        navController.navigate(Screen.Login.route) {
                            // Clear the back stack so the user can't navigate back after logout
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(elevation = 4.dp, shape = RoundedCornerShape(8.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE74C3C)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        AppIcons.ExitToApp,
                        contentDescription = "Logout",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        "Logout",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Success message banner
            AnimatedVisibility(
                visible = showSuccessMessage,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .shadow(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = PrimaryBlue
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            AppIcons.Check,
                            contentDescription = "Success",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            successMessage,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            // Biometric Enable Confirmation Dialog
            if (showBiometricEnableDialog) {
                AlertDialog(
                    onDismissRequest = { showBiometricEnableDialog = false },
                    title = { Text("Enable Biometric Authentication") },
                    text = {
                        Text(
                            "To enable biometric authentication, you need to verify your identity. This will allow you to unlock the app using your fingerprint or face recognition."
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showBiometricEnableDialog = false
                                activity?.let {
                                    BiometricHelper.showBiometricPrompt(
                                        activity = it,
                                        title = "Verify your identity",
                                        description = "Confirm your identity to enable biometric authentication",
                                        negativeButtonText = "Cancel",
                                        onSuccess = {
                                            biometricEnabled = true
                                            UserDataManager.biometricEnabled.value = true
                                            successMessage = "Biometric authentication enabled"
                                            showSuccessMessage = true
                                        },
                                        onError = { _, errorMessage ->
                                            Toast.makeText(context, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
                                        },
                                        onFailed = {
                                            Toast.makeText(context, "Authentication failed", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        ) {
                            Text("Proceed")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { 
                                showBiometricEnableDialog = false 
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
            
            // Profile Management Dialog
            if (showProfileDialog) {
                AlertDialog(
                    onDismissRequest = { 
                        showProfileDialog = false
                        // Refresh photos when dialog is closed to ensure consistency
                        UserDataManager.refreshProfilePhoto()
                    },
                    title = { Text("Your Profile") },
                    text = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Profile photo
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryBlue.copy(alpha = 0.1f))
                                    .clickable {
                                        imagePickerLauncher.launch("image/*")
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                val profilePhoto = UserDataManager.profilePhotoUri.value
                                if (profilePhoto != null) {
                                    Image(
                                        painter = rememberAsyncImagePainter(profilePhoto),
                                        contentDescription = "Profile Photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = AppIcons.Person,
                                        contentDescription = "Add Profile Photo",
                                        modifier = Modifier.size(48.dp),
                                        tint = PrimaryBlue
                                    )
                                }
                                
                                // Camera icon overlay for changing photo
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryBlue)
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = AppIcons.CameraAlt,
                                        contentDescription = "Change Photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Tap to change photo",
                                style = MaterialTheme.typography.bodySmall,
                                color = textColorTransition.value.copy(alpha = 0.7f)
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Display current profile information
                            ProfileInfoRow(
                                icon = AppIcons.Person,
                                label = "Username",
                                value = UserDataManager.username.value,
                                canEdit = true,
                                onEdit = { showUsernameDialog = true },
                                textColor = textColorTransition.value
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            ProfileInfoRow(
                                icon = AppIcons.Email,
                                label = "Email",
                                value = UserDataManager.email.value,
                                canEdit = false,
                                onEdit = { },
                                textColor = textColorTransition.value
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            ProfileInfoRow(
                                icon = AppIcons.Phone,
                                label = "Phone",
                                value = UserDataManager.phoneNumber.value,
                                canEdit = false,
                                onEdit = { },
                                textColor = textColorTransition.value
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = { showProfileDialog = false }) {
                            Text("Close")
                        }
                    }
                )
            }
            
            // Username Change Dialog
            if (showUsernameDialog) {
                AlertDialog(
                    onDismissRequest = { showUsernameDialog = false },
                    title = { Text("Change Username") },
                    text = {
                        Column {
                            Text(
                                "Enter a new username",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            
                            OutlinedTextField(
                                value = newUsername,
                                onValueChange = { 
                                    newUsername = it
                                    usernameError = ""
                                },
                                label = { Text("Username") },
                                singleLine = true,
                                isError = usernameError.isNotEmpty(),
                                supportingText = {
                                    if (usernameError.isNotEmpty()) {
                                        Text(usernameError)
                                    }
                                }
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (newUsername.length < 3) {
                                    usernameError = "Username must be at least 3 characters"
                                } else {
                                    // Update local state immediately
                                    UserDataManager.username.value = newUsername
                                    
                                    // Always close dialogs
                                    showUsernameDialog = false
                                    showProfileDialog = false
                                    
                                    // Show loading message
                                    Toast.makeText(context, "Updating username...", Toast.LENGTH_SHORT).show()
                                    
                                    try {
                                        // Force initialize AuthManager if needed
                                        if (AuthManager.auth == null) {
                                            // Auth isn't initialized - update locally only
                                            Log.e("UsernameUpdate", "Auth not initialized")
                                            Toast.makeText(context, "Username updated locally only", Toast.LENGTH_SHORT).show()
                                            successMessage = "Username updated locally"
                                            showSuccessMessage = true
                                        } else {
                                            val firebaseUser = AuthManager.auth?.currentUser
                                            if (firebaseUser == null) {
                                                // Not signed in - update locally only
                                                Log.e("UsernameUpdate", "Not signed in")
                                                Toast.makeText(context, "Username updated locally only", Toast.LENGTH_SHORT).show()
                                                successMessage = "Username updated locally"
                                                showSuccessMessage = true
                                            } else {
                                                // Create profile update request
                                                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                                    .setDisplayName(newUsername)
                                                    .build()
                                                
                                                // Update Firebase Auth
                                                firebaseUser.updateProfile(profileUpdates)
                                                    .addOnSuccessListener {
                                                        Log.d("UsernameUpdate", "Firebase Auth updated successfully")
                                                        
                                                        // Update AuthManager state
                                                        AuthManager.currentUser.value = AuthManager.currentUser.value?.copy(
                                                            displayName = newUsername
                                                        )
                                                        
                                                        // Show success message
                                                        Toast.makeText(context, "Username updated successfully", Toast.LENGTH_SHORT).show()
                                                        successMessage = "Username updated successfully"
                                                        showSuccessMessage = true
                                                        
                                                        // Update Firestore if available
                                                        if (AuthManager.firestore != null) {
                                                            AuthManager.firestore?.collection("users")
                                                                ?.document(firebaseUser.uid)
                                                                ?.update("displayName", newUsername)
                                                        }
                                                    }
                                                    .addOnFailureListener { e ->
                                                        Log.e("UsernameUpdate", "Error updating profile: ${e.message}")
                                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                                        successMessage = "Username updated locally only"
                                                        showSuccessMessage = true
                                                    }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        // Handle any unexpected errors
                                        Log.e("UsernameUpdate", "Exception: ${e.message}")
                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                        successMessage = "Username updated locally only"
                                        showSuccessMessage = true
                                    }
                                }
                            }
                        ) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showUsernameDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
            
            // Password Change Dialog
            if (showPasswordDialog) {
                var showCurrentPassword by remember { mutableStateOf(false) }
                var showNewPassword by remember { mutableStateOf(false) }
                var showConfirmPassword by remember { mutableStateOf(false) }
                
                AlertDialog(
                    onDismissRequest = { showPasswordDialog = false },
                    title = { Text("Change Password") },
                    text = {
                        Column {
                            Text(
                                "Please enter your current password and a new password",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            
                            OutlinedTextField(
                                value = currentPassword,
                                onValueChange = { 
                                    currentPassword = it
                                    passwordError = ""
                                },
                                label = { Text("Current Password") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                visualTransformation = if (showCurrentPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showCurrentPassword = !showCurrentPassword }) {
                                        Icon(
                                            if (showCurrentPassword) AppIcons.VisibilityOff else AppIcons.Visibility,
                                            contentDescription = "Toggle password visibility"
                                        )
                                    }
                                }
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = newPassword,
                                onValueChange = { 
                                    newPassword = it
                                    passwordError = ""
                                },
                                label = { Text("New Password") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showNewPassword = !showNewPassword }) {
                                        Icon(
                                            if (showNewPassword) AppIcons.VisibilityOff else AppIcons.Visibility,
                                            contentDescription = "Toggle password visibility"
                                        )
                                    }
                                }
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { 
                                    confirmPassword = it
                                    passwordError = ""
                                },
                                label = { Text("Confirm New Password") },
                                singleLine = true,
                                isError = passwordError.isNotEmpty(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                                        Icon(
                                            if (showConfirmPassword) AppIcons.VisibilityOff else AppIcons.Visibility,
                                            contentDescription = "Toggle password visibility"
                                        )
                                    }
                                },
                                supportingText = {
                                    if (passwordError.isNotEmpty()) {
                                        Text(passwordError)
                                    }
                                }
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (currentPassword.isEmpty()) {
                                    passwordError = "Current password is required"
                                } else if (newPassword.length < 6) {
                                    passwordError = "Password must be at least 6 characters"
                                } else if (newPassword != confirmPassword) {
                                    passwordError = "Passwords do not match"
                                } else {
                                    // In a real app, you would validate the current password
                                    // and update the password in a secure way
                                    showPasswordDialog = false
                                    successMessage = "Password updated successfully"
                                    showSuccessMessage = true
                                    
                                    // Reset password fields
                                    currentPassword = ""
                                    newPassword = ""
                                    confirmPassword = ""
                                }
                            }
                        ) {
                            Text("Update Password")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { 
                                showPasswordDialog = false
                                // Reset password fields
                                currentPassword = ""
                                newPassword = ""
                                confirmPassword = ""
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
            
            // Delete Account Confirmation Dialog
            if (showDeleteAccountDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteAccountDialog = false },
                    title = { Text("Delete Account") },
                    text = {
                        Text(
                            "Are you sure you want to delete your account? This action cannot be undone and all your data will be permanently lost.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                scope.launch {
                                    val success = AuthManager.deleteAccount(context)
                                    if (success) {
                                        Toast.makeText(context, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                                        // Navigate to the new user screen
                                        navController.navigate(Screen.NewUser.route) {
                                            // Clear the back stack so user can't go back
                                            popUpTo(0) { inclusive = true }
                                        }
                                    } else {
                                        Toast.makeText(context, "Failed to delete account", Toast.LENGTH_SHORT).show()
                                    }
                                    showDeleteAccountDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE74C3C)
                            )
                        ) {
                            Text("Delete Account")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteAccountDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
            
            // Export Data Dialog
            if (showExportDialog) {
                AlertDialog(
                    onDismissRequest = { showExportDialog = false },
                    title = { Text("Export Data") },
                    text = {
                        Text(
                            "Choose a format to export your mood tracking data.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showExportDialog = false
                                successMessage = "Data export started"
                                showSuccessMessage = true
                            }
                        ) {
                            Text("Export")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showExportDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ProfileDialog(
    onDismiss: () -> Unit,
    onChangeUsername: () -> Unit,
    textColor: Color
) {
    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            UserDataManager.profilePhotoUri.value = it
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Profile Settings") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Profile photo
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue.copy(alpha = 0.1f))
                        .clickable {
                            imagePickerLauncher.launch("image/*")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val profilePhoto = UserDataManager.profilePhotoUri.value
                    if (profilePhoto != null) {
                        Image(
                            painter = rememberAsyncImagePainter(profilePhoto),
                            contentDescription = "Profile Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = AppIcons.Person,
                            contentDescription = "Add Profile Photo",
                            modifier = Modifier.size(48.dp),
                            tint = PrimaryBlue
                        )
                    }
                    
                    // Camera icon overlay for changing photo
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.CameraAlt,
                            contentDescription = "Change Photo",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Tap to change photo",
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Display current profile information
                ProfileInfoRow(
                    icon = AppIcons.Person,
                    label = "Username",
                    value = UserDataManager.username.value,
                    canEdit = true,
                    onEdit = onChangeUsername,
                    textColor = textColor
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                ProfileInfoRow(
                    icon = AppIcons.Email,
                    label = "Email",
                    value = UserDataManager.email.value,
                    canEdit = false,
                    onEdit = { },
                    textColor = textColor
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                ProfileInfoRow(
                    icon = AppIcons.Phone,
                    label = "Phone",
                    value = UserDataManager.phoneNumber.value,
                    canEdit = false,
                    onEdit = { },
                    textColor = textColor
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun ProfileInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    canEdit: Boolean,
    onEdit: () -> Unit,
    textColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = canEdit) { onEdit() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.padding(end = 12.dp)
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
        }
        
        if (canEdit) {
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = AppIcons.Edit,
                    contentDescription = "Edit"
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    section: SettingsSection,
    surfaceColor: Color,
    textColor: Color,
    subtextColor: Color,
    dividerColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = surfaceColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleMedium,
                color = textColor,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            section.items.forEachIndexed { index, item ->
                when (item) {
                    is SettingsItem.ClickableItem -> {
                        ClickableSettingItem(
                            item = item,
                            textColor = textColor,
                            subtextColor = subtextColor
                        )
                    }
                    is SettingsItem.SwitchItem -> {
                        var checked by remember { mutableStateOf(item.initialValue) }
                        SwitchSettingItem(
                            item = item,
                            checked = checked,
                            textColor = textColor,
                            subtextColor = subtextColor,
                            onCheckedChange = { newValue ->
                                checked = newValue
                                item.onCheckedChange(newValue)
                            }
                        )
                    }
                }
                
                if (index < section.items.size - 1) {
                    Divider(
                        color = dividerColor,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ClickableSettingItem(
    item: SettingsItem.ClickableItem,
    textColor: Color,
    subtextColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { item.onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(PrimaryBlue.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = subtextColor
            )
        }
        
        Icon(
            imageVector = AppIcons.ChevronRight,
            contentDescription = "Navigate",
            tint = subtextColor,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SwitchSettingItem(
    item: SettingsItem.SwitchItem,
    checked: Boolean,
    textColor: Color,
    subtextColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(PrimaryBlue.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = subtextColor
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PrimaryBlue,
                checkedTrackColor = PrimaryBlue.copy(alpha = 0.5f),
                uncheckedThumbColor = subtextColor,
                uncheckedTrackColor = if (ThemeManager.isDarkMode.value) DarkBackground else Color.LightGray
            )
        )
    }
}

@Composable
fun SettingsItem(
    item: SettingsItemData,
    textColor: Color = Color.Black
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { item.onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            tint = item.iconTint,
            modifier = Modifier
                .size(24.dp)
                .padding(end = 8.dp)
        )
        
        // Content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
            
            if (item.subtitle.isNotBlank()) {
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.7f)
                )
            }
        }
        
        // Toggle or Arrow
        if (item.hasToggle) {
            Switch(
                checked = item.isToggled,
                onCheckedChange = { item.onToggle(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PrimaryBlue,
                    checkedTrackColor = PrimaryBlue.copy(alpha = 0.5f)
                )
            )
        } else {
            Icon(
                imageVector = AppIcons.KeyboardArrowRight,
                contentDescription = "More",
                tint = textColor.copy(alpha = 0.5f)
            )
        }
    }
} 