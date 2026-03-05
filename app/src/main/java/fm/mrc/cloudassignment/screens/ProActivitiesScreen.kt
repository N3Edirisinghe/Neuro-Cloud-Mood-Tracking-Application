package fm.mrc.cloudassignment.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import fm.mrc.cloudassignment.components.BottomNavBar
import fm.mrc.cloudassignment.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProActivitiesScreen(navController: NavController) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Pro Activities",
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Header section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = PrimaryBlue.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Coming Soon",
                            style = MaterialTheme.typography.titleMedium,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Unlock premium activities designed to enhance your mental wellbeing. Our team of experts is crafting these features to help you on your journey to better mental health.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextWhite
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Activities section
                Text(
                    text = "Premium Activities",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextWhite,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                ProActivityItem(
                    title = "Guided Meditation",
                    description = "Daily meditation sessions with professional instructors to help reduce stress and anxiety",
                    icon = Icons.Default.SelfImprovement,
                    comingSoon = "Coming in June 2024"
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                ProActivityItem(
                    title = "Mood Journal",
                    description = "Structured journaling prompts for emotional awareness and self-reflection",
                    icon = Icons.Default.Edit,
                    comingSoon = "Coming in July 2024"
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                ProActivityItem(
                    title = "Breathing Exercises",
                    description = "Guided breathing techniques for stress relief and relaxation",
                    icon = Icons.Default.Air,
                    comingSoon = "Coming in August 2024"
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                ProActivityItem(
                    title = "Sleep Stories",
                    description = "Calming bedtime stories and meditation for better sleep quality",
                    icon = Icons.Default.Bedtime,
                    comingSoon = "Coming in September 2024"
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                ProActivityItem(
                    title = "Mood Analytics",
                    description = "Advanced insights and patterns in your mood data with AI-powered recommendations",
                    icon = Icons.Default.Analytics,
                    comingSoon = "Coming in October 2024"
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Benefits section
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
                            text = "Pro Benefits",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextWhite,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        BenefitItem(
                            icon = Icons.Default.CheckCircle,
                            text = "Access to all premium activities"
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        BenefitItem(
                            icon = Icons.Default.CheckCircle,
                            text = "Weekly and monthly mood reports"
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        BenefitItem(
                            icon = Icons.Default.CheckCircle,
                            text = "Personalized recommendations"
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        BenefitItem(
                            icon = Icons.Default.CheckCircle,
                            text = "Ad-free experience"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProActivityItem(
    title: String,
    description: String,
    icon: ImageVector,
    comingSoon: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface
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
                    icon,
                    contentDescription = title,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextWhite,
                    fontWeight = FontWeight.SemiBold
                )
                
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = comingSoon,
                    style = MaterialTheme.typography.bodySmall,
                    color = PrimaryBlue,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Icon(
                Icons.Default.Lock,
                contentDescription = "Locked",
                tint = PrimaryBlue,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun BenefitItem(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = PrimaryBlue,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextWhite
        )
    }
} 