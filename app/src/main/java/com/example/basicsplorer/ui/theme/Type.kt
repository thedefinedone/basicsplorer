package com.example.basicsplorer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.basicsplorer.R

val PressStartFont = FontFamily(
    Font(R.font.press_start_2p, FontWeight.Normal)
)

val Typography = Typography(
    titleLarge = TextStyle(
        fontFamily = PressStartFont,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = PressStartFont,
        fontSize = 18.sp
    ),
    bodySmall = TextStyle(
        fontFamily = PressStartFont,
        fontSize = 12.sp
    )
)