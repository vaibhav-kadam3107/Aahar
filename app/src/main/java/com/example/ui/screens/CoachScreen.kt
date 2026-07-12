package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.data.model.MealLog
import com.example.ui.theme.*
import com.example.ui.viewmodel.AaharViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CoachScreen(
    viewModel: AaharViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToDiary: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    val chatMessages by viewModel.chatMessages.collectAsState()
    val isSendingMessage by viewModel.isSendingMessage.collectAsState()
    val dailyGoals by viewModel.dailyGoals.collectAsState()
    val mealLogs by viewModel.mealLogs.collectAsState()

    val isKeyboardVisible = WindowInsets.isImeVisible

    var inputText by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()

    // Load history on initial composition
    LaunchedEffect(Unit) {
        viewModel.loadChatHistory(context)
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(chatMessages.size, isSendingMessage) {
        if (chatMessages.isNotEmpty()) {
            lazyListState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    // Prepare system instructions with all context
    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val todayMeals = remember(mealLogs) {
        mealLogs.filter { it.timestamp >= todayStart }
    }

    val systemInstruction = remember(dailyGoals, todayMeals) {
        buildString {
            append("You are an expert personal AI nutrition and diet coach inside Aahar, a premium calorie and macro tracking app.\n")
            append("Your job is to provide highly personalized, scientifically accurate, concise, and friendly guidance on nutrition, meals, fitness, and health.\n\n")
            
            append("CRITICAL: You MUST answer the user's questions using their actual logged meal data and profile targets/metrics where available. Do not make up generic statements if real numbers are present in the context below.\n\n")
            
            append("--- USER PROFILE & METRICS ---\n")
            append("Name: ${dailyGoals.userName.ifBlank { "User" }}\n")
            append("Email: ${dailyGoals.userEmail.ifBlank { "Not configured" }}\n")
            val height = dailyGoals.heightCm
            val weight = dailyGoals.weightKg
            append("Height: ${height?.let { "$it cm" } ?: "Not set"}\n")
            append("Weight: ${weight?.let { "$it kg" } ?: "Not set"}\n")
            append("Age: ${dailyGoals.age?.let { "$it years" } ?: "Not set"}\n")
            append("Gender: ${dailyGoals.gender?.replaceFirstChar { it.uppercase() } ?: "Not set"}\n")
            if (height != null && weight != null && height > 0) {
                val hMet = height / 100f
                val bmi = weight / (hMet * hMet)
                append(String.format("Calculated BMI: %.1f\n", bmi))
            } else {
                append("Calculated BMI: Not set (Height/Weight missing)\n")
            }
            append("\n")

            append("--- DAILY NUTRITIONAL GOALS (TARGETS) ---\n")
            append("Calories: ${dailyGoals.calories} kcal\n")
            append("Protein: ${dailyGoals.protein}g\n")
            append("Carbohydrates: ${dailyGoals.carbs}g\n")
            append("Fats: ${dailyGoals.fat}g\n")
            append("Fiber: ${dailyGoals.fiber}g\n")
            append("\n")

            append("--- RECENT FOOD LOGS & INTAKE ---\n")
            val totalCalories = todayMeals.sumOf { it.totalCalories }
            val totalProtein = todayMeals.sumOf { it.totalProtein }
            val totalCarbs = todayMeals.sumOf { it.totalCarbs }
            val totalFat = todayMeals.sumOf { it.totalFat }
            val totalFiber = todayMeals.sumOf { it.totalFiber }

            append("Today's Total Intake: $totalCalories kcal ($totalProtein g Protein, $totalCarbs g Carbs, $totalFat g Fat, $totalFiber g Fiber)\n")
            if (todayMeals.isEmpty()) {
                append("Today's Meals: No meals logged today yet.\n")
            } else {
                append("Today's Logged Meals:\n")
                todayMeals.forEachIndexed { index, meal ->
                    append("  ${index + 1}. ${meal.name} at ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(meal.timestamp))} " +
                           "(${meal.totalCalories} kcal, ${meal.totalProtein}g P, ${meal.totalCarbs}g C, ${meal.totalFat}g F, ${meal.totalFiber}g Fib)\n")
                }
            }
            append("\n")

            append("--- GENERAL COACH RULES ---\n")
            append("1. Keep responses concise, supportive, and conversational. Do NOT output long generic essays or walls of text.\n")
            append("2. When asked about nutrient/macro intake or progress, reference their specific targets and today's logs to calculate exactly what is left or exceeded. E.g. 'You've logged 60g protein out of your 120g target today, so you need 60g more.'\n")
            append("3. If height, weight, daily targets, or logged meals are missing or not set, kindly note that you have limited context, and gently encourage them to log a meal using the Capture FAB or fill in their details in the User Profile section (accessible via top-left avatar icon on Home screen) to unlock full coach personalization.\n")
        }
    }

    val suggestedPrompts = listOf(
        "Am I hitting my protein goal today?",
        "What should I eat for dinner to hit my macros?",
        "Is my BMI in a healthy range for my goals?",
        "Why is my fiber intake low this week?"
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("coach_screen"),
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DarkBackground
                ),
                title = {
                    Text(
                        text = "COACH",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            letterSpacing = 2.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                },
                actions = {
                    if (chatMessages.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearChatHistory(context) },
                            modifier = Modifier.testTag("clear_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear Conversation",
                                tint = LightMuted
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (!isKeyboardVisible) {
                CustomBottomNavigation(
                    currentScreen = "coach",
                    onNavigateToHome = onNavigateToHome,
                    onNavigateToDiary = onNavigateToDiary,
                    onNavigateToCoach = { }
                )
            }
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            // Chat thread or Suggestion State
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                if (chatMessages.isEmpty()) {
                    // Empty state centered helper
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 60.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = null,
                                tint = MintAccent,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Meet your Personal Coach",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarmWhite
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Get real-time insights based on your logged meals, BMI, targets, and goals. Ask anything below or tap a suggestion.",
                            fontSize = 12.sp,
                            color = LightMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            letterSpacing = 0.2.sp
                        )
                    }
                } else {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        items(chatMessages, key = { it.id }) { message ->
                            ChatBubbleRow(message = message)
                        }

                        if (isSendingMessage) {
                            item(key = "typing_indicator") {
                                CoachTypingIndicator()
                            }
                        }
                    }
                }
            }

            // Suggested Chips Panel (Visible when chat is empty or above keyboard)
            if (chatMessages.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Suggested Questions:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightMuted.copy(alpha = 0.6f),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    suggestedPrompts.forEach { prompt ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurface)
                                .border(BorderStroke(1.dp, LightMuted.copy(alpha = 0.08f)), RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.sendMessage(context, prompt, systemInstruction)
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .testTag("suggested_chip_${prompt.take(15).replace(" ", "_")}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = prompt,
                                    fontSize = 13.sp,
                                    color = WarmWhite,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MintAccent.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Input bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, LightMuted.copy(alpha = 0.08f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Ask Coach about your nutrition...", color = DarkMuted, fontSize = 14.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                            focusedTextColor = WarmWhite,
                            unfocusedTextColor = WarmWhite,
                            cursorColor = MintAccent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("coach_chat_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputText.isNotBlank() && !isSendingMessage) {
                                    viewModel.sendMessage(context, inputText, systemInstruction)
                                    inputText = ""
                                    focusManager.clearFocus()
                                }
                            }
                        )
                    )

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank() && !isSendingMessage) {
                                viewModel.sendMessage(context, inputText, systemInstruction)
                                inputText = ""
                                focusManager.clearFocus()
                            }
                        },
                        enabled = inputText.isNotBlank() && !isSendingMessage,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (inputText.isNotBlank()) EmeraldPrimary else DarkSurfaceHigh)
                            .testTag("coach_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send message",
                            tint = if (inputText.isNotBlank()) WarmWhite else DarkMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubbleRow(message: ChatMessage) {
    val isUser = message.sender == "user"
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .background(if (isUser) EmeraldPrimary else DarkSurfaceHigh)
                    .border(
                        BorderStroke(1.dp, if (isUser) EmeraldPrimaryLight.copy(alpha = 0.3f) else LightMuted.copy(alpha = 0.05f)),
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = message.text,
                    color = WarmWhite,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    letterSpacing = 0.15.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp))
            Text(
                text = timeStr,
                fontSize = 10.sp,
                color = DarkMuted,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
fun CoachTypingIndicator() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = 4.dp,
                            bottomEnd = 16.dp
                        )
                    )
                    .background(DarkSurfaceHigh)
                    .border(
                        BorderStroke(1.dp, LightMuted.copy(alpha = 0.05f)),
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = 4.dp,
                            bottomEnd = 16.dp
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MintAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Coach is typing...",
                        color = LightMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
