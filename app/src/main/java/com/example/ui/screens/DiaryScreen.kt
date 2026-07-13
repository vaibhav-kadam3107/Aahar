package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
fun DiaryScreen(
    viewModel: AaharViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToCapture: () -> Unit,
    onNavigateToCoach: () -> Unit
) {
    val mealLogs by viewModel.mealLogs.collectAsState()
    val dailyGoals by viewModel.dailyGoals.collectAsState()

    // Setup active selected date (defaults to current day)
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var selectedMealForDetail by remember { mutableStateOf<MealLog?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Generate dates centered around selectedDateMillis to avoid shifting unless outside range
    var datesRangeCenter by remember { mutableStateOf(System.currentTimeMillis()) }

    val dates = remember(datesRangeCenter) {
        val list = mutableListOf<Long>()
        val cal = Calendar.getInstance().apply { timeInMillis = datesRangeCenter }
        cal.add(Calendar.DAY_OF_YEAR, -3) // center on selected/center day
        for (i in 0..6) {
            list.add(cal.timeInMillis)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    val sdfCompare = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val selectedDayStr = sdfCompare.format(Date(selectedDateMillis))

    val isWithinRange = remember(selectedDateMillis, dates) {
        dates.any { sdfCompare.format(Date(it)) == selectedDayStr }
    }

    LaunchedEffect(selectedDateMillis) {
        if (!isWithinRange) {
            datesRangeCenter = selectedDateMillis
        }
    }

    // Filter meals for selected day
    val mealsForSelectedDay = remember(mealLogs, selectedDateMillis) {
        mealLogs.filter { sdfCompare.format(Date(it.timestamp)) == selectedDayStr }
    }

    // Group meals by type (Breakfast, Lunch, Dinner, Snack)
    val groupedByType = remember(mealsForSelectedDay) {
        mealsForSelectedDay.groupBy { getMealType(it.timestamp) }
    }

    // Daily totals calculation
    val dailyTotals = remember(mealLogs, dates) {
        dates.map { dateMillis ->
            val dateStr = sdfCompare.format(Date(dateMillis))
            val totalCal = mealLogs.filter { sdfCompare.format(Date(it.timestamp)) == dateStr }
                .sumOf { it.totalCalories }
            dateMillis to totalCal
        }
    }

    val proteinTotals = remember(mealLogs, dates) {
        dates.map { dateMillis ->
            val dateStr = sdfCompare.format(Date(dateMillis))
            val totalProt = mealLogs.filter { sdfCompare.format(Date(it.timestamp)) == dateStr }
                .sumOf { it.totalProtein }
            dateMillis to totalProt
        }
    }

    // 7-day average calculation
    val weeklyAvg = remember(dailyTotals) {
        if (dailyTotals.isEmpty()) 0 else (dailyTotals.sumOf { it.second } / dailyTotals.size)
    }

    // Streak count calculation
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

    // Macro sums for the selected day
    val dayCalories = remember(mealsForSelectedDay) { mealsForSelectedDay.sumOf { it.totalCalories } }
    val dayProtein = remember(mealsForSelectedDay) { mealsForSelectedDay.sumOf { it.totalProtein } }
    val dayCarbs = remember(mealsForSelectedDay) { mealsForSelectedDay.sumOf { it.totalCarbs } }
    val dayFat = remember(mealsForSelectedDay) { mealsForSelectedDay.sumOf { it.totalFat } }
    val dayFiber = remember(mealsForSelectedDay) { mealsForSelectedDay.sumOf { it.totalFiber } }

    val selectedDayLabel = remember(selectedDateMillis) {
        val sdf = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
        sdf.format(Date(selectedDateMillis))
    }

    // Determine the next likely meal to log
    val nextMissingMeal = remember(mealsForSelectedDay) {
        val loggedTypes = mealsForSelectedDay.map { getMealType(it.timestamp) }.toSet()
        when {
            !loggedTypes.contains("Breakfast") -> "Breakfast"
            !loggedTypes.contains("Lunch") -> "Lunch"
            !loggedTypes.contains("Dinner") -> "Dinner"
            !loggedTypes.contains("Snack") -> "Snack"
            else -> null
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selected = datePickerState.selectedDateMillis
                        if (selected != null) {
                            selectedDateMillis = selected
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Select", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = LightMuted)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = DarkSurface
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = DarkSurface,
                    titleContentColor = WarmWhite,
                    headlineContentColor = WarmWhite,
                    weekdayContentColor = LightMuted,
                    subheadContentColor = LightMuted,
                    navigationContentColor = WarmWhite,
                    yearContentColor = WarmWhite,
                    selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                    selectedDayContentColor = DarkBackground,
                    todayContentColor = MaterialTheme.colorScheme.primary,
                    todayDateBorderColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = DarkBackground),
                title = { Text("FOOD DIARY", fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp, color = MaterialTheme.colorScheme.primary) },
                actions = {
                    // Unobtrusive streak indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = LimeAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "$streakCount-day streak",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = LimeAccent
                        )
                    }
                    IconButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.testTag("choose_date_btn")
                    ) {
                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = "Choose Date", tint = LightMuted)
                    }
                }
            )
        },
        bottomBar = {
            CustomBottomNavigation(
                currentScreen = "diary",
                onNavigateToHome = onNavigateToHome,
                onNavigateToDiary = { },
                onNavigateToCoach = onNavigateToCoach
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Horizontal Date Navigation Strip
            DateStripSection(
                dates = dates,
                selectedDateMillis = selectedDateMillis,
                onDateSelected = { newDateMillis -> selectedDateMillis = newDateMillis }
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. DIARY RECEIPT/LOG HEADER (Replacing macro breakdown and mini-bar-graph)
                item {
                    DiaryDayReceiptHeader(
                        dayLabel = selectedDayLabel,
                        calories = dayCalories,
                        protein = dayProtein,
                        carbs = dayCarbs,
                        fat = dayFat,
                        fiber = dayFiber
                    )
                }

                item {
                    WeeklyProteinTrendsSection(
                        dates = dates,
                        proteinTotals = proteinTotals,
                        goalProtein = dailyGoals.protein
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 3. MEAL LIST
                    if (mealsForSelectedDay.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No meals logged for this day.",
                                    color = LightMuted,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        val showMealHeaders = mealsForSelectedDay.size > 1
                        val orderOfMealTypes = listOf("Breakfast", "Lunch", "Dinner", "Snack")

                        orderOfMealTypes.forEach { mealType ->
                            val mealsOfType = groupedByType[mealType] ?: emptyList()
                            if (mealsOfType.isNotEmpty()) {
                                if (showMealHeaders) {
                                    item {
                                        Text(
                                            text = mealType.uppercase(Locale.getDefault()),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = LightMuted,
                                            letterSpacing = 1.sp,
                                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                        )
                                    }
                                }

                                items(mealsOfType) { meal ->
                                    DiaryMealItemRow(
                                        meal = meal,
                                        onDelete = { viewModel.deleteMealLog(meal) },
                                        onClick = { selectedMealForDetail = meal }
                                    )
                                }
                            }
                        }
                    }

                    // 4. EMPTY/SPARSE STATE PROMPT
                    if (nextMissingMeal != null) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToCapture() }
                                    .testTag("suggested_meal_card"),
                                colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.4f)),
                                border = BorderStroke(1.dp, LightMuted.copy(alpha = 0.05f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddCircleOutline,
                                            contentDescription = null,
                                            tint = LightMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "No $nextMissingMeal logged yet",
                                            fontSize = 13.sp,
                                            color = LightMuted,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Log $nextMissingMeal",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }

    if (selectedMealForDetail != null) {
        MealDetailView(
            meal = selectedMealForDetail!!,
            onDismiss = { selectedMealForDetail = null },
            onDelete = {
                viewModel.deleteMealLog(selectedMealForDetail!!)
                selectedMealForDetail = null
            },
            onUpdateMeal = { updatedMeal ->
                viewModel.updateMealLog(updatedMeal)
                selectedMealForDetail = updatedMeal
            },
            builtInFoods = viewModel.builtInFoodList
        )
    }
}

@Composable
fun WeeklyProteinTrendsSection(
    dates: List<Long>,
    proteinTotals: List<Pair<Long, Int>>,
    goalProtein: Int
) {
    val sdfCompare = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val todayStr = sdfCompare.format(Date())
    val dayOfWeekFormat = SimpleDateFormat("EEEEE", Locale.getDefault()) // Single letter: M, T, W...

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("weekly_protein_trends_section"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, LightMuted.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "WEEKLY PROTEIN TRENDS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = LightMuted
                    )
                    Text(
                        text = "Track consistency vs ${goalProtein}g goal",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = DarkMuted
                    )
                }
                
                // Consistency badge (number of days meeting target)
                val daysMet = proteinTotals.count { it.second >= goalProtein }
                Surface(
                    color = if (daysMet >= 4) ProteinProgress.copy(alpha = 0.15f) else DarkSurfaceHigh,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (daysMet >= 4) ProteinProgress.copy(alpha = 0.3f) else LightMuted.copy(alpha = 0.05f))
                ) {
                    Text(
                        text = "$daysMet/7 DAYS MET",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = if (daysMet >= 4) ProteinProgress else LightMuted,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        letterSpacing = 0.5.sp
                    )
                }
            }
            
            HorizontalDivider(color = LightMuted.copy(alpha = 0.08f), thickness = 1.dp)

            // Draw Bars Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                proteinTotals.forEach { (dateMillis, totalProtein) ->
                    val dateStr = sdfCompare.format(Date(dateMillis))
                    val isToday = dateStr == todayStr
                    val dayLetter = dayOfWeekFormat.format(Date(dateMillis))

                    val fraction = if (goalProtein > 0) (totalProtein.toFloat() / goalProtein).coerceIn(0f, 1.3f) else 0f
                    val barColor = if (totalProtein >= goalProtein) {
                        ProteinProgress
                    } else if (totalProtein > 0) {
                        ProteinProgress.copy(alpha = 0.4f)
                    } else {
                        LightMuted.copy(alpha = 0.15f)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Display protein quantity above bar
                        Text(
                            text = "${totalProtein}g",
                            fontSize = 10.sp,
                            fontWeight = if (totalProtein >= goalProtein) FontWeight.Bold else FontWeight.Normal,
                            color = if (totalProtein >= goalProtein) ProteinProgress else LightMuted
                        )

                        // Vertical bar
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .height((8.dp + (70.dp * (fraction / 1.3f))))
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                                .background(barColor)
                        )

                        // Day Letter label
                        Text(
                            text = dayLetter,
                            fontSize = 11.sp,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                            color = if (isToday) MaterialTheme.colorScheme.primary else DarkMuted
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun DiaryDayReceiptHeader(
    dayLabel: String,
    calories: Int,
    protein: Int,
    carbs: Int,
    fat: Int,
    fiber: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("diary_receipt_header"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, LightMuted.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dayLabel.uppercase(Locale.getDefault()),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = LightMuted
                )
                Text(
                    text = "RECEIPT LOG",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    letterSpacing = 0.5.sp
                )
            }
            
            HorizontalDivider(color = LightMuted.copy(alpha = 0.1f), thickness = 1.dp)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TOTAL ENERGY",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkMuted
                    )
                    Text(
                        text = "$calories kcal",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = WarmWhite
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "MACRONUTRIENT SPLIT",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkMuted
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "P: ${protein}g",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ProteinProgress
                        )
                        Text(
                            text = "•",
                            fontSize = 10.sp,
                            color = DarkMuted
                        )
                        Text(
                            text = "C: ${carbs}g",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CarbsProgress
                        )
                        Text(
                            text = "•",
                            fontSize = 10.sp,
                            color = DarkMuted
                        )
                        Text(
                            text = "F: ${fat}g",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = FatsProgress
                        )
                        Text(
                            text = "•",
                            fontSize = 10.sp,
                            color = DarkMuted
                        )
                        Text(
                            text = "Fi: ${fiber}g",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = FiberProgress
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun WeeklyOverviewStrip(
    dates: List<Long>,
    selectedDateMillis: Long,
    dailyTotals: List<Pair<Long, Int>>,
    dailyGoalCalories: Int,
    weeklyAvg: Int,
    onDateSelected: (Long) -> Unit
) {
    val sdfCompare = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val selectedDateStr = sdfCompare.format(Date(selectedDateMillis))
    val dayOfWeekFormat = SimpleDateFormat("EEEEE", Locale.getDefault()) // single letter, e.g. M, T, W...

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, LightMuted.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Weekly Activity",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = WarmWhite
                )
                Text(
                    text = "Weekly avg: $weeklyAvg kcal/day",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = LightMuted
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                dailyTotals.forEach { (dateMillis, totalCal) ->
                    val dateStr = sdfCompare.format(Date(dateMillis))
                    val isSelected = dateStr == selectedDateStr
                    val dayLetter = dayOfWeekFormat.format(Date(dateMillis))

                    val goal = if (dailyGoalCalories > 0) dailyGoalCalories else 2200
                    val fraction = (totalCal.toFloat() / goal).coerceIn(0f, 1.2f)

                    // Vertical bar height from min 4.dp to max 40.dp
                    val barHeight = (4.dp + (36.dp * (fraction / 1.2f)))

                    val barColor = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        fraction >= 0.8f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        fraction > 0f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        else -> LightMuted.copy(alpha = 0.15f)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onDateSelected(dateMillis) }
                            .padding(vertical = 2.dp, horizontal = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .height(barHeight)
                                .clip(CircleShape)
                                .background(barColor)
                        )

                        Text(
                            text = dayLetter,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else DarkMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TodayMacroBreakdown(
    dayLabel: String,
    calorieTotal: Int,
    goalCalories: Int,
    protein: Int,
    goalProtein: Int,
    carbs: Int,
    goalCarbs: Int,
    fat: Int,
    goalFat: Int,
    fiber: Int,
    goalFiber: Int
) {
    DayGroupHeader(
        dayLabel = dayLabel,
        calorieTotal = calorieTotal,
        goalCalories = goalCalories
    )

    Spacer(modifier = Modifier.height(10.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, LightMuted.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Protein Progress (visually flagged if goals aren't fully met)
                Box(modifier = Modifier.weight(1f)) {
                    val isProteinUnderGoal = protein < goalProtein
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Protein",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarmWhite
                            )
                            if (isProteinUnderGoal) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Below protein target",
                                    tint = LimeAccent,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                        
                        Text(
                            text = "${protein}g / ${goalProtein}g",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isProteinUnderGoal) LimeAccent else WarmWhite,
                            modifier = if (isProteinUnderGoal) {
                                Modifier.drawBehind {
                                    val strokeWidth = 1.dp.toPx()
                                    val y = size.height - strokeWidth
                                    drawLine(
                                        color = LimeAccent,
                                        start = androidx.compose.ui.geometry.Offset(0f, y),
                                        end = androidx.compose.ui.geometry.Offset(size.width, y),
                                        strokeWidth = strokeWidth
                                    )
                                }
                            } else Modifier
                        )

                        val pProgress = if (goalProtein > 0) protein.toFloat() / goalProtein.toFloat() else 0f
                        LinearProgressIndicator(
                            progress = { pProgress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape),
                            color = ProteinProgress,
                            trackColor = CalorieRingBg
                        )
                    }
                }

                // Carbs Progress
                Box(modifier = Modifier.weight(1f)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Carbs",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarmWhite
                        )
                        Text(
                            text = "${carbs}g / ${goalCarbs}g",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightMuted
                        )
                        val cProgress = if (goalCarbs > 0) carbs.toFloat() / goalCarbs.toFloat() else 0f
                        LinearProgressIndicator(
                            progress = { cProgress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape),
                            color = CarbsProgress,
                            trackColor = CalorieRingBg
                        )
                    }
                }

                // Fat Progress
                Box(modifier = Modifier.weight(1f)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Fat",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarmWhite
                        )
                        Text(
                            text = "${fat}g / ${goalFat}g",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightMuted
                        )
                        val fProgress = if (goalFat > 0) fat.toFloat() / goalFat.toFloat() else 0f
                        LinearProgressIndicator(
                            progress = { fProgress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape),
                            color = FatsProgress,
                            trackColor = CalorieRingBg
                        )
                    }
                }

                // Fiber Progress
                Box(modifier = Modifier.weight(1f)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Fiber",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarmWhite
                        )
                        Text(
                            text = "${fiber}g / ${goalFiber}g",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightMuted
                        )
                        val fibProgress = if (goalFiber > 0) fiber.toFloat() / goalFiber.toFloat() else 0f
                        LinearProgressIndicator(
                            progress = { fibProgress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape),
                            color = FiberProgress,
                            trackColor = CalorieRingBg
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DateStripSection(
    dates: List<Long>,
    selectedDateMillis: Long,
    onDateSelected: (Long) -> Unit
) {
    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val numFormat = SimpleDateFormat("dd", Locale.getDefault())

    val sdfCompare = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val selectedDayStr = sdfCompare.format(Date(selectedDateMillis))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
                cal.add(Calendar.DAY_OF_YEAR, -1)
                onDateSelected(cal.timeInMillis)
            }
        ) {
            Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Previous Day", tint = LightMuted)
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(dates) { dateMillis ->
                val dateStr = sdfCompare.format(Date(dateMillis))
                val isSelected = dateStr == selectedDayStr

                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .height(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else DarkSurface)
                        .border(
                            BorderStroke(
                                1.dp,
                                if (isSelected) Color.Transparent else LightMuted.copy(alpha = 0.05f)
                            ),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onDateSelected(dateMillis) }
                        .testTag("date_strip_day_$dateStr"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = dayFormat.format(Date(dateMillis)),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) DarkBackground else DarkMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = numFormat.format(Date(dateMillis)),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isSelected) DarkBackground else WarmWhite
                        )
                    }
                }
            }
        }

        IconButton(
            onClick = {
                val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
                cal.add(Calendar.DAY_OF_YEAR, 1)
                onDateSelected(cal.timeInMillis)
            }
        ) {
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next Day", tint = LightMuted)
        }
    }
}

@Composable
fun DayGroupHeader(dayLabel: String, calorieTotal: Int, goalCalories: Int) {
    val friendlyLabel = remember(dayLabel) {
        val sdfCompare = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
        val todayStr = sdfCompare.format(Date())
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayStr = sdfCompare.format(yesterday.time)

        when (dayLabel) {
            todayStr -> "Today"
            yesterdayStr -> "Yesterday"
            else -> dayLabel.substringBefore(",")
        }
    }

    val subtitle = dayLabel.substringAfter(", ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(0.dp, Color.Transparent))
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column {
            Text(
                text = friendlyLabel,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = DarkMuted,
                fontWeight = FontWeight.SemiBold
            )
        }

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = String.format("%,d", calorieTotal),
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = WarmWhite
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "kcal",
                fontSize = 11.sp,
                color = DarkMuted,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
    }
}

@Composable
fun EmptyDiaryPlaceholder(onNavigateToCapture: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(DarkSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = DarkMuted,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your food diary is empty",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = WarmWhite
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Snap a photo of your plate to automatically size portions and log nutritional details.",
                fontSize = 13.sp,
                color = LightMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onNavigateToCapture,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = CircleShape,
                modifier = Modifier.testTag("empty_diary_capture_btn")
            ) {
                Text("Capture Food Now", color = DarkBackground, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DiaryMealItemRow(
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
            // Photo thumbnail with subtle 1px hairline border
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

            // Description
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

            // Calories, Macros, and Delete
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
                    text = "P: ${meal.totalProtein}g  C: ${meal.totalCarbs}g  F: ${meal.totalFat}g",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealDetailView(
    meal: MealLog,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onUpdateMeal: (MealLog) -> Unit,
    builtInFoods: List<FoodItem>
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(meal.name) }

    val moshi = remember { Moshi.Builder().add(KotlinJsonAdapterFactory()).build() }
    val listType = remember { Types.newParameterizedType(List::class.java, FoodItem::class.java) }
    val adapter = remember { moshi.adapter<List<FoodItem>>(listType) }

    var editedItems by remember(meal.itemsJson) {
        mutableStateOf(
            try {
                adapter.fromJson(meal.itemsJson) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        )
    }

    val totalCalories = remember(editedItems) { editedItems.sumOf { it.calories } }
    val totalProtein = remember(editedItems) { editedItems.sumOf { it.protein } }
    val totalCarbs = remember(editedItems) { editedItems.sumOf { it.carbs } }
    val totalFat = remember(editedItems) { editedItems.sumOf { it.fat } }
    val totalFiber = remember(editedItems) { editedItems.sumOf { it.fiber } }

    var showSearchDialog by remember { mutableStateOf(false) }
    var activeReplacementIndex by remember { mutableStateOf<Int?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredBuiltInFoods = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            builtInFoods
        } else {
            builtInFoods.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .clickable(enabled = false) {}
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
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
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(listOf(DarkSurface, DarkBackground))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = DarkMuted,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .statusBarsPadding(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(DarkBackground.copy(alpha = 0.6f))
                        ) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Close", tint = WarmWhite)
                        }

                        if (!isEditing) {
                            IconButton(
                                onClick = { isEditing = true },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(DarkBackground.copy(alpha = 0.6f))
                                    .testTag("detail_edit_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Meal", tint = WarmWhite)
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(
                                    onClick = {
                                        isEditing = false
                                        editedName = meal.name
                                        editedItems = try {
                                            adapter.fromJson(meal.itemsJson) ?: emptyList()
                                        } catch (e: Exception) {
                                            emptyList()
                                        }
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = LightMuted)
                                ) {
                                    Text("CANCEL", fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = {
                                        val updatedJson = adapter.toJson(editedItems)
                                        val updatedMeal = meal.copy(
                                            name = editedName,
                                            totalCalories = totalCalories,
                                            totalProtein = totalProtein,
                                            totalCarbs = totalCarbs,
                                            totalFat = totalFat,
                                            totalFiber = totalFiber,
                                            itemsJson = updatedJson
                                        )
                                        onUpdateMeal(updatedMeal)
                                        isEditing = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = CircleShape
                                ) {
                                    Text("SAVE", color = DarkBackground, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, DarkBackground),
                                    startY = 0f,
                                    endY = Float.POSITIVE_INFINITY
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(24.dp)
                    ) {
                        val formattedTime = remember(meal.timestamp) {
                            val sdf = SimpleDateFormat("EEEE, MMM dd • hh:mm a", Locale.getDefault())
                            sdf.format(Date(meal.timestamp))
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "MEAL LOG DETAIL",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MintAccent,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (isEditing) {
                            TextField(
                                value = editedName,
                                onValueChange = { editedName = it },
                                textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = WarmWhite),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                    unfocusedIndicatorColor = LightMuted.copy(alpha = 0.5f),
                                    focusedTextColor = WarmWhite,
                                    unfocusedTextColor = WarmWhite
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("edit_detail_name_field")
                            )
                        } else {
                            Text(
                                text = editedName,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarmWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formattedTime,
                            fontSize = 12.sp,
                            color = DarkMuted,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(
                        text = "Macronutrient Breakdown",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .weight(1.3f)
                                .height(90.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            border = BorderStroke(1.dp, LightMuted.copy(alpha = 0.05f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("Total Calories", fontSize = 11.sp, color = DarkMuted, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "$totalCalories",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = WarmWhite
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("kcal", fontSize = 11.sp, color = LightMuted)
                                }
                            }
                        }

                        Card(
                            modifier = Modifier
                                .weight(2.2f)
                                .height(90.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            border = BorderStroke(1.dp, LightMuted.copy(alpha = 0.05f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DetailMacroColumn(label = "Protein", value = "${totalProtein}g", color = ProteinProgress)
                                Box(modifier = Modifier.width(1.dp).height(40.dp).background(LightMuted.copy(alpha = 0.1f)))
                                DetailMacroColumn(label = "Carbs", value = "${totalCarbs}g", color = CarbsProgress)
                                Box(modifier = Modifier.width(1.dp).height(40.dp).background(LightMuted.copy(alpha = 0.1f)))
                                DetailMacroColumn(label = "Fat", value = "${totalFat}g", color = FatsProgress)
                                Box(modifier = Modifier.width(1.dp).height(40.dp).background(LightMuted.copy(alpha = 0.1f)))
                                DetailMacroColumn(label = "Fiber", value = "${totalFiber}g", color = FiberProgress)
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "Food Items",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarmWhite
                    )

                    if (isEditing) {
                        Text(
                            text = "ADD ITEM",
                            modifier = Modifier
                                .clickable {
                                    activeReplacementIndex = null
                                    searchQuery = ""
                                    showSearchDialog = true
                                }
                                .testTag("detail_add_item_btn"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            if (editedItems.isEmpty()) {
                item {
                    Text(
                        text = "No food items recorded for this meal.",
                        fontSize = 13.sp,
                        color = DarkMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    )
                }
            } else {
                itemsIndexed(editedItems) { index, item ->
                    if (isEditing) {
                        DetailFoodPortionAdjustmentCard(
                            index = index,
                            item = item,
                            onSliderValueChange = { newValue ->
                                val current = editedItems.toMutableList()
                                current[index] = current[index].recalculate(newValue)
                                editedItems = current
                            },
                            onReplaceItem = {
                                activeReplacementIndex = index
                                searchQuery = ""
                                showSearchDialog = true
                            },
                            onRemoveItem = {
                                val current = editedItems.toMutableList()
                                current.removeAt(index)
                                editedItems = current
                            }
                        )
                    } else {
                        DetailFoodItemRow(item = item)
                    }
                }
            }

            if (!isEditing) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Button(
                            onClick = onDelete,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("detail_delete_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete Meal Entry", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        if (showSearchDialog) {
            AlertDialog(
                onDismissRequest = { showSearchDialog = false },
                containerColor = DarkSurface,
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, LightMuted.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
                title = {
                    Text(
                        text = if (activeReplacementIndex != null) "Replace Food Item" else "Add Manual Food",
                        color = WarmWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("detail_food_search_input"),
                            placeholder = { Text("Search built-in foods...", color = DarkMuted) },
                            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = DarkMuted) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = DarkBackground,
                                unfocusedContainerColor = DarkBackground,
                                focusedTextColor = WarmWhite,
                                unfocusedTextColor = WarmWhite,
                                focusedIndicatorColor = MintAccent
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Text("RESULTS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkMuted, letterSpacing = 1.sp)

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (filteredBuiltInFoods.isEmpty()) {
                                item {
                                    Text(
                                        text = "No standard matches.",
                                        fontSize = 13.sp,
                                        color = DarkMuted,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp)
                                    )
                                }
                            } else {
                                itemsIndexed(filteredBuiltInFoods) { _, builtInFood ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val current = editedItems.toMutableList()
                                                if (activeReplacementIndex != null) {
                                                    current[activeReplacementIndex!!] = builtInFood
                                                } else {
                                                    current.add(builtInFood)
                                                }
                                                editedItems = current
                                                showSearchDialog = false
                                            }
                                            .testTag("detail_search_result_${builtInFood.name.replace(" ", "_")}"),
                                        colors = CardDefaults.cardColors(containerColor = DarkBackground),
                                        border = BorderStroke(1.dp, LightMuted.copy(alpha = 0.05f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(text = builtInFood.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = WarmWhite)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Serving: ${builtInFood.portionSize} • P: ${builtInFood.protein}g • C: ${builtInFood.carbs}g • F: ${builtInFood.fat}g",
                                                    fontSize = 11.sp,
                                                    color = DarkMuted
                                                )
                                            }
                                            Text(text = "${builtInFood.calories} kcal", color = MintAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSearchDialog = false }) {
                        Text("CLOSE", color = MintAccent, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

@Composable
fun DetailFoodItemRow(item: FoodItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, LightMuted.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = WarmWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Portion Size: ${item.portionSize}",
                        fontSize = 12.sp,
                        color = DarkMuted
                    )
                }
                Text(
                    text = "${item.calories} kcal",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                NutrientMiniBadge(label = "Protein", value = "${item.protein}g", color = ProteinProgress)
                NutrientMiniBadge(label = "Carbs", value = "${item.carbs}g", color = CarbsProgress)
                NutrientMiniBadge(label = "Fat", value = "${item.fat}g", color = FatsProgress)
                NutrientMiniBadge(label = "Fiber", value = "${item.fiber}g", color = FiberProgress)
            }
        }
    }
}

@Composable
fun NutrientMiniBadge(label: String, value: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = "$label: $value",
            fontSize = 11.sp,
            color = LightMuted,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun DetailMacroColumn(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 11.sp, color = DarkMuted, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun DetailFoodPortionAdjustmentCard(
    index: Int,
    item: FoodItem,
    onSliderValueChange: (Float) -> Unit,
    onReplaceItem: () -> Unit,
    onRemoveItem: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .testTag("detail_portion_card_$index"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, LightMuted.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = WarmWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Estimate: ${item.calories} kcal • P: ${item.protein}g • C: ${item.carbs}g • F: ${item.fat}g",
                        fontSize = 11.sp,
                        color = DarkMuted
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onReplaceItem, modifier = Modifier.size(28.dp).testTag("detail_replace_food_btn_$index")) {
                        Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = "Swap Food", tint = MintAccent, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onRemoveItem, modifier = Modifier.size(28.dp).testTag("detail_remove_food_btn_$index")) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Remove Item", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Portion Multiplier",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = LightMuted
                )
                Text(
                    text = String.format("%.1fx Servings", item.portionValue),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = LimeAccent
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Slider(
                value = item.portionValue,
                onValueChange = onSliderValueChange,
                valueRange = 0.5f..3.0f,
                steps = 4,
                colors = SliderDefaults.colors(
                    thumbColor = MintAccent,
                    activeTrackColor = MintAccent,
                    inactiveTrackColor = CalorieRingBg,
                    activeTickColor = LimeAccent,
                    inactiveTickColor = DarkMuted
                ),
                modifier = Modifier.testTag("detail_portion_slider_$index")
            )
        }
    }
}

fun getMealType(timestamp: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..10 -> "Breakfast"
        in 11..15 -> "Lunch"
        in 16..21 -> "Dinner"
        else -> "Snack"
    }
}
