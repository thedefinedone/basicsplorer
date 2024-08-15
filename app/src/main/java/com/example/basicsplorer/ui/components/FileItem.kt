package com.example.basicsplorer.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.example.basicsplorer.ui.theme.PressStartFont

@Composable
fun FileItem(
    file: DocumentFile,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val gold = Color(0xFFFFD700)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable {
                runCatching {
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(file.uri, file.type)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }.also(context::startActivity)
                }.onFailure {
                    Toast.makeText(context, "Cannot open file", Toast.LENGTH_SHORT).show()
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
                    text = file.name ?: "(Unnamed file)",
                    fontFamily = PressStartFont,
                    fontSize = 11.sp,
                    color = Color.White
                )
                if (!file.isDirectory && file.length() > 0) {
                    Text(
                        text = "${file.length() / 1024} KB",
                        fontFamily = PressStartFont,
                        fontSize = 8.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Custom Retro Share Trigger
            Button(
                onClick = {
                    runCatching {
                        Intent(Intent.ACTION_SEND).apply {
                            type = file.type ?: "*/*"
                            putExtra(Intent.EXTRA_STREAM, file.uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }.also {
                            context.startActivity(Intent.createChooser(it, "Share via"))
                        }
                    }.onFailure {
                        Toast.makeText(context, "Cannot share file", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .height(36.dp)
                    .padding(start = 8.dp),
                shape = RectangleShape,
                border = BorderStroke(1.dp, gold),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = gold),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "SHARE",
                    fontFamily = PressStartFont,
                    fontSize = 8.sp,
                    color = gold
                )
            }
        }
    }
}