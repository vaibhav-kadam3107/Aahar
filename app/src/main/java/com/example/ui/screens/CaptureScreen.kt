package com.example.ui.screens

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.ui.theme.*
import com.example.ui.viewmodel.AnalysisState
import com.example.ui.viewmodel.AaharViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
    viewModel: AaharViewModel,
    onNavigateToConfirm: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val analysisState by viewModel.analysisState.collectAsState()

    var isDownloadingPreset by remember { mutableStateOf(false) }
    var downloadErrorText by remember { mutableStateOf<String?>(null) }

    // Preset meals
    val presets = listOf(
        PresetMeal(
            "Avocado Poached Egg Toast",
            "Sourdough, fresh avocado, free-range poached egg, microgreens",
            "https://lh3.googleusercontent.com/aida-public/AB6AXuDp6s5lTpPYn_vwCMd-he-5N_4PneW9IUJ623ItP54OT2b7yALSYDAznJ_VA2sftlz9MVFillwX5mGu45qiuKg1VfoeVvoL8oVzgRZ49WtkRgEXM3FT-cnjM-4l9MuuDtytQJRuW7LR_Qwx0KlOUAkoLdFScXT7QduD_7tYwDKq-kmZToXxtzSym9Y2Adzk89jF4o38gg72cwyfjHEz6q-8XaVD9e_t4vbfX4UAUIPfnDbYSD0k0_b5Uji_ag7YSbEgSypHaS1UsR4"
        ),
        PresetMeal(
            "Grilled Salmon Salad",
            "Omega-3 rich salmon fillet, baby greens, sweet tomatoes, quinoa",
            "https://lh3.googleusercontent.com/aida-public/AB6AXuCwVw5GrkbmUAsvYmHgbSznF_0ro9AvYeS5lcrsb5XEKYLPt3uiHrw6odmEATj16dztOByrFAEqVHrCTgzdOkDLirWiPGbXUj1y_DS6GUL_pWi9FgTcr5MFgJzdggAcr7dNsoyvEaNP_Az59mGm-bvcQHMaEeCHCfKaDIHgN-6dnxj4izvmn6lGcBkie8AlSZn3aucutJEl-j7bUvYpVk2TW2ixW4Bvf9pv0pf2lVSpZy-HTtA5UraMBOWN-O6U4XP85g45AIIYPME"
        ),
        PresetMeal(
            "Harvest Quinoa Bowl",
            "Warm quinoa, chicken strips, sweet potatoes, tahini drizzle",
            "https://lh3.googleusercontent.com/aida-public/AB6AXuAF7_svtWt9SRpmZch7MU4aHfWhmPoGyrWfWZkpofmdfau4dqutuQ0P6RnQ8plfdZCUmCo3GNnYFzzmh7RZKYRg8HwxxA6TUiaoj-rkVLGGV-viIyWGRjuZcX3wPOHpAZGZE7oKs-2E8Lpx3H1VXXeW-j61lXcKAc1qcsWS7mUoI4aGbiCLF2H3jQS8OOhHIY0hQaVRNMbCEFMOTqH8Dw9xQ1w2ouaUeC-VzbS3Qc-UB5JvkSDqnlpAahMb5V1YttgIXnvcIPTpIXM"
        )
    )

    // Launcher for taking a picture with the camera
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            viewModel.startAnalysis(bitmap)
        } else {
            downloadErrorText = "Failed to capture image or camera was canceled."
        }
    }

    // Launcher for camera permission request
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            downloadErrorText = "Camera permission is required to capture photos."
        }
    }

    // Launcher for selecting a picture from device storage
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val bitmap = loadUriAsBitmap(context, uri)
                if (bitmap != null) {
                    viewModel.startAnalysis(bitmap)
                } else {
                    downloadErrorText = "Failed to load the selected image."
                }
            }
        }
    }

    // React to success state and automatically forward to confirmation
    LaunchedEffect(analysisState) {
        if (analysisState is AnalysisState.Success) {
            onNavigateToConfirm()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = DarkBackground),
                title = { Text("CAPTURE MEAL", fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_btn")) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = WarmWhite)
                    }
                }
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
        ) {
            when (val state = analysisState) {
                is AnalysisState.Loading -> {
                    AnalysisLoadingView()
                }
                is AnalysisState.Error -> {
                    AnalysisErrorView(
                        errorMessage = state.message,
                        onRetry = { viewModel.setActivePhoto(null) },
                        onRetryAnalysis = { viewModel.retryAnalysis() }
                    )
                }
                else -> {
                    // Selection view
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Title
                        Text(
                            text = "Log with a single photo",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = WarmWhite,
                                fontSize = 24.sp
                            )
                        )

                        Text(
                            text = "Select a photo of your plate from your gallery, or choose an Instant Demo preset below to test-drive Gemini's advanced multimodal portion estimation.",
                            fontSize = 14.sp,
                            color = LightMuted,
                            lineHeight = 22.sp
                        )

                        // Camera & Gallery Capture Options
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Camera Card
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(130.dp)
                                    .clickable {
                                        val hasCameraPermission = ContextCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.CAMERA
                                        ) == PackageManager.PERMISSION_GRANTED

                                        if (hasCameraPermission) {
                                            cameraLauncher.launch(null)
                                        } else {
                                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                        }
                                    }
                                    .testTag("select_camera_card"),
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                border = BorderStroke(1.dp, LightMuted.copy(alpha = 0.05f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(LimeAccent.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PhotoCamera,
                                            contentDescription = null,
                                            tint = LimeAccent,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Take Photo",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WarmWhite
                                    )
                                }
                            }

                            // Gallery Card
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(130.dp)
                                    .clickable { galleryLauncher.launch("image/*") }
                                    .testTag("select_gallery_card"),
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                border = BorderStroke(1.dp, LightMuted.copy(alpha = 0.05f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldPrimary.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PhotoLibrary,
                                            contentDescription = null,
                                            tint = MintAccent,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "From Gallery",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WarmWhite
                                    )
                                }
                            }
                        }

                        // Presets Title
                        Text(
                            text = "Instant Try: Demo Meal Presets",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (isDownloadingPreset) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = MintAccent)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Fetching high-res meal preset...", fontSize = 13.sp, color = LightMuted)
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(1),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(presets) { preset ->
                                    PresetMealCard(
                                        preset = preset,
                                        onClick = {
                                            isDownloadingPreset = true
                                            downloadErrorText = null
                                            scope.launch {
                                                val bitmap = downloadBitmapFromUrl(context, preset.imageUrl)
                                                isDownloadingPreset = false
                                                if (bitmap != null) {
                                                    viewModel.startAnalysis(bitmap)
                                                } else {
                                                    downloadErrorText = "Failed to download the demo meal. Please check internet connection."
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        downloadErrorText?.let { error ->
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

data class PresetMeal(
    val name: String,
    val description: String,
    val imageUrl: String
)

@Composable
fun PresetMealCard(preset: PresetMeal, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clickable { onClick() }
            .testTag("preset_card_${preset.name.replace(" ", "_")}"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, LightMuted.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model = preset.imageUrl,
                contentDescription = preset.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkBackground)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = preset.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = WarmWhite
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = preset.description,
                    fontSize = 11.sp,
                    color = DarkMuted,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
fun AnalysisLoadingView() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading_anim")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    var loadingStep by remember { mutableStateOf(0) }
    val steps = listOf(
        "Scanning visual plate patterns...",
        "Identifying food components...",
        "Estimating exact portion sizes...",
        "Cross-referencing nutritional database...",
        "Synthesizing calorie & macro profiles..."
    )

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(2000)
            loadingStep = (loadingStep + 1) % steps.size
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(150.dp),
            contentAlignment = Alignment.Center
        ) {
            // Animated radar/scanner outline
            Canvas(modifier = Modifier.size(140.dp * scale)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(MintAccent.copy(alpha = 0.1f), Color.Transparent)
                    )
                )
                drawCircle(
                    color = MintAccent.copy(alpha = 0.3f),
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // Spinner Ring
            CircularProgressIndicator(
                progress = { 0.3f },
                modifier = Modifier.size(100.dp),
                color = MintAccent,
                strokeWidth = 4.dp
            )

            Icon(
                imageVector = Icons.Default.Info, // Generic aesthetic icon
                contentDescription = null,
                tint = LimeAccent,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "AI VISION ANALYSIS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = steps[loadingStep],
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = WarmWhite,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Powered by Google Gemini 3.5 Flash",
            fontSize = 11.sp,
            color = DarkMuted
        )
    }
}

@Composable
fun AnalysisErrorView(
    errorMessage: String,
    onRetry: () -> Unit,
    onRetryAnalysis: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.RestartAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Analysis Failed",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = WarmWhite
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = errorMessage,
            fontSize = 14.sp,
            color = LightMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRetryAnalysis,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .testTag("retry_analysis_btn")
        ) {
            Text("Retry Analysis", color = DarkBackground, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onRetry,
            border = BorderStroke(1.dp, LightMuted.copy(alpha = 0.3f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = WarmWhite),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .testTag("error_retry_btn")
        ) {
            Text("Try Another Photo", fontWeight = FontWeight.Medium)
        }
    }
}

// Utility functions
private suspend fun loadUriAsBitmap(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(inputStream)
    } catch (e: Exception) {
        Log.e("CaptureScreen", "Uri decode failed", e)
        null
    }
}

private suspend fun downloadBitmapFromUrl(context: Context, url: String): Bitmap? = withContext(Dispatchers.IO) {
    try {
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false) // We MUST set allowHardware to false to get a Software Bitmap drawable for Base64 coding!
            .build()
        val result = loader.execute(request)
        if (result is SuccessResult) {
            (result.drawable as? BitmapDrawable)?.bitmap
        } else {
            null
        }
    } catch (e: Exception) {
        Log.e("CaptureScreen", "Coil image download failed: ${e.message}")
        null
    }
}
