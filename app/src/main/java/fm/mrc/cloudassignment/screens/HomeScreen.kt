package fm.mrc.cloudassignment.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import fm.mrc.cloudassignment.R
import fm.mrc.cloudassignment.auth.AuthManager
import fm.mrc.cloudassignment.components.BottomNavBar
import fm.mrc.cloudassignment.navigation.Screen
import fm.mrc.cloudassignment.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.platform.LocalContext
import fm.mrc.cloudassignment.data.DatabaseHelper

// Import UserDataManager from SettingsScreen
import fm.mrc.cloudassignment.screens.UserDataManager

data class InsightData(
    val title: String,
    val value: String,
    val icon: @Composable () -> Unit,
    val color: Color
)

data class ActivityItem(
    val icon: @Composable () -> Unit,
    val title: String,
    val subtitle: String,
    val time: String,
    val route: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    
    // Animation states
    var showWelcome by remember { mutableStateOf(false) }
    var showInsights by remember { mutableStateOf(false) }
    var showActivities by remember { mutableStateOf(false) }
    var showQuickActions by remember { mutableStateOf(false) }
    
    // State for insights data
    var insights by remember { mutableStateOf<List<InsightData>>(emptyList()) }
    
    // Function to update insights data
    fun updateInsights() {
        // Get the current user ID
        val userId = AuthManager.auth?.currentUser?.uid?.hashCode()?.toLong() ?: 1L
        
        // Get the database helper
        val databaseHelper = DatabaseHelper(context)
        
        // Calculate time periods
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        
        // Get mood data for different time periods
        val todayMoodEntries = try {
            calendar.add(Calendar.DAY_OF_YEAR, -1) // Last 24 hours
            val startTime = calendar.timeInMillis
            databaseHelper.getMoodEntries(userId, startTime, endTime)
        } catch (e: Exception) {
            Log.e("HomeScreen", "Error getting today's mood data: ${e.message}")
            emptyList()
        }
        
        val weekMoodEntries = try {
            calendar.timeInMillis = endTime
            calendar.add(Calendar.WEEK_OF_YEAR, -1) // Last week
            val startTime = calendar.timeInMillis
            databaseHelper.getMoodEntries(userId, startTime, endTime)
        } catch (e: Exception) {
            Log.e("HomeScreen", "Error getting week's mood data: ${e.message}")
            emptyList()
        }
        
        // Calculate average mood scores
        val todayAvgMood = if (todayMoodEntries.isNotEmpty()) {
            todayMoodEntries.map { it.moodScore }.average()
        } else {
            3.0 // Default neutral mood if no data
        }
        
        val weekAvgMood = if (weekMoodEntries.isNotEmpty()) {
            weekMoodEntries.map { it.moodScore }.average()
        } else {
            3.0 // Default neutral mood if no data
        }
        
        // Calculate activity count
        val activityCount = weekMoodEntries.size
        
        // Determine mood text based on average score
        val getMoodText = { score: Double ->
            when {
                score >= 4.5 -> "Very Positive"
                score >= 3.5 -> "Positive"
                score >= 2.5 -> "Neutral"
                score >= 1.5 -> "Negative"
                else -> "Very Negative"
            }
        }
        
        // Create insights with real data
        insights = listOf(
            InsightData(
                title = "Mood Average",
                value = getMoodText(todayAvgMood),
                icon = { Icon(Icons.Filled.Favorite, contentDescription = "Mood", tint = Color.White) },
                color = when {
                    todayAvgMood >= 4.5 -> HappyMoodColor
                    todayAvgMood >= 3.5 -> HappyMoodColor
                    todayAvgMood >= 2.5 -> Color(0xFFFFA726) // Orange for neutral
                    todayAvgMood >= 1.5 -> Color(0xFFEF5350) // Red for negative
                    else -> Color(0xFFD32F2F) // Dark red for very negative
                }
            ),
            InsightData(
                title = "Weekly Mood",
                value = getMoodText(weekAvgMood),
                icon = { Icon(Icons.Filled.Bedtime, contentDescription = "Sleep", tint = Color.White) },
                color = when {
                    weekAvgMood >= 4.5 -> HappyMoodColor
                    weekAvgMood >= 3.5 -> HappyMoodColor
                    weekAvgMood >= 2.5 -> Color(0xFFFFA726) // Orange for neutral
                    weekAvgMood >= 1.5 -> Color(0xFFEF5350) // Red for negative
                    else -> Color(0xFFD32F2F) // Dark red for very negative
                }
            ),
            InsightData(
                title = "Activities",
                value = activityCount.toString(),
                icon = { Icon(Icons.Filled.DirectionsRun, contentDescription = "Activities", tint = Color.White) },
                color = PrimaryBlue
            )
        )
        
        Log.d("HomeScreen", "Insights updated: Today's mood: ${getMoodText(todayAvgMood)}, Weekly mood: ${getMoodText(weekAvgMood)}, Activities: $activityCount")
    }
    
    // Trigger animations sequentially and update insights
    LaunchedEffect(Unit) {
        // Refresh user data when screen appears
        UserDataManager.updateFromAuthManager()
        
        // Update insights data
        updateInsights()
        
        showWelcome = true
        delay(200)
        showInsights = true
        delay(200)
        showActivities = true
        delay(200)
        showQuickActions = true
    }
    
    // Ensure username is always fresh by periodically checking
    LaunchedEffect(Unit) {
        while(true) {
            delay(2000) // Check every 2 seconds
            UserDataManager.updateFromAuthManager()
        }
    }
    
    // Periodically update insights data
    LaunchedEffect(Unit) {
        while(true) {
            delay(5000) // Update every 5 seconds
            updateInsights()
        }
    }
    
    // Force UI refresh when this screen becomes visible
    DisposableEffect(Unit) {
        // Log when screen comes to foreground
        Log.d("HomeScreen", "HomeScreen became visible, updating username and insights")
        UserDataManager.updateFromAuthManager()
        updateInsights()
        
        onDispose {
            // Log when screen goes to background
            Log.d("HomeScreen", "HomeScreen disposed")
        }
    }
    
    val currentDate = remember {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
    }
    
    val currentTime = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good morning"
            hour < 18 -> "Good afternoon"
            else -> "Good evening"
        }
    }
    
    // Get the username directly from Firebase Auth when possible, with fallbacks
    val usernameState = remember { mutableStateOf("") }
    val username by usernameState
    
    // Function to update from all possible sources
    fun updateUsername() {
        // Try Firebase Auth first (most authoritative)
        val currentUser = AuthManager.auth?.currentUser
        val firebaseDisplayName = currentUser?.displayName
        
        if (!firebaseDisplayName.isNullOrBlank()) {
            usernameState.value = firebaseDisplayName
            Log.d("HomeScreen", "Username from Firebase Auth: $firebaseDisplayName")
        } 
        // Then try AuthManager state
        else if (!AuthManager.currentUser.value?.displayName.isNullOrBlank()) {
            usernameState.value = AuthManager.currentUser.value?.displayName ?: "User"
            Log.d("HomeScreen", "Username from AuthManager: ${usernameState.value}")
        }
        // Finally try UserDataManager
        else if (UserDataManager.username.value.isNotBlank()) {
            usernameState.value = UserDataManager.username.value
            Log.d("HomeScreen", "Username from UserDataManager: ${usernameState.value}")
        }
        // Default fallback
        else {
            usernameState.value = "User"
            Log.d("HomeScreen", "Using default username")
        }
    }
    
    // Initial update
    LaunchedEffect(Unit) {
        updateUsername()
    }
    
    // Set up Firebase Auth state listener
    DisposableEffect(Unit) {
        // Create an Firebase Auth state listener
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            Log.d("HomeScreen", "Auth state changed")
            updateUsername()
        }
        
        // Register the listener
        AuthManager.auth?.addAuthStateListener(authStateListener)
        
        onDispose {
            // Clean up
            AuthManager.auth?.removeAuthStateListener(authStateListener)
        }
    }
    
    // Set up periodic updates
    LaunchedEffect(Unit) {
        while(true) {
            delay(1000)
            updateUsername()
        }
    }
    
    // Recent Activities - Update to show real activities
    val recentActivities = remember {
        // Get the current user ID
        val userId = AuthManager.auth?.currentUser?.uid?.hashCode()?.toLong() ?: 1L
        
        // Get the database helper
        val databaseHelper = DatabaseHelper(context)
        
        // Calculate time periods
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        
        // Get mood entries for the past week
        val weekMoodEntries = try {
            calendar.add(Calendar.WEEK_OF_YEAR, -1) // Last week
            val startTime = calendar.timeInMillis
            databaseHelper.getMoodEntries(userId, startTime, endTime)
        } catch (e: Exception) {
            Log.e("HomeScreen", "Error getting week's mood data for activities: ${e.message}")
            emptyList()
        }
        
        // Create activity items from mood entries
        val activities = mutableListOf<ActivityItem>()
        
        // Add mood tracking sessions
        weekMoodEntries.take(2).forEach { entry ->
            val date = Date(entry.timestamp)
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
            val today = Calendar.getInstance()
            val entryDate = Calendar.getInstance().apply { time = date }
            
            val timeString = if (entryDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) &&
                                entryDate.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
                "Today, ${timeFormat.format(date)}"
            } else if (entryDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) - 1 &&
                      entryDate.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
                "Yesterday, ${timeFormat.format(date)}"
            } else {
                "${dateFormat.format(date)}, ${timeFormat.format(date)}"
            }
            
            activities.add(
                ActivityItem(
                    icon = { Icon(Icons.Outlined.SelfImprovement, contentDescription = "Mood Tracking", tint = Color(0xFF7EBCF2)) },
                    title = "Mood Tracking Session",
                    subtitle = "Tracked your mood",
                    time = timeString,
                    route = Screen.Track.route
                )
            )
        }
        
        // Add a new Mood Tracking Session if we don't have enough entries
        if (activities.isEmpty()) {
            // Add a Mood Tracking Session for today
            activities.add(
                ActivityItem(
                    icon = { Icon(Icons.Outlined.SelfImprovement, contentDescription = "Mood Tracking", tint = Color(0xFF7EBCF2)) },
                    title = "Mood Tracking Session",
                    subtitle = "Tracked your mood",
                    time = "Today, 8:30 AM",
                    route = Screen.Track.route
                )
            )
            
            // Second Mood Tracking Session removed
        } else if (activities.size == 1) {
            // Second Mood Tracking Session removed
        } else if (activities.size == 2) {
            // Third Mood Tracking Session removed
        }
        
        // Add AI chat sessions (simulated for now)
        if (activities.size < 2) {
            activities.add(
                ActivityItem(
                    icon = { Icon(Icons.Outlined.Message, contentDescription = "Chat", tint = Color(0xFFB692F6)) },
                    title = "AI Chat Session",
                    subtitle = "Chat with AI assistant",
                    time = "Today, 10:15 AM",
                    route = Screen.Chat.route
                )
            )
        }
        
        activities
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { 
                    Text(
                        text = "Neuro Cloud",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextWhite
                ),
                actions = {
                    IconButton(onClick = { 
                        scope.launch {
                            snackbarHostState.showSnackbar("Notifications")
                        }
                    }) {
                        Icon(
                            Icons.Outlined.Notifications, 
                            contentDescription = "Notifications",
                            tint = TextWhite
                        )
                    }
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(
                            Icons.Outlined.Settings, 
                            contentDescription = "Settings",
                            tint = TextWhite
                        )
                    }
                }
            )
        },
        bottomBar = { BottomNavBar(navController) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Welcome Card with animation
            AnimatedVisibility(
                visible = showWelcome,
                enter = fadeIn(animationSpec = tween(500, easing = FastOutSlowInEasing)) +
                        slideInVertically(
                            animationSpec = tween(500, easing = FastOutSlowInEasing),
                            initialOffsetY = { it / 2 }
                        )
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF2C3E50),
                                        Color(0xFF4CA1AF)
                                    )
                                )
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "$currentTime,",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextWhite.copy(alpha = 0.9f)
                                )
                                Text(
                                    text = username,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = TextWhite
                                )
                                Text(
                                    text = currentDate,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextWhite.copy(alpha = 0.7f)
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Button(
                                    onClick = { navController.navigate(Screen.Track.route) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = TextWhite
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "Track Today's Mood",
                                        color = Color(0xFF2C3E50)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            // Profile avatar placeholder - you can replace with a real avatar
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(TextWhite.copy(alpha = 0.2f))
                                    .clickable { navController.navigate(Screen.Settings.route) },
                                contentAlignment = Alignment.Center
                            ) {
                                // Observe profilePhotoUri in a way that will cause recomposition when it changes
                                val profilePhoto by remember(UserDataManager.profilePhotoUri.value) {
                                    mutableStateOf(UserDataManager.profilePhotoUri.value)
                                }
                                
                                if (profilePhoto != null) {
                                    Image(
                                        painter = rememberAsyncImagePainter(
                                            model = profilePhoto,
                                            // Add a unique key to force refresh when URI changes
                                            onLoading = { Log.d("ProfilePhoto", "Loading profile photo: $profilePhoto") },
                                            onSuccess = { Log.d("ProfilePhoto", "Profile photo loaded successfully") },
                                            onError = { Log.e("ProfilePhoto", "Error loading profile photo: ${it.result.throwable.message}") }
                                        ),
                                        contentDescription = "Profile Photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Log.d("HomeScreen", "Displaying profile photo: $profilePhoto")
                                } else {
                                    Icon(
                                        Icons.Filled.Person,
                                        contentDescription = "Profile",
                                        tint = TextWhite,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Log.d("HomeScreen", "No profile photo to display")
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Insights Section
            AnimatedVisibility(
                visible = showInsights,
                enter = fadeIn(animationSpec = tween(500, easing = FastOutSlowInEasing)) +
                        slideInVertically(
                            animationSpec = tween(500, easing = FastOutSlowInEasing),
                            initialOffsetY = { it / 2 }
                        )
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Your Insights",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextWhite
                        )
                        
                        TextButton(onClick = { navController.navigate(Screen.Report.route) }) {
                            Text(
                                text = "View Reports",
                                color = PrimaryBlue
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        insights.forEach { insight ->
                            InsightCard(
                                insightData = insight,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Recent Activities
            AnimatedVisibility(
                visible = showActivities,
                enter = fadeIn(animationSpec = tween(500, easing = FastOutSlowInEasing)) +
                        slideInVertically(
                            animationSpec = tween(500, easing = FastOutSlowInEasing),
                            initialOffsetY = { it / 2 }
                        )
            ) {
                Column {
                    Text(
                        text = "Recent Activities",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextWhite,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    recentActivities.forEach { activity ->
                        ActivityCard(
                            activity = activity,
                            onClick = { navController.navigate(activity.route) }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Quick Actions Section
            AnimatedVisibility(
                visible = showQuickActions,
                enter = fadeIn(animationSpec = tween(500, easing = FastOutSlowInEasing)) +
                        slideInVertically(
                            animationSpec = tween(500, easing = FastOutSlowInEasing),
                            initialOffsetY = { it / 2 }
                        )
            ) {
                Column {
                    Text(
                        text = "Quick Actions",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextWhite,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Track Mood Button
                        QuickActionCard(
                            icon = { 
                                Icon(
                                    Icons.Outlined.Face,
                                    contentDescription = "Track Mood",
                                    tint = Color.White
                                ) 
                            },
                            title = "Track Mood",
                            backgroundColor = HappyMoodColor,
                            onClick = { navController.navigate(Screen.Track.route) },
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Talk to AI Button
                        QuickActionCard(
                            icon = { 
                                Icon(
                                    Icons.Outlined.Chat,
                                    contentDescription = "Chat with AI",
                                    tint = Color.White
                                ) 
                            },
                            title = "Talk to AI",
                            backgroundColor = PrimaryBlue,
                            onClick = { navController.navigate(Screen.Chat.route) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // View Reports Button
                        QuickActionCard(
                            icon = { 
                                Icon(
                                    Icons.Outlined.Analytics,
                                    contentDescription = "View Reports",
                                    tint = Color.White
                                ) 
                            },
                            title = "View Reports",
                            backgroundColor = Color(0xFF6B7EFC),
                            onClick = { navController.navigate(Screen.Report.route) },
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Settings Button
                        QuickActionCard(
                            icon = { 
                                Icon(
                                    Icons.Outlined.Settings,
                                    contentDescription = "Settings",
                                    tint = Color.White
                                ) 
                            },
                            title = "Settings",
                            backgroundColor = Color(0xFF6C757D),
                            onClick = { navController.navigate(Screen.Settings.route) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Tip of the day
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = DarkSurface
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Lightbulb,
                        contentDescription = "Tip",
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    
                    Text(
                        text = "Taking 5 minutes for mindful breathing can reduce stress and improve focus.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextWhite.copy(alpha = 0.9f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(80.dp)) // Extra space at bottom for better scrolling
        }
    }
}

@Composable
fun InsightCard(insightData: InsightData, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(insightData.color),
                contentAlignment = Alignment.Center
            ) {
                insightData.icon()
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = insightData.value,
                style = MaterialTheme.typography.titleLarge,
                color = TextWhite,
                fontSize = when {
                    insightData.title == "Mood Average" || insightData.title == "Weekly Mood" -> 18.sp
                    else -> 16.sp
                },
                fontWeight = when {
                    insightData.title == "Mood Average" || insightData.title == "Weekly Mood" -> FontWeight.Bold
                    else -> FontWeight.Normal
                }
            )
            
            Text(
                text = insightData.title,
                style = MaterialTheme.typography.bodySmall,
                color = TextGray,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun ActivityCard(activity: ActivityItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(DarkBackground),
                contentAlignment = Alignment.Center
            ) {
                activity.icon()
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextWhite
                )
                
                Text(
                    text = activity.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray
                )
            }
        }
    }
}

@Composable
fun QuickActionCard(
    icon: @Composable () -> Unit,
    title: String,
    backgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(vertical = 4.dp)
            .aspectRatio(1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextWhite,
                textAlign = TextAlign.Center
            )
        }
    }
} 