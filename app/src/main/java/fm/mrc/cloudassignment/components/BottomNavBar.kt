package fm.mrc.cloudassignment.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import fm.mrc.cloudassignment.navigation.Screen
import fm.mrc.cloudassignment.ui.theme.*

@Composable
fun BottomNavBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    val items = listOf(
        Triple("Home", Icons.Default.Home, Screen.Home.route),
        Triple("Track", Icons.Default.AddCircle, Screen.Track.route),
        Triple("Chat", Icons.Default.Chat, Screen.Chat.route),
        Triple("Pro", Icons.Default.Star, Screen.ProActivities.route),
        Triple("Report", Icons.Default.Assessment, Screen.Report.route),
        Triple("Settings", Icons.Default.Settings, Screen.Settings.route)
    )
    
    NavigationBar(
        containerColor = DarkSurface
    ) {
        items.forEach { (title, icon, route) ->
            NavigationBarItem(
                icon = { Icon(icon, contentDescription = title) },
                label = { 
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    ) 
                },
                selected = currentRoute == route,
                onClick = {
                    if (currentRoute != route) {
                        navController.navigate(route) {
                            // Clear the back stack up to the home screen
                            popUpTo(Screen.Home.route) {
                                inclusive = false
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryBlue,
                    selectedTextColor = PrimaryBlue,
                    unselectedIconColor = TextGray,
                    unselectedTextColor = TextGray,
                    indicatorColor = DarkBackground
                )
            )
        }
    }
} 