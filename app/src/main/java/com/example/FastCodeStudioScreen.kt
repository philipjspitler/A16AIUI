package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Super-Fast Code Studio & App Maker Screen.
 * Enables instant one-prompt app creation, live interactive sandbox running,
 * code customization, and 1-tap installation to the Android Home Screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FastCodeStudioScreen(
    appManagerViewModel: AppManagerViewModel? = null,
    onBack: (() -> Unit)? = null,
    initialPrompt: String = ""
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    var promptInput by remember { mutableStateOf(initialPrompt) }
    var currentBuildResult by remember {
        mutableStateOf(
            FastAppBuilderEngine.buildAppFromPrompt(initialPrompt.ifBlank { "Space Arcade Defender" })
        )
    }

    var selectedViewMode by remember { mutableStateOf(0) } // 0 = Live Interactive Sandbox, 1 = Code Editor, 2 = Kotlin Spec
    var editableHtmlCode by remember { mutableStateOf(currentBuildResult.htmlCode) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isBuilding by remember { mutableStateOf(false) }
    var isInstalledToHome by remember { mutableStateOf(false) }
    var showFullscreenApp by remember { mutableStateOf(false) }
    var studioNotice by remember { mutableStateOf<String?>(null) }

    fun triggerBuild(prompt: String) {
        if (prompt.isBlank()) return
        focusManager.clearFocus()
        isBuilding = true
        coroutineScope.launch {
            delay(40) // Ultra fast simulation
            val result = FastAppBuilderEngine.buildAppFromPrompt(prompt)
            currentBuildResult = result
            editableHtmlCode = result.htmlCode
            isBuilding = false
            isInstalledToHome = false
            studioNotice = "⚡ App Built in ${result.buildTimeMs}ms!"
            webViewRef?.loadDataWithBaseURL(null, result.htmlCode, "text/html", "utf-8", null)
        }
    }

    fun applyCodeEdit() {
        focusManager.clearFocus()
        currentBuildResult = currentBuildResult.copy(
            htmlCode = editableHtmlCode,
            buildTimeMs = 14
        )
        webViewRef?.loadDataWithBaseURL(null, editableHtmlCode, "text/html", "utf-8", null)
        studioNotice = "⚡ Live Hot-Reload Applied (<15ms)"
    }

    fun installToHomeScreen() {
        val fileUri = FastAppBuilderEngine.saveAppToFile(
            context = context,
            appId = currentBuildResult.id.take(8),
            htmlContent = editableHtmlCode
        )
        appManagerViewModel?.installCustomWebApp(
            name = currentBuildResult.appTitle,
            url = fileUri,
            iconKey = "code"
        )
        isInstalledToHome = true
        studioNotice = "🎉 \"${currentBuildResult.appTitle}\" added to Home Screen!"
        Toast.makeText(context, "Pinned to Home Screen", Toast.LENGTH_SHORT).show()
    }

    fun copyCodeToClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("App Source Code", editableHtmlCode)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Code copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚡ Fast Code Studio", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Turbo Builder",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "Super-fast on-device app compiler & sandbox",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { installToHomeScreen() }) {
                        Icon(
                            imageVector = if (isInstalledToHome) Icons.Filled.CheckCircle else Icons.Filled.AddHome,
                            contentDescription = "Install to Home Screen",
                            tint = if (isInstalledToHome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { copyCodeToClipboard() }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Code")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Notice banner if available
            AnimatedVisibility(visible = studioNotice != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = studioNotice ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        IconButton(
                            onClick = { studioNotice = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Quick Prompt Generator Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = promptInput,
                            onValueChange = { promptInput = it },
                            placeholder = { Text("Describe an app to build (e.g. Space shooter, synth, calculator...)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { triggerBuild(promptInput) }),
                            trailingIcon = {
                                if (promptInput.isNotBlank()) {
                                    IconButton(onClick = { promptInput = "" }) {
                                        Icon(Icons.Filled.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { triggerBuild(promptInput) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(56.dp),
                            enabled = !isBuilding
                        ) {
                            if (isBuilding) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Icon(Icons.Filled.Bolt, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Build", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Instant Pre-Built Recipes
                    Text(
                        text = "Instant App Recipes:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(FastAppBuilderEngine.TEMPLATES) { template ->
                            val isSelected = currentBuildResult.appTitle == template.title
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    promptInput = template.title
                                    currentBuildResult = FastAppBuilderEngine.buildAppFromPrompt(template.title)
                                    editableHtmlCode = currentBuildResult.htmlCode
                                    isInstalledToHome = false
                                    studioNotice = "⚡ Loaded ${template.title} in <15ms"
                                    webViewRef?.loadDataWithBaseURL(null, editableHtmlCode, "text/html", "utf-8", null)
                                },
                                label = { Text(template.title, fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(
                                        when (template.iconKey) {
                                            "game" -> Icons.Filled.SportsEsports
                                            "music" -> Icons.Filled.MusicNote
                                            "calculate" -> Icons.Filled.Calculate
                                            "edit" -> Icons.Filled.ViewKanban
                                            else -> Icons.Filled.Palette
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Mode Selector Bar (Live Sandbox | Code Editor | Kotlin Spec)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabRow(
                    selectedTabIndex = selectedViewMode,
                    modifier = Modifier.weight(1f),
                    containerColor = Color.Transparent,
                    indicator = {},
                    divider = {}
                ) {
                    Tab(
                        selected = selectedViewMode == 0,
                        onClick = { selectedViewMode = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Live Sandbox", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    )
                    Tab(
                        selected = selectedViewMode == 1,
                        onClick = { selectedViewMode = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Code, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("HTML / JS", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    )
                    Tab(
                        selected = selectedViewMode == 2,
                        onClick = { selectedViewMode = 2 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Android, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Kotlin / Compose", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    )
                }

                // Diagnostics badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.padding(start = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Speed, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${currentBuildResult.buildTimeMs}ms", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Main Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                when (selectedViewMode) {
                    0 -> {
                        // LIVE INTERACTIVE SANDBOX
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Sandbox Header Bar
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF4CAF50),
                                            modifier = Modifier.size(8.dp)
                                        ) {}
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = currentBuildResult.appTitle,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                webViewRef?.loadDataWithBaseURL(null, editableHtmlCode, "text/html", "utf-8", null)
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Filled.Refresh, contentDescription = "Reload Sandbox", modifier = Modifier.size(16.dp))
                                        }
                                        FilledTonalButton(
                                            onClick = { installToHomeScreen() },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Icon(Icons.Filled.AddHome, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Install to Home", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // Interactive WebView
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
                                            settings.allowFileAccess = true
                                            settings.allowContentAccess = true
                                            settings.loadWithOverviewMode = true
                                            settings.useWideViewPort = true
                                            settings.mediaPlaybackRequiresUserGesture = false

                                            webViewClient = object : WebViewClient() {}
                                            webChromeClient = object : WebChromeClient() {}

                                            loadDataWithBaseURL(null, editableHtmlCode, "text/html", "utf-8", null)
                                            webViewRef = this
                                        }
                                    },
                                    update = { webView ->
                                        // Trigger updates when editableHtmlCode changes
                                    }
                                )
                            }
                        }
                    }

                    1 -> {
                        // LIVE HTML/JS CODE EDITOR
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                        ) {
                            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("HTML5 / JavaScript Code", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(
                                            onClick = { applyCodeEdit() },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Run (<15ms)", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = editableHtmlCode,
                                    onValueChange = { editableHtmlCode = it },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF0B0F19), RoundedCornerShape(8.dp)),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = Color(0xFFE2E8F0)
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF38BDF8),
                                        unfocusedBorderColor = Color(0xFF334155)
                                    )
                                )
                            }
                        }
                    }

                    2 -> {
                        // KOTLIN / JETPACK COMPOSE NATIVE SPEC
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E))
                        ) {
                            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Jetpack Compose Native Code", color = Color(0xFFCBA6F7), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Compose Code", currentBuildResult.kotlinCode)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Kotlin code copied!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", tint = Color(0xFFCBA6F7), modifier = Modifier.size(16.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Surface(
                                    color = Color(0xFF181825),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    LazyColumn(modifier = Modifier.padding(12.dp)) {
                                        item {
                                            Text(
                                                text = currentBuildResult.kotlinCode,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 12.sp,
                                                color = Color(0xFFCDD6F4)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
