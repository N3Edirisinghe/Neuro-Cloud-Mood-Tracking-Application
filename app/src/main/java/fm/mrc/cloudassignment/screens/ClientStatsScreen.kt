package fm.mrc.cloudassignment.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import fm.mrc.cloudassignment.components.BottomNavBar
import fm.mrc.cloudassignment.data.ClientStats
import fm.mrc.cloudassignment.data.DatabaseHelper
import fm.mrc.cloudassignment.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientStatsScreen(navController: NavController) {
    val context = LocalContext.current
    val databaseHelper = remember { DatabaseHelper(context) }
    var clientStats by remember { mutableStateOf<List<ClientStats>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        clientStats = databaseHelper.getAllClientsStats()
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Client Statistics",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DarkSurface
                )
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
            if (clientStats.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No client activity data available",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextGray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(clientStats) { stats ->
                        ClientStatsCard(stats)
                    }
                }
            }
        }
    }
}

@Composable
private fun ClientStatsCard(stats: ClientStats) {
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
            // Client info header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stats.userName,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Text(
                        text = stats.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                }
                
                Text(
                    text = "Last active: ${formatDate(stats.lastActive)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = PrimaryBlue
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Activity statistics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn(
                    value = stats.totalActivities.toString(),
                    label = "Total\nActivities",
                    icon = Icons.Default.Assessment
                )
                
                StatColumn(
                    value = stats.moodUpdates.toString(),
                    label = "Mood\nUpdates",
                    icon = Icons.Default.AddCircle
                )
                
                StatColumn(
                    value = stats.chatSessions.toString(),
                    label = "Chat\nSessions",
                    icon = Icons.Default.Chat
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Media statistics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn(
                    value = stats.photoUploads.toString(),
                    label = "Photos\nUploaded",
                    icon = Icons.Default.PhotoCamera
                )
                
                StatColumn(
                    value = stats.voiceRecordings.toString(),
                    label = "Voice\nRecordings",
                    icon = Icons.Default.Mic
                )
                
                StatColumn(
                    value = String.format("%.1f", stats.averageMood),
                    label = "Average\nMood",
                    icon = Icons.Default.Mood
                )
            }
        }
    }
}

@Composable
private fun StatColumn(
    value: String,
    label: String,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = PrimaryBlue,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = TextWhite,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextGray,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return formatter.format(date)
} 