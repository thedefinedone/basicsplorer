package com.example.basicsplorer.ui.screens

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.basicsplorer.ui.theme.PressStartFont

@Composable
fun StartScreen(onStartClicked: () -> Unit) {
    val gold = Color(0xFFFFD700)
    val context = LocalContext.current

    val infiniteTransition = rememberInfiniteTransition(label = "BlinkTransition")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AlphaAnimation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable {
                val prefs = context.getSharedPreferences("basicsplorer_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("is_first_launch", false).apply()
                onStartClicked()
            }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.65f)
                .border(4.dp, gold, RectangleShape)
                .padding(vertical = 36.dp, horizontal = 12.dp)
        ) {
            Text(
                text = "BASICSPLORER",
                fontFamily = PressStartFont,
                fontSize = 24.sp,
                color = gold,
                maxLines = 1
            )

            Text(
                text = "v0.35",
                fontFamily = PressStartFont,
                fontSize = 10.sp,
                color = Color.Gray
            )

            Text(
                text = "PRESS ANYWHERE\nTO START",
                fontFamily = PressStartFont,
                fontSize = 16.sp,
                color = gold,
                lineHeight = 26.sp,
                modifier = Modifier.alpha(alpha)
            )

            Text(
                text = "CREATED BY: THEDEFINEDONE",
                fontFamily = PressStartFont,
                fontSize = 9.sp,
                color = Color.DarkGray
            )
        }
    }
}