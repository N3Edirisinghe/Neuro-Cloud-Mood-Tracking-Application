package fm.mrc.cloudassignment.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * A manager class for handling authentication operations
 */
object AuthManager {
    var auth: FirebaseAuth? = null
        private set
    var firestore: FirebaseFirestore? = null
        private set
    private var googleSignInClient: GoogleSignInClient? = null
    private var isInitialized = false
    
    // Current user state
    val currentUser = mutableStateOf<UserData?>(null)
    val isLoggedIn = mutableStateOf(false)
    
    // Result launcher for Google sign-in
    private var signInLauncher: ActivityResultLauncher<Intent>? = null
    
    private const val TAG = "AuthManager"
    
    /**
     * Initialize the AuthManager with required components
     */
    fun initialize(activity: ComponentActivity, webClientId: String) {
        try {
            // Initialize Firebase components
            auth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()
            
            // Configure Google Sign-In
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build()
            
            googleSignInClient = GoogleSignIn.getClient(activity, gso)
            
            // Setup sign-in launcher
            signInLauncher = activity.registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                try {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    handleGoogleSignInResult(task, activity)
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing sign in result: ${e.message}")
                    Toast.makeText(activity, "Sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            
            // Check if user is already signed in
            isInitialized = true
            checkCurrentUser()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AuthManager: ${e.message}")
            isInitialized = false
        }
    }
    
    /**
     * Check if the user is already signed in
     */
    private fun checkCurrentUser() {
        if (!isInitialized) return
        
        try {
            val firebaseUser = auth?.currentUser
            if (firebaseUser != null) {
                loadUserData(firebaseUser)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking current user: ${e.message}")
        }
    }
    
    /**
     * Start Google sign-in flow
     */
    fun signInWithGoogle() {
        if (!isInitialized) {
            Log.e(TAG, "Cannot sign in: AuthManager not initialized")
            return
        }
        
        try {
            val signInIntent = googleSignInClient?.signInIntent ?: return
            signInLauncher?.launch(signInIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting Google sign-in: ${e.message}")
        }
    }
    
    /**
     * Handle the result from Google sign-in
     */
    private fun handleGoogleSignInResult(task: Task<GoogleSignInAccount>, context: Context) {
        try {
            val account = task.getResult()
            if (account != null) {
                firebaseAuthWithGoogle(account, context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google sign-in failed: ${e.message}")
            Toast.makeText(context, "Google sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Authenticate with Firebase using Google credentials
     */
    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount, context: Context) {
        if (auth == null) {
            Log.e(TAG, "Firebase Auth not initialized")
            return
        }
        
        try {
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            
            auth?.signInWithCredential(credential)
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val firebaseUser = auth?.currentUser
                        if (firebaseUser != null) {
                            // Create or update user in Firestore
                            saveUserToFirestore(firebaseUser, account.email ?: "")
                            loadUserData(firebaseUser)
                            Toast.makeText(context, "Signed in as ${account.displayName}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.e(TAG, "Authentication failed: ${task.exception?.message}")
                        Toast.makeText(context, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error during Firebase authentication: ${e.message}")
            Toast.makeText(context, "Authentication error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Save user data to Firestore
     */
    private fun saveUserToFirestore(firebaseUser: FirebaseUser, email: String) {
        if (firestore == null) {
            Log.e(TAG, "Cannot save user: Firestore not initialized")
            return
        }
        
        try {
            val userData = mapOf(
                "email" to email,
                "displayName" to (firebaseUser.displayName ?: ""),
                "photoUrl" to (firebaseUser.photoUrl?.toString() ?: ""),
                "createdAt" to System.currentTimeMillis()
            )
            
            firestore?.collection("users")
                ?.document(firebaseUser.uid)
                ?.set(userData)
                ?.addOnSuccessListener {
                    Log.d(TAG, "User data saved to Firestore successfully")
                }
                ?.addOnFailureListener { e ->
                    Log.e(TAG, "Error saving user data to Firestore: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user data to Firestore: ${e.message}")
        }
    }
    
    /**
     * Load user data from Firebase and update the state
     */
    private fun loadUserData(firebaseUser: FirebaseUser) {
        try {
            val userData = UserData(
                uid = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                displayName = firebaseUser.displayName ?: "",
                photoUrl = firebaseUser.photoUrl
            )
            
            currentUser.value = userData
            isLoggedIn.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Error loading user data: ${e.message}")
        }
    }
    
    /**
     * Sign out the current user
     */
    fun signOut(context: Context) {
        // Always reset the state variables first
        currentUser.value = null
        isLoggedIn.value = false
        
        if (!isInitialized) {
            Log.e(TAG, "Cannot sign out: AuthManager not initialized")
            Toast.makeText(context, "Signed out (local only)", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            // Firebase sign out
            auth?.signOut()
            
            // Google sign out
            try {
                googleSignInClient?.signOut()?.addOnCompleteListener {
                    Log.d(TAG, "Google sign out complete")
                    Toast.makeText(context, "Signed out successfully", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during Google sign out: ${e.message}")
                // Still consider the sign-out successful from the user's perspective
                Toast.makeText(context, "Signed out (local only)", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error signing out: ${e.message}")
            // Even if Firebase sign-out fails, we've already reset the local state
            Toast.makeText(context, "Signed out (local only)", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Sign up user with email and password
     */
    suspend fun signUpWithEmail(email: String, password: String, displayName: String): Boolean {
        if (!isInitialized || auth == null) {
            Log.e(TAG, "Cannot sign up: AuthManager not initialized")
            return false
        }
        
        try {
            Log.d(TAG, "Attempting to create user with email: $email")
            
            // Create user in Firebase Auth
            val authResult = auth?.createUserWithEmailAndPassword(email, password)?.await()
            val firebaseUser = authResult?.user
            
            if (firebaseUser != null) {
                Log.d(TAG, "Firebase user created with UID: ${firebaseUser.uid}")
                
                // Update profile with display name
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
                
                try {
                    firebaseUser.updateProfile(profileUpdates).await()
                    Log.d(TAG, "Profile updated with display name: $displayName")
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating profile: ${e.message}")
                    // Continue despite profile update error
                }
                
                // Update local state immediately
                currentUser.value = UserData(
                    uid = firebaseUser.uid,
                    email = email,
                    displayName = displayName,
                    photoUrl = firebaseUser.photoUrl
                )
                isLoggedIn.value = true
                Log.d(TAG, "Local auth state updated: user logged in")
                
                // Save user to Firestore - do this after setting local state
                try {
                    val userData = mapOf(
                        "email" to email,
                        "displayName" to displayName,
                        "photoUrl" to (firebaseUser.photoUrl?.toString() ?: ""),
                        "createdAt" to System.currentTimeMillis()
                    )
                    
                    firestore?.collection("users")
                        ?.document(firebaseUser.uid)
                        ?.set(userData)
                        ?.await()
                    
                    Log.d(TAG, "User data saved to Firestore successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving user data to Firestore: ${e.message}")
                    // Continue despite Firestore error
                }
                
                Log.d(TAG, "User signed up successfully: $email")
                return true
            } else {
                Log.e(TAG, "Sign up failed: No user returned")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error signing up: ${e.message}", e)
            return false
        }
    }
    
    /**
     * Sign in user with email and password
     */
    suspend fun signInWithEmail(email: String, password: String): Boolean {
        if (!isInitialized || auth == null) {
            Log.e(TAG, "Cannot sign in: AuthManager not initialized")
            return false
        }
        
        Log.d(TAG, "Attempting to sign in with email: $email")
        
        try {
            // Attempt to sign in with Firebase Auth
            val authResult = auth?.signInWithEmailAndPassword(email, password)?.await()
            val firebaseUser = authResult?.user
            
            if (firebaseUser != null) {
                Log.d(TAG, "Sign in successful for user: ${firebaseUser.uid}")
                
                // Update local state with user data
                loadUserData(firebaseUser)
                
                // Update login timestamp in Firestore
                try {
                    firestore?.collection("users")
                        ?.document(firebaseUser.uid)
                        ?.update("lastLogin", System.currentTimeMillis())
                        ?.await()
                    Log.d(TAG, "Last login time updated in Firestore")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update last login time: ${e.message}")
                    // Continue despite this error
                }
                
                return true
            } else {
                Log.e(TAG, "Sign in failed: No user returned")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error signing in: ${e.message}", e)
            // Let the calling function handle the error
            throw e
        }
    }
    
    /**
     * Update user's display name
     */
    fun updateUserDisplayName(newDisplayName: String) {
        if (!isInitialized || auth == null) {
            Log.e(TAG, "Cannot update display name: AuthManager not initialized")
            return
        }
        
        try {
            // Update local state immediately for better UX
            Log.d(TAG, "Updating display name from: ${currentUser.value?.displayName} to: $newDisplayName")
            currentUser.value = currentUser.value?.copy(displayName = newDisplayName)
            
            val firebaseUser = auth?.currentUser
            if (firebaseUser != null) {
                // First update in Firestore as it's more reliable
                if (firestore != null) {
                    firestore?.collection("users")
                        ?.document(firebaseUser.uid)
                        ?.update("displayName", newDisplayName)
                        ?.addOnSuccessListener {
                            Log.d(TAG, "Firestore displayName updated successfully to: $newDisplayName")
                            
                            // Now update Firebase Auth profile after Firestore success
                            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                .setDisplayName(newDisplayName)
                                .build()
                            
                            firebaseUser.updateProfile(profileUpdates)
                                .addOnSuccessListener {
                                    Log.d(TAG, "Firebase Auth displayName updated successfully to: $newDisplayName")
                                    
                                    // Force reload user
                                    firebaseUser.reload().addOnSuccessListener {
                                        // Verify the update actually happened
                                        if (firebaseUser.displayName == newDisplayName) {
                                            Log.d(TAG, "Verified display name updated to: ${firebaseUser.displayName}")
                                        } else {
                                            Log.e(TAG, "Display name verification failed. Current: ${firebaseUser.displayName}, Expected: $newDisplayName")
                                        }
                                        
                                        // Reload user data to ensure consistency
                                        loadUserData(firebaseUser)
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Error updating display name in Firebase Auth: ${e.message}")
                                }
                        }
                        ?.addOnFailureListener { e ->
                            Log.e(TAG, "Error updating display name in Firestore: ${e.message}")
                        }
                } else {
                    // If Firestore is not available, update Auth directly
                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(newDisplayName)
                        .build()
                    
                    firebaseUser.updateProfile(profileUpdates)
                        .addOnSuccessListener {
                            Log.d(TAG, "Firebase Auth displayName updated successfully to: $newDisplayName")
                            loadUserData(firebaseUser)
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error updating display name: ${e.message}")
                        }
                }
            } else {
                Log.e(TAG, "Cannot update profile: Current user is null")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating display name: ${e.message}")
        }
    }
    
    /**
     * Delete the current user's account
     */
    suspend fun deleteAccount(context: Context): Boolean {
        if (!isInitialized || auth == null || firestore == null) {
            Log.e(TAG, "Cannot delete account: AuthManager not initialized")
            return false
        }
        
        try {
            val firebaseUser = auth?.currentUser
            if (firebaseUser == null) {
                Log.e(TAG, "Cannot delete account: No user is signed in")
                return false
            }
            
            // First, get the user ID to use in Firestore
            val userId = firebaseUser.uid
            
            try {
                // Delete user data from Firestore first
                firestore?.collection("users")
                    ?.document(userId)
                    ?.delete()
                    ?.await()
                
                Log.d(TAG, "User data deleted from Firestore")
                
                // Then delete the Firebase Auth user
                firebaseUser.delete().await()
                Log.d(TAG, "Firebase Auth account deleted")
                
                // Reset the auth state
                currentUser.value = null
                isLoggedIn.value = false
                
                // Sign out from Google if needed
                googleSignInClient?.signOut()?.await()
                
                return true
            } catch (e: Exception) {
                Log.e(TAG, "Error during account deletion: ${e.message}", e)
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting account: ${e.message}", e)
            return false
        }
    }
}

/**
 * Data class representing user data
 */
data class UserData(
    val uid: String,
    val email: String,
    val displayName: String,
    val photoUrl: Uri? = null
) 