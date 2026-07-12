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
import com.example.ui.screens.*
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AaharViewModel
import com.example.ui.viewmodel.ViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
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
                onNavigateBack = { navController.popBackStack() }
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
