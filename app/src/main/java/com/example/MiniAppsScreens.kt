package com.example

import android.content.Context
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.*

// =========================================================================
// 1. NOTES & CHECKLIST APP
// =========================================================================

data class NoteItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val isDone: Boolean = false,
    val timestamp: String = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date())
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesAppScreen(onBack: () -> Unit) {
    var notes by remember {
        mutableStateOf(
            listOf(
                NoteItem(title = "Local LLM Prompts", content = "Research quantized models like SmolLM2 and Qwen 2.5 for mobile execution.", isDone = false),
                NoteItem(title = "Shopping & Gear", content = "USB-C OTG cable, high-speed micro-SD card for storage.", isDone = true),
                NoteItem(title = "Idea: Private Voice Assistant", content = "Build an on-device whisper transcription pipeline.", isDone = false)
            )
        )
    }
    var showAddDialog by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newContent by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Offline Notes & Tasks", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Note")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search local notes...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp)
            )

            val filteredNotes = notes.filter {
                it.title.contains(searchQuery, ignoreCase = true) || it.content.contains(searchQuery, ignoreCase = true)
            }

            if (filteredNotes.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No notes found. Tap + to write your first note!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredNotes, key = { it.id }) { note ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (note.isDone) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = note.isDone,
                                    onCheckedChange = { checked ->
                                        notes = notes.map { if (it.id == note.id) it.copy(isDone = checked) else it }
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = note.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        textDecoration = if (note.isDone) TextDecoration.LineThrough else TextDecoration.None
                                    )
                                    if (note.content.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = note.content,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = note.timestamp,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                IconButton(
                                    onClick = { notes = notes.filterNot { it.id == note.id } }
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("New Note / Task") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = newContent,
                        onValueChange = { newContent = it },
                        label = { Text("Details & Markdown") },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newTitle.isNotBlank()) {
                        notes = listOf(NoteItem(title = newTitle.trim(), content = newContent.trim())) + notes
                        newTitle = ""
                        newContent = ""
                        showAddDialog = false
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// =========================================================================
// 2. SCIENTIFIC CALCULATOR & UNIT CONVERTER
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorAppScreen(onBack: () -> Unit) {
    var display by remember { mutableStateOf("0") }
    var expression by remember { mutableStateOf("") }
    var isNewNumber by remember { mutableStateOf(true) }

    fun appendDigit(digit: String) {
        if (isNewNumber || display == "0") {
            display = digit
            isNewNumber = false
        } else {
            display += digit
        }
    }

    fun appendOp(op: String) {
        expression = "$display $op "
        isNewNumber = true
    }

    fun calculate() {
        try {
            val parts = expression.trim().split(" ")
            if (parts.size >= 2) {
                val num1 = parts[0].toDoubleOrNull() ?: 0.0
                val op = parts[1]
                val num2 = display.toDoubleOrNull() ?: 0.0
                val result = when (op) {
                    "+" -> num1 + num2
                    "-" -> num1 - num2
                    "×" -> num1 * num2
                    "÷" -> if (num2 != 0.0) num1 / num2 else Double.NaN
                    "%" -> num1 % num2
                    "^" -> num1.pow(num2)
                    else -> num2
                }
                expression = ""
                display = if (result.isNaN()) "Error" else if (result % 1.0 == 0.0) result.toLong().toString() else String.format(Locale.US, "%.4f", result).trimEnd('0').trimEnd('.')
                isNewNumber = true
            }
        } catch (e: Exception) {
            display = "Error"
            isNewNumber = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scientific Calculator", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Display Board
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().height(140.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = expression,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = display,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scientific Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(onClick = {
                    val num = display.toDoubleOrNull() ?: 0.0
                    display = String.format(Locale.US, "%.4f", sqrt(num)).trimEnd('0').trimEnd('.')
                }, modifier = Modifier.weight(1f)) { Text("√x") }

                FilledTonalButton(onClick = {
                    val num = display.toDoubleOrNull() ?: 0.0
                    display = String.format(Locale.US, "%.4f", sin(Math.toRadians(num))).trimEnd('0').trimEnd('.')
                }, modifier = Modifier.weight(1f)) { Text("sin") }

                FilledTonalButton(onClick = {
                    val num = display.toDoubleOrNull() ?: 0.0
                    display = String.format(Locale.US, "%.4f", cos(Math.toRadians(num))).trimEnd('0').trimEnd('.')
                }, modifier = Modifier.weight(1f)) { Text("cos") }

                FilledTonalButton(onClick = {
                    val num = display.toDoubleOrNull() ?: 0.0
                    display = String.format(Locale.US, "%.4f", if (num > 0) ln(num) else 0.0).trimEnd('0').trimEnd('.')
                }, modifier = Modifier.weight(1f)) { Text("ln") }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Keypad Grid
            val rows = listOf(
                listOf("C", "±", "%", "÷"),
                listOf("7", "8", "9", "×"),
                listOf("4", "5", "6", "-"),
                listOf("1", "2", "3", "+"),
                listOf("0", ".", "π", "=")
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { key ->
                            val isOp = key in listOf("÷", "×", "-", "+", "=")
                            val isSpecial = key in listOf("C", "±", "%")

                            Button(
                                onClick = {
                                    when (key) {
                                        "C" -> { display = "0"; expression = ""; isNewNumber = true }
                                        "±" -> { display = if (display.startsWith("-")) display.drop(1) else "-$display" }
                                        "π" -> { display = "3.14159"; isNewNumber = true }
                                        "=" -> calculate()
                                        "+", "-", "×", "÷", "%" -> appendOp(key)
                                        else -> appendDigit(key)
                                    }
                                },
                                colors = if (isOp) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                else if (isSpecial) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                                else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = MaterialTheme.colorScheme.onSurface),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(1f).height(54.dp)
                            ) {
                                Text(key, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 3. FOCUS & POMODORO TIMER
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusTimerScreen(onBack: () -> Unit) {
    var totalSeconds by remember { mutableIntStateOf(25 * 60) }
    var remainingSeconds by remember { mutableIntStateOf(25 * 60) }
    var isRunning by remember { mutableStateOf(false) }
    var isBreakMode by remember { mutableStateOf(false) }
    var laps by remember { mutableStateOf(listOf<String>()) }

    LaunchedEffect(isRunning, remainingSeconds) {
        if (isRunning && remainingSeconds > 0) {
            delay(1000)
            remainingSeconds -= 1
        } else if (isRunning && remainingSeconds == 0) {
            isRunning = false
            // Switch mode
            isBreakMode = !isBreakMode
            totalSeconds = if (isBreakMode) 5 * 60 else 25 * 60
            remainingSeconds = totalSeconds
        }
    }

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val formattedTime = String.format(Locale.US, "%02d:%02d", minutes, seconds)
    val progress = if (totalSeconds > 0) 1f - (remainingSeconds.toFloat() / totalSeconds) else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Focus & Pomodoro Timer", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Mode selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                FilterChip(
                    selected = !isBreakMode,
                    onClick = {
                        isBreakMode = false
                        isRunning = false
                        totalSeconds = 25 * 60
                        remainingSeconds = totalSeconds
                    },
                    label = { Text("Focus (25m)") }
                )
                Spacer(modifier = Modifier.width(12.dp))
                FilterChip(
                    selected = isBreakMode,
                    onClick = {
                        isBreakMode = true
                        isRunning = false
                        totalSeconds = 5 * 60
                        remainingSeconds = totalSeconds
                    },
                    label = { Text("Short Break (5m)") }
                )
            }

            // Big Circular Progress Timer
            Box(
                modifier = Modifier.size(240.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 12.dp,
                    color = if (isBreakMode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formattedTime,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (isBreakMode) "Relax & Recharge" else "Deep Work Session",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = {
                        isRunning = false
                        remainingSeconds = totalSeconds
                    }
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset")
                }

                Button(
                    onClick = { isRunning = !isRunning },
                    modifier = Modifier.height(52.dp).padding(horizontal = 12.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        if (isRunning) Icons.Filled.Close else Icons.Filled.Check,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isRunning) "Pause" else "Start Session", fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                    onClick = {
                        if (isRunning) {
                            laps = listOf("Lap ${laps.size + 1}: $formattedTime") + laps
                        }
                    },
                    enabled = isRunning
                ) {
                    Text("Lap")
                }
            }

            // Laps History
            if (laps.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth().height(100.dp)
                ) {
                    LazyColumn(modifier = Modifier.padding(12.dp)) {
                        items(laps) { lap ->
                            Text(lap, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

// =========================================================================
// 4. DEV PROMPT & TOKEN LAB
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptLabScreen(onBack: () -> Unit) {
    var systemPrompt by remember { mutableStateOf("You are a helpful, expert offline AI assistant running locally on Android.") }
    var userPrompt by remember { mutableStateOf("Write a concise function in Kotlin to parse JSON.") }
    var temperature by remember { mutableFloatStateOf(0.7f) }
    var topP by remember { mutableFloatStateOf(0.9f) }
    var simulatedOutput by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

    val estimatedTokens = (systemPrompt.length + userPrompt.length) / 4

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Prompt & Token Lab", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Metrics Header
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Estimated Tokens", style = MaterialTheme.typography.labelSmall)
                        Text("$estimatedTokens", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Context Window", style = MaterialTheme.typography.labelSmall)
                        Text("4096", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Engine", style = MaterialTheme.typography.labelSmall)
                        Text("GGUF v3", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // System Prompt
            OutlinedTextField(
                value = systemPrompt,
                onValueChange = { systemPrompt = it },
                label = { Text("System Instructions") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // User Prompt
            OutlinedTextField(
                value = userPrompt,
                onValueChange = { userPrompt = it },
                label = { Text("User Prompt") },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                shape = RoundedCornerShape(12.dp)
            )

            // Sliders
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Temperature: ${String.format(Locale.US, "%.2f", temperature)}", style = MaterialTheme.typography.labelMedium)
                    Text("Top-P: ${String.format(Locale.US, "%.2f", topP)}", style = MaterialTheme.typography.labelMedium)
                }
                Slider(value = temperature, onValueChange = { temperature = it }, valueRange = 0f..1.5f)
            }

            // Run Bench Button
            Button(
                onClick = {
                    isGenerating = true
                    simulatedOutput = ""
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Simulate Local Inference")
            }

            LaunchedEffect(isGenerating) {
                if (isGenerating) {
                    val codeResponse = "```kotlin\nimport kotlinx.serialization.json.Json\n\n@Serializable\ndata class Config(val key: String, val value: Int)\n\nfun parse(raw: String): Config = Json.decodeFromString(raw)\n```\n\n*Generated locally in 184ms via quantized GGUF kernel.*"
                    for (i in 1..codeResponse.length) {
                        simulatedOutput = codeResponse.take(i)
                        delay(12)
                    }
                    isGenerating = false
                }
            }

            if (simulatedOutput.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Inference Output:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = simulatedOutput,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// 5. AMBIENT SOUNDSCAPE & WHITE NOISE
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmbientNoiseScreen(onBack: () -> Unit) {
    var rainVol by remember { mutableFloatStateOf(0.6f) }
    var campfireVol by remember { mutableFloatStateOf(0.4f) }
    var windVol by remember { mutableFloatStateOf(0.2f) }
    var wavesVol by remember { mutableFloatStateOf(0.5f) }
    var isPlaying by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ambient Soundscape", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Focus & Binaural Synthesizer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Mix relaxing ambient channels for work and sleep.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { isPlaying = !isPlaying },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(if (isPlaying) Icons.Filled.Close else Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isPlaying) "Mute Soundscape" else "Play Soundscape")
                    }
                }
            }

            // Sliders
            SoundSliderItem(title = "🌧️ Gentle Rain", value = rainVol, onValueChange = { rainVol = it })
            SoundSliderItem(title = "🔥 Campfire & Crackle", value = campfireVol, onValueChange = { campfireVol = it })
            SoundSliderItem(title = "🍃 Forest Wind", value = windVol, onValueChange = { windVol = it })
            SoundSliderItem(title = "🌊 Ocean Waves", value = wavesVol, onValueChange = { wavesVol = it })
        }
    }
}

@Composable
fun SoundSliderItem(title: String, value: Float, onValueChange: (Float) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                Text("${(value * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
            }
            Slider(value = value, onValueChange = onValueChange, valueRange = 0f..1f)
        }
    }
}

// =========================================================================
// 6. ATMOSPHERE & OFFLINE WEATHER FORECAST
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtmosphereWeatherScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Atmosphere & Weather", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Weather Hero
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("San Francisco, CA", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("68°F", fontSize = 54.sp, fontWeight = FontWeight.Bold)
                    Text("Partly Cloudy • Humidity 58%", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        WeatherMetricItem(label = "Wind", value = "12 mph NW")
                        WeatherMetricItem(label = "Barometer", value = "1014 hPa")
                        WeatherMetricItem(label = "UV Index", value = "3 (Moderate)")
                    }
                }
            }

            Text("Hourly Forecast", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(listOf("Now" to "68°", "1 PM" to "70°", "2 PM" to "72°", "3 PM" to "71°", "4 PM" to "69°", "5 PM" to "66°", "6 PM" to "63°")) { (time, temp) ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(time, style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.height(6.dp))
                            Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(temp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherMetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

// =========================================================================
// 7. CUSTOM WEB / PWA APP RUNNER
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomWebAppRunnerScreen(
    title: String,
    url: String,
    onBack: () -> Unit
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var currentTitle by remember { mutableStateOf(title) }
    var isLoading by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(currentTitle, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(url, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, maxLines = 1)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { webViewRef?.reload() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Reload")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                            }
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                super.onReceivedTitle(view, title)
                                if (!title.isNullOrBlank()) currentTitle = title
                            }
                        }

                        loadUrl(url)
                        webViewRef = this
                    }
                }
            )

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
        }
    }
}
