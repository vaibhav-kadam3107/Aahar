package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.api.InsForgeClient
import com.example.ui.screens.*
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AaharViewModel
import com.example.ui.viewmodel.ViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize persistent secure session store on app boot
        InsForgeClient.initialize(applicationContext)

        // Create the database-aware shared ViewModel
        val factory = ViewModelFactory(applicationContext)
        val viewModel = ViewModelProvider(this, factory)[AaharViewModel::class.java]

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    AaharAppNavigation(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun AaharAppNavigation(viewModel: AaharViewModel) {
    val navController = rememberNavController()
    
    // Choose start destination based on secure authentication state
    val startDest = if (InsForgeClient.isLoggedIn()) "home" else "auth"

    NavHost(
        navController = navController,
        startDestination = startDest
    ) {
        composable("auth") {
            AuthScreen(
                viewModel = viewModel,
                onAuthSuccess = {
                    navController.navigate("home") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToCapture = { navController.navigate("capture") },
                onNavigateToDiary = { navController.navigate("diary") },
                onNavigateToCoach = { navController.navigate("coach") },
                onNavigateToProfile = { navController.navigate("profile") }
            )
        }

        composable("profile") {
            ProfileScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAuth = {
                    navController.navigate("auth") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("capture") {
            CaptureScreen(
                viewModel = viewModel,
                onNavigateToConfirm = { navController.navigate("confirm") },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("confirm") {
            ConfirmationScreen(
                viewModel = viewModel,
                onNavigateHome = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("diary") {
            DiaryScreen(
                viewModel = viewModel,
                onNavigateToHome = { navController.navigate("home") },
                onNavigateToCapture = { navController.navigate("capture") },
                onNavigateToCoach = { navController.navigate("coach") }
            )
        }

        composable("coach") {
            CoachScreen(
                viewModel = viewModel,
                onNavigateToHome = { navController.navigate("home") },
                onNavigateToDiary = { navController.navigate("diary") }
            )
        }
    }
}
