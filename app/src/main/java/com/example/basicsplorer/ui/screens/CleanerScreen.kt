package com.example.basicsplorer.ui.screens

import android.content.Context
import android.os.Environment
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.basicsplorer.ui.theme.PressStartFont
import kotlinx.coroutines.*
import java.io.File

@Composable
fun CleanerScreen(onBack: () -> Unit) {
    val gold = Color(0xFFFFD700)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var junk by remember { mutableStateOf<List<File>?>(null) }
    var duplicates by remember { mutableStateOf<List<File>?>(null) }
    var corpses by remember { mutableStateOf<List<File>?>(null) }

    var selJunk by remember { mutableStateOf(emptySet<File>()) }
    var selDups by remember { mutableStateOf(emptySet<File>()) }
    var selCorpses by remember { mutableStateOf(emptySet<File>()) }

    var scanningJunk by remember { mutableStateOf(false) }
    var scanningDups by remember { mutableStateOf(false) }
    var scanningDeep by remember { mutableStateOf(false) }

    val root = Environment.getExternalStorageDirectory()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Text("STORAGE CLEANER", fontFamily = PressStartFont, fontSize = 16.sp, color = gold)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    scanningJunk = true
                    scope.launch(Dispatchers.IO) {
                        junk = findJunk(root)
                        scanningJunk = false
                    }
                },
                enabled = !scanningJunk,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RectangleShape,
                border = BorderStroke(2.dp, gold),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = gold),
                contentPadding = PaddingValues(2.dp)
            ) {
                Text("NORMAL", fontFamily = PressStartFont, fontSize = 9.sp)
            }

            Button(
                onClick = {
                    scanningDups = true
                    scope.launch(Dispatchers.IO) {
                        duplicates = findDuplicates(root)
                        scanningDups = false
                    }
                },
                enabled = !scanningDups,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RectangleShape,
                border = BorderStroke(2.dp, gold),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = gold),
                contentPadding = PaddingValues(2.dp)
            ) {
                Text("DUPES", fontFamily = PressStartFont, fontSize = 9.sp)
            }

            Button(
                onClick = {
                    scanningDeep = true
                    scope.launch(Dispatchers.IO) {
                        corpses = findCorpses(root, context)
                        scanningDeep = false
                    }
                },
                enabled = !scanningDeep,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RectangleShape,
                border = BorderStroke(2.dp, gold),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = gold),
                contentPadding = PaddingValues(2.dp)
            ) {
                Text("DEEP", fontFamily = PressStartFont, fontSize = 9.sp)
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(2.dp, gold, RectangleShape)
                .padding(12.dp)
        ) {
            if (junk == null && duplicates == null && corpses == null && !scanningJunk && !scanningDups && !scanningDeep) {
                Column {
                    Text(">>> LIVE CONSOLE STANDBY <<<", fontFamily = PressStartFont, fontSize = 10.sp, color = gold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("SELECT A SCAN OPTION ABOVE:", fontFamily = PressStartFont, fontSize = 9.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("• NORMAL: LOGS, TEMP & CACHE", fontFamily = PressStartFont, fontSize = 8.sp, color = Color.Gray)
                    Text("• DUPES : FILES >1MB DUPLICATES", fontFamily = PressStartFont, fontSize = 8.sp, color = Color.Gray)
                    Text("• DEEP  : ORPHANED APP FOLDERS", fontFamily = PressStartFont, fontSize = 8.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("READY FOR INPUT...", fontFamily = PressStartFont, fontSize = 9.sp, color = Color.Green)
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(">>> LIVE CONSOLE OUTPUT <<<", fontFamily = PressStartFont, fontSize = 10.sp, color = gold)
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            ScrollableSection("JUNK FILES", junk, selJunk, { selJunk = it }, scanningJunk) {
                                selJunk.forEach { it.deleteRecursively() }
                                junk = junk?.filter { it.exists() }
                                selJunk = emptySet()
                            }
                        }

                        item {
                            ScrollableSection("DUPLICATES", duplicates, selDups, { selDups = it }, scanningDups) {
                                selDups.forEach { it.deleteRecursively() }
                                duplicates = duplicates?.filter { it.exists() }
                                selDups = emptySet()
                            }
                        }

                        item {
                            ScrollableSection("ORPHANED DATA", corpses, selCorpses, { selCorpses = it }, scanningDeep) {
                                selCorpses.forEach { it.deleteRecursively() }
                                corpses = corpses?.filter { it.exists() }
                                selCorpses = emptySet()
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
            Text("<< EXIT >>", fontFamily = PressStartFont, fontSize = 14.sp)
        }
    }
}

@Composable
private fun ScrollableSection(
    title: String,
    files: List<File>?,
    selected: Set<File>,
    onSelect: (Set<File>) -> Unit,
    scanning: Boolean,
    onPurge: () -> Unit
) {
    val gold = Color(0xFFFFD700)

    if (scanning) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
            CircularProgressIndicator(color = gold, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("SCANNING $title...", fontFamily = PressStartFont, color = gold, fontSize = 9.sp)
        }
    } else if (files != null) {
        if (files.isEmpty()) {
            Text("> $title: NONE FOUND", fontFamily = PressStartFont, color = Color.Gray, fontSize = 9.sp, modifier = Modifier.padding(vertical = 4.dp))
        } else {
            Text("> $title (${files.size} FOUND):", fontFamily = PressStartFont, color = Color.Yellow, fontSize = 9.sp)
            Column(modifier = Modifier.padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                files.forEach { file ->
                    val isChecked = selected.contains(file)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 40.dp)
                            .clickable {
                                onSelect(if (isChecked) selected - file else selected + file)
                            },
                        color = Color.Black,
                        border = BorderStroke(1.dp, Color.DarkGray)
                    ) {
                        Row(
                            modifier = Modifier.padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(file.name, fontFamily = PressStartFont, fontSize = 8.sp, color = Color.White)
                                Text("${file.length() / 1024} KB", fontFamily = PressStartFont, fontSize = 7.sp, color = Color.Gray)
                            }
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = {
                                    onSelect(if (it) selected + file else selected - file)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = gold,
                                    uncheckedColor = Color.White,
                                    checkmarkColor = Color.Black
                                )
                            )
                        }
                    }
                }
            }
            val sizeKB = selected.sumOf { it.length() / 1024 }
            Button(
                onClick = onPurge,
                enabled = selected.isNotEmpty(),
                shape = RectangleShape,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                border = BorderStroke(2.dp, Color.Red),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
            ) {
                Text("PURGE ${sizeKB}KB", fontFamily = PressStartFont, color = Color.Red, fontSize = 9.sp)
            }
        }
    }
}

private fun findJunk(root: File): List<File> {
    val targets = listOf("log", "tmp", "temp", "cache")
    val list = mutableListOf<File>()
    try {
        root.walkTopDown().maxDepth(6).forEach { file ->
            if (file.isFile && (targets.any { file.name.lowercase().endsWith(it) } || file.parentFile?.name?.contains("cache", true) == true)) {
                list.add(file)
            }
        }
    } catch (_: Exception) {}
    return list
}

private fun findDuplicates(root: File): List<File> {
    val map = mutableMapOf<Long, MutableList<File>>()
    try {
        root.walkTopDown().filter { it.isFile && it.length() > 1024 * 1024 }.forEach {
            map.getOrPut(it.length()) { mutableListOf() }.add(it)
        }
    } catch (_: Exception) {}
    return map.filter { it.value.size > 1 }.flatMap { it.value }
}

private fun findCorpses(root: File, context: Context): List<File> {
    val pm = context.packageManager
    val installed = try { pm.getInstalledPackages(0).map { it.packageName }.toSet() } catch (e: Exception) { emptySet() }
    val list = mutableListOf<File>()

    try {
        listOf("Android/data", "Android/obb").forEach { sub ->
            File(root, sub).listFiles()?.filter { it.isDirectory && !installed.contains(it.name) }?.forEach { dir ->
                dir.walkTopDown().forEach { list.add(it) }
            }
        }

        val thumbs = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), ".thumbnails")
        if (thumbs.exists()) thumbs.walkTopDown().filter { it.isFile && it.length() > 5 * 1024 * 1024 }.forEach { list.add(it) }
    } catch (_: Exception) {}

    return list
}

private fun File.deleteRecursively(): Boolean {
    if (isDirectory) listFiles()?.forEach { it.deleteRecursively() }
    return delete()
}