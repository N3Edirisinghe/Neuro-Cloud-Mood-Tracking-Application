package fm.mrc.cloudassignment.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import android.net.Uri
import fm.mrc.cloudassignment.screens.*
import kotlinx.coroutines.launch
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Home : Screen("home")
    object Track : Screen("track")
    object Chat : Screen("chat")
    object Report : Screen("report")
    object Settings : Screen("settings")
    object NewUser : Screen("newuser")
    object ProActivities : Screen("pro_activities")
    object ClientStats : Screen("client_stats")
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(navController)
        }
        composable(Screen.SignUp.route) {
            SignUpScreen(navController)
        }
        composable(Screen.Home.route) {
            HomeScreen(navController)
        }
        composable(Screen.Track.route) {
            TrackScreen(navController)
        }
        composable(Screen.Chat.route) {
            ChatScreen(navController)
        }
        composable(Screen.ProActivities.route) {
            ProActivitiesScreen(navController)
        }
        composable(Screen.Report.route) {
            ReportScreen(navController)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController)
        }
        composable(Screen.ClientStats.route) {
            ClientStatsScreen(navController)
        }
    }
} 