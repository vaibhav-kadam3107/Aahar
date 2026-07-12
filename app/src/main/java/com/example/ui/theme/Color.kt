package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color

// Global theme state for toggling between dark and light mode
object AppThemeState {
    var isDark by mutableStateOf(true)
}

// Premium Dark & Light Editorial Theme colors with dynamic getters
val DarkBackground: Color get() = if (AppThemeState.isDark) Color(0xFF0C0E0B) else Color(0xFFF7F6F0)
val DarkSurface: Color get() = if (AppThemeState.isDark) Color(0xFF151912) else Color(0xFFEEEDE6)
val DarkSurfaceHigh: Color get() = if (AppThemeState.isDark) Color(0xFF1F241B) else Color(0xFFE4E3DB)
val DarkSurfaceLowest: Color get() = if (AppThemeState.isDark) Color(0xFF090A08) else Color(0xFFFDFDFB)

val EmeraldPrimary: Color get() = if (AppThemeState.isDark) Color(0xFF2D5A27) else Color(0xFF1B4D17)
val EmeraldPrimaryLight: Color get() = if (AppThemeState.isDark) Color(0xFF437A3D) else Color(0xFF387B32)
val MintAccent: Color get() = if (AppThemeState.isDark) Color(0xFFBCF0AE) else Color(0xFF2D5A27)
val LimeAccent: Color get() = if (AppThemeState.isDark) Color(0xFFCCF078) else Color(0xFF6B8E23)
val WarmWhite: Color get() = if (AppThemeState.isDark) Color(0xFFFBF9F4) else Color(0xFF1B1C1A)
val PaperWhite: Color get() = if (AppThemeState.isDark) Color(0xFFECECE8) else Color(0xFF262724)
val DarkMuted: Color get() = if (AppThemeState.isDark) Color(0xFF727A6E) else Color(0xFF8C9486)
val LightMuted: Color get() = if (AppThemeState.isDark) Color(0xFFC2C9BB) else Color(0xFF5C6356)

val CalorieRingBg: Color get() = if (AppThemeState.isDark) Color(0xFF1E2419) else Color(0xFFE4EADF)
val CalorieRingFg: Color get() = if (AppThemeState.isDark) Color(0xFFBCF0AE) else Color(0xFF2D5A27)
val ProteinProgress: Color get() = if (AppThemeState.isDark) Color(0xFFCCF078) else Color(0xFF6B8E23)
val CarbsProgress: Color get() = if (AppThemeState.isDark) Color(0xFF8CD36C) else Color(0xFF437A3D)
val FatsProgress: Color get() = if (AppThemeState.isDark) Color(0xFF74BDCC) else Color(0xFF1E88E5)
val FiberProgress: Color get() = if (AppThemeState.isDark) Color(0xFFFFB74D) else Color(0xFFF57C00)
val VitaminProgress: Color get() = if (AppThemeState.isDark) Color(0xFFBA68C8) else Color(0xFF7B1FA2)

