package fm.mrc.cloudassignment

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.google.firebase.FirebaseApp
import fm.mrc.cloudassignment.auth.AuthManager
import fm.mrc.cloudassignment.auth.GoogleSignInHelper
import fm.mrc.cloudassignment.navigation.NavGraph
import fm.mrc.cloudassignment.screens.ThemeManager
import fm.mrc.cloudassignment.ui.theme.NeuroCloudTheme
import fm.mrc.cloudassignment.workers.ReportScheduler
import fm.mrc.cloudassignment.workers.ReportWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    // Keep track of the sign-in callback state
    private var pendingGoogleSignIn: ((data: Intent?) -> Unit)? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            // Initialize Firebase
            if (!isFirebaseInitialized()) {
                FirebaseApp.initializeApp(this)
            }
            
            // Initialize AuthManager with a temporary client ID 
            // This prevents crashes when missing proper configuration
            val webClientId = try {
                getString(R.string.web_client_id)
            } catch (e: Exception) {
                "temporary_client_id"
            }
            
            if (webClientId != "YOUR_WEB_CLIENT_ID") {
                try {
                    AuthManager.initialize(
                        activity = this,
                        webClientId = webClientId
                    )
                    
                    // Check if user is already signed in
                    if (AuthManager.auth?.currentUser != null) {
                        // Record first login time if not already recorded
                        recordFirstLoginTime()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Auth initialization failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            
            // Schedule report generation
            ReportScheduler.scheduleReportGeneration(this)
            
        } catch (e: Exception) {
            Toast.makeText(this, "Firebase initialization failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
        
        setContent {
            NeuroCloudTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController)
                }
            }
        }
    }
    
    // Handle activity results for Google Sign-In
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        // Check if this is the result of a Google Sign-In
        if (requestCode == 9001) { // RC_SIGN_IN
            // Use CoroutineScope to handle async operations
            val scope = CoroutineScope(Dispatchers.Main)
            scope.launch {
                try {
                    // Process the sign-in result through our helper
                    pendingGoogleSignIn?.invoke(data)
                    
                    // Record first login time after successful sign-in
                    recordFirstLoginTime()
                    
                    // Clear the pending callback
                    pendingGoogleSignIn = null
                } catch (e: Exception) {
                    Toast.makeText(
                        this@MainActivity,
                        "Error handling sign-in result: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    // Method to register a Google sign-in callback
    fun setGoogleSignInCallback(callback: (data: Intent?) -> Unit) {
        pendingGoogleSignIn = callback
    }
    
    private fun isFirebaseInitialized(): Boolean {
        return try {
            FirebaseApp.getInstance() != null
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Record the first login time if not already recorded
     */
    private fun recordFirstLoginTime() {
        val prefs = getSharedPreferences(ReportWorker.PREFS_NAME, MODE_PRIVATE)
        val firstLoginTime = prefs.getLong(ReportWorker.KEY_FIRST_LOGIN_TIME, 0)
        
        if (firstLoginTime == 0L) {
            // First login, record the time
            val currentTime = System.currentTimeMillis()
            prefs.edit().putLong(ReportWorker.KEY_FIRST_LOGIN_TIME, currentTime).apply()
            Log.d("MainActivity", "First login time recorded: $currentTime")
            
            // Also set the initial report generation timestamps
            prefs.edit()
                .putLong(ReportWorker.KEY_LAST_DAILY_REPORT, 0)
                .putLong(ReportWorker.KEY_LAST_WEEKLY_REPORT, 0)
                .putLong(ReportWorker.KEY_LAST_YEARLY_REPORT, 0)
                .apply()
                
            // Ensure the report scheduler is running
            ReportScheduler.scheduleReportGeneration(this)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Cancel report generation when the app is destroyed
        ReportScheduler.cancelReportGeneration(this)
    }
}