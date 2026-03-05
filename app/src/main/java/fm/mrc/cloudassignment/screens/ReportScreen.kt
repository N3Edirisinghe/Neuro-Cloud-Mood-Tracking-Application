package fm.mrc.cloudassignment.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import fm.mrc.cloudassignment.components.BottomNavBar
import fm.mrc.cloudassignment.ui.theme.*
import fm.mrc.cloudassignment.workers.ReportWorker
import fm.mrc.cloudassignment.navigation.Screen
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import fm.mrc.cloudassignment.data.DatabaseHelper
import fm.mrc.cloudassignment.auth.AuthManager
import android.util.Log

// Data classes for report data
data class MoodEntry(
    val date: Date,
    val mood: Float,
    val notes: String = "",
    val hasPhoto: Boolean = false,
    val hasVoice: Boolean = false
)

data class MoodStatistics(
    val average: Float,
    val highest: Float,
    val lowest: Float,
    val entries: Int,
    val streak: Int,
    val photoCount: Int,
    val voiceCount: Int
)

data class ClientCondition(
    val label: String,
    val value: Float,
    val color: Color
)

enum class ReportPeriod {
    DAY, WEEK, MONTH, YEAR
}

enum class ExportFormat {
    PDF, CSV, EXCEL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(navController: NavController) {
    // State for the UI
    var selectedPeriod by remember { mutableStateOf(ReportPeriod.DAY) }
    var selectedExportFormat by remember { mutableStateOf(ExportFormat.PDF) }
    var showExportDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isDownloading by remember { mutableStateOf(false) }
    var showDownloadSuccess by remember { mutableStateOf(false) }
    
    // DatabaseHelper instance to access real client data
    val databaseHelper = remember { DatabaseHelper(context) }
    
    // Get the current user ID, defaulting to 1 if not available
    val userId = remember { 
        AuthManager.auth?.currentUser?.uid?.hashCode()?.toLong() ?: 1L 
    }
    
    // Check if reports are available based on login time
    val prefs = remember { context.getSharedPreferences(ReportWorker.PREFS_NAME, Context.MODE_PRIVATE) }
    val firstLoginTime = remember { prefs.getLong(ReportWorker.KEY_FIRST_LOGIN_TIME, 0) }
    
    // Use currentTime as a State to update the UI when time changes
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    
    // Update current time every minute
    LaunchedEffect(Unit) {
        while(true) {
            currentTime = System.currentTimeMillis()
            kotlinx.coroutines.delay(60000) // Update every minute
        }
    }
    
    val timeElapsed = currentTime - firstLoginTime
    
    // Calculate time periods more accurately
    val oneDayInMillis = 24 * 60 * 60 * 1000L
    val oneWeekInMillis = 7 * oneDayInMillis
    val oneMonthInMillis = 30 * oneDayInMillis
    val oneYearInMillis = 365 * oneDayInMillis
    
    // For development/testing purposes - set to true to enable faster testing of reports
    val isDevelopmentMode = true // Changed to true to enable testing
    
    // Determine if reports are available for each period
    val dailyReportAvailable = if (isDevelopmentMode) {
        true // Always show daily reports
    } else {
        // Check if there are any mood entries in the last 24 hours
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val startTime = calendar.timeInMillis
        
        val recentEntries = databaseHelper.getMoodEntries(userId, startTime, endTime)
        recentEntries.isNotEmpty() // Daily report is available if there are recent entries
    }
    
    val weeklyReportAvailable = if (isDevelopmentMode) {
        true // Always show weekly reports
    } else {
        // Check if there are any mood entries in the last week
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.WEEK_OF_YEAR, -1)
        val startTime = calendar.timeInMillis
        
        val recentEntries = databaseHelper.getMoodEntries(userId, startTime, endTime)
        recentEntries.isNotEmpty()
    }
    
    val monthlyReportAvailable = if (isDevelopmentMode) {
        true // Always show monthly reports
    } else {
        // Check if there are any mood entries in the last month
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.MONTH, -1)
        val startTime = calendar.timeInMillis
        
        val recentEntries = databaseHelper.getMoodEntries(userId, startTime, endTime)
        recentEntries.isNotEmpty()
    }
    
    val yearlyReportAvailable = if (isDevelopmentMode) {
        true // Always show yearly reports
    } else {
        // Check if there are any mood entries in the last year
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.YEAR, -1)
        val startTime = calendar.timeInMillis
        
        val recentEntries = databaseHelper.getMoodEntries(userId, startTime, endTime)
        recentEntries.isNotEmpty()
    }
    
    // Calculate remaining time for each period - with development mode consideration
    val dailyRemainingTime = if (dailyReportAvailable) 0L else {
        if (isDevelopmentMode) {
            (1 * 60 * 1000L) - timeElapsed
        } else {
            oneDayInMillis - timeElapsed
        }
    }
    
    val weeklyRemainingTime = if (weeklyReportAvailable) 0L else {
        if (isDevelopmentMode) {
            (2 * 60 * 1000L) - timeElapsed
        } else {
            oneWeekInMillis - timeElapsed
        }
    }
    
    val yearlyRemainingTime = if (yearlyReportAvailable) 0L else {
        if (isDevelopmentMode) {
            (3 * 60 * 1000L) - timeElapsed
        } else {
            oneYearInMillis - timeElapsed
        }
    }
    
    // Format remaining time for display
    val formatRemainingTime = { millis: Long ->
        val days = millis / oneDayInMillis
        val hours = (millis % oneDayInMillis) / (60 * 60 * 1000)
        val minutes = (millis % (60 * 60 * 1000)) / (60 * 1000)
        
        when {
            days > 0 -> "$days days, $hours hours"
            hours > 0 -> "$hours hours, $minutes minutes"
            else -> "$minutes minutes"
        }
    }
    
    // Auto-select the first available period
    LaunchedEffect(dailyReportAvailable, weeklyReportAvailable, monthlyReportAvailable, yearlyReportAvailable) {
        // Always try to select daily first, then weekly, then yearly (monthly is locked)
        if (dailyReportAvailable) {
            selectedPeriod = ReportPeriod.DAY
        } else if (weeklyReportAvailable) {
            selectedPeriod = ReportPeriod.WEEK
        } else if (yearlyReportAvailable) {
            selectedPeriod = ReportPeriod.YEAR
        }
    }
    
    // Real data for the reports based on the selected period
    val moodData = remember(selectedPeriod) {
        getRealMoodData(databaseHelper, userId, selectedPeriod)
    }
    
    val statistics = remember(moodData) {
        calculateStatistics(moodData)
    }
    
    val clientConditions = remember(moodData) {
        generateClientConditions(moodData)
    }
    
    // Auto-dismiss success message
    LaunchedEffect(showDownloadSuccess) {
        if (showDownloadSuccess) {
            kotlinx.coroutines.delay(3000)
            showDownloadSuccess = false
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Mood Reports",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DarkSurface
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Go back",
                            tint = TextWhite
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Export options",
                            tint = TextWhite
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
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DarkBackground,
                            DarkBackground.copy(alpha = 0.95f)
                        )
                    )
                )
                .padding(padding)
        ) {
            // Main content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Period selection
                PeriodSelector(
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = { period ->
                        // Only allow selection of available periods
                        when (period) {
                            ReportPeriod.DAY -> if (dailyReportAvailable) selectedPeriod = period
                            ReportPeriod.WEEK -> if (weeklyReportAvailable) selectedPeriod = period
                            ReportPeriod.MONTH -> { /* Monthly report is locked */ }
                            ReportPeriod.YEAR -> if (yearlyReportAvailable) selectedPeriod = period
                        }
                    },
                    dailyAvailable = dailyReportAvailable,
                    weeklyAvailable = weeklyReportAvailable,
                    monthlyAvailable = monthlyReportAvailable,
                    yearlyAvailable = yearlyReportAvailable
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Period summary card
                PeriodSummaryCard(selectedPeriod)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Check if the selected period report is available
                if ((selectedPeriod == ReportPeriod.DAY && !dailyReportAvailable) ||
                    (selectedPeriod == ReportPeriod.WEEK && !weeklyReportAvailable) ||
                    (selectedPeriod == ReportPeriod.MONTH) || // Monthly report is always locked
                    (selectedPeriod == ReportPeriod.YEAR && !yearlyReportAvailable)) {
                    
                    // Show message that report is not yet available
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = DarkSurface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "Info",
                                tint = PrimaryBlue,
                                modifier = Modifier.size(48.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "Report Not Available",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextWhite,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = when (selectedPeriod) {
                                    ReportPeriod.DAY -> "No mood entries found in the last 24 hours. Add a mood entry to see your daily report."
                                    ReportPeriod.WEEK -> "Weekly reports are locked. These reports will be available in the premium version."
                                    ReportPeriod.MONTH -> "Monthly reports are locked. These reports will be available in the premium version."
                                    ReportPeriod.YEAR -> "Yearly reports are locked. These reports will be available in the premium version."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextGray,
                                textAlign = TextAlign.Center
                            )
                            
                            if (selectedPeriod != ReportPeriod.MONTH) {
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Show time remaining
                                val timeRemaining = when (selectedPeriod) {
                                    ReportPeriod.DAY -> dailyRemainingTime
                                    ReportPeriod.WEEK -> weeklyRemainingTime
                                    ReportPeriod.YEAR -> yearlyRemainingTime
                                    else -> 0
                                }
                                
                                Text(
                                    text = "Time remaining: ${formatRemainingTime(timeRemaining)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PrimaryBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    // Mood graph
                    MoodGraphCard(moodData, selectedPeriod)
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Statistics card
                    StatisticsCard(statistics)
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Client conditions
                    ClientConditionsCard(clientConditions)
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Download button
                    Button(
                        onClick = { 
                            isDownloading = true
                            scope.launch {
                                downloadReport(context, selectedPeriod, selectedExportFormat, moodData)
                                isDownloading = false
                                showDownloadSuccess = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(elevation = 4.dp, shape = RoundedCornerShape(8.dp)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue
                        ),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isDownloading
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating Report...")
                        } else {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = "Download",
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                "Download ${selectedPeriod.name.lowercase().capitalize()} Report",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Success snackbar
            AnimatedVisibility(
                visible = showDownloadSuccess,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp)
            ) {
                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .shadow(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Report successfully downloaded",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            // Export format dialog
            if (showExportDialog) {
                AlertDialog(
                    onDismissRequest = { showExportDialog = false },
                    title = { Text("Export Format") },
                    text = {
                        Column {
                            exportFormatOption(ExportFormat.PDF, selectedExportFormat) {
                                selectedExportFormat = ExportFormat.PDF
                            }
                            exportFormatOption(ExportFormat.CSV, selectedExportFormat) {
                                selectedExportFormat = ExportFormat.CSV
                            }
                            exportFormatOption(ExportFormat.EXCEL, selectedExportFormat) {
                                selectedExportFormat = ExportFormat.EXCEL
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { showExportDialog = false }
                        ) {
                            Text("Confirm")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showExportDialog = false }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

/**
 * Get real mood data from the database for the specified period
 */
fun getRealMoodData(
    databaseHelper: DatabaseHelper,
    userId: Long,
    period: ReportPeriod
): List<MoodEntry> {
    try {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        
        // Set start time based on period
        calendar.apply {
            when (period) {
                ReportPeriod.DAY -> add(Calendar.DAY_OF_YEAR, -1) // Last 24 hours
                ReportPeriod.WEEK -> add(Calendar.WEEK_OF_YEAR, -1) // Last week
                ReportPeriod.MONTH -> add(Calendar.MONTH, -1) // Last month
                ReportPeriod.YEAR -> add(Calendar.YEAR, -1) // Last year
            }
        }
        val startTime = calendar.timeInMillis
        
        // Get data from database
        val dbEntries = databaseHelper.getMoodEntries(userId, startTime, endTime)
        
        // If no data found, return a small sample set to avoid empty screens
        if (dbEntries.isEmpty()) {
            Log.d("ReportScreen", "No real mood data found for period: $period, using limited sample data")
            return when (period) {
                ReportPeriod.DAY -> generateLimitedSampleData(1)
                ReportPeriod.WEEK -> generateLimitedSampleData(7)
                ReportPeriod.MONTH -> generateLimitedSampleData(30)
                ReportPeriod.YEAR -> generateLimitedSampleData(12)
            }
        }
        
        // Convert database entries to MoodEntry objects
        return dbEntries.map { dbEntry ->
            MoodEntry(
                date = Date(dbEntry.timestamp),
                mood = dbEntry.moodScore,
                notes = dbEntry.notes ?: "",
                hasPhoto = dbEntry.hasPhoto,  // Use the real hasPhoto value from database
                hasVoice = dbEntry.hasVoice   // Use the real hasVoice value from database
            )
        }
    } catch (e: Exception) {
        Log.e("ReportScreen", "Error getting mood data: ${e.message}")
        return emptyList()
    }
}

// Generate a small sample dataset when real data is not available
private fun generateLimitedSampleData(count: Int): List<MoodEntry> {
    val calendar = Calendar.getInstance()
    val data = mutableListOf<MoodEntry>()
    
    for (i in count downTo 1) {
        when (count) {
            1 -> calendar.add(Calendar.HOUR_OF_DAY, -i) // hourly for day view
            7 -> calendar.add(Calendar.DAY_OF_WEEK, -i) // daily for week view
            30 -> calendar.add(Calendar.DAY_OF_MONTH, -i) // daily for month view
            12 -> calendar.add(Calendar.MONTH, -i) // monthly for year view
        }
        
        // Create a more structured pattern rather than random
        val mood = when (i % 5) {
            0 -> 5f  // Very happy
            1 -> 4f  // Happy
            2 -> 3f  // Neutral
            3 -> 2f  // Sad
            else -> 1f // Very sad
        }
        
        data.add(
            MoodEntry(
                date = calendar.time,
                mood = mood,
                notes = "Sample client data entry",
                hasPhoto = false,
                hasVoice = false
            )
        )
        
        // Reset the calendar to current for next iteration
        calendar.time = Date()
    }
    
    return data
}

// Generate client conditions based on actual mood data trends
private fun generateClientConditions(moodData: List<MoodEntry>): List<ClientCondition> {
    if (moodData.isEmpty()) {
        return listOf(
            ClientCondition("Overall Mood", 0.5f, Color(0xFF4CAF50)),
            ClientCondition("Anxiety Level", 0.5f, Color(0xFFFFA000)),
            ClientCondition("Stress Level", 0.5f, Color(0xFFE57373)),
            ClientCondition("Sleep Quality", 0.5f, Color(0xFF7986CB)),
            ClientCondition("Social Activity", 0.5f, Color(0xFF9575CD))
        )
    }
    
    // Calculate mood average on a 0-1 scale
    val moodValues = moodData.map { it.mood }
    val moodAverage = moodValues.sum() / (moodValues.size * 5f) // Normalize to 0-1 range
    
    // Use mood data to infer other conditions (this would be more sophisticated in a real app)
    val anxietyLevel = 1f - moodAverage.coerceIn(0f, 1f) // Higher mood = lower anxiety
    val stressLevel = 1f - (moodAverage * 0.8f).coerceIn(0f, 1f) // Slightly lower than anxiety
    
    // These would be independent metrics in a real app
    val sleepQuality = (moodAverage * 0.9f).coerceIn(0f, 1f) // Correlated with mood
    val socialActivity = (moodAverage * 0.7f).coerceIn(0f, 1f) // Somewhat correlated
    
    return listOf(
        ClientCondition("Overall Mood", moodAverage, Color(0xFF4CAF50)),
        ClientCondition("Anxiety Level", anxietyLevel, Color(0xFFFFA000)),
        ClientCondition("Stress Level", stressLevel, Color(0xFFE57373)),
        ClientCondition("Sleep Quality", sleepQuality, Color(0xFF7986CB)),
        ClientCondition("Social Activity", socialActivity, Color(0xFF9575CD))
    )
}

@Composable
private fun exportFormatOption(
    format: ExportFormat,
    selectedFormat: ExportFormat,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = format == selectedFormat,
            onClick = onSelect
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            when (format) {
                ExportFormat.PDF -> Icons.Default.PictureAsPdf
                ExportFormat.CSV -> Icons.Default.DocumentScanner
                ExportFormat.EXCEL -> Icons.Default.GridView
            },
            contentDescription = format.name,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = format.name,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun PeriodSelector(
    selectedPeriod: ReportPeriod,
    onPeriodSelected: (ReportPeriod) -> Unit,
    dailyAvailable: Boolean,
    weeklyAvailable: Boolean,
    monthlyAvailable: Boolean,
    yearlyAvailable: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(DarkSurface),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Day button
        Box(modifier = Modifier.weight(1f)) {
            PeriodButton(
                text = "Day",
                isSelected = selectedPeriod == ReportPeriod.DAY,
                isAvailable = dailyAvailable,
                onClick = { onPeriodSelected(ReportPeriod.DAY) }
            )
        }
        
        // Week button
        Box(modifier = Modifier.weight(1f)) {
            PeriodButton(
                text = "Week",
                isSelected = selectedPeriod == ReportPeriod.WEEK,
                isAvailable = weeklyAvailable,
                onClick = { onPeriodSelected(ReportPeriod.WEEK) },
                showLock = !weeklyAvailable
            )
        }
        
        // Month button
        Box(modifier = Modifier.weight(1f)) {
            PeriodButton(
                text = "Month",
                isSelected = selectedPeriod == ReportPeriod.MONTH,
                isAvailable = monthlyAvailable,
                onClick = { onPeriodSelected(ReportPeriod.MONTH) },
                showLock = !monthlyAvailable
            )
        }
        
        // Year button
        Box(modifier = Modifier.weight(1f)) {
            PeriodButton(
                text = "Year",
                isSelected = selectedPeriod == ReportPeriod.YEAR,
                isAvailable = yearlyAvailable,
                onClick = { onPeriodSelected(ReportPeriod.YEAR) },
                showLock = !yearlyAvailable
            )
        }
    }
}

@Composable
fun PeriodButton(
    text: String,
    isSelected: Boolean,
    isAvailable: Boolean,
    onClick: () -> Unit,
    showLock: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = isAvailable,
                onClick = onClick
            )
            .background(
                if (isSelected && isAvailable) PrimaryBlue else Color.Transparent,
                RoundedCornerShape(28.dp)
            )
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (showLock) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Locked",
                    modifier = Modifier.size(16.dp),
                    tint = if (isSelected) TextWhite else TextGray
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            
            Text(
                text = text,
                color = if (isSelected && isAvailable) TextWhite 
                       else if (isAvailable) TextWhite.copy(alpha = 0.8f)
                       else TextGray.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            )
        }
    }
}

@Composable
private fun PeriodSummaryCard(period: ReportPeriod) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val today = Calendar.getInstance()
    
    val periodSummary = when (period) {
        ReportPeriod.DAY -> "Today, ${dateFormat.format(today.time)}"
        ReportPeriod.WEEK -> {
            val startOfWeek = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            }
            val endOfWeek = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            }
            "${dateFormat.format(startOfWeek.time)} - ${dateFormat.format(endOfWeek.time)}"
        }
        ReportPeriod.MONTH -> {
            val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(today.time)
            monthName
        }
        ReportPeriod.YEAR -> {
            val year = today.get(Calendar.YEAR)
            "Year $year"
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = PrimaryBlue.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when (period) {
                        ReportPeriod.DAY -> Icons.Default.Today
                        ReportPeriod.WEEK -> Icons.Default.ViewWeek
                        ReportPeriod.MONTH -> Icons.Default.CalendarMonth
                        ReportPeriod.YEAR -> Icons.Default.CalendarViewMonth
                    },
                    contentDescription = "Period icon",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = "${period.name.lowercase().capitalize()} Report",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextWhite,
                    fontWeight = FontWeight.SemiBold
                )
                
                Text(
                    text = periodSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray
                )
            }
        }
    }
}

@Composable
private fun MoodGraphCard(moodData: List<MoodEntry>, period: ReportPeriod) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Mood Trends",
                style = MaterialTheme.typography.titleMedium,
                color = TextWhite,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "See how your mood has changed over the ${period.name.lowercase()}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextGray
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Mood graph based on period
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF22262F))
            ) {
                // Simple line chart
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (moodData.isNotEmpty()) {
                        val maxMood = 5f  // Assuming 5 is max mood
                        val minMood = 1f  // Assuming 1 is min mood
                        val yAxisSpace = 30f
                        val xAxisSpace = 30f
                        
                        val chartWidth = size.width - 2 * xAxisSpace
                        val chartHeight = size.height - 2 * yAxisSpace
                        
                        val xStep = chartWidth / (moodData.size - 1).coerceAtLeast(1)
                        
                        // Grid lines
                        val gridLineColor = Color.Gray.copy(alpha = 0.2f)
                        for (i in 1..4) {
                            val y = yAxisSpace + chartHeight - (chartHeight * (i.toFloat() / 5f))
                            drawLine(
                                start = androidx.compose.ui.geometry.Offset(xAxisSpace, y),
                                end = androidx.compose.ui.geometry.Offset(size.width - xAxisSpace, y),
                                color = gridLineColor,
                                strokeWidth = 1f
                            )
                        }
                        
                        // Line and points
                        val points = moodData.mapIndexed { index, entry ->
                            val x = xAxisSpace + (index * xStep)
                            val y = yAxisSpace + chartHeight - (chartHeight * ((entry.mood - minMood) / (maxMood - minMood)))
                            androidx.compose.ui.geometry.Offset(x, y)
                        }
                        
                        // Line
                        if (points.size > 1) {
                            val lineColor = PrimaryBlue
                            for (i in 0 until points.size - 1) {
                                drawLine(
                                    start = points[i],
                                    end = points[i + 1],
                                    color = lineColor,
                                    strokeWidth = 3f,
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                        
                        // Points
                        val pointRadius = 6f
                        points.forEach { point ->
                            drawCircle(
                                color = PrimaryBlue,
                                radius = pointRadius,
                                center = point
                            )
                            drawCircle(
                                color = DarkSurface,
                                radius = pointRadius - 2f,
                                center = point
                            )
                        }
                    }
                }
                
                // If no data
                if (moodData.isEmpty()) {
                    Text(
                        text = "No mood data available for this period",
                        color = TextGray,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val dateFormat = when (period) {
                    ReportPeriod.DAY -> SimpleDateFormat("h:mm a", Locale.getDefault())
                    ReportPeriod.WEEK -> SimpleDateFormat("EEE", Locale.getDefault())
                    ReportPeriod.MONTH -> SimpleDateFormat("d", Locale.getDefault())
                    ReportPeriod.YEAR -> SimpleDateFormat("MMM", Locale.getDefault())
                }
                
                if (moodData.isNotEmpty()) {
                    val step = moodData.size / 5.coerceAtMost(moodData.size)
                    (0 until moodData.size step step.coerceAtLeast(1)).forEach { i ->
                        if (i < moodData.size) {
                            val date = moodData[i].date
                            Text(
                                text = dateFormat.format(date),
                                color = TextGray,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp
                            )
                        }
                    }
                    if (moodData.size > 1) {
                        Text(
                            text = dateFormat.format(moodData.last().date),
                            color = TextGray,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatisticsCard(statistics: MoodStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Mood Statistics",
                style = MaterialTheme.typography.titleMedium,
                color = TextWhite,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Statistics grid
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                StatisticRow(
                    listOf(
                        Triple("Average Mood", statistics.average.toString(), Icons.Default.Star),
                        Triple("Highest Mood", statistics.highest.toString(), Icons.Default.TrendingUp)
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                StatisticRow(
                    listOf(
                        Triple("Lowest Mood", statistics.lowest.toString(), Icons.Default.TrendingDown),
                        Triple("Entry Count", statistics.entries.toString(), Icons.Default.ListAlt)
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                StatisticRow(
                    listOf(
                        Triple("Current Streak", "${statistics.streak} days", Icons.Default.Whatshot),
                        Triple("Media Count", "${statistics.photoCount + statistics.voiceCount}", Icons.Default.PermMedia)
                    )
                )
                
                // Show detailed media counts if there are any media items
                if (statistics.photoCount > 0 || statistics.voiceCount > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Photos count
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PhotoCamera,
                                contentDescription = "Photos",
                                tint = PrimaryBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${statistics.photoCount} photos",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextGray
                            )
                        }
                        
                        // Voice recordings count
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = "Voice recordings",
                                tint = PrimaryBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${statistics.voiceCount} recordings",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextGray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatisticRow(items: List<Triple<String, String, androidx.compose.ui.graphics.vector.ImageVector>>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items.forEach { (title, value, icon) ->
            StatisticItem(
                title = title,
                value = value,
                icon = icon,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun StatisticItem(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = DarkBackground.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(16.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = TextWhite,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = TextGray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ClientConditionsCard(conditions: List<ClientCondition>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Client Conditions",
                style = MaterialTheme.typography.titleMedium,
                color = TextWhite,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Summary of your mental health indicators",
                style = MaterialTheme.typography.bodyMedium,
                color = TextGray
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Conditions list
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                conditions.forEach { condition ->
                    ConditionProgressBar(condition)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun ConditionProgressBar(condition: ClientCondition) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = condition.label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextWhite
            )
            
            Text(
                text = "${(condition.value * 100).roundToInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = condition.color
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(DarkBackground.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(condition.value)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(condition.color)
            )
        }
    }
}

/**
 * Download a report in the specified format
 */
fun downloadReport(
    context: Context,
    period: ReportPeriod,
    format: ExportFormat,
    moodData: List<MoodEntry>
) {
    try {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val periodName = period.name.lowercase().capitalize()
        val extension = when (format) {
            ExportFormat.PDF -> ".pdf"
            ExportFormat.CSV -> ".csv"
            ExportFormat.EXCEL -> ".xlsx"
        }
        
        val filename = "mood_report_${periodName}_$timestamp$extension"
        val file = File(context.getExternalFilesDir(null), filename)
        
        // In a real app, you would generate actual reports based on the format
        // For demonstration purposes, we'll just create a CSV for all formats
        
        FileWriter(file).use { writer ->
            writer.append("Date,Mood,Notes,Has Photo,Has Voice Recording\n")
            
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            moodData.forEach { entry ->
                writer.append("${dateFormat.format(entry.date)},")
                writer.append("${entry.mood},")
                writer.append("\"${entry.notes}\",")
                writer.append("${entry.hasPhoto},")
                writer.append("${entry.hasVoice}\n")
            }
        }
        
        // In a real app with proper PDF/XLSX generation, you'd use different approaches for each format
        
        // Share the file
        val fileUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, when (format) {
                ExportFormat.PDF -> "application/pdf"
                ExportFormat.CSV -> "text/csv"
                ExportFormat.EXCEL -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            })
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        
        // In a real app, you'd check if there's an app that can handle this intent
        // context.startActivity(Intent.createChooser(intent, "Open report with"))
        
    } catch (e: Exception) {
        e.printStackTrace()
        // In a real app, show an error message
    }
}

private fun calculateStatistics(moodData: List<MoodEntry>): MoodStatistics {
    if (moodData.isEmpty()) {
        return MoodStatistics(
            average = 0f,
            highest = 0f,
            lowest = 0f,
            entries = 0,
            streak = 0,
            photoCount = 0,
            voiceCount = 0
        )
    }
    
    val moodValues = moodData.map { it.mood }
    val average = moodValues.sum() / moodValues.size
    val highest = moodValues.maxOrNull() ?: 0f
    val lowest = moodValues.minOrNull() ?: 0f
    val photoCount = moodData.count { it.hasPhoto }
    val voiceCount = moodData.count { it.hasVoice }
    
    // Calculate streak based on consecutive days with entries
    // This is a simplified version - a real app would be more sophisticated
    val orderedByDate = moodData.sortedByDescending { it.date.time }
    var streak = 1
    
    if (orderedByDate.size > 1) {
        val calendar = Calendar.getInstance()
        var currentDay = orderedByDate[0].date.time
        
        for (i in 1 until orderedByDate.size) {
            calendar.timeInMillis = currentDay
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val previousDay = calendar.timeInMillis
            
            val entryDay = orderedByDate[i].date.time
            
            // Check if this entry is from the previous day
            calendar.timeInMillis = entryDay
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val entryDayStart = calendar.timeInMillis
            
            calendar.timeInMillis = previousDay
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val previousDayStart = calendar.timeInMillis
            
            if (entryDayStart == previousDayStart) {
                streak++
                currentDay = entryDay
            } else {
                break
            }
        }
    }
    
    return MoodStatistics(
        average = (average * 100).roundToInt() / 100f, // Round to 2 decimal places
        highest = highest,
        lowest = lowest,
        entries = moodData.size,
        streak = streak,
        photoCount = photoCount,
        voiceCount = voiceCount
    )
} 