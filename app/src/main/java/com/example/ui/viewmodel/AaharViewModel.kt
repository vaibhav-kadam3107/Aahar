package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DailyGoals
import com.example.data.model.FoodItem
import com.example.data.model.MealLog
import com.example.data.repository.MealRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

sealed interface AnalysisState {
    object Idle : AnalysisState
    object Loading : AnalysisState
    data class Success(val items: List<FoodItem>) : AnalysisState
    data class Error(val message: String) : AnalysisState
}

class AaharViewModel(private val repository: MealRepository) : ViewModel() {

    private val TAG = "AaharViewModel"

    // Raw streams from DB
    val mealLogs: StateFlow<List<MealLog>> = repository.allMealLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val dailyGoals: StateFlow<DailyGoals> = repository.dailyGoals
        .map { it ?: DailyGoals() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DailyGoals()
        )

    // UI States for Captured/Active Meal Creation
    private val _activePhoto = MutableStateFlow<Bitmap?>(null)
    val activePhoto: StateFlow<Bitmap?> = _activePhoto.asStateFlow()

    private val _analysisState = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    val analysisState: StateFlow<AnalysisState> = _analysisState.asStateFlow()

    // Temporary list of items being edited/confirmed
    private val _identifiedItems = MutableStateFlow<List<FoodItem>>(emptyList())
    val identifiedItems: StateFlow<List<FoodItem>> = _identifiedItems.asStateFlow()

    private val _mealName = MutableStateFlow("")
    val mealName: StateFlow<String> = _mealName.asStateFlow()

    // Built-in offline food list for manual selection & text search
    val builtInFoodList = listOf(
        FoodItem("Grilled Chicken Breast", "150g", 1.0f, 165, 31, 0, 4, 1),
        FoodItem("Grilled Salmon Fillet", "150g", 1.0f, 320, 34, 0, 18, 0),
        FoodItem("Boiled Quinoa", "150g", 1.0f, 180, 6, 32, 3, 4),
        FoodItem("Organic Poached Egg", "1 Large", 1.0f, 70, 6, 0, 5, 0),
        FoodItem("Avocado Slices", "100g", 1.0f, 160, 2, 9, 15, 7),
        FoodItem("Sourdough Toast Slice", "1 Slice", 1.0f, 120, 4, 24, 1, 2),
        FoodItem("Greek Yogurt Plain", "150g", 1.0f, 100, 15, 6, 0, 0),
        FoodItem("Whey Protein Shake", "1 Scoop", 1.0f, 120, 24, 3, 1, 0),
        FoodItem("Roasted Sweet Potato", "150g", 1.0f, 130, 2, 30, 0, 4),
        FoodItem("Steamed White Rice", "150g", 1.0f, 195, 4, 43, 0, 1),
        FoodItem("Fresh Broccoli Florets", "100g", 1.0f, 35, 3, 7, 0, 3),
        FoodItem("Mixed Fresh Berries", "100g", 1.0f, 50, 1, 12, 0, 3),
        FoodItem("Creamy Peanut Butter", "1 tbsp", 1.0f, 95, 4, 3, 8, 1),
        FoodItem("Harvest Oatmeal Bowl", "1 Bowl", 1.0f, 150, 5, 27, 2, 4)
    )

    init {
        viewModelScope.launch {
            repository.ensureDefaultGoalsExist()
        }
    }

    fun setActivePhoto(bitmap: Bitmap?) {
        _activePhoto.value = bitmap
        if (bitmap == null) {
            _analysisState.value = AnalysisState.Idle
            _identifiedItems.value = emptyList()
            _mealName.value = ""
        }
    }

    fun setMealName(name: String) {
        _mealName.value = name
    }

    /**
     * Start Gemini photo analysis
     */
    fun startAnalysis(bitmap: Bitmap) {
        _activePhoto.value = bitmap
        _analysisState.value = AnalysisState.Loading
        _identifiedItems.value = emptyList()
        _mealName.value = "Analyzing..."

        viewModelScope.launch {
            try {
                val results = repository.analyzePhoto(bitmap)
                _identifiedItems.value = results
                _analysisState.value = AnalysisState.Success(results)
                
                // Set default meal name as the first item name or a general descriptor
                if (results.isNotEmpty()) {
                    _mealName.value = results.first().name
                } else {
                    _mealName.value = "Healthy Plate"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Analysis failed: ", e)
                _analysisState.value = AnalysisState.Error(e.message ?: "Failed to analyze photo")
                _mealName.value = "Analysis Failed"
            }
        }
    }

    /**
     * Retry analysis of the current active photo
     */
    fun retryAnalysis() {
        val bitmap = _activePhoto.value
        if (bitmap != null) {
            startAnalysis(bitmap)
        } else {
            _analysisState.value = AnalysisState.Error("No photo available to retry analysis.")
        }
    }

    /**
     * Transition manual mode (when "search instead" fallback is triggered)
     */
    fun setManualModeWithItems(items: List<FoodItem>) {
        _identifiedItems.value = items
        _analysisState.value = AnalysisState.Success(items)
        if (items.isNotEmpty()) {
            _mealName.value = items.first().name
        } else {
            _mealName.value = "New Meal"
        }
    }

    fun updateItemPortion(index: Int, newPortion: Float) {
        val current = _identifiedItems.value.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].recalculate(newPortion)
            _identifiedItems.value = current
        }
    }

    fun replaceItem(index: Int, newItem: FoodItem) {
        val current = _identifiedItems.value.toMutableList()
        if (index in current.indices) {
            current[index] = newItem
            _identifiedItems.value = current
            // Also update meal name if it was titled after the replaced item
            if (_mealName.value.isEmpty() || _mealName.value == "Analyzing...") {
                _mealName.value = newItem.name
            }
        }
    }

    fun removeItem(index: Int) {
        val current = _identifiedItems.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _identifiedItems.value = current
        }
    }

    fun addItem(newItem: FoodItem) {
        val current = _identifiedItems.value.toMutableList()
        current.add(newItem)
        _identifiedItems.value = current
        if (_mealName.value.isEmpty() || _mealName.value == "Analyzing..." || _mealName.value == "New Meal") {
            _mealName.value = newItem.name
        }
    }

    private fun saveBitmapToLocalFile(context: Context, bitmap: Bitmap): String? {
        return try {
            val filename = "meal_${System.currentTimeMillis()}.jpg"
            val file = File(context.filesDir, filename)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save bitmap to file", e)
            null
        }
    }

    /**
     * Confirms the meal and logs it to the Room database.
     */
    fun logActiveMeal(context: Context) {
        val items = _identifiedItems.value
        if (items.isEmpty()) return

        val totalCal = items.sumOf { it.calories }
        val totalProtein = items.sumOf { it.protein }
        val totalCarbs = items.sumOf { it.carbs }
        val totalFat = items.sumOf { it.fat }
        val totalFiber = items.sumOf { it.fiber }

        // Serialize items back to JSON
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val listType = Types.newParameterizedType(List::class.java, FoodItem::class.java)
        val adapter = moshi.adapter<List<FoodItem>>(listType)
        val itemsJson = adapter.toJson(items)

        val logName = _mealName.value.ifBlank { items.firstOrNull()?.name ?: "Mixed Plate" }

        val activeBitmap = _activePhoto.value

        viewModelScope.launch {
            val savedPath = activeBitmap?.let { bitmap ->
                saveBitmapToLocalFile(context, bitmap)
            }

            val mealLog = MealLog(
                name = logName,
                totalCalories = totalCal,
                totalProtein = totalProtein,
                totalCarbs = totalCarbs,
                totalFat = totalFat,
                totalFiber = totalFiber,
                imageUri = savedPath,
                itemsJson = itemsJson,
                timestamp = System.currentTimeMillis()
            )

            repository.insertMealLog(mealLog)
            // Reset active meal logs
            setActivePhoto(null)
        }
    }

    fun deleteMealLog(mealLog: MealLog) {
        viewModelScope.launch {
            repository.deleteMealLog(mealLog)
        }
    }

    fun updateMealLog(mealLog: MealLog) {
        viewModelScope.launch {
            repository.updateMealLog(mealLog)
        }
    }

    fun saveGoals(goals: DailyGoals) {
        viewModelScope.launch {
            repository.saveGoals(goals)
        }
    }

    // --- AI COACH CHAT STATE & METRICS ---
    private val _chatMessages = MutableStateFlow<List<com.example.data.model.ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<com.example.data.model.ChatMessage>> = _chatMessages.asStateFlow()

    private val _isSendingMessage = MutableStateFlow(false)
    val isSendingMessage: StateFlow<Boolean> = _isSendingMessage.asStateFlow()

    fun loadChatHistory(context: Context) {
        val prefs = context.getSharedPreferences("aahar_prefs", Context.MODE_PRIVATE)
        val jsonText = prefs.getString("chat_history", null)
        if (jsonText != null) {
            try {
                val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                val listType = Types.newParameterizedType(List::class.java, com.example.data.model.ChatMessage::class.java)
                val adapter = moshi.adapter<List<com.example.data.model.ChatMessage>>(listType)
                _chatMessages.value = adapter.fromJson(jsonText) ?: emptyList()
            } catch (e: Exception) {
                _chatMessages.value = emptyList()
            }
        } else {
            _chatMessages.value = emptyList()
        }
    }

    private fun saveChatHistory(context: Context, messages: List<com.example.data.model.ChatMessage>) {
        val prefs = context.getSharedPreferences("aahar_prefs", Context.MODE_PRIVATE)
        try {
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val listType = Types.newParameterizedType(List::class.java, com.example.data.model.ChatMessage::class.java)
            val adapter = moshi.adapter<List<com.example.data.model.ChatMessage>>(listType)
            val jsonText = adapter.toJson(messages)
            prefs.edit().putString("chat_history", jsonText).apply()
        } catch (e: Exception) {
            // ignore
        }
    }

    fun clearChatHistory(context: Context) {
        val prefs = context.getSharedPreferences("aahar_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("chat_history").apply()
        _chatMessages.value = emptyList()
    }

    fun sendMessage(context: Context, text: String, systemInstruction: String) {
        if (text.isBlank()) return

        val userMsg = com.example.data.model.ChatMessage(
            id = java.util.UUID.randomUUID().toString(),
            sender = "user",
            text = text,
            timestamp = System.currentTimeMillis()
        )

        val updatedMessages = _chatMessages.value + userMsg
        _chatMessages.value = updatedMessages
        saveChatHistory(context, updatedMessages)

        _isSendingMessage.value = true

        viewModelScope.launch {
            try {
                val coachResponse = com.example.data.api.GeminiClient.getCoachResponse(updatedMessages, systemInstruction)
                val coachMsg = com.example.data.model.ChatMessage(
                    id = java.util.UUID.randomUUID().toString(),
                    sender = "coach",
                    text = coachResponse,
                    timestamp = System.currentTimeMillis()
                )
                val finalMessages = _chatMessages.value + coachMsg
                _chatMessages.value = finalMessages
                saveChatHistory(context, finalMessages)
            } catch (e: java.lang.Exception) {
                val errorMsg = com.example.data.model.ChatMessage(
                    id = java.util.UUID.randomUUID().toString(),
                    sender = "coach",
                    text = "I'm having trouble connecting to my servers right now (${e.localizedMessage ?: "Unknown error"}). Please ensure your API Key is configured in AI Studio and try again.",
                    timestamp = System.currentTimeMillis()
                )
                val finalMessages = _chatMessages.value + errorMsg
                _chatMessages.value = finalMessages
                saveChatHistory(context, finalMessages)
            } finally {
                _isSendingMessage.value = false
            }
        }
    }
}
