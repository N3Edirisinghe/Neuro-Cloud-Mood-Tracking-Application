package fm.mrc.cloudassignment.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import fm.mrc.cloudassignment.components.BottomNavBar
import fm.mrc.cloudassignment.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val message: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isAnimated: Boolean = true
)

enum class ConversationTopic {
    GREETING,
    MOOD,
    ACTIVITY,
    SLEEP,
    MEDICATION,
    THERAPY,
    GENERAL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavController) {
    // State for messages
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var currentMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    
    // Conversation state tracking
    var currentTopic by remember { mutableStateOf(ConversationTopic.GREETING) }
    var conversationStage by remember { mutableStateOf(0) }
    var userName by remember { mutableStateOf("") }
    var userMood by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }
    var isSendButtonActive by remember { mutableStateOf(false) }
    var isInputFieldFocused by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var isCheckingInactivity by remember { mutableStateOf(true) }
    
    // Debug - Test message to verify bot is responding
    val testBot = {
        messages = messages + ChatMessage(message = "TEST: The chatbot is active and functioning. How can I help you?", isUser = false)
    }
    
    // Function to check if a message ends with a question
    val endsWithQuestion = { message: String ->
        message.trim().endsWith("?") || 
        message.lowercase().contains(" what ") ||
        message.lowercase().contains(" how ") ||
        message.lowercase().contains(" why ") ||
        message.lowercase().contains(" would you ") ||
        message.lowercase().contains(" do you ") ||
        message.lowercase().contains(" have you ") ||
        message.lowercase().contains(" can you ")
    }
    
    // Function to generate follow-up questions if the bot message doesn't already end with one
    val ensureQuestionEnding = { botMessage: String, topic: ConversationTopic ->
        if (endsWithQuestion(botMessage)) {
            botMessage
        } else {
            // Add a follow-up question based on the topic
            botMessage + when (topic) {
                ConversationTopic.GREETING -> " How are you feeling today?"
                ConversationTopic.MOOD -> " Would you like to discuss this further?"
                ConversationTopic.ACTIVITY -> " Have you tried any of these approaches recently?"
                ConversationTopic.SLEEP -> " How has your sleep been lately?"
                ConversationTopic.MEDICATION -> " Have you discussed this with your doctor?"
                ConversationTopic.THERAPY -> " Would you like to know more about therapy options?"
                ConversationTopic.GENERAL -> " Is there a specific aspect of this you'd like to explore?"
            }
        }
    }
    
    // Function to send a bot message
    val sendBotMessage = { message: String ->
        // Ensure message ends with a question to maintain conversation
        val finalMessage = ensureQuestionEnding(message, currentTopic)
        
        messages = messages + ChatMessage(
            message = finalMessage,
            isUser = false
        )
        if (messages.isNotEmpty()) {
            scope.launch {
                lazyListState.animateScrollToItem(messages.size - 1)
            }
        }
    }
    
    // Process the message and generate response with enhanced continuity
    val processMessageWithContinuity = { userMessage: String ->
        // Extract important keywords for more relevant responses
        val keywords = extractKeywords(userMessage.lowercase())
        
        // Process the message and generate response
        val (nextTopic, nextStage, initialBotResponse) = processUserMessage(
            userMessage,
            currentTopic,
            conversationStage,
            userName,
            userMood,
            keywords
        )
        
        // Ensure the response ends with a question to maintain conversation
        val finalBotResponse = ensureQuestionEnding(initialBotResponse, nextTopic)
        
        Triple(nextTopic, nextStage, finalBotResponse)
    }
    
    // Function to send user message and get bot response
    val sendUserMessageAndGetResponse = { userText: String ->
        if (userText.isNotBlank()) {
            // Add user message to chat
            messages = messages + ChatMessage(message = userText, isUser = true)
            
            // Show typing indicator
            isTyping = true
            
            // Update interaction time
            lastInteractionTime = System.currentTimeMillis()
            
            // Process in coroutine to not block UI
            scope.launch {
                // Short delay for typing effect
                delay(500)
                
                // Get response
                val (newTopic, newStage, response) = processMessageWithContinuity(userText)
                
                // Update state
                currentTopic = newTopic
                conversationStage = newStage
                
                // Handle name extraction
                if (newTopic == ConversationTopic.GREETING && newStage == 1) {
                    val name = extractName(userText)
                    if (name.isNotEmpty()) {
                        userName = name
                    }
                } else if (newTopic == ConversationTopic.MOOD && newStage == 1) {
                    userMood = userText
                }
                
                // Update chat with bot response
                isTyping = false
                messages = messages + ChatMessage(message = response, isUser = false)
                
                // Scroll to see latest message
                lazyListState.animateScrollToItem(messages.size - 1)
            }
        }
    }
    
    // Initialize the conversation with greeting
    LaunchedEffect(Unit) {
        delay(500)
        sendBotMessage("Hi there! I'm your mental health assistant. How are you feeling today?")
        delay(2000)
        testBot() // Add test message to verify bot is working
        lastInteractionTime = System.currentTimeMillis()
    }
    
    // Animation states
    val contentPaddingAnimate by animateDpAsState(
        targetValue = if (isInputFieldFocused) 8.dp else 16.dp,
        animationSpec = tween(durationMillis = 300),
        label = "content padding animation"
    )
    
    // Suggested responses
    val suggestedResponses = remember { mutableStateOf(listOf<String>()) }
    
    // Check for inactivity and prompt user
    LaunchedEffect(isCheckingInactivity) {
        while (isCheckingInactivity) {
            delay(60000) // Check every minute
            val currentTime = System.currentTimeMillis()
            val inactiveTime = currentTime - lastInteractionTime
            
            if (inactiveTime > 120000 && !isTyping && messages.isNotEmpty() && messages.last().isUser) { // 2 minutes of inactivity
                isTyping = true
                delay(1000)
                
                val followUpMessage = when (currentTopic) {
                    ConversationTopic.GREETING -> "Are you still there? How are you feeling today?"
                    ConversationTopic.MOOD -> "I noticed you've been quiet. Would you like to explore some mood-improving activities?"
                    ConversationTopic.ACTIVITY -> "Is there a specific activity you'd like to discuss that might help your mental wellbeing?"
                    ConversationTopic.SLEEP -> "Sleep is so important for mental health. Did you want to discuss your sleep patterns more?"
                    ConversationTopic.MEDICATION -> "Just checking in. Would you like to talk more about maintaining your medication routine?"
                    ConversationTopic.THERAPY -> "I'm here to support you. Any aspects of therapy you'd like to explore further?"
                    ConversationTopic.GENERAL -> "I'm still here if you'd like to continue our conversation. Is there anything on your mind?"
                }
                
                isTyping = false
                sendBotMessage(followUpMessage)
                lastInteractionTime = System.currentTimeMillis()
            }
        }
    }
    
    // Generate suggestions based on conversation state
    LaunchedEffect(currentTopic, conversationStage, messages) {
        suggestedResponses.value = when (currentTopic) {
            ConversationTopic.GREETING -> listOf(
                "Hi there!", 
                "Hello", 
                "I'm feeling good",
                "How can you help me?",
                "What can you do?"
            )
            ConversationTopic.MOOD -> when (conversationStage) {
                0 -> listOf(
                    "I'm feeling happy", 
                    "Feeling a bit down", 
                    "Anxious today", 
                    "Just tired",
                    "I feel overwhelmed",
                    "I'm feeling angry"
                )
                else -> listOf(
                    "Yes, please", 
                    "Tell me more", 
                    "That helps, thanks", 
                    "I'd prefer not to",
                    "Why do you ask?",
                    "How does that work?"
                )
            }
            ConversationTopic.ACTIVITY -> listOf(
                "I enjoy walking", 
                "I like reading", 
                "Music helps me", 
                "Meditation works for me",
                "How do I start meditating?",
                "What activities help with anxiety?",
                "Tell me about breathing exercises"
            )
            ConversationTopic.SLEEP -> listOf(
                "Sleeping well", 
                "Having trouble sleeping", 
                "Need better sleep habits",
                "I have nightmares",
                "How can I fall asleep faster?",
                "What affects sleep quality?"
            )
            ConversationTopic.MEDICATION -> listOf(
                "I take medication",
                "Not on medication",
                "How do medications help?",
                "Are there side effects?",
                "Should I consider medication?"
            )
            ConversationTopic.THERAPY -> listOf(
                "I'm in therapy",
                "Considering therapy",
                "How do I find a therapist?",
                "What types of therapy exist?",
                "Is therapy effective?"
            )
            else -> listOf(
                "Tell me more", 
                "That's helpful", 
                "I'd like to try that", 
                "Thanks for listening",
                "What is mental health?",
                "How to improve my mood?",
                "Can you help with stress?"
            )
        }
    }
    
    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(messages.size - 1)
        }
    }
    
    // Clean up
    DisposableEffect(Unit) {
        onDispose {
            isCheckingInactivity = false
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            PrimaryBlue,
                                            PrimaryBlue.copy(alpha = 0.7f)
                                        )
                                    )
                                )
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Psychology,
                                contentDescription = "Assistant",
                                tint = Color.White
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column {
                            Text(
                                "Neuro Cloud Assistant",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = TextWhite
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.Green)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Online",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextWhite.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DarkSurface
                ),
                actions = {
                    IconButton(onClick = { /* Info action */ }) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = "Information",
                            tint = TextWhite
                        )
                    }
                },
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
        bottomBar = { BottomNavBar(navController) },
        containerColor = DarkBackground
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DarkBackground,
                            DarkBackground.copy(alpha = 0.95f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Chat messages
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = contentPaddingAnimate, vertical = 8.dp),
                        state = lazyListState,
                        reverseLayout = false,
                        contentPadding = PaddingValues(bottom = if (suggestedResponses.value.isNotEmpty()) 68.dp else 8.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Chat date header
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = DarkSurface.copy(alpha = 0.7f)
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "Today",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextGray,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        
                        items(messages) { message ->
                            ChatBubble(message)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                    
                    // Suggested responses
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = !isInputFieldFocused && suggestedResponses.value.isNotEmpty() && !isTyping,
                            enter = fadeIn() + slideInVertically { it },
                            exit = fadeOut() + slideOutVertically { it },
                            label = "suggested responses visibility"
                        ) {
                            SuggestedResponses(
                                responses = suggestedResponses.value,
                                onResponseSelected = { response ->
                                    sendUserMessageAndGetResponse(response)
                                }
                            )
                        }
                    }
                    
                    // Typing indicator
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isTyping,
                            enter = fadeIn() + slideInVertically { it / 2 },
                            exit = fadeOut() + slideOutVertically { it / 2 },
                            label = "typing indicator visibility"
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = DarkSurface
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Assistant is typing",
                                        color = TextGray
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    TypingAnimation()
                                }
                            }
                        }
                    }
                }

                // Message input
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .shadow(elevation = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = DarkSurface
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Message field
                        TextField(
                            value = currentMessage,
                            onValueChange = { 
                                currentMessage = it
                                isSendButtonActive = it.isNotBlank()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { 
                                    isInputFieldFocused = it.isFocused 
                                },
                            placeholder = { 
                                Text(
                                    "Message your assistant...", 
                                    color = TextGray
                                ) 
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface,
                                disabledContainerColor = DarkSurface,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                                cursorColor = PrimaryBlue,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            maxLines = 3,
                            textStyle = MaterialTheme.typography.bodyLarge
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Send button with animation
                        IconButton(
                            onClick = { 
                                sendUserMessageAndGetResponse(currentMessage.trim())
                                currentMessage = ""
                                isSendButtonActive = false 
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSendButtonActive) PrimaryBlue else DarkSurface,
                                )
                                .padding(4.dp),
                            enabled = isSendButtonActive
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "Send",
                                tint = if (isSendButtonActive) TextWhite else TextGray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestedResponses(
    responses: List<String>,
    onResponseSelected: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        responses.forEach { response ->
            SuggestionChip(
                onClick = { onResponseSelected(response) },
                label = { Text(response) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = DarkSurface,
                    labelColor = TextWhite
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = PrimaryBlue.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
private fun TypingAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing dots")
    val dotSize by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot size"
    )
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(4.dp)
    ) {
        for (i in 0..2) {
            val delay = i * 150
            val dotAlpha by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = delay, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot alpha $i"
            )
            
            Box(
                modifier = Modifier
                    .size(dotSize.dp)
                    .clip(CircleShape)
                    .background(TextGray.copy(alpha = dotAlpha))
            )
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) { 
        timeFormatter.format(Date(message.timestamp)) 
    }
    
    // Animation for new messages
    val animatedScale = remember { Animatable(if (message.isAnimated) 0.8f else 1f) }
    val animatedAlpha = remember { Animatable(if (message.isAnimated) 0f else 1f) }
    
    LaunchedEffect(Unit) {
        if (message.isAnimated) {
            animatedScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(200, easing = FastOutSlowInEasing)
            )
            animatedAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(150)
            )
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(
                scaleX = animatedScale.value,
                scaleY = animatedScale.value,
                alpha = animatedAlpha.value
            ),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        // Sender name for assistant messages
        if (!message.isUser) {
            Text(
                text = "Assistant",
                style = MaterialTheme.typography.bodySmall,
                color = PrimaryBlue,
                modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
            )
        }
        
        // Message bubble
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser) PrimaryBlue else DarkSurface
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 1.dp
            ),
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .widthIn(max = 300.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message.message,
                    color = if (message.isUser) TextWhite else TextWhite,
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (message.isUser) TextWhite.copy(alpha = 0.7f) else TextGray,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp)
                )
            }
        }
    }
}

private fun extractName(message: String): String {
    // Try to extract a name from messages like "I'm John" or "My name is Jane"
    val lowerMessage = message.lowercase()
    return when {
        lowerMessage.contains("i am ") || lowerMessage.contains("i'm ") -> {
            val parts = if (lowerMessage.contains("i am ")) {
                message.split("I am ", "i am ")
            } else {
                message.split("I'm ", "i'm ")
            }
            if (parts.size > 1) {
                val nameWithExtra = parts[1].trim()
                nameWithExtra.split(" ")[0].trim(',', '.', '!', '?')
            } else ""
        }
        lowerMessage.contains("my name is ") -> {
            val parts = message.split("my name is ", "My name is ")
            if (parts.size > 1) {
                val nameWithExtra = parts[1].trim()
                nameWithExtra.split(" ")[0].trim(',', '.', '!', '?')
            } else ""
        }
        else -> ""
    }
}

// Extract important keywords from user message for better response matching
private fun extractKeywords(message: String): List<String> {
    val stopWords = setOf(
        "a", "an", "the", "and", "but", "or", "for", "nor", "on", "at", "to", "from", 
        "by", "with", "in", "out", "is", "am", "are", "was", "were", "be", "been", "being",
        "have", "has", "had", "do", "does", "did", "will", "would", "shall", "should",
        "can", "could", "may", "might", "must", "i", "you", "he", "she", "it", "we", "they",
        "me", "him", "her", "us", "them", "this", "that", "these", "those", "my", "your", "his",
        "its", "our", "their", "of", "about", "like"
    )
    
    return message.split(" ", ".", ",", "!", "?", ":", ";", "-")
        .map { it.trim().lowercase() }
        .filter { it.length > 2 && it !in stopWords }
        .distinct()
        .take(5) // Most significant keywords
}

private fun processUserMessage(
    message: String,
    currentTopic: ConversationTopic,
    stage: Int,
    userName: String,
    userMood: String,
    keywords: List<String> = emptyList() // Add keywords parameter
): Triple<ConversationTopic, Int, String> {
    val lowerMessage = message.lowercase()
    
    // Check for exact keyword matches first for more relevant responses
    if (keywords.isNotEmpty()) {
        if (keywords.any { it in setOf("suicide", "kill", "die", "death", "harm") }) {
            return Triple(
                ConversationTopic.GENERAL,
                0,
                "I notice you mentioned something concerning. If you're having thoughts of harming yourself, please reach out to a crisis helpline immediately. In the US, you can call or text 988 to reach the Suicide and Crisis Lifeline. Would you like me to provide other resources that might help?"
            )
        }
        
        if (keywords.any { it in setOf("panic", "attack", "breath", "breathing") }) {
            return Triple(
                ConversationTopic.ACTIVITY,
                0,
                "It sounds like you might be experiencing anxiety. Let's try a quick breathing exercise: breathe in slowly for 4 counts, hold for 2, then exhale for 6. Repeat this 3-5 times. How are you feeling now? Would you like more techniques to manage anxiety?"
            )
        }
        
        if (keywords.any { it in setOf("lonely", "alone", "isolation", "isolated") }) {
            return Triple(
                ConversationTopic.GENERAL,
                0,
                "Feeling lonely can be really difficult. Social connection is important for our wellbeing. Is there someone you could reach out to today, even for a brief conversation? What activities help you feel more connected to others?"
            )
        }
        
        if (keywords.any { it in setOf("trauma", "ptsd", "flashback", "abuse") }) {
            return Triple(
                ConversationTopic.THERAPY,
                0,
                "I understand you're dealing with something difficult. Trauma can have a significant impact on mental health. Working with a trauma-informed therapist can be very helpful. Have you considered speaking with a professional about these experiences?"
            )
        }
    }
    
    // Detect if the message is a question
    val isQuestion = lowerMessage.contains("?") || 
                    lowerMessage.startsWith("how") || 
                    lowerMessage.startsWith("what") || 
                    lowerMessage.startsWith("why") || 
                    lowerMessage.startsWith("when") || 
                    lowerMessage.startsWith("where") || 
                    lowerMessage.startsWith("which") || 
                    lowerMessage.startsWith("can") || 
                    lowerMessage.startsWith("do") || 
                    lowerMessage.startsWith("could") || 
                    lowerMessage.startsWith("would") || 
                    lowerMessage.startsWith("should") || 
                    lowerMessage.startsWith("is") || 
                    lowerMessage.startsWith("are")
    
    // Direct responses to common questions, regardless of current topic
    if (isQuestion) {
        when {
            lowerMessage.contains("how are you") -> {
                return Triple(
                    currentTopic,
                    stage,
                    "I'm doing well, thank you for asking! I'm here to support you. How are you feeling today?"
                )
            }
            lowerMessage.contains("what can you do") || lowerMessage.contains("what do you do") || lowerMessage.contains("help me with") -> {
                return Triple(
                    ConversationTopic.GENERAL,
                    0,
                    "I can help you track your mental wellbeing, discuss coping strategies for stress or anxiety, suggest mood-boosting activities, and provide a space for you to express yourself. What specifically would you like help with today?"
                )
            }
            lowerMessage.contains("who made you") || lowerMessage.contains("who created you") -> {
                return Triple(
                    currentTopic,
                    stage,
                    "I was created by the Neuro Cloud team to provide mental health support and conversation. Is there something specific about mental health you'd like to discuss?"
                )
            }
            lowerMessage.contains("how to cope with") || lowerMessage.contains("how to deal with") -> {
                if (lowerMessage.contains("stress") || lowerMessage.contains("anxiety")) {
                    return Triple(
                        ConversationTopic.ACTIVITY,
                        0,
                        "Coping with stress and anxiety can involve several approaches: deep breathing exercises, physical activity, mindfulness meditation, or talking with someone you trust. Which of these would you like to explore further?"
                    )
                } else if (lowerMessage.contains("depress") || lowerMessage.contains("sad")) {
                    return Triple(
                        ConversationTopic.ACTIVITY,
                        0,
                        "When dealing with feelings of depression, small steps can help: try to maintain a routine, get some sunlight, engage in light physical activity, and reach out to supportive people. Professional help is also very important. Would you like to discuss any of these strategies in more detail?"
                    )
                } else if (lowerMessage.contains("sleep") || lowerMessage.contains("insomnia")) {
                    return Triple(
                        ConversationTopic.SLEEP,
                        0,
                        "Improving sleep can involve establishing a regular sleep schedule, creating a restful environment, limiting screen time before bed, and relaxation techniques. Which aspect of your sleep would you like to work on most?"
                    )
                }
            }
            lowerMessage.contains("what is mental health") -> {
                return Triple(
                    ConversationTopic.GENERAL,
                    0,
                    "Mental health includes our emotional, psychological, and social well-being. It affects how we think, feel, act, handle stress, relate to others, and make choices. Taking care of your mental health is just as important as physical health. Is there a specific aspect of mental health you're curious about?"
                )
            }
            lowerMessage.contains("how to improve") || lowerMessage.contains("how to boost") -> {
                if (lowerMessage.contains("mood")) {
                    return Triple(
                        ConversationTopic.ACTIVITY,
                        0,
                        "To boost your mood, you might try physical exercise, spending time in nature, practicing gratitude, connecting with loved ones, or doing activities you enjoy. Would you like me to elaborate on any of these approaches?"
                    )
                } else if (lowerMessage.contains("mental health") || lowerMessage.contains("wellbeing")) {
                    return Triple(
                        ConversationTopic.GENERAL,
                        0,
                        "Improving mental wellbeing involves multiple approaches: regular physical activity, adequate sleep, stress management, social connection, and seeking professional help when needed. Which area would you like to focus on first?"
                    )
                }
            }
            lowerMessage.contains("should i see") && (lowerMessage.contains("therapist") || lowerMessage.contains("doctor") || lowerMessage.contains("professional")) -> {
                return Triple(
                    ConversationTopic.THERAPY,
                    0,
                    "If you're consistently feeling overwhelmed, experiencing significant mood changes, or finding it hard to cope with daily life, talking to a mental health professional can be very beneficial. They can provide personalized support and strategies. Have you considered reaching out to a therapist or counselor?"
                )
            }
        }
    }
    
    // Check for topic changes
    if (lowerMessage.contains("goodbye") || lowerMessage.contains("bye")) {
        return Triple(ConversationTopic.GREETING, 0, "Goodbye! Take care of yourself. Feel free to chat anytime you need support. Is there anything specific you'd like to discuss next time?")
    }
    
    // Handle based on current topic and stage
    return when (currentTopic) {
        ConversationTopic.GREETING -> {
            when (stage) {
                0 -> {
                    val name = extractName(message)
                    val nameGreeting = if (name.isNotEmpty()) {
                        "Nice to meet you, $name! "
                    } else ""
                    
                    Triple(
                        ConversationTopic.MOOD,
                        0,
                        "${nameGreeting}I'm here to help you track your mental health. How are you feeling today? You can be honest - I'm here to support you."
                    )
                }
                else -> Triple(ConversationTopic.MOOD, 0, "How are you feeling today? I'm interested in understanding your current emotional state.")
            }
        }
        
        ConversationTopic.MOOD -> {
            when (stage) {
                0 -> {
                    val response = when {
                        lowerMessage.contains("good") || lowerMessage.contains("great") || 
                        lowerMessage.contains("happy") || lowerMessage.contains("fine") -> {
                            "I'm glad to hear you're doing well! What's been making you feel good lately? Understanding what positively affects your mood can help maintain it."
                        }
                        lowerMessage.contains("sad") || lowerMessage.contains("depressed") || 
                        lowerMessage.contains("down") || lowerMessage.contains("unhappy") -> {
                            "I'm sorry to hear you're feeling down. Would you like to talk about what's bothering you? Sometimes sharing our concerns can help lighten the mental load."
                        }
                        lowerMessage.contains("anxious") || lowerMessage.contains("nervous") || 
                        lowerMessage.contains("stress") || lowerMessage.contains("worry") -> {
                            "I understand anxiety can be challenging. Would you like to try a quick breathing exercise together? Or would you prefer to discuss what's causing your anxiety?"
                        }
                        lowerMessage.contains("tired") || lowerMessage.contains("exhausted") || 
                        lowerMessage.contains("fatigue") -> {
                            "Being tired can affect your mental well-being. Have you been getting enough rest lately? Are there specific factors disrupting your sleep or energy levels?"
                        }
                        lowerMessage.contains("angry") || lowerMessage.contains("frustrated") || 
                        lowerMessage.contains("mad") || lowerMessage.contains("upset") -> {
                            "It sounds like you're feeling frustrated. Would you like to talk about what's triggering these feelings? Understanding our anger triggers is the first step to managing them."
                        }
                        lowerMessage.contains("overwhelm") || lowerMessage.contains("too much") -> {
                            "Feeling overwhelmed is common in our busy lives. Can you identify what specifically is contributing to this feeling? Breaking things down into smaller parts can sometimes help."
                        }
                        lowerMessage.contains("confus") || lowerMessage.contains("uncertain") || 
                        lowerMessage.contains("don't know") || lowerMessage.contains("unsure") -> {
                            "Uncertainty can be difficult to navigate. Is there a particular situation that's causing this confusion? Sometimes talking through our thoughts can bring clarity."
                        }
                        else -> {
                            "Thank you for sharing how you're feeling. Would you like to talk about what activities might help improve your mood? Or is there something specific you'd like to discuss?"
                        }
                    }
                    Triple(ConversationTopic.MOOD, 1, response)
                }
                
                1 -> {
                    if (lowerMessage.contains("yes") && (userMood.contains("down") || userMood.contains("sad"))) {
                        Triple(
                            ConversationTopic.ACTIVITY, 
                            0, 
                            "Sometimes small activities can help improve our mood. Have you tried taking a short walk or listening to uplifting music today? These simple actions can sometimes shift our perspective."
                        )
                    } else if (lowerMessage.contains("breathing") || lowerMessage.contains("exercise")) {
                        Triple(
                            ConversationTopic.ACTIVITY, 
                            0, 
                            "Let's try a simple breathing exercise. Inhale deeply for 4 counts, hold for 2, then exhale for 6 counts. Try this 3 times. How do you feel afterward? Did you notice any change in your tension level?"
                        )
                    } else {
                        Triple(
                            ConversationTopic.ACTIVITY, 
                            0, 
                            "What activities do you usually enjoy that help your mental well-being? Sometimes returning to activities we love can reconnect us with positive feelings."
                        )
                    }
                }
                
                else -> Triple(ConversationTopic.ACTIVITY, 0, "Let's talk about activities that might help your mental health. What do you enjoy doing? Even small enjoyable activities can have a significant impact.")
            }
        }
        
        ConversationTopic.ACTIVITY -> {
            when (stage) {
                0 -> {
                    if (lowerMessage.contains("walk") || lowerMessage.contains("exercise") || lowerMessage.contains("workout")) {
                        Triple(
                            ConversationTopic.ACTIVITY,
                            1,
                            "Physical activity is excellent for mental health! Even 10 minutes can release endorphins. Would you like me to remind you to move regularly? Or would you like some suggestions for simple exercises you can do anywhere?"
                        )
                    } else if (lowerMessage.contains("read") || lowerMessage.contains("book") || lowerMessage.contains("music")) {
                        Triple(
                            ConversationTopic.ACTIVITY,
                            1,
                            "Reading and music are wonderful ways to relax and focus your mind. Do you have any favorites you turn to when you need a mood boost? I'd love to hear what kinds of books or music resonate with you."
                        )
                    } else if (lowerMessage.contains("meditat") || lowerMessage.contains("yoga") || lowerMessage.contains("mindful")) {
                        Triple(
                            ConversationTopic.ACTIVITY,
                            1,
                            "Mindfulness practices are powerful tools! Even 5 minutes of meditation can help calm your mind. Would you like some guided meditation suggestions? Or do you have a particular mindfulness practice that works for you?"
                        )
                    } else if (lowerMessage.contains("friend") || lowerMessage.contains("talk") || lowerMessage.contains("social") || lowerMessage.contains("people")) {
                        Triple(
                            ConversationTopic.ACTIVITY,
                            1,
                            "Social connection is vital for mental health. Spending time with supportive people can really boost our mood. Is there someone you could reach out to today for a brief chat? Even short social interactions can be beneficial."
                        )
                    } else if (lowerMessage.contains("hobby") || lowerMessage.contains("craft") || lowerMessage.contains("art") || lowerMessage.contains("creat")) {
                        Triple(
                            ConversationTopic.ACTIVITY,
                            1,
                            "Creative activities can be wonderfully therapeutic! They help us express emotions and enter a flow state. What kind of creative activities do you enjoy? Or is there something new you've been wanting to try?"
                        )
                    } else {
                        Triple(
                            ConversationTopic.SLEEP,
                            0,
                            "Those are great activities! Another important aspect of mental health is sleep. How have you been sleeping lately? Quality sleep can significantly impact how we feel during the day."
                        )
                    }
                }
                
                else -> Triple(ConversationTopic.SLEEP, 0, "Let's talk about your sleep patterns. How have you been sleeping lately? Sleep quality and mental health are closely connected.")
            }
        }
        
        ConversationTopic.SLEEP -> {
            when (stage) {
                0 -> {
                    if (lowerMessage.contains("bad") || lowerMessage.contains("trouble") || 
                       lowerMessage.contains("insomnia") || lowerMessage.contains("difficult")) {
                        Triple(
                            ConversationTopic.SLEEP,
                            1,
                            "Sleep difficulties can significantly impact your mental health. Have you tried establishing a regular bedtime routine to help signal your body it's time to rest? What typically helps you fall asleep when you're struggling?"
                        )
                    } else if (lowerMessage.contains("good") || lowerMessage.contains("fine") || 
                              lowerMessage.contains("well") || lowerMessage.contains("enough")) {
                        Triple(
                            ConversationTopic.GENERAL,
                            0,
                            "That's great to hear! Good sleep is crucial for mental well-being. Is there anything else you'd like to discuss about your mental health? Any specific questions or concerns on your mind?"
                        )
                    } else if (lowerMessage.contains("dream") || lowerMessage.contains("nightmare")) {
                        Triple(
                            ConversationTopic.SLEEP,
                            1,
                            "Dreams and nightmares can sometimes reflect our subconscious thoughts and emotions. Have your dreams been affecting your sleep quality? Some people find it helpful to journal about intense dreams to process them."
                        )
                    } else {
                        Triple(
                            ConversationTopic.SLEEP,
                            1,
                            "Creating a consistent sleep schedule can help. Try to avoid screens an hour before bed and consider relaxation techniques like deep breathing. Would you like more sleep tips? Or are there specific sleep challenges you're facing?"
                        )
                    }
                }
                
                else -> Triple(ConversationTopic.GENERAL, 0, "Taking care of your sleep is so important. Is there anything specific about your mental health you'd like to focus on improving? I'm here to discuss any questions or concerns you might have.")
            }
        }
        
        ConversationTopic.MEDICATION -> {
            Triple(
                ConversationTopic.THERAPY,
                0,
                "Regular medication is important if prescribed. Have you considered talking to a mental health professional regularly? Many people find a combination of medication and therapy to be most effective. Do you have any questions about this approach?"
            )
        }
        
        ConversationTopic.THERAPY -> {
            Triple(
                ConversationTopic.GENERAL,
                0,
                "Professional support can be very beneficial. Is there anything else on your mind today that you'd like to discuss? Or do you have specific questions about therapy or other mental health resources?"
            )
        }
        
        ConversationTopic.GENERAL -> {
            // Check for specific topics
            when {
                lowerMessage.contains("depress") -> {
                    Triple(
                        ConversationTopic.GENERAL,
                        1,
                        "Depression can be challenging to navigate. Small steps like regular movement, social connection, and professional support can help. Would you like to track your mood daily in the app? It could help identify patterns and triggers. Is there a specific aspect of depression you'd like to discuss?"
                    )
                }
                lowerMessage.contains("anxiet") || lowerMessage.contains("anxious") || lowerMessage.contains("panic") -> {
                    Triple(
                        ConversationTopic.GENERAL,
                        1,
                        "Anxiety can be managed with techniques like deep breathing, mindfulness, and gradual exposure to triggers. Have you tried the grounding technique of naming 5 things you can see, 4 you can touch, 3 you can hear, 2 you can smell, and 1 you can taste? Would you like to try it now? What situations tend to trigger your anxiety?"
                    )
                }
                lowerMessage.contains("thank") -> {
                    Triple(
                        ConversationTopic.GREETING,
                        0,
                        "You're welcome! I'm here to support you anytime. Is there anything specific about your mental wellbeing you'd like to discuss today? I'm here to listen and answer any questions you might have."
                    )
                }
                lowerMessage.contains("medic") || lowerMessage.contains("pill") || lowerMessage.contains("prescri") -> {
                    Triple(
                        ConversationTopic.MEDICATION,
                        0,
                        "Medication can be an important part of mental health care for many people. Have you discussed medication options with a healthcare provider? It's important to work closely with your doctor on any medication plan. Do you have specific questions about this topic?"
                    )
                }
                lowerMessage.contains("relation") || lowerMessage.contains("friend") || lowerMessage.contains("family") || lowerMessage.contains("partner") -> {
                    Triple(
                        ConversationTopic.GENERAL,
                        1,
                        "Relationships play a significant role in our mental health. Supportive connections can be protective, while challenging relationships can increase stress. Is there a specific relationship you're finding difficult right now? Or are you looking for ways to strengthen your support network?"
                    )
                }
                lowerMessage.contains("work") || lowerMessage.contains("job") || lowerMessage.contains("career") || lowerMessage.contains("study") -> {
                    Triple(
                        ConversationTopic.GENERAL,
                        1,
                        "Work and study environments can significantly impact our mental wellbeing. Finding balance is important. Are you experiencing stress related to your work or studies? Would you like to discuss strategies for managing work-related mental health challenges?"
                    )
                }
                else -> {
                    Triple(
                        ConversationTopic.GENERAL,
                        0,
                        "I'm here to listen and support you. You can track your moods, set reminders for self-care activities, or just chat anytime. What aspect of mental wellness would you like to focus on today? Do you have any specific questions I can help with?"
                    )
                }
            }
        }
    }
} 