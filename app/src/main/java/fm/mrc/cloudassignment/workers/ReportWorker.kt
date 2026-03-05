package fm.mrc.cloudassignment.workers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import fm.mrc.cloudassignment.data.DatabaseHelper
import fm.mrc.cloudassignment.screens.ReportPeriod
import fm.mrc.cloudassignment.screens.downloadReport
import fm.mrc.cloudassignment.screens.getRealMoodData
import fm.mrc.cloudassignment.auth.AuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Worker class to generate reports at the end of each period (daily, weekly, monthly, yearly)
 */
class ReportWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "ReportWorker"
        const val PREFS_NAME = "report_preferences"
        const val KEY_FIRST_LOGIN_TIME = "first_login_time"
        const val KEY_LAST_DAILY_REPORT = "last_daily_report"
        const val KEY_LAST_WEEKLY_REPORT = "last_weekly_report"
        const val KEY_LAST_MONTHLY_REPORT = "last_monthly_report"
        const val KEY_LAST_YEARLY_REPORT = "last_yearly_report"
        
        // Time constants for more accurate calculations
        const val ONE_DAY_IN_MILLIS = 24 * 60 * 60 * 1000L
        const val ONE_WEEK_IN_MILLIS = 7 * ONE_DAY_IN_MILLIS
        const val ONE_MONTH_IN_MILLIS = 30 * ONE_DAY_IN_MILLIS
        const val ONE_YEAR_IN_MILLIS = 365 * ONE_DAY_IN_MILLIS
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting report generation worker")
            
            // Get the current user ID
            val userId = AuthManager.auth?.currentUser?.uid?.hashCode()?.toLong() ?: 1L
            
            // Get the database helper
            val databaseHelper = DatabaseHelper(applicationContext)
            
            // Get shared preferences for tracking report generation
            val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            
            // Check if this is the first login
            val firstLoginTime = prefs.getLong(KEY_FIRST_LOGIN_TIME, 0)
            if (firstLoginTime == 0L) {
                // First login, record the time
                prefs.edit().putLong(KEY_FIRST_LOGIN_TIME, System.currentTimeMillis()).apply()
                Log.d(TAG, "First login recorded")
                return@withContext Result.success()
            }
            
            // Calculate time elapsed since first login
            val currentTime = System.currentTimeMillis()
            val timeElapsed = currentTime - firstLoginTime
            
            // Check if it's time to generate reports based on elapsed time
            val lastDailyReport = prefs.getLong(KEY_LAST_DAILY_REPORT, 0)
            val lastWeeklyReport = prefs.getLong(KEY_LAST_WEEKLY_REPORT, 0)
            val lastYearlyReport = prefs.getLong(KEY_LAST_YEARLY_REPORT, 0)
            
            // Generate daily report after 24 hours
            if (timeElapsed >= ONE_DAY_IN_MILLIS && (lastDailyReport == 0L || currentTime - lastDailyReport >= ONE_DAY_IN_MILLIS)) {
                Log.d(TAG, "Generating daily report (24 hours elapsed)")
                generateReport(databaseHelper, userId, ReportPeriod.DAY)
                prefs.edit().putLong(KEY_LAST_DAILY_REPORT, currentTime).apply()
            }
            
            // Generate weekly report after 7 days
            if (timeElapsed >= ONE_WEEK_IN_MILLIS && (lastWeeklyReport == 0L || currentTime - lastWeeklyReport >= ONE_WEEK_IN_MILLIS)) {
                Log.d(TAG, "Generating weekly report (7 days elapsed)")
                generateReport(databaseHelper, userId, ReportPeriod.WEEK)
                prefs.edit().putLong(KEY_LAST_WEEKLY_REPORT, currentTime).apply()
            }
            
            // Monthly report is locked - skip generation
            
            // Generate yearly report after 365 days
            if (timeElapsed >= ONE_YEAR_IN_MILLIS && (lastYearlyReport == 0L || currentTime - lastYearlyReport >= ONE_YEAR_IN_MILLIS)) {
                Log.d(TAG, "Generating yearly report (365 days elapsed)")
                generateReport(databaseHelper, userId, ReportPeriod.YEAR)
                prefs.edit().putLong(KEY_LAST_YEARLY_REPORT, currentTime).apply()
            }
            
            Log.d(TAG, "Report generation worker completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in report generation worker: ${e.message}")
            Result.failure()
        }
    }
    
    /**
     * Generate a report for the specified period
     */
    private suspend fun generateReport(
        databaseHelper: DatabaseHelper,
        userId: Long,
        period: ReportPeriod
    ) {
        try {
            // Get the mood data for the period
            val moodData = getRealMoodData(databaseHelper, userId, period)
            
            // Generate the report in CSV format (most compatible)
            downloadReport(
                applicationContext,
                period,
                fm.mrc.cloudassignment.screens.ExportFormat.CSV,
                moodData
            )
            
            Log.d(TAG, "Generated ${period.name.lowercase()} report successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error generating ${period.name.lowercase()} report: ${e.message}")
        }
    }
} 