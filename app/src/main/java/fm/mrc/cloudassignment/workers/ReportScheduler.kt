package fm.mrc.cloudassignment.workers

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Scheduler class to set up periodic report generation
 */
object ReportScheduler {
    
    private const val REPORT_WORKER_TAG = "report_generation_worker"
    
    /**
     * Schedule the report generation worker to run every 6 hours
     * This allows us to check if it's time to generate reports based on elapsed time
     */
    fun scheduleReportGeneration(context: Context) {
        // Create constraints for the worker
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()
        
        // Create a periodic work request that runs every 6 hours
        val reportWorkRequest = PeriodicWorkRequestBuilder<ReportWorker>(
            6, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .addTag(REPORT_WORKER_TAG)
            .build()
        
        // Enqueue the work request
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                REPORT_WORKER_TAG,
                ExistingPeriodicWorkPolicy.REPLACE,
                reportWorkRequest
            )
    }
    
    /**
     * Cancel the scheduled report generation
     */
    fun cancelReportGeneration(context: Context) {
        WorkManager.getInstance(context)
            .cancelUniqueWork(REPORT_WORKER_TAG)
    }
} 