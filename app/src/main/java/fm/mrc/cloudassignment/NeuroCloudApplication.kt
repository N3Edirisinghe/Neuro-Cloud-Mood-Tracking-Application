package fm.mrc.cloudassignment

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager

/**
 * Custom Application class to initialize WorkManager
 */
class NeuroCloudApplication : Application(), Configuration.Provider {
    
    override fun onCreate() {
        super.onCreate()
        // Initialize WorkManager with custom configuration
        WorkManager.initialize(
            this,
            workManagerConfiguration
        )
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
} 