package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.InsForgeClient
import com.example.ui.theme.*
import com.example.ui.viewmodel.AaharViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: AaharViewModel,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("auth_screen"),
        containerColor = DarkBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            EmeraldPrimary.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Application Branding Logo
                Icon(
                    imageVector = Icons.Default.RestaurantMenu,
                    contentDescription = null,
                    tint = MintAccent,
                    modifier = Modifier.size(72.dp)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "AAHAR",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp,
                        color = WarmWhite,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Your AI Nutritional Companion",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = LightMuted,
                        textAlign = TextAlign.Center
                    )
                }

                // Main Authentication Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, LightMuted.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isLoginMode) "SIGN IN" else "CREATE ACCOUNT",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = LimeAccent,
                            letterSpacing = 1.5.sp
                        )

                        errorMessage?.let { msg ->
                            Text(
                                text = msg,
                                color = Color(0xFFEF5350), // Soft red
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_error_message")
                            )
                        }

                        // Name Field (only in Signup Mode)
                        AnimatedVisibility(
                            visible = !isLoginMode,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Your Name", color = LightMuted) },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = LightMuted) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_input_name"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MintAccent,
                                    unfocusedBorderColor = LightMuted.copy(alpha = 0.2f),
                                    focusedLabelColor = MintAccent,
                                    cursorColor = MintAccent,
                                    focusedTextColor = WarmWhite,
                                    unfocusedTextColor = WarmWhite
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // Email Field
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address", color = LightMuted) },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = LightMuted) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_input_email"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MintAccent,
                                unfocusedBorderColor = LightMuted.copy(alpha = 0.2f),
                                focusedLabelColor = MintAccent,
                                cursorColor = MintAccent,
                                focusedTextColor = WarmWhite,
                                unfocusedTextColor = WarmWhite
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Password Field
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password", color = LightMuted) },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = LightMuted) },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                                        tint = LightMuted
                                    )
                                }
                            },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_input_password"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MintAccent,
                                unfocusedBorderColor = LightMuted.copy(alpha = 0.2f),
                                focusedLabelColor = MintAccent,
                                cursorColor = MintAccent,
                                focusedTextColor = WarmWhite,
                                unfocusedTextColor = WarmWhite
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Submit Button
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                errorMessage = null
                                if (email.isBlank() || password.isBlank()) {
                                    errorMessage = "Please enter both email and password."
                                    return@Button
                                }
                                if (!isLoginMode && name.isBlank()) {
                                    errorMessage = "Please enter your name."
                                    return@Button
                                }

                                isLoading = true
                                coroutineScope.launch {
                                    try {
                                        val success = if (isLoginMode) {
                                            InsForgeClient.login(context, email.trim(), password.trim())
                                        } else {
                                            InsForgeClient.signup(context, email.trim(), password.trim(), name.trim())
                                        }

                                        if (success) {
                                            // Trigger callback on success
                                            onAuthSuccess()
                                        } else {
                                            errorMessage = if (isLoginMode) {
                                                "Invalid email or password. Please try again."
                                            } else {
                                                "Registration failed. Email might already exist."
                                            }
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = e.message ?: "Authentication error occurred."
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("auth_submit_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MintAccent,
                                contentColor = DarkBackground
                            ),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = DarkBackground,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Text(
                                    text = if (isLoginMode) "Sign In" else "Create Account",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Toggle Login/Register
                        TextButton(
                            onClick = {
                                isLoginMode = !isLoginMode
                                errorMessage = null
                            },
                            modifier = Modifier.testTag("auth_mode_toggle_button")
                        ) {
                            Text(
                                text = if (isLoginMode) "Don't have an account? Sign Up" else "Already have an account? Sign In",
                                color = LightMuted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}
