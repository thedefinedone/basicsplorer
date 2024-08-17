package com.example.basicsplorer.ui.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.basicsplorer.ui.theme.PressStartFont

@Composable
fun DashboardScreen(navController: NavController) {
    val gold = Color(0xFFFFD700)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "BASICSPLORER",
            fontFamily = PressStartFont,
            fontSize = 22.sp,
            color = gold
        )

        Text(
            text = "v0.35 // OFFLINE DISK UTILITY",
            fontFamily = PressStartFont,
            fontSize = 8.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 6.dp)
        )

        Spacer(modifier = Modifier.height(36.dp))

        Button(
            onClick = {
                val safePath = Uri.encode("/storage/emulated/0")
                navController.navigate("explorer?path=$safePath")
            },
            modifier = Modifier.fillMaxWidth().height(72.dp),
            shape = RectangleShape,
            border = BorderStroke(4.dp, gold),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = gold)
        ) {
            Text("INTERNAL STORAGE", fontFamily = PressStartFont, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                val safePath = Uri.encode("/storage")
                navController.navigate("explorer?path=$safePath")
            },
            modifier = Modifier.fillMaxWidth().height(72.dp),
            shape = RectangleShape,
            border = BorderStroke(4.dp, gold),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = gold)
        ) {
            Text("EXTERNAL / ROOT", fontFamily = PressStartFont, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { navController.navigate("cleaner") },
                modifier = Modifier.weight(1f).height(72.dp),
                shape = RectangleShape,
                border = BorderStroke(4.dp, gold),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = gold),
                contentPadding = PaddingValues(4.dp)
            ) {
                Text("STORAGE\nCLEANER", fontFamily = PressStartFont, fontSize = 11.sp)
            }

            Button(
                onClick = { navController.navigate("analyzer") },
                modifier = Modifier.weight(1f).height(72.dp),
                shape = RectangleShape,
                border = BorderStroke(4.dp, gold),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = gold),
                contentPadding = PaddingValues(4.dp)
            ) {
                Text("SYSTEM\nANALYSER", fontFamily = PressStartFont, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "[ DEV: THEDEFINEDONE ]",
            fontFamily = PressStartFont,
            fontSize = 9.sp,
            color = gold
        )
    }
}