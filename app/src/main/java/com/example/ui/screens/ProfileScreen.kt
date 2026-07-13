package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import java.io.File
import java.io.FileOutputStream
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.DailyGoals
import com.example.ui.theme.*
import com.example.ui.viewmodel.AaharViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun saveUriToInternalStorage(context: Context, uri: Uri): Uri? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        context.filesDir.listFiles { _, name -> name.startsWith("profile_photo_") }?.forEach { 
            it.delete()
        }
        val file = File(context.filesDir, "profile_photo_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { outputStream ->
            inputStream.use { input ->
                input.copyTo(outputStream)
            }
        }
        Uri.fromFile(file)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: AaharViewModel,
    onNavigateBack: () -> Unit
) {
    val dailyGoals by viewModel.dailyGoals.collectAsState()
    val mealLogs by viewModel.mealLogs.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    // Profile Photo Picker Launcher
    val profilePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedUri = saveUriToInternalStorage(context, uri)
            if (savedUri != null) {
                viewModel.saveGoals(dailyGoals.copy(profilePhotoUri = savedUri.toString()))
            } else {
                viewModel.saveGoals(dailyGoals.copy(profilePhotoUri = uri.toString()))
            }
        }
    }

    // Dialog state for Name and Email editing
    var showEditNameDialog by remember { mutableStateOf(false) }
    var editNameText by remember { mutableStateOf("") }

    var showEditEmailDialog by remember { mutableStateOf(false) }
    var editEmailText by remember { mutableStateOf("") }

    // Goal Editing Fields
    var caloriesStr by remember { mutableStateOf("") }
    var proteinStr by remember { mutableStateOf("") }
    var carbsStr by remember { mutableStateOf("") }
    var fatStr by remember { mutableStateOf("") }
    var fiberStr by remember { mutableStateOf("") }

    var isEditingGoals by remember { mutableStateOf(false) }
    var showSaveMessage by remember { mutableStateOf(false) }

    // Metrics Editing Fields
    var isEditingMetrics by remember { mutableStateOf(false) }
    var heightStr by remember { mutableStateOf("") }
    var weightStr by remember { mutableStateOf("") }
    var ageStr by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf("") }

    // Initialize editing values from DB goals
    LaunchedEffect(dailyGoals) {
        if (!isEditingGoals) {
            caloriesStr = dailyGoals.calories.toString()
            proteinStr = dailyGoals.protein.toString()
            carbsStr = dailyGoals.carbs.toString()
            fatStr = dailyGoals.fat.toString()
            fiberStr = dailyGoals.fiber.toString()
        }
        if (!isEditingMetrics) {
            heightStr = dailyGoals.heightCm?.toString() ?: ""
            weightStr = dailyGoals.weightKg?.toString() ?: ""
            ageStr = dailyGoals.age?.toString() ?: ""
            selectedGender = dailyGoals.gender ?: ""
        }
    }

    // Calculated Mock & Actual statistics
    val totalLoggedMeals = mealLogs.size
    val totalCaloriesTracked = mealLogs.sumOf { it.totalCalories }
    val averageCaloriesPerMeal = if (totalLoggedMeals > 0) totalCaloriesTracked / totalLoggedMeals else 0

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("profile_screen"),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "USER PROFILE",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            letterSpacing = 2.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = LightMuted
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Profile Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, LightMuted.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Decorative Abstract Background Gradient Accent
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        EmeraldPrimary.copy(alpha = 0.5f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Glowing Animated Avatar frame
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val scaleAnimation by infiniteTransition.animateFloat(
                            initialValue = 0.98f,
                            targetValue = 1.02f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1800, easing = EaseInOutSine),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "scale"
                        )

                        val avatarUrl = dailyGoals.profilePhotoUri ?: "https://lh3.googleusercontent.com/aida-public/AB6AXuDrpFaAGOJf5i7_XKnUm3E1-iofKOO2CQzHOAuG5PqtZ1VxIHZ8B91UYcz1gJ5ZcXuWiDD_yy_LsDh_rdtBkwurFCE0lmv_8VtAUnErDrtzbtW7QGCZxnQ_SXHJm8QT53mNyUWZCKlUUIPGbAz70lK2HpQ7-dXA9V7YbT4xpCJJWu-xgFVUhr-1OidFI3pd5rxI3cAUWcJkNtfQy0OyVfoCc8LCE7o0PIraGRd4gxhEYTn7Lecf6PsF7qI1yZX2ztrsLAKhjL-OHUc"

                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .shadow(24.dp, CircleShape)
                                .border(
                                    BorderStroke(
                                        2.dp, Brush.linearGradient(
                                            colors = listOf(LimeAccent, MintAccent)
                                        )
                                    ),
                                    CircleShape
                                )
                                .clickable { profilePhotoPickerLauncher.launch("image/*") }
                                .padding(4.dp)
                                .testTag("profile_photo_click_area")
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(DarkSurfaceHigh)
                            ) {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = "User Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // Camera / Edit Badge overlaid on bottom-right corner
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .background(MintAccent)
                                    .border(BorderStroke(1.5.dp, DarkSurface), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Edit Profile Picture",
                                    tint = DarkBackground,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Name
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    editNameText = dailyGoals.userName
                                    showEditNameDialog = true
                                }
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .testTag("profile_name_click_area")
                        ) {
                            Text(
                                text = dailyGoals.userName,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = WarmWhite,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Name",
                                tint = MintAccent,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        // Email
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    editEmailText = dailyGoals.userEmail
                                    showEditEmailDialog = true
                                }
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .testTag("profile_email_click_area")
                        ) {
                            Text(
                                text = dailyGoals.userEmail,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = LightMuted,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Email",
                                tint = MintAccent,
                                modifier = Modifier.size(12.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.padding(top = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text("Premium Member") },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    labelColor = LimeAccent,
                                    containerColor = EmeraldPrimary.copy(alpha = 0.2f)
                                ),
                                border = BorderStroke(1.dp, LimeAccent.copy(alpha = 0.3f))
                            )
                            SuggestionChip(
                                onClick = {},
                                label = { Text("AI Active") },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    labelColor = MintAccent,
                                    containerColor = EmeraldPrimary.copy(alpha = 0.2f)
                                ),
                                border = BorderStroke(1.dp, MintAccent.copy(alpha = 0.3f))
                            )
                        }
                    }
                }
            }

            // Dark Mode Toggle Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { AppThemeState.isDark = !AppThemeState.isDark }
                    .testTag("dark_mode_toggle"),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, LightMuted.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (AppThemeState.isDark) EmeraldPrimary.copy(alpha = 0.2f)
                                    else LimeAccent.copy(alpha = 0.2f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (AppThemeState.isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = if (AppThemeState.isDark) MintAccent else EmeraldPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Color Theme",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarmWhite
                            )
                            Text(
                                text = if (AppThemeState.isDark) "Dark Mode active" else "Light Mode active",
                                fontSize = 13.sp,
                                color = LightMuted
                            )
                        }
                    }

                    Switch(
                        checked = AppThemeState.isDark,
                        onCheckedChange = { AppThemeState.isDark = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MintAccent,
                            checkedTrackColor = EmeraldPrimary,
                            uncheckedThumbColor = EmeraldPrimary,
                            uncheckedTrackColor = DarkSurfaceHigh
                        ),
                        modifier = Modifier.testTag("dark_mode_switch")
                    )
                }
            }

            // Quick Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, LightMuted.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Meals Logged",
                            fontSize = 12.sp,
                            color = LightMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = totalLoggedMeals.toString(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = LimeAccent
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, LightMuted.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Avg Cal / Meal",
                            fontSize = 12.sp,
                            color = LightMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$averageCaloriesPerMeal kcal",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = MintAccent
                        )
                    }
                }
            }

            // Body Metrics Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .testTag("body_metrics_card"),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, LightMuted.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessibilityNew,
                                    contentDescription = null,
                                    tint = MintAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "Body Metrics",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarmWhite
                            )
                        }

                        IconButton(
                            onClick = {
                                if (isEditingMetrics) {
                                    // Save changes
                                    val newHeight = heightStr.toFloatOrNull()
                                    val newWeight = weightStr.toFloatOrNull()
                                    val newAge = ageStr.toIntOrNull()
                                    val newGender = selectedGender.ifBlank { null }

                                    viewModel.saveGoals(
                                        dailyGoals.copy(
                                            heightCm = newHeight,
                                            weightKg = newWeight,
                                            age = newAge,
                                            gender = newGender
                                        )
                                    )
                                    isEditingMetrics = false
                                    focusManager.clearFocus()
                                } else {
                                    // prefill edit fields
                                    heightStr = dailyGoals.heightCm?.toString() ?: ""
                                    weightStr = dailyGoals.weightKg?.toString() ?: ""
                                    ageStr = dailyGoals.age?.toString() ?: ""
                                    selectedGender = dailyGoals.gender ?: ""
                                    isEditingMetrics = true
                                }
                            },
                            modifier = Modifier.testTag("edit_metrics_button")
                        ) {
                            Icon(
                                imageVector = if (isEditingMetrics) Icons.Default.Check else Icons.Default.Edit,
                                contentDescription = if (isEditingMetrics) "Save Metrics" else "Edit Metrics",
                                tint = if (isEditingMetrics) LimeAccent else MintAccent
                            )
                        }
                    }

                    if (isEditingMetrics) {
                        // Editable Input Fields
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    MetricInputField(
                                        label = "Height (cm)",
                                        value = heightStr,
                                        onValueChange = { heightStr = it },
                                        testTag = "input_height"
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    MetricInputField(
                                        label = "Weight (kg)",
                                        value = weightStr,
                                        onValueChange = { weightStr = it },
                                        testTag = "input_weight"
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    MetricInputField(
                                        label = "Age (years)",
                                        value = ageStr,
                                        onValueChange = { ageStr = it },
                                        testTag = "input_age"
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1.5f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Gender",
                                        fontSize = 11.sp,
                                        color = LightMuted,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf("Male", "Female", "Other").forEach { g ->
                                            val isSelected = selectedGender.equals(g, ignoreCase = true)
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(36.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) MintAccent.copy(alpha = 0.2f) else DarkSurfaceHigh)
                                                    .border(
                                                        BorderStroke(
                                                            1.dp,
                                                            if (isSelected) MintAccent else LightMuted.copy(alpha = 0.15f)
                                                        ),
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable { selectedGender = g }
                                                    .testTag("gender_chip_$g"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = g,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) MintAccent else LightMuted
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Display Mode
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            val heightDisp = dailyGoals.heightCm?.let { "${it.toInt()} cm" } ?: "Not set"
                            val weightDisp = dailyGoals.weightKg?.let { "$it kg" } ?: "Not set"
                            val ageDisp = dailyGoals.age?.toString() ?: "Not set"
                            val genderDisp = dailyGoals.gender?.replaceFirstChar { it.uppercase() } ?: "Not set"

                            MetricDisplayRow(
                                label = "Height",
                                value = heightDisp,
                                icon = Icons.Default.Height,
                                tint = LimeAccent
                            )
                            Divider(color = LightMuted.copy(alpha = 0.05f))
                            MetricDisplayRow(
                                label = "Weight",
                                value = weightDisp,
                                icon = Icons.Default.Scale,
                                tint = MintAccent
                            )
                            Divider(color = LightMuted.copy(alpha = 0.05f))
                            MetricDisplayRow(
                                label = "Age",
                                value = ageDisp,
                                icon = Icons.Default.CalendarToday,
                                tint = Color(0xFF64B5F6) // Soft blue
                            )
                            Divider(color = LightMuted.copy(alpha = 0.05f))
                            MetricDisplayRow(
                                label = "Gender",
                                value = genderDisp,
                                icon = Icons.Default.Person,
                                tint = Color(0xFFBA68C8) // Soft purple
                            )
                        }
                    }

                    // BMI prominence area
                    val currentHeight = dailyGoals.heightCm
                    val currentWeight = dailyGoals.weightKg
                    if (currentHeight != null && currentWeight != null && currentHeight > 0) {
                        val hMet = currentHeight / 100f
                        val bmi = currentWeight / (hMet * hMet)
                        val (catName, catColor) = when {
                            bmi < 18.5f -> "Underweight" to Color(0xFFE5C158)
                            bmi < 25.0f -> "Normal" to MintAccent
                            bmi < 30.0f -> "Overweight" to Color(0xFFE5C158)
                            else -> "Obese" to Color(0xFFE5C158)
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkSurfaceHigh)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Calculated BMI",
                                fontSize = 11.sp,
                                color = LightMuted,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format("%.1f", bmi),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = catColor,
                                modifier = Modifier.testTag("bmi_value")
                            )
                            Text(
                                text = catName.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = catColor,
                                letterSpacing = 1.sp,
                                modifier = Modifier.testTag("bmi_category")
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "BMI is a general estimate and does not account for muscle mass",
                                fontSize = 10.sp,
                                color = LightMuted.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    } else {
                        // Empty State for BMI
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkSurfaceHigh)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Add your height and weight to see your BMI",
                                fontSize = 12.sp,
                                color = LightMuted,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.testTag("bmi_empty_state")
                            )
                        }
                    }
                }
            }

            // Goal Management / Customizer Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, LightMuted.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrackChanges,
                                    contentDescription = null,
                                    tint = MintAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "Daily Nutritional Goals",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarmWhite
                            )
                        }

                        IconButton(
                            onClick = {
                                if (isEditingGoals) {
                                    // Save changes
                                    val newCal = caloriesStr.toIntOrNull() ?: dailyGoals.calories
                                    val newProt = proteinStr.toIntOrNull() ?: dailyGoals.protein
                                    val newCarb = carbsStr.toIntOrNull() ?: dailyGoals.carbs
                                    val newFat = fatStr.toIntOrNull() ?: dailyGoals.fat
                                    val newFib = fiberStr.toIntOrNull() ?: dailyGoals.fiber

                                    viewModel.saveGoals(
                                        DailyGoals(
                                            id = dailyGoals.id,
                                            calories = newCal,
                                            protein = newProt,
                                            carbs = newCarb,
                                            fat = newFat,
                                            fiber = newFib
                                        )
                                    )
                                    isEditingGoals = false
                                    focusManager.clearFocus()
                                    showSaveMessage = true
                                    coroutineScope.launch {
                                        delay(3000)
                                        showSaveMessage = false
                                    }
                                } else {
                                    isEditingGoals = true
                                }
                            },
                            modifier = Modifier.testTag("edit_goals_button")
                        ) {
                            Icon(
                                imageVector = if (isEditingGoals) Icons.Default.Check else Icons.Default.Edit,
                                contentDescription = if (isEditingGoals) "Save Goals" else "Edit Goals",
                                tint = if (isEditingGoals) LimeAccent else MintAccent
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = showSaveMessage,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Text(
                            text = "Goals successfully updated in local database!",
                            color = LimeAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }

                    if (isEditingGoals) {
                        // Editable Form Fields
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            GoalInputField(
                                label = "Calories Target (kcal)",
                                value = caloriesStr,
                                onValueChange = { caloriesStr = it },
                                testTag = "input_calories"
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    GoalInputField(
                                        label = "Protein (g)",
                                        value = proteinStr,
                                        onValueChange = { proteinStr = it },
                                        testTag = "input_protein"
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    GoalInputField(
                                        label = "Carbs (g)",
                                        value = carbsStr,
                                        onValueChange = { carbsStr = it },
                                        testTag = "input_carbs"
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    GoalInputField(
                                        label = "Fats (g)",
                                        value = fatStr,
                                        onValueChange = { fatStr = it },
                                        testTag = "input_fats"
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    GoalInputField(
                                        label = "Fiber (g)",
                                        value = fiberStr,
                                        onValueChange = { fiberStr = it },
                                        testTag = "input_fiber"
                                    )
                                }
                            }
                        }
                    } else {
                        // Read-Only Display Mode with progress indicators
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            NutrientGoalRow(label = "Calories", value = "${dailyGoals.calories} kcal", icon = Icons.Default.LocalFireDepartment, tint = LimeAccent)
                            Divider(color = LightMuted.copy(alpha = 0.05f))
                            NutrientGoalRow(label = "Protein", value = "${dailyGoals.protein}g", icon = Icons.Default.FitnessCenter, tint = ProteinProgress)
                            Divider(color = LightMuted.copy(alpha = 0.05f))
                            NutrientGoalRow(label = "Carbohydrates", value = "${dailyGoals.carbs}g", icon = Icons.Default.BreakfastDining, tint = CarbsProgress)
                            Divider(color = LightMuted.copy(alpha = 0.05f))
                            NutrientGoalRow(label = "Fats", value = "${dailyGoals.fat}g", icon = Icons.Default.WaterDrop, tint = FatsProgress)
                            Divider(color = LightMuted.copy(alpha = 0.05f))
                            NutrientGoalRow(label = "Fiber", value = "${dailyGoals.fiber}g", icon = Icons.Default.Eco, tint = FiberProgress)
                        }
                    }
                }
            }

            // Quick App Insights Banner (Modern visual gradient overlay)
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(EmeraldPrimary, EmeraldPrimaryLight)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = LimeAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Smart Assistant Tip",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarmWhite
                            )
                        }
                        Text(
                            text = "To test Gemini's high-fidelity nutritional estimation, click the camera FAB on the home screen and choose one of our offline plates or click 'Take Photo'!",
                            fontSize = 13.sp,
                            color = PaperWhite,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Dialogs for Editing Name and Email
            if (showEditNameDialog) {
                AlertDialog(
                    onDismissRequest = { showEditNameDialog = false },
                    containerColor = DarkSurface,
                    title = { Text("Edit Name", color = WarmWhite, fontWeight = FontWeight.Bold) },
                    text = {
                        OutlinedTextField(
                            value = editNameText,
                            onValueChange = { editNameText = it },
                            label = { Text("Name", color = LightMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MintAccent,
                                unfocusedBorderColor = LightMuted.copy(alpha = 0.3f),
                                focusedLabelColor = MintAccent,
                                unfocusedLabelColor = LightMuted,
                                focusedTextColor = WarmWhite,
                                unfocusedTextColor = WarmWhite
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("edit_name_input")
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (editNameText.isNotBlank()) {
                                    viewModel.saveGoals(dailyGoals.copy(userName = editNameText))
                                    showEditNameDialog = false
                                }
                            },
                            modifier = Modifier.testTag("save_name_button")
                        ) {
                            Text("SAVE", color = MintAccent, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEditNameDialog = false }) {
                            Text("CANCEL", color = LightMuted)
                        }
                    }
                )
            }

            if (showEditEmailDialog) {
                AlertDialog(
                    onDismissRequest = { showEditEmailDialog = false },
                    containerColor = DarkSurface,
                    title = { Text("Edit Email", color = WarmWhite, fontWeight = FontWeight.Bold) },
                    text = {
                        OutlinedTextField(
                            value = editEmailText,
                            onValueChange = { editEmailText = it },
                            label = { Text("Email", color = LightMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MintAccent,
                                unfocusedBorderColor = LightMuted.copy(alpha = 0.3f),
                                focusedLabelColor = MintAccent,
                                unfocusedLabelColor = LightMuted,
                                focusedTextColor = WarmWhite,
                                unfocusedTextColor = WarmWhite
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("edit_email_input")
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (editEmailText.isNotBlank()) {
                                    viewModel.saveGoals(dailyGoals.copy(userEmail = editEmailText))
                                    showEditEmailDialog = false
                                }
                            },
                            modifier = Modifier.testTag("save_email_button")
                        ) {
                            Text("SAVE", color = MintAccent, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEditEmailDialog = false }) {
                            Text("CANCEL", color = LightMuted)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun GoalInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    testTag: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.all { char -> char.isDigit() }) onValueChange(it) },
        label = { Text(label, fontSize = 12.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MintAccent,
            unfocusedBorderColor = LightMuted.copy(alpha = 0.3f),
            focusedLabelColor = MintAccent,
            unfocusedLabelColor = LightMuted,
            focusedTextColor = WarmWhite,
            unfocusedTextColor = WarmWhite
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    )
}

@Composable
fun NutrientGoalRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                fontSize = 14.sp,
                color = PaperWhite,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = WarmWhite
        )
    }
}

@Composable
fun MetricInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    testTag: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            if (input.all { it.isDigit() || it == '.' } && input.count { it == '.' } <= 1) {
                onValueChange(input)
            }
        },
        label = { Text(label, fontSize = 12.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MintAccent,
            unfocusedBorderColor = LightMuted.copy(alpha = 0.3f),
            focusedLabelColor = MintAccent,
            unfocusedLabelColor = LightMuted,
            focusedTextColor = WarmWhite,
            unfocusedTextColor = WarmWhite
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    )
}

@Composable
fun MetricDisplayRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                fontSize = 14.sp,
                color = PaperWhite,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = WarmWhite
        )
    }
}
