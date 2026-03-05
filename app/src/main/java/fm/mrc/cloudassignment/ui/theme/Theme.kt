package fm.mrc.cloudassignment.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import fm.mrc.cloudassignment.screens.ThemeManager

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = TextWhite,
    primaryContainer = PrimaryVariant,
    onPrimaryContainer = TextWhite,
    
    secondary = SecondaryTeal,
    onSecondary = TextWhite,
    secondaryContainer = SecondaryVariant,
    onSecondaryContainer = TextWhite,
    
    tertiary = NeutralMoodColor,
    onTertiary = DarkBackground,
    
    background = DarkBackground,
    onBackground = TextWhite,
    
    surface = DarkSurface,
    onSurface = TextWhite,
    surfaceVariant = MoodCardBg,
    onSurfaceVariant = TextGray,
    
    error = ErrorRed,
    onError = TextWhite
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = PrimaryVariant,
    onPrimaryContainer = Color.White,
    
    secondary = SecondaryTeal,
    onSecondary = Color.White,
    secondaryContainer = SecondaryVariant,
    onSecondaryContainer = Color.White,
    
    tertiary = NeutralMoodColor,
    onTertiary = Color(0xFF303030),
    
    background = Color(0xFFF0F2F5),
    onBackground = Color(0xFF303030),
    
    surface = Color.White,
    onSurface = Color(0xFF303030),
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF707070),
    
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun CloudAssignmentTheme(
    darkTheme: Boolean = ThemeManager.isDarkMode.value,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = if (darkTheme) DarkSurface.toArgb() else PrimaryBlue.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun NeuroCloudTheme(
    content: @Composable () -> Unit
) {
    // Use the ThemeManager's isDarkMode state to control theme
    val darkTheme = ThemeManager.isDarkMode.value
    
    // Select color scheme based on dark mode setting
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val view = LocalView.current
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Set status bar color based on theme
            window.statusBarColor = if (darkTheme) DarkSurface.toArgb() else PrimaryBlue.toArgb()
            // Set status bar icons to light if using light theme
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}