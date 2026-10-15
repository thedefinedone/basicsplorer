package com.thedefinedone.basicsplorer.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.thedefinedone.basicsplorer.ui.theme.PressStartFont
import java.io.File

@Composable
fun FileExplorerScreen(
    path: String,
    onBack: () -> Unit,
    onFolderClick: (String) -> Unit
) {
    val gold = Color(0xFFFFD700)
    val context = LocalContext.current
    val currentDir = remember(path) { File(path) }

    // null = Android refused to list this folder
    val listing = remember(path) { currentDir.listFiles()?.toList() }
    val files = remember(listing) {
        listing?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()
    }

    val root = Environment.getExternalStorageDirectory().absolutePath
    val rel = currentDir.absolutePath.removePrefix(root).trimStart('/')
    val isProtectedArea = rel == "Android/data" || rel == "Android/obb" ||
            rel.startsWith("Android/data/") || rel.startsWith("Android/obb/")
    val blocked = listing == null || (isProtectedArea && files.isEmpty())

    BackHandler { onBack() }

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

        when {
            blocked -> {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "ACCESS BLOCKED BY ANDROID",
                        fontFamily = PressStartFont, fontSize = 11.sp, color = Color(0xFFFF9800)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (isProtectedArea)
                            "ANDROID HIDES APP DATA FOLDERS (ANDROID/DATA, ANDROID/OBB) FROM OTHER APPS ON NEWER PHONES. THE SYSTEM FILES APP CAN STILL OPEN THEM."
                        else
                            "THIS FOLDER CAN'T BE READ BY THIS APP.",
                        fontFamily = PressStartFont, fontSize = 8.sp, color = Color.Gray, lineHeight = 13.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { openSystemFilesAt(context, rel) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RectangleShape,
                        border = BorderStroke(2.dp, gold),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = gold)
                    ) {
                        Text("OPEN IN SYSTEM FILES", fontFamily = PressStartFont, fontSize = 10.sp)
                    }
                }
            }

            files.isEmpty() -> {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("EMPTY FOLDER", fontFamily = PressStartFont, fontSize = 10.sp, color = Color.Gray)
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(files, key = { it.absolutePath }) { file ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .clickable {
                                    if (file.isDirectory) onFolderClick(file.absolutePath)
                                    else openFile(context, file)
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

private fun openFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val mime = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(file.extension.lowercase()) ?: "*/*"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Open with"))
    } catch (e: Exception) {
        Toast.makeText(context, "Can't open this file", Toast.LENGTH_SHORT).show()
    }
}

/** Opens the system Files app, at [relPath] (like "Android/data") if the phone allows it. */
private fun openSystemFilesAt(context: Context, relPath: String) {
    val packages = listOf("com.google.android.documentsui", "com.android.documentsui")

    // 1) Try to open the system file manager right at this folder
    if (relPath.isNotEmpty()) {
        val uri = DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", "primary:$relPath")
        for (pkg in packages) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, DocumentsContract.Document.MIME_TYPE_DIR)
                    setPackage(pkg)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                return
            } catch (_: Exception) { /* try next */ }
        }
    }

    // 2) Fall back to just opening the system Files app
    for (pkg in packages) {
        try {
            context.startActivity(Intent(Intent.ACTION_MAIN).apply {
                setClassName(pkg, "com.android.documentsui.files.FilesActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            return
        } catch (_: Exception) { /* try next */ }
    }
    Toast.makeText(context, "System Files app not available on this phone", Toast.LENGTH_LONG).show()
}