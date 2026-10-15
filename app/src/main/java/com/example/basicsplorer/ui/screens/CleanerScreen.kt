package com.example.basicsplorer.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.basicsplorer.ui.theme.PressStartFont
import kotlinx.coroutines.*
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Gold = Color(0xFFFFD700)
private val Good = Color(0xFF39FF14)
private val Danger = Color(0xFFFF4444)

/* ───────── MODEL ───────── */

/** How much deleting something could hurt. */
private enum class Risk(val label: String, val color: Color, val autoSelect: Boolean, val selectable: Boolean) {
    SAFE("SAFE", Color(0xFF39FF14), true, true),          // nothing needs it
    LOW("LOW", Color(0xFFFFEB3B), true, true),            // rebuilt automatically / identical copy
    CAUTION("CAUTION", Color(0xFFFF9800), false, true),   // personal or unrecognised file
    DANGER("DANGER", Color(0xFFFF4444), false, true),     // can reset an app or lose data
    LOCKED("KEPT", Color(0xFF4DD0E1), false, false)       // the original we refuse to delete
}

private data class Item(
    val file: File,
    val size: Long,
    val count: Int,
    val risk: Risk,
    val reason: String
)

private class Env(val root: File, private val pm: PackageManager, context: Context) {
    // Since Android 11 an app only sees other apps if it holds QUERY_ALL_PACKAGES.
    // Without it "not installed" would be a guess, so we assume everything IS installed.
    val canSeeAll: Boolean = Build.VERSION.SDK_INT < 30 ||
            context.checkSelfPermission("android.permission.QUERY_ALL_PACKAGES") == PackageManager.PERMISSION_GRANTED

    fun installed(pkg: String): Boolean {
        if (!canSeeAll) return true
        return try { pm.getPackageInfo(pkg, 0); true } catch (_: Exception) { false }
    }

    fun rel(f: File): String = f.absolutePath.removePrefix(root.absolutePath).trimStart('/')
}

private val USER_DIRS = setOf("DCIM", "Pictures", "Movies", "Music", "Documents", "Download", "Downloads", "Recordings", "Podcasts")

/* ───────── SCREEN ───────── */

@Composable
fun CleanerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val root = Environment.getExternalStorageDirectory()
    val env = remember { Env(root, context.packageManager, context) }

    var junk by remember { mutableStateOf<List<Item>?>(null) }
    var duplicates by remember { mutableStateOf<List<Item>?>(null) }
    var corpses by remember { mutableStateOf<List<Item>?>(null) }

    var selJunk by remember { mutableStateOf(emptySet<Item>()) }
    var selDups by remember { mutableStateOf(emptySet<Item>()) }
    var selCorpses by remember { mutableStateOf(emptySet<Item>()) }

    var scanningJunk by remember { mutableStateOf(false) }
    var scanningDups by remember { mutableStateOf(false) }
    var scanningDeep by remember { mutableStateOf(false) }

    var totalBytes by remember { mutableStateOf(0L) }
    var freeBytes by remember { mutableStateOf(0L) }
    var freedSession by remember { mutableStateOf(0L) }
    var lastFreed by remember { mutableStateOf<Long?>(null) }

    fun refreshStorage() {
        try {
            val s = StatFs(root.path)
            totalBytes = s.totalBytes
            freeBytes = s.availableBytes
        } catch (_: Exception) {}
    }
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { refreshStorage() } }

    val allSelected = (selJunk + selDups + selCorpses).distinctBy { it.file.path }

    fun purge(victims: Set<Item>, afterDelete: () -> Unit) {
        val real = victims.filter { it.risk.selectable }
        scope.launch(Dispatchers.IO) {
            real.forEach { it.file.wipe() }
            val freed = real.filter { !it.file.exists() }.sumOf { it.size }
            afterDelete()
            freedSession += freed
            lastFreed = freed
            refreshStorage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Text("STORAGE CLEANER", fontFamily = PressStartFont, fontSize = 16.sp, color = Gold)
        Spacer(modifier = Modifier.height(12.dp))

        ImpactPanel(
            total = totalBytes,
            free = freeBytes,
            selected = allSelected,
            freedSession = freedSession,
            lastFreed = lastFreed
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ScanButton("NORMAL", !scanningJunk, Modifier.weight(1f)) {
                scanningJunk = true; selJunk = emptySet()
                scope.launch(Dispatchers.IO) { junk = findJunk(env); scanningJunk = false }
            }
            ScanButton("DUPES", !scanningDups, Modifier.weight(1f)) {
                scanningDups = true; selDups = emptySet()
                scope.launch(Dispatchers.IO) { duplicates = findDuplicates(env); scanningDups = false }
            }
            ScanButton("DEEP", !scanningDeep, Modifier.weight(1f)) {
                scanningDeep = true; selCorpses = emptySet()
                scope.launch(Dispatchers.IO) { corpses = findCorpses(env); scanningDeep = false }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(2.dp, Gold, RectangleShape)
                .padding(12.dp)
        ) {
            if (junk == null && duplicates == null && corpses == null && !scanningJunk && !scanningDups && !scanningDeep) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(">>> LIVE CONSOLE STANDBY <<<", fontFamily = PressStartFont, fontSize = 10.sp, color = Gold)
                    Spacer(Modifier.height(12.dp))
                    Text("SELECT A SCAN OPTION ABOVE:", fontFamily = PressStartFont, fontSize = 9.sp, color = Color.White)
                    Spacer(Modifier.height(10.dp))
                    Text("• NORMAL: LOGS, TEMP & CACHE", fontFamily = PressStartFont, fontSize = 8.sp, color = Color.Gray)
                    Text("• DUPES : VERIFIED IDENTICAL COPIES", fontFamily = PressStartFont, fontSize = 8.sp, color = Color.Gray)
                    Text("• DEEP  : LEFTOVERS OF UNINSTALLED APPS", fontFamily = PressStartFont, fontSize = 8.sp, color = Color.Gray)
                    Spacer(Modifier.height(14.dp))
                    Text("RISK LEVELS:", fontFamily = PressStartFont, fontSize = 8.sp, color = Color.White)
                    Spacer(Modifier.height(4.dp))
                    Risk.values().forEach {
                        val txt = when (it) {
                            Risk.SAFE -> "NOTHING NEEDS IT"
                            Risk.LOW -> "REBUILT BY APPS / IDENTICAL COPY"
                            Risk.CAUTION -> "PERSONAL OR UNKNOWN FILE"
                            Risk.DANGER -> "CAN RESET AN APP OR LOSE DATA"
                            Risk.LOCKED -> "ORIGINAL, NEVER DELETED"
                        }
                        Text("■ ${it.label}: $txt", fontFamily = PressStartFont, fontSize = 7.sp, color = it.color, modifier = Modifier.padding(vertical = 2.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("TIP: TAP VIEW ON ANY ROW TO PREVIEW A FILE BEFORE DELETING.", fontFamily = PressStartFont, fontSize = 7.sp, color = Color.Gray, lineHeight = 11.sp)
                    Spacer(Modifier.height(14.dp))
                    Text("READY FOR INPUT...", fontFamily = PressStartFont, fontSize = 9.sp, color = Good)
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(">>> LIVE CONSOLE OUTPUT <<<", fontFamily = PressStartFont, fontSize = 10.sp, color = Gold)
                    Spacer(Modifier.height(12.dp))

                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            Section(
                                title = "JUNK FILES", items = junk, selected = selJunk, onSelect = { selJunk = it },
                                scanning = scanningJunk, freeNow = freeBytes, note = null
                            ) {
                                val v = selJunk
                                purge(v) { junk = junk?.filter { it.file.exists() }; selJunk = emptySet() }
                            }
                        }
                        item {
                            Section(
                                title = "DUPLICATES", items = duplicates, selected = selDups, onSelect = { selDups = it },
                                scanning = scanningDups, freeNow = freeBytes,
                                note = "Matched by size + start/middle/end content. The oldest copy of each group is KEPT."
                            ) {
                                val v = selDups
                                purge(v) { duplicates = duplicates?.filter { it.file.exists() }; selDups = emptySet() }
                            }
                        }
                        item {
                            Section(
                                title = "ORPHANED DATA", items = corpses, selected = selCorpses, onSelect = { selCorpses = it },
                                scanning = scanningDeep, freeNow = freeBytes,
                                note = if (!env.canSeeAll)
                                    "LIMITED: this app can't see which apps are installed (needs QUERY_ALL_PACKAGES), so app folders are skipped to protect your apps."
                                else if (Build.VERSION.SDK_INT >= 30)
                                    "Android 11+ hides Android/data from file managers, so this may find little."
                                else null
                            ) {
                                val v = selCorpses
                                purge(v) { corpses = corpses?.filter { it.file.exists() }; selCorpses = emptySet() }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { openSystemFiles(context) },
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RectangleShape,
            border = BorderStroke(2.dp, Gold),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Gold)
        ) {
            Text("OPEN SYSTEM FILES (ANDROID/DATA)", fontFamily = PressStartFont, fontSize = 8.sp)
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RectangleShape,
            border = BorderStroke(4.dp, Gold),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Gold)
        ) {
            Text("<< EXIT >>", fontFamily = PressStartFont, fontSize = 14.sp)
        }
    }
}


/* ───────── IMPACT PANEL ───────── */

@Composable
private fun ImpactPanel(total: Long, free: Long, selected: List<Item>, freedSession: Long, lastFreed: Long?) {
    val bytes = selected.sumOf { it.size }
    val used = (total - free).coerceAtLeast(0L)
    val afterFree = (free + bytes).coerceAtMost(total)
    val pctNow = if (total > 0) (used * 100 / total).toInt() else 0
    val pctAfter = if (total > 0) ((used - bytes).coerceAtLeast(0L) * 100 / total).toInt() else 0
    val byRisk = selected.groupBy { it.risk }

    Column(
        modifier = Modifier.fillMaxWidth().border(2.dp, Gold, RectangleShape).padding(10.dp)
    ) {
        Text("IMPACT", fontFamily = PressStartFont, fontSize = 9.sp, color = Gold)
        Spacer(Modifier.height(8.dp))

        if (total > 0) {
            val keep = (used - bytes).coerceAtLeast(0L).toFloat().coerceAtLeast(0.0001f)
            val toFree = bytes.coerceAtMost(used).toFloat().coerceAtLeast(0.0001f)
            val freeW = free.toFloat().coerceAtLeast(0.0001f)
            Row(Modifier.fillMaxWidth().height(14.dp).border(1.dp, Color.DarkGray, RectangleShape)) {
                Box(Modifier.weight(keep).fillMaxHeight().background(Color.LightGray))
                if (bytes > 0) Box(Modifier.weight(toFree).fillMaxHeight().background(Danger))
                Box(Modifier.weight(freeW).fillMaxHeight().background(Color(0xFF1A1A1A)))
            }
            Spacer(Modifier.height(6.dp))
            Text("USED ${fmt(used)} / ${fmt(total)}  ($pctNow%)", fontFamily = PressStartFont, fontSize = 7.sp, color = Color.Gray)
        } else {
            Text("READING STORAGE...", fontFamily = PressStartFont, fontSize = 7.sp, color = Color.Gray)
        }

        Spacer(Modifier.height(8.dp))

        if (selected.isEmpty()) {
            Text("TICK FILES TO SEE THE IMPACT", fontFamily = PressStartFont, fontSize = 8.sp, color = Color.Gray)
        } else {
            Text("SELECTED: ${selected.size} ITEMS · ${fmt(bytes)}", fontFamily = PressStartFont, fontSize = 8.sp, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text("FREE: ${fmt(free)} → ${fmt(afterFree)}   USED: $pctNow% → $pctAfter%", fontFamily = PressStartFont, fontSize = 7.sp, color = Good)
            Spacer(Modifier.height(6.dp))
            Text(riskSummary(selected), fontFamily = PressStartFont, fontSize = 7.sp)
            Spacer(Modifier.height(6.dp))

            val (verdict, vColor) = when {
                byRisk.containsKey(Risk.DANGER) -> "⚠ WILL AFFECT INSTALLED APPS: LOGINS, SAVES OR SETTINGS MAY BE LOST" to Risk.DANGER.color
                byRisk.containsKey(Risk.CAUTION) -> "REVIEW FIRST: INCLUDES PERSONAL OR UNRECOGNISED FILES" to Risk.CAUTION.color
                byRisk.containsKey(Risk.LOW) -> "✓ OK: APPS REBUILD THESE. NO DATA LOSS" to Risk.LOW.color
                else -> "✓ SAFE: NOTHING THE SYSTEM OR APPS NEED" to Risk.SAFE.color
            }
            Text(verdict, fontFamily = PressStartFont, fontSize = 8.sp, color = vColor, lineHeight = 12.sp)
        }

        if (freedSession > 0) {
            Spacer(Modifier.height(8.dp))
            Text(
                "✓ FREED THIS SESSION: ${fmt(freedSession)}" + (lastFreed?.let { "  (LAST: +${fmt(it)})" } ?: ""),
                fontFamily = PressStartFont, fontSize = 8.sp, color = Good
            )
        }
    }
}

private fun riskSummary(items: List<Item>): AnnotatedString = buildAnnotatedString {
    val g = items.groupBy { it.risk }
    var first = true
    Risk.values().forEach { r ->
        val list = g[r] ?: return@forEach
        if (!first) withStyle(SpanStyle(color = Color.Gray)) { append("  ·  ") }
        first = false
        withStyle(SpanStyle(color = r.color)) { append("${r.label} ${list.size} (${fmt(list.sumOf { it.size })})") }
    }
}

/* ───────── SECTIONS ───────── */

@Composable
private fun ScanButton(label: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled, modifier = modifier.height(52.dp), shape = RectangleShape,
        border = BorderStroke(2.dp, Gold),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Gold),
        contentPadding = PaddingValues(2.dp)
    ) { Text(label, fontFamily = PressStartFont, fontSize = 9.sp) }
}

@Composable
private fun PickChip(label: String, active: Boolean, hasItems: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .border(1.dp, if (active) Gold else Color.DarkGray, RectangleShape)
            .background(if (active) Color(0x33FFD700) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label, fontFamily = PressStartFont, fontSize = 7.sp,
            color = if (active) Gold else if (hasItems) Color.White else Color.DarkGray
        )
    }
}

@Composable
private fun Section(
    title: String,
    items: List<Item>?,
    selected: Set<Item>,
    onSelect: (Set<Item>) -> Unit,
    scanning: Boolean,
    freeNow: Long,
    note: String?,
    onPurge: () -> Unit
) {
    var confirm by remember { mutableStateOf(false) }
    var ack by remember { mutableStateOf(false) }
    var shown by remember(items) { mutableStateOf(60) }
    var preview by remember { mutableStateOf<Item?>(null) }

    if (scanning) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
            CircularProgressIndicator(color = Gold, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text("SCANNING $title...", fontFamily = PressStartFont, color = Gold, fontSize = 9.sp)
        }
        return
    }
    if (items == null) return

    if (items.isEmpty()) {
        Text("> $title: NONE FOUND", fontFamily = PressStartFont, color = Color.Gray, fontSize = 9.sp, modifier = Modifier.padding(vertical = 4.dp))
        if (note != null) Text(note, fontFamily = PressStartFont, color = Risk.CAUTION.color, fontSize = 7.sp, lineHeight = 11.sp)
        return
    }

    val deletable = items.filter { it.risk.selectable }
    val safeTarget = items.filter { it.risk.autoSelect }.toSet()
    val allTarget = deletable.toSet()
    val selectedBytes = remember(selected) { selected.sumOf { it.size } }

    Text("> $title (${deletable.size} FOUND · ${fmt(deletable.sumOf { it.size })}):", fontFamily = PressStartFont, color = Color.Yellow, fontSize = 9.sp)
    Spacer(Modifier.height(4.dp))
    Text(riskSummary(items), fontFamily = PressStartFont, fontSize = 7.sp, lineHeight = 11.sp)
    if (note != null) {
        Spacer(Modifier.height(4.dp))
        Text(note, fontFamily = PressStartFont, color = Color.Gray, fontSize = 7.sp, lineHeight = 11.sp)
    }

    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        PickChip("SAFE+LOW (${safeTarget.size})", safeTarget.isNotEmpty() && selected == safeTarget, safeTarget.isNotEmpty()) { onSelect(safeTarget) }
        PickChip("ALL (${allTarget.size})", allTarget.isNotEmpty() && selected == allTarget, allTarget.isNotEmpty()) { onSelect(allTarget) }
        PickChip("NONE", selected.isEmpty(), true) { onSelect(emptySet()) }
    }
    Spacer(Modifier.height(2.dp))
    Text("ALL INCLUDES CAUTION/DANGER ITEMS. YOU'LL BE ASKED TO CONFIRM.", fontFamily = PressStartFont, fontSize = 6.sp, color = Color.Gray, lineHeight = 10.sp)

    Column(modifier = Modifier.padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.take(shown).forEach { item ->
            val isChecked = selected.contains(item)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .clickable(enabled = item.risk.selectable) {
                        onSelect(if (isChecked) selected - item else selected + item)
                    },
                color = Color.Black,
                border = BorderStroke(1.dp, if (isChecked) item.risk.color else Color.DarkGray)
            ) {
                Row(modifier = Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                item.risk.label,
                                fontFamily = PressStartFont, fontSize = 6.sp, color = Color.Black,
                                modifier = Modifier.background(item.risk.color).padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(item.file.name, fontFamily = PressStartFont, fontSize = 8.sp, color = Color.White, maxLines = 1, modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            (if (item.count > 1) "${item.count} FILES · " else "") + fmt(item.size),
                            fontFamily = PressStartFont, fontSize = 7.sp, color = Color.Gray
                        )
                        Text(item.reason, fontFamily = PressStartFont, fontSize = 6.sp, color = item.risk.color, lineHeight = 10.sp, maxLines = 3)
                    }
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .border(1.dp, Gold, RectangleShape)
                            .clickable { preview = item }
                            .padding(horizontal = 7.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("VIEW", fontFamily = PressStartFont, fontSize = 7.sp, color = Gold) }
                    Checkbox(
                        checked = isChecked,
                        enabled = item.risk.selectable,
                        onCheckedChange = { onSelect(if (it) selected + item else selected - item) },
                        colors = CheckboxDefaults.colors(checkedColor = item.risk.color, uncheckedColor = Color.White, checkmarkColor = Color.Black)
                    )
                }
            }
        }
        if (items.size > shown) {
            TextButton(onClick = { shown += 60 }) {
                Text("SHOW MORE (${items.size - shown} HIDDEN)", fontFamily = PressStartFont, fontSize = 8.sp, color = Gold)
            }
        }
    }

    Button(
        onClick = { ack = false; confirm = true },
        enabled = selected.isNotEmpty(),
        shape = RectangleShape,
        modifier = Modifier.fillMaxWidth().height(40.dp),
        border = BorderStroke(2.dp, Danger),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
    ) {
        Text("PURGE ${selected.size} · ${fmt(selectedBytes)}", fontFamily = PressStartFont, color = Danger, fontSize = 9.sp)
    }

    preview?.let { p ->
        PreviewDialog(
            item = p,
            isSelected = selected.contains(p),
            onToggle = { onSelect(if (selected.contains(p)) selected - p else selected + p) },
            onDismiss = { preview = null }
        )
    }

    if (confirm) {
        val needsAck = selected.any { it.risk == Risk.CAUTION || it.risk == Risk.DANGER }
        AlertDialog(
            onDismissRequest = { confirm = false },
            containerColor = Color.Black,
            shape = RectangleShape,
            title = { Text("DELETE ${selected.size} ITEMS?", fontFamily = PressStartFont, fontSize = 11.sp, color = Danger) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("FREES ${fmt(selectedBytes)}", fontFamily = PressStartFont, fontSize = 9.sp, color = Good)
                    Spacer(Modifier.height(4.dp))
                    Text("FREE: ${fmt(freeNow)} → ${fmt(freeNow + selectedBytes)}", fontFamily = PressStartFont, fontSize = 8.sp, color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    Text(riskSummary(selected.toList()), fontFamily = PressStartFont, fontSize = 7.sp, lineHeight = 11.sp)
                    Spacer(Modifier.height(8.dp))
                    if (needsAck) {
                        val worst = if (selected.any { it.risk == Risk.DANGER })
                            "THIS INCLUDES DATA OF INSTALLED APPS. THEY MAY LOG YOU OUT OR LOSE SAVES."
                        else "THIS INCLUDES PERSONAL OR UNRECOGNISED FILES."
                        Text(worst, fontFamily = PressStartFont, fontSize = 7.sp, color = Risk.DANGER.color, lineHeight = 11.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { ack = !ack },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = ack, onCheckedChange = { ack = it },
                                colors = CheckboxDefaults.colors(checkedColor = Danger, uncheckedColor = Color.White, checkmarkColor = Color.Black)
                            )
                            Text("I UNDERSTAND", fontFamily = PressStartFont, fontSize = 8.sp, color = Color.White)
                        }
                    } else {
                        Text("NOTHING THE SYSTEM OR APPS NEED.", fontFamily = PressStartFont, fontSize = 7.sp, color = Good)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("THIS CANNOT BE UNDONE.", fontFamily = PressStartFont, fontSize = 7.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                TextButton(enabled = !needsAck || ack, onClick = { confirm = false; onPurge() }) {
                    Text("DELETE", fontFamily = PressStartFont, fontSize = 10.sp, color = if (!needsAck || ack) Danger else Color.DarkGray)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text("CANCEL", fontFamily = PressStartFont, fontSize = 10.sp, color = Gold) }
            }
        )
    }
}

/* ───────── PREVIEW ───────── */

private const val BINARY_MARK = "\u0000binary"

@Composable
private fun PreviewDialog(item: Item, isSelected: Boolean, onToggle: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val file = item.file
    val isImage = file.extension.lowercase() in setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")

    val image by produceState<ImageBitmap?>(null, file) {
        value = if (isImage) withContext(Dispatchers.IO) { loadThumb(file, 700) } else null
    }
    val text by produceState<String?>(null, file) {
        value = if (!isImage && file.isFile) withContext(Dispatchers.IO) { readPreviewText(file) } else null
    }
    val children by produceState<List<String>?>(null, file) {
        value = if (file.isDirectory) withContext(Dispatchers.IO) { listChildren(file) } else null
    }

    val modified = remember(file) {
        try { SimpleDateFormat("d MMM yyyy HH:mm", Locale.getDefault()).format(Date(file.lastModified())) } catch (_: Exception) { "?" }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.Black,
        shape = RectangleShape,
        title = {
            Text(file.name, fontFamily = PressStartFont, fontSize = 10.sp, color = Gold, lineHeight = 14.sp, maxLines = 3)
        },
        text = {
            Column(modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.risk.label, fontFamily = PressStartFont, fontSize = 7.sp, color = Color.Black,
                        modifier = Modifier.background(item.risk.color).padding(horizontal = 5.dp, vertical = 3.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        (if (item.count > 1) "${item.count} FILES · " else "") + fmt(item.size) + " · $modified",
                        fontFamily = PressStartFont, fontSize = 7.sp, color = Color.Gray, lineHeight = 11.sp
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(item.reason, fontFamily = PressStartFont, fontSize = 7.sp, color = item.risk.color, lineHeight = 11.sp)
                Spacer(Modifier.height(6.dp))
                Text(file.absolutePath, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Gray)
                Spacer(Modifier.height(10.dp))

                Box(Modifier.fillMaxWidth().border(1.dp, Color.DarkGray, RectangleShape).padding(8.dp)) {
                    when {
                        isImage -> {
                            val bmp = image
                            if (bmp != null) Image(bmp, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp))
                            else Text("LOADING IMAGE...", fontFamily = PressStartFont, fontSize = 7.sp, color = Color.Gray)
                        }
                        file.isDirectory -> {
                            val list = children
                            if (list == null) Text("LOADING...", fontFamily = PressStartFont, fontSize = 7.sp, color = Color.Gray)
                            else Column {
                                list.forEach { Text(it, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White) }
                            }
                        }
                        else -> {
                            val t = text
                            when {
                                t == null -> Text("LOADING...", fontFamily = PressStartFont, fontSize = 7.sp, color = Color.Gray)
                                t == BINARY_MARK -> Text("BINARY FILE · NO TEXT PREVIEW. USE OPEN TO VIEW IT IN ANOTHER APP.", fontFamily = PressStartFont, fontSize = 7.sp, color = Color.Gray, lineHeight = 11.sp)
                                else -> Text(t, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }

                if (file.isFile) {
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .border(1.dp, Gold, RectangleShape)
                            .clickable { openExternal(context, file) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) { Text("OPEN IN ANOTHER APP", fontFamily = PressStartFont, fontSize = 8.sp, color = Gold) }
                }
            }
        },
        confirmButton = {
            if (item.risk.selectable) {
                TextButton(onClick = { onToggle(); onDismiss() }) {
                    Text(
                        if (isSelected) "UNMARK" else "MARK FOR DELETE",
                        fontFamily = PressStartFont, fontSize = 9.sp, color = if (isSelected) Gold else Danger
                    )
                }
            } else {
                TextButton(onClick = onDismiss) { Text("CLOSE", fontFamily = PressStartFont, fontSize = 9.sp, color = Gold) }
            }
        },
        dismissButton = {
            if (item.risk.selectable) {
                TextButton(onClick = onDismiss) { Text("CLOSE", fontFamily = PressStartFont, fontSize = 9.sp, color = Gold) }
            }
        }
    )
}

private fun loadThumb(f: File, max: Int): ImageBitmap? = try {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(f.path, bounds)
    var sample = 1
    while (bounds.outWidth / sample > max || bounds.outHeight / sample > max) sample *= 2
    BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
} catch (_: Throwable) { null }

private fun readPreviewText(f: File): String = try {
    val buf = ByteArray(2048)
    val n = f.inputStream().use { it.read(buf) }
    when {
        n <= 0 -> "(EMPTY FILE)"
        (0 until n).any { buf[it] == 0.toByte() } -> BINARY_MARK
        else -> String(buf, 0, n, Charsets.UTF_8)
    }
} catch (_: Exception) { "(CAN'T READ THIS FILE)" }

private fun listChildren(d: File): List<String> {
    val kids = d.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        ?: return listOf("(EMPTY OR NO ACCESS)")
    if (kids.isEmpty()) return listOf("(EMPTY FOLDER)")
    val lines = kids.take(25).map {
        (if (it.isDirectory) "[DIR] " else "") + it.name + (if (it.isFile) "  ·  ${fmt(it.length())}" else "")
    }
    return if (kids.size > 25) lines + "… +${kids.size - 25} MORE" else lines
}

private fun openExternal(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase()) ?: "*/*"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Open with"))
    } catch (e: Exception) {
        Toast.makeText(context, "Can't open this file", Toast.LENGTH_SHORT).show()
    }
}

/* ───────── RISK CLASSIFIER ───────── */

private fun assessJunk(env: Env, f: File): Pair<Risk, String> {
    val parts = env.rel(f).split('/')
    val ext = f.extension.lowercase()

    if (parts.size >= 3 && parts[0] == "Android" && parts[1] in setOf("data", "obb", "media")) {
        val pkg = parts[2]
        if (!env.installed(pkg)) return Risk.SAFE to "Leftover of uninstalled app $pkg. Nothing uses it."
        if (parts[1] == "media") return Risk.CAUTION to "Media saved by $pkg (e.g. chat photos). May be your only copy."
        val sub = parts.getOrNull(3)?.lowercase()
        return when {
            sub == "cache" || sub == "code_cache" -> Risk.LOW to "Cache of $pkg. Rebuilt automatically, app may open slower once."
            ext in setOf("log", "tmp", "temp") -> Risk.LOW to "Temp or log file of $pkg. The app recreates it."
            parts[1] == "obb" -> Risk.DANGER to "Data pack of $pkg. The app or game must re-download it."
            else -> Risk.DANGER to "Data of installed app $pkg. Deleting can reset logins, saves or settings."
        }
    }
    if (parts.any { it.equals(".thumbnails", true) || it.equals("thumbnails", true) })
        return Risk.SAFE to "Gallery thumbnails. Rebuilt on demand."
    if (ext in setOf("tmp", "temp", "cache"))
        return Risk.SAFE to "Temporary file. Nothing depends on it."
    if (ext == "log")
        return Risk.SAFE to "Old log file. Only useful for debugging."
    if (parts.any { it.contains("cache", true) })
        return Risk.SAFE to "Cache folder content. Regenerated when needed."
    return if (parts.firstOrNull() in USER_DIRS)
        Risk.CAUTION to "Inside your personal folder (${parts[0]}). Check it first."
    else Risk.CAUTION to "Unrecognised location. Check before deleting."
}

private fun assessDupe(env: Env, f: File, keep: File): Pair<Risk, String> {
    val parts = env.rel(f).split('/')
    return when {
        parts.any { it.contains("cache", true) } -> Risk.SAFE to "Identical copy inside a cache folder."
        parts.firstOrNull() in USER_DIRS ->
            Risk.LOW to "Matches '${keep.name}' in ${env.rel(keep.parentFile ?: keep)}. The original stays."
        else -> Risk.CAUTION to "Looks identical to '${keep.name}' but sits outside your normal folders."
    }
}

/* ───────── SCANNERS ───────── */

private fun findJunk(env: Env): List<Item> {
    val exts = setOf("log", "tmp", "temp", "cache")
    val out = mutableListOf<Item>()
    try {
        env.root.walkTopDown().maxDepth(6).forEach { f ->
            if (f.isFile && (f.extension.lowercase() in exts || f.parentFile?.name?.contains("cache", true) == true)) {
                val (risk, why) = assessJunk(env, f)
                out += Item(f, f.length(), 1, risk, why)
            }
        }
    } catch (_: Exception) {}
    return out.sortedWith(compareBy<Item>({ it.risk.ordinal }, { -it.size }))
}

private fun findDuplicates(env: Env): List<Item> {
    val bySize = mutableMapOf<Long, MutableList<File>>()
    try {
        env.root.walkTopDown().maxDepth(12)
            // App-private data is never a "duplicate" candidate.
            .onEnter { d -> !(d.parentFile == env.root && d.name == "Android") }
            .filter { it.isFile && it.length() > 1024 * 1024 }
            .forEach { bySize.getOrPut(it.length()) { mutableListOf() }.add(it) }
    } catch (_: Exception) {}

    val out = mutableListOf<Item>()
    bySize.values.filter { it.size > 1 }.forEach { sameSize ->
        sameSize.groupBy { fingerprint(it) }.filterKeys { it != null }.values.filter { it.size > 1 }.forEach { group ->
            val keep = group.sortedWith(compareBy<File>({ it.lastModified() }, { it.absolutePath.length })).first()
            out += Item(keep, keep.length(), 1, Risk.LOCKED, "Original, kept. The copies below match it. Never deleted.")
            group.filter { it != keep }.forEach { f ->
                val (r, why) = assessDupe(env, f, keep)
                out += Item(f, f.length(), 1, r, why)
            }
        }
    }
    return out // grouped: original first, then its copies
}

private fun findCorpses(env: Env): List<Item> {
    val out = mutableListOf<Item>()
    try {
        listOf("Android/data", "Android/obb").forEach { sub ->
            File(env.root, sub).listFiles()
                ?.filter { it.isDirectory && !env.installed(it.name) }
                ?.forEach { dir ->
                    val files = dir.walkTopDown().filter { it.isFile }.toList()
                    out += Item(
                        dir, files.sumOf { it.length() }, files.size, Risk.SAFE,
                        "Leftover from an uninstalled app (${dir.name}). Nothing uses it."
                    )
                }
        }
        val thumbs = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), ".thumbnails")
        if (thumbs.exists()) thumbs.walkTopDown().filter { it.isFile && it.length() > 5 * 1024 * 1024 }.forEach {
            out += Item(it, it.length(), 1, Risk.SAFE, "Gallery thumbnail cache. Rebuilt on demand.")
        }
    } catch (_: Exception) {}
    return out.sortedByDescending { it.size }
}

/** Cheap content check: size + SHA-1 of the first, middle and last 256 KB. */
private fun fingerprint(f: File): String? = try {
    val md = MessageDigest.getInstance("SHA-1")
    RandomAccessFile(f, "r").use { raf ->
        val len = raf.length()
        val chunk = 256 * 1024
        val buf = ByteArray(chunk)
        fun sample(pos: Long) {
            raf.seek(pos.coerceAtLeast(0))
            val n = raf.read(buf)
            if (n > 0) md.update(buf, 0, n)
        }
        sample(0)
        if (len > chunk * 3L) { sample(len / 2 - chunk / 2); sample(len - chunk) }
    }
    md.digest().joinToString("") { "%02x".format(it) }
} catch (_: Exception) { null }

private fun openSystemFiles(context: Context) {
    val targets = listOf(
        "com.google.android.documentsui" to "com.android.documentsui.files.FilesActivity",
        "com.android.documentsui" to "com.android.documentsui.files.FilesActivity"
    )
    for ((pkg, cls) in targets) {
        try {
            context.startActivity(Intent(Intent.ACTION_MAIN).apply {
                setClassName(pkg, cls)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            return
        } catch (_: Exception) { /* try the next one */ }
    }
    Toast.makeText(context, "System Files app not available on this phone", Toast.LENGTH_LONG).show()
}

/* ───────── HELPERS ───────── */

private fun fmt(bytes: Long): String {
    val kb = 1024.0; val mb = kb * 1024; val gb = mb * 1024
    return when {
        bytes >= gb -> String.format("%.2f GB", bytes / gb)
        bytes >= mb -> String.format("%.1f MB", bytes / mb)
        bytes >= kb -> String.format("%.0f KB", bytes / kb)
        else -> "$bytes B"
    }
}

private fun File.wipe(): Boolean {
    if (isDirectory) listFiles()?.forEach { it.wipe() }
    return delete()
}