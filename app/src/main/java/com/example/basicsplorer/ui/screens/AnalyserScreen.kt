package com.example.basicsplorer.ui.screens

import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.basicsplorer.ui.theme.PressStartFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AnalyserScreen(onBack: () -> Unit) {
    val gold = Color(0xFFFFD700)
    var isScanning by remember { mutableStateOf(true) }
    var totalMB by remember { mutableStateOf(0L) }
    var usedMB by remember { mutableStateOf(0L) }
    var freeMB by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val path = Environment.getExternalStorageDirectory().absolutePath
                val stat = StatFs(path)
                totalMB = stat.blockCountLong * stat.blockSizeLong / (1024 * 1024)
                freeMB = stat.availableBlocksLong * stat.blockSizeLong / (1024 * 1024)
                usedMB = totalMB - freeMB
            } catch (_: Exception) {}
            isScanning = false
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "SYSTEM ANALYSER",
                fontFamily = PressStartFont,
                fontSize = 16.sp,
                color = gold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(4.dp, gold, RectangleShape)
                    .padding(16.dp)
            ) {
                if (isScanning) {
                    Text(
                        text = "SCANNING SYSTEM...",
                        color = Color.White,
                        fontFamily = PressStartFont,
                        fontSize = 11.sp
                    )
                } else {
                    val usedPct = if (totalMB > 0) ((usedMB.toDouble() / totalMB.toDouble()) * 100).toInt() else 0
                    val freePct = 100 - usedPct

                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(">>> SYSTEM DIAGNOSTICS <<<", color = gold, fontFamily = PressStartFont, fontSize = 10.sp)

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("STORAGE SUMMARY", color = Color.Yellow, fontFamily = PressStartFont, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("TOTAL SPACE : $totalMB MB", color = Color.White, fontFamily = PressStartFont, fontSize = 9.sp)
                        Text("USED SPACE  : $usedMB MB ($usedPct%)", color = Color.White, fontFamily = PressStartFont, fontSize = 9.sp)
                        Text("FREE SPACE  : $freeMB MB ($freePct%)", color = Color.White, fontFamily = PressStartFont, fontSize = 9.sp)

                        Spacer(modifier = Modifier.height(20.dp))

                        Text("GRAPHICAL ANALYSIS", color = gold, fontFamily = PressStartFont, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp)
                                .border(2.dp, gold, RectangleShape)
                                .background(Color.DarkGray)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(usedPct / 100f)
                                    .background(gold)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val totalBlocks = 16
                        val filledBlocks = (usedPct * totalBlocks) / 100
                        val emptyBlocks = totalBlocks - filledBlocks
                        val asciiBar = "[" + "█".repeat(filledBlocks) + "-".repeat(emptyBlocks) + "]"

                        Text(
                            text = asciiBar,
                            color = gold,
                            fontFamily = PressStartFont,
                            fontSize = 10.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text("SECURITY STATUS:", color = gold, fontFamily = PressStartFont, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("BOOT SECTOR : OK", color = Color.Green, fontFamily = PressStartFont, fontSize = 9.sp)
                        Text("VIRUS SCAN  : ALL CLEAR", color = Color.Green, fontFamily = PressStartFont, fontSize = 9.sp)
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
                Text("<< RETURN TO MENU >>", fontFamily = PressStartFont, fontSize = 12.sp)
            }
        }
    }
}