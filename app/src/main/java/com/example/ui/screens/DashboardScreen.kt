package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.DailyGoals
import com.example.data.model.FoodItem
import com.example.data.model.MealLog
import com.example.ui.theme.*
import com.example.ui.viewmodel.AaharViewModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: AaharViewModel,
    onNavigateToCapture: () -> Unit,
    onNavigateToDiary: () -> Unit,
    onNavigateToCoach: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val mealLogs by viewModel.mealLogs.collectAsState()
    val dailyGoals by viewModel.dailyGoals.collectAsState()

    // Filter meals logged today
    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val todayMeals = remember(mealLogs) {
        mealLogs.filter { it.timestamp >= todayStart }
    }

    val todayMealsPreview = remember(todayMeals) {
        todayMeals.sortedByDescending { it.timestamp }.take(4)
    }

    val totalCalories = todayMeals.sumOf { it.totalCalories }
    val totalProtein = todayMeals.sumOf { it.totalProtein }
    val totalCarbs = todayMeals.sumOf { it.totalCarbs }
    val totalFat = todayMeals.sumOf { it.totalFat }
    val totalFiber = todayMeals.sumOf { it.totalFiber }

    var activeSelectedMeal by remember { mutableStateOf<MealLog?>(null) }

    // Past 7 Days calculation for consistency
    val dates = remember {
        val list = mutableListOf<Long>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6) // Past 7 days up to today
        for (i in 0..6) {
            list.add(cal.timeInMillis)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    val sdfCompare = remember { SimpleDateFormat("yyyyMMdd", Locale.getDefault()) }

    val dailyTotals = remember(mealLogs, dates) {
        dates.map { dateMillis ->
            val dateStr = sdfCompare.format(Date(dateMillis))
            val totalCal = mealLogs.filter { sdfCompare.format(Date(it.timestamp)) == dateStr }
                .sumOf { it.totalCalories }
            dateMillis to totalCal
        }
    }

    val weeklyAvg = remember(dailyTotals) {
        if (dailyTotals.isEmpty()) 0 else (dailyTotals.sumOf { it.second } / dailyTotals.size)
    }

    val streakCount = remember(mealLogs) {
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val loggedDays = mealLogs.map { sdf.format(Date(it.timestamp)) }.toSet()
        
        var streak = 0
        val cal = Calendar.getInstance()
        
        var currentDayStr = sdf.format(cal.time)
        if (loggedDays.contains(currentDayStr)) {
            while (loggedDays.contains(currentDayStr)) {
                streak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
                currentDayStr = sdf.format(cal.time)
            }
        } else {
            cal.add(Calendar.DAY_OF_YEAR, -1)
            currentDayStr = sdf.format(cal.time)
            while (loggedDays.contains(currentDayStr)) {
                streak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
                currentDayStr = sdf.format(cal.time)
            }
        }
        streak
    }

    val avatarUrl = dailyGoals.profilePhotoUri ?: "https://lh3.googleusercontent.com/aida-public/AB6AXuDrpFaAGOJf5i7_XKnUm3E1-iofKOO2CQzHOAuG5PqtZ1VxIHZ8B91UYcz1gJ5ZcXuWiDD_yy_LsDh_rdtBkwurFCE0lmv_8VtAUnErDrtzbtW7QGCZxnQ_SXHJm8QT53mNyUWZCKlUUIPGbAz70lK2HpQ7-dXA9V7YbT4xpCJJWu-xgFVUhr-1OidFI3pd5rxI3cAUWcJkNtfQy0OyVfoCc8LCE7o0PIraGRd4gxhEYTn7Lecf6PsF7qI1yZX2ztrsLAKhjL-OHUc"

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DarkBackground
                ),
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "AAHAR",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            letterSpacing = 2.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("app_logo")
                        )
                        Text(
                            text = "track what fuels you",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = LightMuted,
                            letterSpacing = 0.5.sp
                        )
                    }
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceHigh)
                            .border(
                                BorderStroke(
                                    1.dp,
                                    Brush.linearGradient(
                                        colors = listOf(MintAccent, LimeAccent)
                                    )
                                ),
                                CircleShape
                            )
                            .clickable { onNavigateToProfile() }
                            .testTag("avatar_button")
                    ) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "User Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToDiary,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Diary View",
                            tint = LightMuted
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCapture,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = DarkBackground,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 16.dp, end = 8.dp)
                    .size(56.dp)
                    .testTag("capture_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.AddAPhoto,
                    contentDescription = "Capture Food",
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        bottomBar = {
            CustomBottomNavigation(
                currentScreen = "home",
                onNavigateToHome = { },
                onNavigateToDiary = onNavigateToDiary,
                onNavigateToCoach = onNavigateToCoach
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                CalorieSummaryCard(totalCalories = totalCalories, targetCalories = dailyGoals.calories)
            }

            item {
                MacroSummaryCard(
                    protein = totalProtein, targetProtein = dailyGoals.protein,
                    carbs = totalCarbs, targetCarbs = dailyGoals.carbs,
                    fat = totalFat, targetFat = dailyGoals.fat
                )
            }

            item {
                SecondaryNutrientRow(
                    fiber = totalFiber, targetFiber = dailyGoals.fiber,
                    dailyGoals = dailyGoals
                )
            }

            item {
                WeeklySnapshotStrip(
                    weeklyAvg = weeklyAvg,
                    streakCount = streakCount
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "Today's Meals",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = WarmWhite
                        )
                    )
                    Text(
                        text = "See Diary",
                        modifier = Modifier
                            .clickable { onNavigateToDiary() }
                            .testTag("see_all_meals_btn"),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            if (todayMealsPreview.isEmpty()) {
                item {
                    EmptyMealsPlaceholder(onNavigateToCapture)
                }
            } else {
                items(todayMealsPreview) { meal ->
                    MealItemRow(
                        meal = meal,
                        onDelete = { viewModel.deleteMealLog(meal) },
                        onClick = { activeSelectedMeal = meal }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    activeSelectedMeal?.let { meal ->
        MealDetailDialog(
            meal = meal,
            onDismiss = { activeSelectedMeal = null },
            onDelete = { viewModel.deleteMealLog(meal) }
        )
    }
}

@Composable
fun CalorieSummaryCard(totalCalories: Int, targetCalories: Int) {
    val caloriesLeft = targetCalories - totalCalories
    val progress = if (targetCalories > 0) totalCalories.toFloat() / targetCalories.toFloat() else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress.coerceIn(0f, 1f))

    val deltaText = if (caloriesLeft >= 0) {
        "$caloriesLeft kcal remaining"
    } else {
        "${-caloriesLeft} kcal over target"
    }
    val deltaColor = if (caloriesLeft >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, LightMuted.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Box(
                modifier = Modifier.size(110.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Background Ring
                    drawCircle(
                        color = CalorieRingBg,
                        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Foreground Progress Ring
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(MintAccent, LimeAccent, MintAccent)
                        ),
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                val percentage = (progress * 100).toInt()
                Text(
                    text = "$percentage%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = WarmWhite
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Daily Calories",
                    fontSize = 13.sp,
                    color = LightMuted,
                    fontWeight = FontWeight.Medium
                )
                
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = String.format("%,d", totalCalories),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = WarmWhite,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = " / of ${String.format("%,d", targetCalories)} kcal",
                        fontSize = 13.sp,
                        color = DarkMuted,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Text(
                    text = deltaText,
                    fontSize = 13.sp,
                    color = deltaColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun MacroSummaryCard(
    protein: Int, targetProtein: Int,
    carbs: Int, targetCarbs: Int,
    fat: Int, targetFat: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, LightMuted.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val macros = listOf(
                Triple("Protein", protein, targetProtein),
                Triple("Carbs", carbs, targetCarbs),
                Triple("Fats", fat, targetFat)
            )
            val progressColors = listOf(ProteinProgress, CarbsProgress, FatsProgress)

            macros.forEachIndexed { index, (label, value, target) ->
                val ratio = if (target > 0) value.toFloat() / target.toFloat() else 0f
                val animatedRatio by animateFloatAsState(targetValue = ratio.coerceIn(0f, 1f))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = progressColors[index]
                    )
                    Text(
                        text = "${value}/${target}g",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WarmWhite
                    )
                    LinearProgressIndicator(
                        progress = { animatedRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                        color = progressColors[index],
                        trackColor = CalorieRingBg
                    )
                }
            }
        }
    }
}

@Composable
fun SecondaryNutrientRow(
    fiber: Int, targetFiber: Int,
    dailyGoals: DailyGoals
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Pill 1: Fiber
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurface)
                .border(BorderStroke(1.dp, LightMuted.copy(alpha = 0.05f)), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Fiber", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FiberProgress)
                Text("${fiber}/${targetFiber}g", fontSize = 12.sp, color = WarmWhite, fontWeight = FontWeight.Medium)
            }
        }
        
        // Pill 2: Dynamic Micronutrient
        val microLabel: String
        val microValue: Int
        val microTarget: Int
        val microUnit: String
        val microColor = VitaminProgress
        
        when {
            dailyGoals.trackMagnesium -> {
                microLabel = "Magnesium"
                microValue = 0
                microTarget = dailyGoals.targetMagnesium
                microUnit = "mg"
            }
            dailyGoals.trackIron -> {
                microLabel = "Iron"
                microValue = 0
                microTarget = dailyGoals.targetIron
                microUnit = "mg"
            }
            dailyGoals.trackVitaminD -> {
                microLabel = "Vitamin D"
                microValue = 0
                microTarget = dailyGoals.targetVitaminD
                microUnit = "mcg"
            }
            dailyGoals.trackZinc -> {
                microLabel = "Zinc"
                microValue = 0
                microTarget = dailyGoals.targetZinc
                microUnit = "mg"
            }
            else -> {
                microLabel = "Water"
                microValue = 0
                microTarget = 2000
                microUnit = "ml"
            }
        }
        
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurface)
                .border(BorderStroke(1.dp, LightMuted.copy(alpha = 0.05f)), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(microLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = microColor)
                Text("${microValue}/${microTarget}${microUnit}", fontSize = 12.sp, color = WarmWhite, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun WeeklySnapshotStrip(
    weeklyAvg: Int,
    streakCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, LightMuted.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.TrendingUp,
                contentDescription = "Weekly Trends",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            val streakText = if (streakCount > 0) " · $streakCount-day streak" else ""
            Text(
                text = "Weekly avg: $weeklyAvg kcal/day$streakText",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = WarmWhite,
                letterSpacing = 0.2.sp
            )
        }
    }
}

@Composable
fun MealItemRow(
    meal: MealLog,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val itemNames = remember(meal.itemsJson) {
        try {
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val listType = Types.newParameterizedType(List::class.java, FoodItem::class.java)
            val adapter = moshi.adapter<List<FoodItem>>(listType)
            val items = adapter.fromJson(meal.itemsJson) ?: emptyList()
            items.joinToString(", ") { it.name }
        } catch (e: Exception) {
            ""
        }
    }

    val formattedTime = remember(meal.timestamp) {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        sdf.format(Date(meal.timestamp))
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1.0f)
    val opacity by animateFloatAsState(if (isPressed) 0.8f else 1.0f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = opacity
            }
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick
            )
            .testTag("meal_item_row_${meal.id}"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, LightMuted.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceHigh)
                    .border(BorderStroke(1.dp, LightMuted.copy(alpha = 0.15f)), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (meal.imageUri != null) {
                    val imageModel = remember(meal.imageUri) {
                        if (meal.imageUri.startsWith("http")) {
                            meal.imageUri
                        } else {
                            java.io.File(meal.imageUri)
                        }
                    }
                    AsyncImage(
                        model = imageModel,
                        contentDescription = meal.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = "Meal Icon",
                        tint = DarkMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = meal.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = WarmWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (itemNames.isNotEmpty()) itemNames else "Logged meal",
                    fontSize = 12.sp,
                    color = DarkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formattedTime,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${meal.totalCalories} kcal",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = WarmWhite
                )
                Text(
                    text = "P: ${meal.totalProtein}g C: ${meal.totalCarbs}g F: ${meal.totalFat}g",
                    fontSize = 10.sp,
                    color = LightMuted,
                    fontWeight = FontWeight.Medium
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp).testTag("delete_meal_btn_${meal.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Log",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyMealsPlaceholder(onNavigateToCapture: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(1.dp, LightMuted.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .clickable { onNavigateToCapture() }
            .testTag("empty_meals_card"),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CalorieRingBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = "No meals logged yet",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WarmWhite
                )
                Text(
                    text = "Tap + to add your first meal",
                    fontSize = 12.sp,
                    color = LightMuted
                )
            }
        }
    }
}

@Composable
fun MealDetailDialog(
    meal: MealLog,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val items = remember(meal.itemsJson) {
        try {
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val listType = Types.newParameterizedType(List::class.java, FoodItem::class.java)
            val adapter = moshi.adapter<List<FoodItem>>(listType)
            adapter.fromJson(meal.itemsJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDelete()
                    onDismiss()
                }
            ) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        title = {
            Text(
                text = meal.name,
                fontWeight = FontWeight.Bold,
                color = WarmWhite,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val formattedTime = remember(meal.timestamp) {
                    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                    sdf.format(Date(meal.timestamp))
                }
                Text(
                    text = "Logged at $formattedTime",
                    fontSize = 12.sp,
                    color = LightMuted
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceHigh)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Calories", fontSize = 11.sp, color = DarkMuted)
                        Text("${meal.totalCalories}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WarmWhite)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Protein", fontSize = 11.sp, color = ProteinProgress)
                        Text("${meal.totalProtein}g", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WarmWhite)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Carbs", fontSize = 11.sp, color = CarbsProgress)
                        Text("${meal.totalCarbs}g", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WarmWhite)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Fat", fontSize = 11.sp, color = FatsProgress)
                        Text("${meal.totalFat}g", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WarmWhite)
                    }
                }

                if (items.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Food Items", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LightMuted)
                        items.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${item.name} (${item.portionSize})",
                                    fontSize = 13.sp,
                                    color = WarmWhite,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${item.calories} kcal",
                                    fontSize = 13.sp,
                                    color = LightMuted,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun CustomBottomNavigation(
    currentScreen: String,
    onNavigateToHome: () -> Unit,
    onNavigateToDiary: () -> Unit,
    onNavigateToCoach: () -> Unit
) {
    NavigationBar(
        containerColor = DarkBackground.copy(alpha = 0.9f),
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        NavigationBarItem(
            selected = currentScreen == "home",
            onClick = onNavigateToHome,
            icon = {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = "Dashboard"
                )
            },
            label = { Text("Home", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DarkBackground,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = DarkMuted,
                unselectedTextColor = DarkMuted
            ),
            modifier = Modifier.testTag("nav_item_home")
        )

        NavigationBarItem(
            selected = currentScreen == "diary",
            onClick = onNavigateToDiary,
            icon = {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Diary"
                )
            },
            label = { Text("Diary", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DarkBackground,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = DarkMuted,
                unselectedTextColor = DarkMuted
            ),
            modifier = Modifier.testTag("nav_item_diary")
        )

        NavigationBarItem(
            selected = currentScreen == "coach",
            onClick = onNavigateToCoach,
            icon = {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = "Coach"
                )
            },
            label = { Text("Coach", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DarkBackground,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = DarkMuted,
                unselectedTextColor = DarkMuted
            ),
            modifier = Modifier.testTag("nav_item_coach")
        )
    }
}
