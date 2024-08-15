package com.example.basicsplorer.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.basicsplorer.ui.theme.PressStartFont
import java.io.File

@Composable
fun FileExplorerScreen(
    path: String,
    onBack: () -> Unit,
    onFolderClick: (String) -> Unit
) {
    val gold = Color(0xFFFFD700)
    val currentDir = File(path)
    val files = currentDir.listFiles()?.toList()?.sortedWith(
        compareBy({ !it.isDirectory }, { it.name.lowercase() })
    ) ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.Black,
            border = BorderStroke(2.dp, gold)
        ) {
            Text(
                text = "DIR: ${currentDir.absolutePath}",
                fontFamily = PressStartFont,
                fontSize = 10.sp,
                color = gold,
                lineHeight = 14.sp,
                modifier = Modifier.padding(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(files) { file ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clickable {
                            if (file.isDirectory) {
                                onFolderClick(file.absolutePath)
                            }
                        },
                    color = Color.Black,
                    border = BorderStroke(2.dp, if (file.isDirectory) gold else Color.DarkGray)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (file.isDirectory) "[DIR]" else "[FILE]",
                            fontFamily = PressStartFont,
                            fontSize = 9.sp,
                            color = if (file.isDirectory) gold else Color.Gray,
                            modifier = Modifier.padding(end = 10.dp)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = file.name,
                                fontFamily = PressStartFont,
                                fontSize = 11.sp,
                                color = Color.White
                            )
                            if (!file.isDirectory) {
                                Text(
                                    text = "${file.length() / 1024} KB",
                                    fontFamily = PressStartFont,
                                    fontSize = 8.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RectangleShape,
            border = BorderStroke(4.dp, gold),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = gold)
        ) {
            Text("<< BACK >>", fontFamily = PressStartFont, fontSize = 14.sp)
        }
    }
}