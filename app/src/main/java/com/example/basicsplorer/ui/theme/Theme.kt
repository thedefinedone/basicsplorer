package com.example.basicsplorer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BlackGoldColorScheme = darkColorScheme(
    primary = Color(0xFFFFD700),
    onPrimary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color(0xFFFFD700)
)

@Composable
fun BasicsplorerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BlackGoldColorScheme,
        typography = Typography,
        content = content
    )
}