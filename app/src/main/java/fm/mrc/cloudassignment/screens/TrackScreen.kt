package fm.mrc.cloudassignment.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import fm.mrc.cloudassignment.components.BottomNavBar
import fm.mrc.cloudassignment.navigation.Screen
import fm.mrc.cloudassignment.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import fm.mrc.cloudassignment.ui.theme.AppIcons
import android.util.Log

// Data class for mood options
data class MoodOption(
    val emoji: String,
    val label: String,
    val value: Float,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackScreen(navController: NavController) {
    // UI state
    var sliderPosition by remember { mutableStateOf(3f) }
    var notes by remember { mutableStateOf("") }
    var showPhotoDialog by remember { mutableStateOf(false) }
    var showVoiceDialog by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var hasRecording by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }
    var showUploadDialog by remember { mutableStateOf(false) }
    var uploadComplete by remember { mutableStateOf(false) }
    
    // Media state
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var voiceFilePath by remember { mutableStateOf<String?>(null) }
    
    // Animation states
    var showHeader by remember { mutableStateOf(false) }
    var showMoodSlider by remember { mutableStateOf(false) }
    var showMediaButtons by remember { mutableStateOf(false) }
    var showNotes by remember { mutableStateOf(false) }
    
    // UI utilities
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Database helper
    val databaseHelper = remember { fm.mrc.cloudassignment.data.DatabaseHelper(context) }
    
    // Scroll state
    val scrollState = rememberScrollState()
    
    // Mood options
    val moodOptions = remember {
        listOf(
            MoodOption("😢", "Very Sad", 1f, SadMoodColor),
            MoodOption("😕", "Sad", 2f, SadMoodColor.copy(alpha = 0.8f)),
            MoodOption("😐", "Neutral", 3f, NeutralMoodColor),
            MoodOption("🙂", "Happy", 4f, HappyMoodColor.copy(alpha = 0.8f)),
            MoodOption("😄", "Very Happy", 5f, HappyMoodColor)
        )
    }
    
    // Current date
    val currentDate = remember {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
    }
    
    // Start animations sequentially
    LaunchedEffect(Unit) {
        showHeader = true
        delay(200)
        showMoodSlider = true
        delay(200)
        showMediaButtons = true
        delay(200)
        showNotes = true
    }
    
    // Helper function to show snackbar messages
    fun showSnackbar(message: String) {
        scope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }
    
    // Media recorder and player
    val mediaRecorder = remember { mutableStateOf<MediaRecorder?>(null) }
    val mediaPlayer = remember { mutableStateOf<MediaPlayer?>(null) }
    
    // File to store audio recording
    val audioFile = remember {
        File(context.cacheDir, "audio_recording.mp3").apply {
            createNewFile()
            deleteOnExit()
        }
    }
    
    // Camera functionality
    var hasPhoto by remember { mutableStateOf(false) }
    
    // Function to stop recording audio
    fun stopRecording() {
        try {
            mediaRecorder.value?.apply {
                stop()
                release()
            }
            mediaRecorder.value = null
            
            isRecording = false
            hasRecording = true
            showSnackbar("Recording saved - ready to upload!")
        } catch (e: Exception) {
            e.printStackTrace()
            showSnackbar("Failed to save recording: ${e.message}")
        }
    }
    
    // Check if we have permissions for storage
    val hasPermissions = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    // Image picker for selecting photos
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            photoUri = uri
            hasPhoto = true
            scope.launch {
                snackbarHostState.showSnackbar("Photo selected - ready to upload!")
            }
        }
    }
    
    // Audio permission launcher
    val requestAudioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, start recording
            val recorder = mediaRecorder.value ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            
            try {
                recorder.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setOutputFile(audioFile.absolutePath)
                    prepare()
                    start()
                }
                mediaRecorder.value = recorder
                isRecording = true
                showVoiceDialog = true
            } catch (e: Exception) {
                e.printStackTrace()
                scope.launch {
                    snackbarHostState.showSnackbar("Failed to start recording: ${e.message}")
                }
            }
        } else {
            // Permission denied, show message
            scope.launch {
                snackbarHostState.showSnackbar("Microphone permission required to record audio")
            }
        }
    }
    
    // Create temporary file for photo
    val photoFile = remember { File.createTempFile("captured_image", ".jpg", context.cacheDir) }
    val imageUri = remember {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
    }
    
    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoUri = imageUri
            hasPhoto = true
            showSnackbar("Photo captured - ready to upload!")
        } else {
            showSnackbar("Failed to capture photo")
        }
    }
    
    // Track which permission is currently being requested
    val currentPermissionRequest = remember { mutableStateOf("") }
    
    // Camera permission launcher
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            hasPermissions.value = true
            // If camera permission was requested, launch camera
            if (currentPermissionRequest.value == Manifest.permission.CAMERA) {
                cameraLauncher.launch(imageUri)
            } 
            // If audio permission was requested, start recording
            else if (currentPermissionRequest.value == Manifest.permission.RECORD_AUDIO) {
                // Inline implementation of recording start
                try {
                    // Create temporary file for recording
                    val audioFile = File.createTempFile("audio_recording", ".3gp", context.cacheDir)
                    val audioFilePath = audioFile.absolutePath
                    
                    // Configure MediaRecorder
                    mediaRecorder.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        MediaRecorder(context)
                    } else {
                        @Suppress("DEPRECATION")
                        MediaRecorder()
                    }
                    
                    mediaRecorder.value?.apply {
                        setAudioSource(MediaRecorder.AudioSource.MIC)
                        setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                        setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                        setOutputFile(audioFilePath)
                        prepare()
                        start()
                    }
                    
                    isRecording = true
                    showSnackbar("Recording started")
                } catch (e: Exception) {
                    e.printStackTrace()
                    showSnackbar("Failed to start recording: ${e.message}")
                }
            }
        } else {
            hasPermissions.value = false
            showSnackbar("Permission denied. Please grant permission to continue.")
        }
    }
    
    // Function to start audio playback
    fun startPlayback(playerState: MutableState<MediaPlayer?>, filePath: String) {
        try {
            // Release previous player if any
            playerState.value?.apply {
                if (isPlaying) {
                    try {
                        stop()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                release()
            }
            
            // Create a new player
            playerState.value = MediaPlayer().apply {
                setDataSource(filePath)
                setOnCompletionListener {
                    try {
                        release()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    playerState.value = null
                    isPlaying = false
                }
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            playerState.value?.apply {
                try {
                    release()
                } catch (releaseException: Exception) {
                    releaseException.printStackTrace()
                }
            }
            playerState.value = null
            isPlaying = false
        }
    }
    
    // Function to stop audio playback
    fun stopPlayback(playerState: MutableState<MediaPlayer?>) {
        try {
            playerState.value?.apply {
                if (isPlaying) {
                    try {
                        stop()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            playerState.value = null
        }
    }
    
    // Function to simulate uploading files
    fun uploadFiles() {
        // Check if we have any media to upload
        if (!hasPhoto && !hasRecording) {
            showSnackbar("Please capture a photo or record audio first")
            return
        }
        
        // Start upload process
        isUploading = true
        uploadProgress = 0f
        showUploadDialog = true
        
        // Log what's being uploaded for debugging
        val uploadingPhoto = hasPhoto && photoUri != null
        val uploadingVoice = hasRecording
        
        // Show details in snackbar
        showSnackbar(
            "Uploading: " + 
            (if (uploadingPhoto) "Photo" else "") +
            (if (uploadingPhoto && uploadingVoice) " & " else "") +
            (if (uploadingVoice) "Voice" else "")
        )
        
        // Simulate upload with increasing progress
        scope.launch {
            repeat(10) {
                delay(500)  // Simulate network delay
                uploadProgress = (it + 1) / 10f
            }
            // Upload complete
            isUploading = false
            uploadComplete = true
            delay(1000)  // Show 100% for a moment
            showUploadDialog = false
            
            // Show success message
            showSnackbar("Media uploaded successfully to Neuro Cloud!")
            
            // Navigate to home screen
            delay(500)
            navController.navigate(Screen.Home.route)
        }
    }

    // Effect to clean up media resources when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            mediaRecorder.value?.apply {
                if (isRecording) {
                    try {
                        stop()
                        release()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                mediaRecorder.value = null
            }
            
            mediaPlayer.value?.apply {
                if (isPlaying) {
                    try {
                        stop()
                        release()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                mediaPlayer.value = null
            }
        }
    }

    // Main UI
    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { 
                    Text(
                        "Track Your Mood",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextWhite
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            AppIcons.Back,
                            contentDescription = "Go back",
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
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // Date Header
            AnimatedVisibility(
                visible = showHeader,
                enter = fadeIn(animationSpec = tween(500, easing = FastOutSlowInEasing)) +
                        slideInVertically(
                            animationSpec = tween(500, easing = FastOutSlowInEasing),
                            initialOffsetY = { it / 2 }
                        )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentDate,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextWhite.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    
                    Text(
                        text = "How are you feeling today?",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextWhite,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                }
            }
            
            // Show photo if captured
            if (hasPhoto && photoUri != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 4.dp
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(photoUri),
                            contentDescription = "Captured photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        
                        // Overlay with delete button
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f))
                                .padding(8.dp),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            IconButton(
                                onClick = { 
                                    photoUri = null
                                    hasPhoto = false
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f))
                            ) {
                                Icon(
                                    AppIcons.Close,
                                    contentDescription = "Remove photo",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Show audio player if recording exists
            if (hasRecording) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = DarkSurface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (isPlaying) AppIcons.PauseCircle else AppIcons.PlayCircle,
                                    contentDescription = if (isPlaying) "Stop playback" else "Play recording",
                                    tint = Color.White
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column {
                                Text(
                                    text = "Voice Recording",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextWhite
                                )
                                
                                Text(
                                    text = if (isPlaying) "Playing..." else "Ready to play",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextGray
                                )
                            }
                        }
                        
                        IconButton(
                            onClick = {
                                if (isPlaying) {
                                    stopPlayback(mediaPlayer)
                                    isPlaying = false
                                } else {
                                    startPlayback(mediaPlayer, audioFile.absolutePath)
                                    isPlaying = true
                                }
                            }
                        ) {
                            Icon(
                                if (isPlaying) Icons.Filled.PauseCircle else Icons.Filled.PlayCircle,
                                contentDescription = if (isPlaying) "Stop playback" else "Play recording",
                                tint = if (isPlaying) Color(0xFFFF5252) else PrimaryBlue,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }

            // Mood Slider Section
            AnimatedVisibility(
                visible = showMoodSlider,
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
                        containerColor = DarkSurface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Current selected mood
                        val currentMood = moodOptions.find { it.value == sliderPosition.roundToInt().toFloat() }
                            ?: moodOptions[2] // Default to neutral
                        
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(
                                    color = currentMood.color.copy(alpha = 0.15f),
                                    shape = CircleShape
                                )
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentMood.emoji,
                                style = MaterialTheme.typography.displayMedium
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = currentMood.label,
                            style = MaterialTheme.typography.titleMedium,
                            color = currentMood.color,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        // Slider
                        Slider(
                            value = sliderPosition,
                            onValueChange = { sliderPosition = it },
                            valueRange = 1f..5f,
                            steps = 3,
                            colors = SliderDefaults.colors(
                                thumbColor = currentMood.color,
                                activeTrackColor = currentMood.color,
                                inactiveTrackColor = DarkBackground
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                        )
                        
                        // Emoji indicators
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            moodOptions.forEach { mood ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = mood.emoji,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.graphicsLayer(alpha =
                                            if (mood.value == sliderPosition.roundToInt().toFloat()) 1f else 0.5f
                                        )
                                    )
                                    
                                    if (mood.value == sliderPosition.roundToInt().toFloat()) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .background(mood.color, CircleShape)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Media capture buttons
            AnimatedVisibility(
                visible = showMediaButtons,
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
                        containerColor = DarkSurface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Capture Media",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextWhite,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Camera button
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .shadow(4.dp, CircleShape)
                                        .clip(CircleShape)
                                        .background(PrimaryBlue)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = rememberRipple(bounded = false, color = Color.White)
                                        ) {
                                            // For camera, always check actual CAMERA permission
                                            if (ContextCompat.checkSelfPermission(
                                                    context,
                                                    Manifest.permission.CAMERA
                                                ) == PackageManager.PERMISSION_GRANTED) {
                                                // Permission already granted, launch camera
                                                cameraLauncher.launch(imageUri)
                                            } else {
                                                // Request camera permission
                                                currentPermissionRequest.value = Manifest.permission.CAMERA
                                                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                                            }
                                        }
                                        .padding(18.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        AppIcons.Camera,
                                        contentDescription = "Take a photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = "Take Photo",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextWhite
                                )
                            }
                            
                            // Audio recording button
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .shadow(4.dp, CircleShape)
                                        .clip(CircleShape)
                                        .background(if (isRecording) Color.Red else PrimaryBlue)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = rememberRipple(bounded = false, color = Color.White)
                                        ) {
                                            if (ContextCompat.checkSelfPermission(
                                                    context,
                                                    Manifest.permission.RECORD_AUDIO
                                                ) == PackageManager.PERMISSION_GRANTED) {
                                                if (!isRecording) {
                                                    // Inline implementation of recording start
                                                    try {
                                                        // Create temporary file for recording
                                                        val audioFile = File.createTempFile("audio_recording", ".3gp", context.cacheDir)
                                                        val audioFilePath = audioFile.absolutePath
                                                        
                                                        // Configure MediaRecorder
                                                        mediaRecorder.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                            MediaRecorder(context)
                                                        } else {
                                                            @Suppress("DEPRECATION")
                                                            MediaRecorder()
                                                        }
                                                        
                                                        mediaRecorder.value?.apply {
                                                            setAudioSource(MediaRecorder.AudioSource.MIC)
                                                            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                                                            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                                                            setOutputFile(audioFilePath)
                                                            prepare()
                                                            start()
                                                        }
                                                        
                                                        isRecording = true
                                                        showSnackbar("Recording started")
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                        showSnackbar("Failed to start recording: ${e.message}")
                                                    }
                                                } else {
                                                    stopRecording()
                                                }
                                            } else {
                                                currentPermissionRequest.value = Manifest.permission.RECORD_AUDIO
                                                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            }
                                        }
                                        .padding(18.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (isRecording) AppIcons.Stop else AppIcons.Mic,
                                        contentDescription = if (isRecording) "Stop recording" else "Record audio",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = if (isRecording) "Stop Recording" else "Record Voice",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextWhite
                                )
                            }
                        }
                    }
                }
            }
            
            // Upload section - separated from media capture for clarity
            AnimatedVisibility(
                visible = showMediaButtons && (hasPhoto || hasRecording),
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
                    colors = CardDefaults.cardColors(
                        containerColor = PrimaryBlue.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryBlue.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    AppIcons.CheckCircle,
                                    contentDescription = "Media ready",
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Ready to Upload",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextWhite
                                )
                                
                                Text(
                                    text = if (hasPhoto && hasRecording) "Photo & Voice recording"
                                          else if (hasPhoto) "Photo only"
                                          else "Voice recording only",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextWhite.copy(alpha = 0.7f)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = { 
                                // Get current user ID or use default
                                val userId = fm.mrc.cloudassignment.auth.AuthManager.auth?.currentUser?.uid?.hashCode()?.toLong() ?: 1L
                                
                                // Save mood with media info
                                val moodScore = sliderPosition
                                val hasPhoto = photoUri != null
                                val hasVoice = voiceFilePath != null
                                
                                // Save to database
                                scope.launch {
                                    try {
                                        isUploading = true
                                        showUploadDialog = true
                                        
                                        // Simulate upload progress
                                        for (i in 1..10) {
                                            uploadProgress = i / 10f
                                            delay(100)
                                        }
                                        
                                        // Save to database
                                        databaseHelper.addMoodEntry(
                                            userId = userId,
                                            moodScore = moodScore,
                                            notes = notes,
                                            hasPhoto = hasPhoto,
                                            hasVoice = hasVoice,
                                            photoPath = photoUri?.toString(),
                                            voicePath = voiceFilePath
                                        )
                                        
                                        uploadComplete = true
                                        delay(500)
                                        
                                        showSnackbar("Mood saved successfully!")
                                        showUploadDialog = false
                                        
                                        delay(500)
                                        navController.navigate(Screen.Home.route)
                                    } catch (e: Exception) {
                                        Log.e("TrackScreen", "Failed to save mood entry: ${e.message}")
                                        showSnackbar("Failed to save mood entry")
                                        showUploadDialog = false
                                        isUploading = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryBlue
                            ),
                            shape = RoundedCornerShape(8.dp),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 4.dp
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    AppIcons.CloudUpload,
                                    contentDescription = "Upload",
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    "Upload to Cloud",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Notes field
            AnimatedVisibility(
                visible = showNotes,
                enter = fadeIn(animationSpec = tween(500, easing = FastOutSlowInEasing)) +
                        slideInVertically(
                            animationSpec = tween(500, easing = FastOutSlowInEasing),
                            initialOffsetY = { it / 2 }
                        )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Add Notes",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextWhite,
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                    )
                    
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        placeholder = { Text("How was your day?", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = TextGray,
                            cursorColor = PrimaryBlue,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        maxLines = 6,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save button
            Button(
                onClick = { 
                    // Get current user ID or use default
                    val userId = fm.mrc.cloudassignment.auth.AuthManager.auth?.currentUser?.uid?.hashCode()?.toLong() ?: 1L
                    
                    // Save mood with media info
                    val moodScore = sliderPosition
                    val hasPhoto = photoUri != null
                    val hasVoice = voiceFilePath != null
                    
                    // Save to database
                    scope.launch {
                        try {
                            isUploading = true
                            showUploadDialog = true
                            
                            // Simulate upload progress
                            for (i in 1..10) {
                                uploadProgress = i / 10f
                                delay(100)
                            }
                            
                            // Save to database
                            databaseHelper.addMoodEntry(
                                userId = userId,
                                moodScore = moodScore,
                                notes = notes,
                                hasPhoto = hasPhoto,
                                hasVoice = hasVoice,
                                photoPath = photoUri?.toString(),
                                voicePath = voiceFilePath
                            )
                            
                            uploadComplete = true
                            delay(500)
                            
                            showSnackbar("Mood saved successfully!")
                            showUploadDialog = false
                            
                            delay(500)
                            navController.navigate(Screen.Home.route)
                        } catch (e: Exception) {
                            Log.e("TrackScreen", "Failed to save mood entry: ${e.message}")
                            showSnackbar("Failed to save mood entry")
                            showUploadDialog = false
                            isUploading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 80.dp)
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "Save",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        "Save Mood",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
    
    // Voice Recording Dialog
    if (showVoiceDialog) {
        Dialog(
            onDismissRequest = { 
                // Don't immediately dismiss while recording
                if (!isRecording || 
                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) 
                    != PackageManager.PERMISSION_GRANTED) {
                    showVoiceDialog = false
                }
            }
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = DarkSurface
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Recording Voice",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextWhite
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Animated recording indicator
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                if (isRecording) 
                                    Color.Red.copy(alpha = 0.2f) 
                                else 
                                    PrimaryBlue.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isRecording) AppIcons.Mic else AppIcons.MicOff,
                            contentDescription = if (isRecording) "Recording" else "Not recording",
                            tint = if (isRecording) Color.Red else PrimaryBlue,
                            modifier = Modifier.size(40.dp)
                        )
                        
                        // Pulsating animation when recording
                        if (isRecording) {
                            val infiniteTransition = rememberInfiniteTransition()
                            val scale by infiniteTransition.animateFloat(
                                initialValue = 1f,
                                targetValue = 1.2f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                )
                            )
                            
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        alpha = 2f - scale
                                    }
                                    .clip(CircleShape)
                                    .border(2.dp, Color.Red.copy(alpha = 0.5f), CircleShape)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = if (isRecording) "Voice recording in progress..." else "Preparing to record...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextWhite
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (isRecording) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = PrimaryBlue
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (isRecording) {
                                    try {
                                        stopRecording()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    isRecording = false
                                }
                                showVoiceDialog = false
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = TextGray
                            ),
                            border = BorderStroke(1.dp, TextGray)
                        ) {
                            Text("Cancel")
                        }
                        
                        Button(
                            onClick = { 
                                if (isRecording) {
                                    try {
                                        stopRecording()
                                        showVoiceDialog = false
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        showSnackbar("Failed to save recording")
                                        isRecording = false
                                        showVoiceDialog = false
                                    }
                                }
                            },
                            enabled = isRecording,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryBlue,
                                disabledContainerColor = PrimaryBlue.copy(alpha = 0.5f)
                            )
                        ) {
                            Text("Stop Recording")
                        }
                    }
                }
            }
        }
    }
    
    // Upload dialog
    if (showUploadDialog) {
        Dialog(
            onDismissRequest = { 
                if (!isUploading) showUploadDialog = false 
            }
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = DarkSurface
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Upload Media",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextWhite
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Upload status icon
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                if (uploadComplete) 
                                    HappyMoodColor.copy(alpha = 0.2f) 
                                else 
                                    PrimaryBlue.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (uploadComplete) AppIcons.CheckCircle else AppIcons.CloudUpload,
                            contentDescription = if (uploadComplete) "Upload complete" else "Uploading",
                            tint = if (uploadComplete) HappyMoodColor else PrimaryBlue,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = if (uploadComplete) 
                                "Upload complete!" 
                              else if (isUploading) 
                                "Uploading to Neuro Cloud..." 
                              else 
                                "Preparing to upload...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextWhite
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    LinearProgressIndicator(
                        progress = { uploadProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = PrimaryBlue
                    )
                    
                    Text(
                        text = "${(uploadProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            if (!isUploading) {
                                if (uploadComplete) {
                                    showUploadDialog = false
                                    navController.navigate(Screen.Home.route)
                                } else {
                                    uploadFiles()
                                }
                            }
                        },
                        enabled = !isUploading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue,
                            disabledContainerColor = PrimaryBlue.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (uploadComplete) "Return to Home" else "Upload",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
} 