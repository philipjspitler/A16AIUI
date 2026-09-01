package com.example

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AndroidAideScreen(
    viewModel: AndroidAideViewModel,
    appManagerViewModel: AppManagerViewModel? = null,
    onBackToHome: () -> Unit,
    onOpenStorage: () -> Unit
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val notice by viewModel.aideNotice.collectAsState()
    val builderPrompt by viewModel.builderPrompt.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(notice) {
        notice?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearNotice()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Android Aide",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Agent & Builder",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Text(
                            text = "Autonomous agent, web browser, phone & fast code builder",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackToHome) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Home")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshTelemetry() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh System Status")
                    }
                    IconButton(onClick = onOpenStorage) {
                        Icon(Icons.Filled.Folder, contentDescription = "Model Manager")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Live Status Banner
            TelemetryStatusBar(telemetry = telemetry)

            // Mode Navigation Tabs
            PrimaryTabRow(
                selectedTabIndex = currentTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = currentTab == AideTab.AGENT,
                    onClick = { viewModel.setTab(AideTab.AGENT) },
                    text = { Text("⚡ Agent") }
                )
                Tab(
                    selected = currentTab == AideTab.CODE_BUILDER,
                    onClick = { viewModel.setTab(AideTab.CODE_BUILDER) },
                    text = { Text("💻 App Maker") }
                )
                Tab(
                    selected = currentTab == AideTab.WEB,
                    onClick = { viewModel.setTab(AideTab.WEB) },
                    text = { Text("🌐 Web") }
                )
                Tab(
                    selected = currentTab == AideTab.PHONE,
                    onClick = { viewModel.setTab(AideTab.PHONE) },
                    text = { Text("📱 Phone") }
                )
            }

            // Tab Content
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (currentTab) {
                    AideTab.AGENT -> AgentAutonomousTab(viewModel = viewModel)
                    AideTab.CODE_BUILDER -> FastCodeStudioScreen(
                        appManagerViewModel = appManagerViewModel,
                        onBack = null,
                        initialPrompt = builderPrompt
                    )
                    AideTab.WEB -> WebBrowserTab(viewModel = viewModel)
                    AideTab.PHONE -> PhoneUseTab(viewModel = viewModel, telemetry = telemetry)
                }
            }
        }
    }
}

@Composable
fun TelemetryStatusBar(telemetry: DeviceTelemetry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Battery status
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (telemetry.isCharging) Icons.Filled.CheckCircle else Icons.Filled.Notifications,
                contentDescription = null,
                tint = if (telemetry.batteryLevel > 20) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Battery: ${telemetry.batteryLevel}%${if (telemetry.isCharging) " ⚡" else ""}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        VerticalDivider(modifier = Modifier.height(12.dp))

        // Network status
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = telemetry.networkStatus,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        VerticalDivider(modifier = Modifier.height(12.dp))

        // Free Storage
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Storage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Storage: ${telemetry.freeStorageGb} GB free",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        VerticalDivider(modifier = Modifier.height(12.dp))

        // Time
        Text(
            text = telemetry.currentFormattedTime,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// =========================================================================
// TAB 1: ⚡ AUTONOMOUS AIDE AGENT (Plans & Executes Goals)
// =========================================================================

@Composable
fun AgentAutonomousTab(viewModel: AndroidAideViewModel) {
    var goalInput by remember { mutableStateOf("") }
    val activeTask by viewModel.activeTask.collectAsState()
    val allTasks by viewModel.agentTasks.collectAsState()
    val focusManager = LocalFocusManager.current

    val presetGoals = remember {
        listOf(
            "⚡ Build a retro arcade space shooter game",
            "🎵 Generate an audio synth & beat maker mini-app",
            "📈 Create a scientific graphing calculator",
            "📋 Build an agile Kanban sprint board app",
            "Research latest quantum computing breakthroughs on Wikipedia",
            "Set a 15-minute cooking timer and prepare reminder",
            "Draft follow-up SMS to colleague about project review"
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Goal input card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Autonomous Goal Planner",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Give the Aide a high-level task. It breaks it down and interacts with web & phone tools.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = goalInput,
                        onValueChange = { goalInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g., Search AI news and set a 10 min reminder...") },
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            if (goalInput.isNotBlank()) {
                                IconButton(onClick = { goalInput = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                focusManager.clearFocus()
                                viewModel.runAutonomousGoal(goalInput)
                                goalInput = ""
                            }
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Suggestions:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.runAutonomousGoal(goalInput.ifBlank { "Research AI models and schedule review" })
                                goalInput = ""
                            },
                            enabled = activeTask?.isRunning != true,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Run Plan")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Preset Chips
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        presetGoals.forEach { preset ->
                            SuggestionChip(
                                onClick = {
                                    goalInput = preset
                                    focusManager.clearFocus()
                                    viewModel.runAutonomousGoal(preset)
                                },
                                label = {
                                    Text(
                                        preset,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // Active Task Card / Live Step Execution Timeline
        activeTask?.let { task ->
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (task.isRunning) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.5.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = "Done",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (task.isRunning) "Agent Executing..." else "Goal Completed",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Goal: \"${task.userGoal}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Execution Steps Pipeline:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        task.steps.forEachIndexed { index, step ->
                            StepItemView(index = index + 1, step = step)
                            if (index < task.steps.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .padding(start = 14.dp)
                                        .width(2.dp)
                                        .height(12.dp)
                                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f))
                                )
                            }
                        }

                        task.finalSummary?.let { summary ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Agent Synthesis:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = summary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    if (task.userGoal.lowercase().contains("app") || task.userGoal.lowercase().contains("game") || task.userGoal.lowercase().contains("build") || task.userGoal.lowercase().contains("calc") || task.userGoal.lowercase().contains("synth")) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(
                                            onClick = {
                                                viewModel.setBuilderPrompt(task.userGoal)
                                                viewModel.setTab(AideTab.CODE_BUILDER)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("🚀 Launch in Fast App Maker & Sandbox", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Previous Task History Header
        if (allTasks.size > 1) {
            item {
                Text(
                    text = "Recent Agent Missions (${allTasks.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            items(allTasks.drop(1)) { histTask ->
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = histTask.userGoal,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${histTask.steps.size} steps",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StepItemView(index: Int, step: AgentExecutionStep) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    when (step.status) {
                        AgentStepStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                        AgentStepStatus.RUNNING -> MaterialTheme.colorScheme.tertiary
                        AgentStepStatus.FAILED -> MaterialTheme.colorScheme.error
                        AgentStepStatus.PENDING -> MaterialTheme.colorScheme.outlineVariant
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (step.status == AgentStepStatus.RUNNING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Text(
                    text = "$index",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = step.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (step.detailLog.isNotBlank()) step.detailLog else step.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// =========================================================================
// TAB 2: 🌐 WEB BROWSER & AGENT COPILOT (Real WebView + Web Tools)
// =========================================================================

@Composable
fun WebBrowserTab(viewModel: AndroidAideViewModel) {
    val currentUrl by viewModel.currentUrl.collectAsState()
    val pageTitle by viewModel.webPageTitle.collectAsState()
    val isLoading by viewModel.webIsLoading.collectAsState()
    val extractedContent by viewModel.extractedContent.collectAsState()
    var urlInput by remember { mutableStateOf(currentUrl) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var showReaderDialog by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    val quickBookmarks = remember {
        listOf(
            "Wikipedia" to "https://en.wikipedia.org/wiki/Artificial_intelligence",
            "Google" to "https://www.google.com",
            "Hacker News" to "https://news.ycombinator.com",
            "Hugging Face" to "https://huggingface.co/models",
            "GitHub" to "https://github.com",
            "arXiv AI" to "https://arxiv.org/list/cs.AI/recent"
        )
    }

    LaunchedEffect(currentUrl) {
        if (urlInput != currentUrl) {
            urlInput = currentUrl
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Omnibox / Search & Navigation Bar
        Surface(
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = { webViewRef?.let { if (it.canGoBack()) it.goBack() } },
                        enabled = webViewRef?.canGoBack() == true
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = { webViewRef?.let { if (it.canGoForward()) it.goForward() } },
                        enabled = webViewRef?.canGoForward() == true
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { webViewRef?.reload() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Reload", modifier = Modifier.size(20.dp))
                    }

                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("Search or URL...", fontSize = 13.sp) },
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        shape = RoundedCornerShape(24.dp),
                        trailingIcon = {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                IconButton(onClick = {
                                    focusManager.clearFocus()
                                    viewModel.setUrl(urlInput)
                                    webViewRef?.loadUrl(viewModel.currentUrl.value)
                                }) {
                                    Icon(Icons.Filled.Search, contentDescription = "Go", modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                focusManager.clearFocus()
                                viewModel.setUrl(urlInput)
                                webViewRef?.loadUrl(viewModel.currentUrl.value)
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                }

                // Quick Bookmarks row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickBookmarks.forEach { (name, link) ->
                        AssistChip(
                            onClick = {
                                urlInput = link
                                viewModel.setUrl(link)
                                webViewRef?.loadUrl(link)
                            },
                            label = { Text(name, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }
        }

        // Web Aide Copilot Toolbar (Extract, Summarize, External Open)
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Aide Tools:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )

                FilledTonalButton(
                    onClick = {
                        // Extract text via JS
                        webViewRef?.evaluateJavascript(
                            "(function() { return document.body.innerText.substring(0, 3000); })();"
                        ) { rawText ->
                            val clean = rawText?.removeSurrounding("\"")
                                ?.replace("\\n", "\n")
                                ?: "No text content found."
                            viewModel.setExtractedContent(clean)
                            showReaderDialog = true
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Extract Page Text", fontSize = 12.sp)
                }

                FilledTonalButton(
                    onClick = {
                        val prompt = "Summarize the key information from: $currentUrl ($pageTitle)"
                        viewModel.runAutonomousGoal(prompt)
                        viewModel.setTab(AideTab.AGENT)
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Agent Summarize", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // ignore
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Open Browser", fontSize = 12.sp)
                }
            }
        }

        // Live WebView
        AndroidView(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
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
                            url?.let { viewModel.setWebPageInfo(title ?: "", it, true) }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            url?.let { viewModel.setWebPageInfo(title ?: "", it, false) }
                        }

                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            return false
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onReceivedTitle(view: WebView?, title: String?) {
                            super.onReceivedTitle(view, title)
                            title?.let { viewModel.setWebPageInfo(it, url ?: "", false) }
                        }
                    }

                    loadUrl(currentUrl)
                    webViewRef = this
                }
            },
            update = { webView ->
                webViewRef = webView
            }
        )
    }

    // Extracted Text Reader Modal
    if (showReaderDialog && extractedContent != null) {
        AlertDialog(
            onDismissRequest = { showReaderDialog = false },
            title = { Text("Extracted Page Text") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = extractedContent ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val summaryPrompt = "Analyze this extracted web page text:\n\n${extractedContent?.take(500)}"
                    showReaderDialog = false
                    viewModel.runAutonomousGoal(summaryPrompt)
                    viewModel.setTab(AideTab.AGENT)
                }) {
                    Text("Analyze with Agent")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReaderDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

// =========================================================================
// TAB 3: 📱 PHONE USE (Device Automation & System Intents)
// =========================================================================

@Composable
fun PhoneUseTab(viewModel: AndroidAideViewModel, telemetry: DeviceTelemetry) {
    var phoneNumber by remember { mutableStateOf("") }
    var smsMessage by remember { mutableStateOf("") }
    var emailRecipient by remember { mutableStateOf("") }
    var emailSubject by remember { mutableStateOf("") }
    var emailBody by remember { mutableStateOf("") }
    var timerMinutes by remember { mutableStateOf("10") }
    var eventTitle by remember { mutableStateOf("") }
    var eventDetails by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: One-Tap Quick Phone Launchers
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Quick App Launchers",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        AppLauncherIcon(
                            icon = Icons.Filled.Mic,
                            label = "Camera",
                            onClick = { viewModel.openAppByIntent(MediaStore.ACTION_IMAGE_CAPTURE) }
                        )
                        AppLauncherIcon(
                            icon = Icons.Filled.Search,
                            label = "Maps",
                            onClick = { viewModel.openAppByIntent(Intent.ACTION_VIEW, "geo:0,0?q=restaurants+nearby") }
                        )
                        AppLauncherIcon(
                            icon = Icons.Filled.Settings,
                            label = "Settings",
                            onClick = { viewModel.openSystemSettings(Settings.ACTION_SETTINGS) }
                        )
                        AppLauncherIcon(
                            icon = Icons.Filled.Notifications,
                            label = "Clock",
                            onClick = { viewModel.openAppByIntent(AlarmClock.ACTION_SHOW_ALARMS) }
                        )
                    }
                }
            }
        }

        // Section: Call & SMS Phone Controls
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Phone & Messaging Automation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone Number / Contact") },
                        placeholder = { Text("e.g. 555-0199 or (800) 555-0123") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = smsMessage,
                        onValueChange = { smsMessage = it },
                        label = { Text("SMS Message Content") },
                        placeholder = { Text("Hi, sending a quick note via Android Aide.") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.launchDialer(phoneNumber)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Dialer")
                        }

                        FilledTonalButton(
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.launchSms(phoneNumber, smsMessage)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Send SMS")
                        }
                    }
                }
            }
        }

        // Section: Timer & Alarms
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Timers & Clock Automation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Quick Presets:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1 to "1 min", 5 to "5 min", 10 to "10 min", 25 to "Pomodoro (25m)", 45 to "45 min").forEach { (mins, label) ->
                            FilledTonalButton(
                                onClick = { viewModel.setDeviceTimer(mins * 60, "Aide: $label") },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(label, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = timerMinutes,
                            onValueChange = { timerMinutes = it },
                            label = { Text("Minutes") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = {
                                val mins = timerMinutes.toIntOrNull() ?: 5
                                viewModel.setDeviceTimer(mins * 60, "Custom AI Timer")
                            },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Arm Timer")
                        }
                    }
                }
            }
        }

        // Section: Calendar & Events
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Calendar & Reminder Dispatch",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = eventTitle,
                        onValueChange = { eventTitle = it },
                        label = { Text("Event / Task Title") },
                        placeholder = { Text("e.g. Project Sync & Review") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = eventDetails,
                        onValueChange = { eventDetails = it },
                        label = { Text("Details & Agenda") },
                        placeholder = { Text("Automated from Android Aide assistant.") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.createCalendarEvent(eventTitle, eventDetails)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add to Android Calendar")
                    }
                }
            }
        }

        // Section: Device System Shortcuts
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "System Settings & Hardware",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.openSystemSettings(Settings.ACTION_WIFI_SETTINGS) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Wi-Fi", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { viewModel.openSystemSettings(Settings.ACTION_BLUETOOTH_SETTINGS) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Bluetooth", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { viewModel.openSystemSettings(Settings.ACTION_DISPLAY_SETTINGS) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Display", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.openSystemSettings(Settings.ACTION_BATTERY_SAVER_SETTINGS) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Battery", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { viewModel.openSystemSettings(Settings.ACTION_SOUND_SETTINGS) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Sound", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { viewModel.openSystemSettings(Settings.ACTION_INTERNAL_STORAGE_SETTINGS) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Storage", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppLauncherIcon(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
