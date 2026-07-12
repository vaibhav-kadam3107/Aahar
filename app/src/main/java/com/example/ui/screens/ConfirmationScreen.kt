package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FoodItem
import com.example.ui.theme.*
import com.example.ui.viewmodel.AaharViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmationScreen(
    viewModel: AaharViewModel,
    onNavigateHome: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activePhoto by viewModel.activePhoto.collectAsState()
    val mealName by viewModel.mealName.collectAsState()
    val identifiedItems by viewModel.identifiedItems.collectAsState()

    // Calculated sums for active plate
    val totalCalories = identifiedItems.sumOf { it.calories }
    val totalProtein = identifiedItems.sumOf { it.protein }
    val totalCarbs = identifiedItems.sumOf { it.carbs }
    val totalFat = identifiedItems.sumOf { it.fat }
    val totalFiber = identifiedItems.sumOf { it.fiber }

    // Dialog state for searching/replacing or adding custom foods
    var showSearchDialog by remember { mutableStateOf(false) }
    var activeReplacementIndex by remember { mutableStateOf<Int?>(null) } // null means "adding a new item"
    var searchQuery by remember { mutableStateOf("") }

    val filteredBuiltInFoods = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            viewModel.builtInFoodList
        } else {
            viewModel.builtInFoodList.filter {
                it.name.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    var isEditingMealName by remember { mutableStateOf(false) }
    var tempNameText by remember { mutableStateOf("") }

    Scaffold(
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface.copy(alpha = 0.95f))
                    .border(BorderStroke(1.dp, LightMuted.copy(alpha = 0.05f)), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .padding(24.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Button(
                    onClick = {
                        viewModel.logActiveMeal(context) // Stores to Room and resets active plate
                        onNavigateHome()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("confirm_log_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = DarkBackground,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Log This Meal",
                        color = DarkBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Full bleed Photo Hero section with gradient overlay
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    if (activePhoto != null) {
                        Image(
                            bitmap = activePhoto!!.asImageBitmap(),
                            contentDescription = "Analyzed Plate",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Color wash fallback
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(listOf(DarkSurface, DarkBackground)))
                        )
                    }

                    // Top action overlay bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(DarkBackground.copy(alpha = 0.6f))
                        ) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = WarmWhite)
                        }

                        IconButton(
                            onClick = {
                                tempNameText = mealName
                                isEditingMealName = true
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(DarkBackground.copy(alpha = 0.6f))
                                .testTag("edit_meal_name_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Name", tint = WarmWhite)
                        }
                    }

                    // Linear soft wash at bottom to merge photo into slate background
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

                    // Text overlay on photo bottom
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "LUNCH SNAPSHOT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MintAccent,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = mealName,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarmWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Summary grid cards
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(
                        text = "Meal Nutrition Summary",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Calories card
                        Card(
                            modifier = Modifier
                                .weight(1.5f)
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
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Black,
                                        color = WarmWhite
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("kcal", fontSize = 12.sp, color = LightMuted)
                                }
                            }
                        }

                        // Macro quick review column
                        Card(
                            modifier = Modifier
                                .weight(2f)
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
                                MacroColumn(label = "Protein", value = "${totalProtein}g", color = ProteinProgress)
                                Box(modifier = Modifier.width(1.dp).height(40.dp).background(LightMuted.copy(alpha = 0.1f)))
                                MacroColumn(label = "Carbs", value = "${totalCarbs}g", color = CarbsProgress)
                                Box(modifier = Modifier.width(1.dp).height(40.dp).background(LightMuted.copy(alpha = 0.1f)))
                                MacroColumn(label = "Fat", value = "${totalFat}g", color = FatsProgress)
                            }
                        }
                    }
                }
            }

            // Food list header row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "Identified Foods",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarmWhite
                    )
                    Text(
                        text = "ADD ITEM",
                        modifier = Modifier
                            .clickable {
                                activeReplacementIndex = null // adding new
                                searchQuery = ""
                                showSearchDialog = true
                            }
                            .testTag("add_item_btn"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Food items with portion slider
            if (identifiedItems.isEmpty()) {
                item {
                    Text(
                        text = "No food items on this plate. Add some manually below.",
                        fontSize = 13.sp,
                        color = DarkMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    )
                }
            } else {
                itemsIndexed(identifiedItems) { index, item ->
                    FoodPortionAdjustmentCard(
                        index = index,
                        item = item,
                        onSliderValueChange = { newValue ->
                            viewModel.updateItemPortion(index, newValue)
                        },
                        onReplaceItem = {
                            activeReplacementIndex = index
                            searchQuery = ""
                            showSearchDialog = true
                        },
                        onRemoveItem = {
                            viewModel.removeItem(index)
                        }
                    )
                }
            }

            // Manual fallbacks
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            viewModel.setManualModeWithItems(emptyList()) // Resets and switches to manual builder mode
                        },
                        modifier = Modifier.testTag("search_instead_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "doesn't look right? search instead",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LightMuted
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        // EDIT MEAL NAME DIALOG
        if (isEditingMealName) {
            AlertDialog(
                onDismissRequest = { isEditingMealName = false },
                containerColor = DarkSurface,
                title = { Text("Edit Meal Name", color = WarmWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                text = {
                    TextField(
                        value = tempNameText,
                        onValueChange = { tempNameText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("meal_name_input"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = DarkBackground,
                            unfocusedContainerColor = DarkBackground,
                            focusedTextColor = WarmWhite,
                            unfocusedTextColor = WarmWhite,
                            focusedIndicatorColor = MintAccent
                        ),
                        placeholder = { Text("e.g. Avocado Salmon Plate", color = DarkMuted) }
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (tempNameText.isNotBlank()) {
                                viewModel.setMealName(tempNameText)
                            }
                            isEditingMealName = false
                        }
                    ) {
                        Text("SAVE", color = MintAccent, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isEditingMealName = false }) {
                        Text("CANCEL", color = DarkMuted)
                    }
                }
            )
        }

        // FOOD SEARCH / CORRECTION SHEET DIALOG
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
                        // Search text field
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("food_search_input"),
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
                                        text = "No standard matches. Create custom below.",
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
                                                if (activeReplacementIndex != null) {
                                                    // Replacing existing item at specific index
                                                    viewModel.replaceItem(activeReplacementIndex!!, builtInFood)
                                                } else {
                                                    // Appending new item
                                                    viewModel.addItem(builtInFood)
                                                }
                                                showSearchDialog = false
                                            }
                                            .testTag("search_result_${builtInFood.name.replace(" ", "_")}"),
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
fun FoodPortionAdjustmentCard(
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
            .testTag("portion_card_$index"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, LightMuted.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Name, Portion Text, Swap & Remove buttons
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
                    IconButton(onClick = onReplaceItem, modifier = Modifier.size(28.dp).testTag("replace_food_btn_$index")) {
                        Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = "Swap Food", tint = MintAccent, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onRemoveItem, modifier = Modifier.size(28.dp).testTag("remove_food_btn_$index")) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Remove Item", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Portion slider row
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
                steps = 4, // steps = (3.0 - 0.5) / 0.5 - 1 = 4 (which are 1.0, 1.5, 2.0, 2.5)
                colors = SliderDefaults.colors(
                    thumbColor = MintAccent,
                    activeTrackColor = MintAccent,
                    inactiveTrackColor = CalorieRingBg,
                    activeTickColor = LimeAccent,
                    inactiveTickColor = DarkMuted
                ),
                modifier = Modifier.testTag("portion_slider_$index")
            )
        }
    }
}

@Composable
fun MacroColumn(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 11.sp, color = DarkMuted, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
    }
}
