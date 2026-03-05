package fm.mrc.cloudassignment.auth

import android.util.Log
import kotlinx.coroutines.tasks.await

object UserAuthUtils {
    private const val TAG = "UserAuthUtils"

    /**
     * Check if a user exists in Firestore with the given email
     */
    suspend fun checkUserExists(email: String): Boolean {
        if (AuthManager.firestore == null) {
            Log.e(TAG, "Cannot check user: Firestore not initialized")
            return false
        }

        return try {
            val querySnapshot = AuthManager.firestore?.collection("users")
                ?.whereEqualTo("email", email)
                ?.limit(1)
                ?.get()
                ?.await()

            querySnapshot != null && !querySnapshot.isEmpty
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if user exists: ${e.message}")
            false
        }
    }
} 