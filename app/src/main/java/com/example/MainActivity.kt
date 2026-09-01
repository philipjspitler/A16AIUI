package com.example

import android.os.Bundle
import android.os.Build
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import java.io.File
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppScreen { 
    HOME, CHAT, IMAGE_GEN, AIDE, CODE_STUDIO, MODEL_MANAGER, NOTES, CALCULATOR, FOCUS_TIMER, PROMPT_LAB, AMBIENT_NOISE, WEATHER, CUSTOM_WEB 
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    var currentScreen by remember { mutableStateOf(AppScreen.HOME) }
    var showModelManager by remember { mutableStateOf(false) }
    var showAppStoreSheet by remember { mutableStateOf(false) }
    var activeWebTitle by remember { mutableStateOf("Web App") }
    var activeWebUrl by remember { mutableStateOf("https://google.com") }

    val focusManager = LocalFocusManager.current
    val chatViewModel: ChatViewModel = viewModel()
    val modelViewModel: ModelDownloadViewModel = viewModel()
    val aideViewModel: AndroidAideViewModel = viewModel()
    val appManagerViewModel: AppManagerViewModel = viewModel()

    fun launchAppById(appId: String, webUrl: String? = null, appTitle: String? = null) {
        focusManager.clearFocus()
        showAppStoreSheet = false
        showModelManager = false

        when (appId) {
            "app_chat" -> {
                currentScreen = AppScreen.CHAT
            }
            "app_aide" -> {
                currentScreen = AppScreen.AIDE
            }
            "app_code_studio" -> {
                currentScreen = AppScreen.CODE_STUDIO
            }
            "app_image_studio" -> {
                currentScreen = AppScreen.IMAGE_GEN
            }
            "app_model_hub", "app_vault_explorer" -> {
                currentScreen = AppScreen.MODEL_MANAGER
            }
            "app_notes" -> {
                currentScreen = AppScreen.NOTES
            }
            "app_calculator" -> {
                currentScreen = AppScreen.CALCULATOR
            }
            "app_focus_timer" -> {
                currentScreen = AppScreen.FOCUS_TIMER
            }
            "app_prompt_lab" -> {
                currentScreen = AppScreen.PROMPT_LAB
            }
            "app_ambient_noise" -> {
                currentScreen = AppScreen.AMBIENT_NOISE
            }
            "app_weather" -> {
                currentScreen = AppScreen.WEATHER
            }
            else -> {
                if (!webUrl.isNullOrBlank()) {
                    activeWebTitle = appTitle ?: "Web App"
                    activeWebUrl = webUrl
                    currentScreen = AppScreen.CUSTOM_WEB
                } else {
                    currentScreen = AppScreen.CHAT
                }
            }
        }
    }

    BackHandler(enabled = currentScreen != AppScreen.HOME || showModelManager || showAppStoreSheet) {
        focusManager.clearFocus()
        if (showModelManager) {
            showModelManager = false
        } else if (showAppStoreSheet) {
            showAppStoreSheet = false
        } else {
            currentScreen = AppScreen.HOME
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            SystemNavigationBarMock(
                onBack = { 
                    focusManager.clearFocus()
                    if (showModelManager) {
                        showModelManager = false
                    } else if (showAppStoreSheet) {
                        showAppStoreSheet = false
                    } else {
                        currentScreen = AppScreen.HOME 
                    }
                },
                onHome = { 
                    focusManager.clearFocus()
                    showModelManager = false
                    showAppStoreSheet = false
                    currentScreen = AppScreen.HOME 
                },
                onOverview = {
                    focusManager.clearFocus()
                    showAppStoreSheet = !showAppStoreSheet
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (currentScreen) {
                AppScreen.HOME -> {
                    HomeScreen(
                        appManagerViewModel = appManagerViewModel,
                        onNavigateToChat = { query -> 
                            focusManager.clearFocus()
                            val promptToSend = if (query.isNotBlank()) query else "What can you do offline?"
                            chatViewModel.sendMessage(promptToSend)
                            currentScreen = AppScreen.CHAT 
                        },
                        onNavigateToImageGen = { 
                            focusManager.clearFocus()
                            currentScreen = AppScreen.IMAGE_GEN 
                        },
                        onNavigateToAide = {
                            focusManager.clearFocus()
                            currentScreen = AppScreen.AIDE
                        },
                        onOpenStorage = { 
                            focusManager.clearFocus()
                            showModelManager = true 
                        },
                        onOpenAppStore = {
                            focusManager.clearFocus()
                            showAppStoreSheet = true
                        },
                        onLaunchApp = { app ->
                            launchAppById(app.id, app.webUrl, app.name)
                        }
                    )
                }
                AppScreen.CHAT -> {
                    ChatScreen(
                        viewModel = chatViewModel, 
                        onOpenStorage = { 
                            focusManager.clearFocus()
                            showModelManager = true 
                        }
                    )
                }
                AppScreen.IMAGE_GEN -> {
                    ImageStudioScreen(
                        onOpenStorage = { 
                            focusManager.clearFocus()
                            showModelManager = true 
                        }
                    )
                }
                AppScreen.AIDE -> {
                    AndroidAideScreen(
                        viewModel = aideViewModel,
                        appManagerViewModel = appManagerViewModel,
                        onBackToHome = {
                            focusManager.clearFocus()
                            currentScreen = AppScreen.HOME
                        },
                        onOpenStorage = {
                            focusManager.clearFocus()
                            showModelManager = true
                        }
                    )
                }
                AppScreen.CODE_STUDIO -> {
                    FastCodeStudioScreen(
                        appManagerViewModel = appManagerViewModel,
                        onBack = { currentScreen = AppScreen.HOME }
                    )
                }
                AppScreen.MODEL_MANAGER -> {
                    ModelManagementScreen(
                        viewModel = modelViewModel,
                        onBack = {
                            focusManager.clearFocus()
                            currentScreen = AppScreen.HOME
                        }
                    )
                }
                AppScreen.NOTES -> {
                    NotesAppScreen(onBack = { currentScreen = AppScreen.HOME })
                }
                AppScreen.CALCULATOR -> {
                    CalculatorAppScreen(onBack = { currentScreen = AppScreen.HOME })
                }
                AppScreen.FOCUS_TIMER -> {
                    FocusTimerScreen(onBack = { currentScreen = AppScreen.HOME })
                }
                AppScreen.PROMPT_LAB -> {
                    PromptLabScreen(onBack = { currentScreen = AppScreen.HOME })
                }
                AppScreen.AMBIENT_NOISE -> {
                    AmbientNoiseScreen(onBack = { currentScreen = AppScreen.HOME })
                }
                AppScreen.WEATHER -> {
                    AtmosphereWeatherScreen(onBack = { currentScreen = AppScreen.HOME })
                }
                AppScreen.CUSTOM_WEB -> {
                    CustomWebAppRunnerScreen(
                        title = activeWebTitle,
                        url = activeWebUrl,
                        onBack = { currentScreen = AppScreen.HOME }
                    )
                }
            }
        }

        if (showAppStoreSheet) {
            AppStoreModalSheet(
                viewModel = appManagerViewModel,
                onDismiss = { 
                    focusManager.clearFocus()
                    showAppStoreSheet = false 
                },
                onOpenApp = { appId ->
                    val installed = appManagerViewModel.installedApps.value.firstOrNull { it.id == appId }
                    val catalogItem = appManagerViewModel.catalog.value.firstOrNull { it.id == appId }
                    launchAppById(appId, installed?.webUrl ?: catalogItem?.webUrl, installed?.name ?: catalogItem?.name)
                }
            )
        }

        if (showModelManager) {
            ModalBottomSheet(
                onDismissRequest = { 
                    focusManager.clearFocus()
                    showModelManager = false 
                },
                modifier = Modifier.fillMaxHeight(0.9f)
            ) {
                ModelManagerSheet(
                    viewModel = modelViewModel,
                    onDismiss = { 
                        focusManager.clearFocus()
                        showModelManager = false 
                    }
                )
            }
        }
    }
}

@Composable
fun SystemNavigationBarMock(
    onBack: () -> Unit = {},
    onHome: () -> Unit = {},
    onOverview: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onHome) {
            Box(modifier = Modifier.size(16.dp).background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape))
        }
        IconButton(onClick = onOverview) {
            Box(modifier = Modifier.size(14.dp).background(MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(2.dp)))
        }
    }
}

@Composable
fun GoogleStyleLogo() {
    val annotatedString = buildAnnotatedString {
        withStyle(SpanStyle(color = Color(0xFF4285F4))) { append("a") }
        withStyle(SpanStyle(color = Color(0xFFEA4335))) { append("i") }
        withStyle(SpanStyle(color = Color(0xFFFBBC05))) { append("d") }
        withStyle(SpanStyle(color = Color(0xFF34A853))) { append("e") }
        append(" ")
        withStyle(SpanStyle(color = Color(0xFF4285F4))) { append("h") }
        withStyle(SpanStyle(color = Color(0xFFEA4335))) { append("o") }
        withStyle(SpanStyle(color = Color(0xFFFBBC05))) { append("m") }
        withStyle(SpanStyle(color = Color(0xFF34A853))) { append("e") }
        append(" ")
        withStyle(SpanStyle(color = Color(0xFF8E24AA))) { append("AIUI") }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(bottom = 24.dp)
    ) {
        Text(
            text = annotatedString,
            fontSize = 36.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Text(
                text = "⚡ Built-in Intelligent Home Launcher",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun HomeScreen(
    appManagerViewModel: AppManagerViewModel,
    onNavigateToChat: (String) -> Unit,
    onNavigateToImageGen: () -> Unit,
    onNavigateToAide: () -> Unit,
    onOpenStorage: () -> Unit,
    onOpenAppStore: () -> Unit,
    onLaunchApp: (InstalledApp) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val installedApps by appManagerViewModel.installedApps.collectAsState()

    val luckyPrompts = remember {
        listOf(
            "Tell me an astonishing fact about the universe.",
            "Write a short creative sci-fi story in 3 sentences.",
            "Give me a mind-bending riddle to solve.",
            "Explain how on-device AI runs with zero internet.",
            "Tell me a fun programming joke with an explanation.",
            "Give me a daily motivational insight and action item.",
            "What are the benefits of open-source GGUF models?"
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Top Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Install Apps Market Button
                FilledTonalButton(
                    onClick = {
                        focusManager.clearFocus()
                        onOpenAppStore()
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Install Apps", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                // Storage Hub Button
                IconButton(
                    onClick = {
                        focusManager.clearFocus()
                        onOpenStorage()
                    }
                ) {
                    Icon(Icons.Filled.Folder, contentDescription = "Manage Models & Storage", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            GoogleStyleLogo()

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search or ask anything...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                trailingIcon = {
                    IconButton(onClick = {
                        focusManager.clearFocus()
                        val voiceSample = "Explain how local AI models work without internet"
                        searchQuery = voiceSample
                        onNavigateToChat(voiceSample)
                    }) {
                        Icon(Icons.Filled.Mic, contentDescription = "Voice", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                shape = RoundedCornerShape(32.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                        onNavigateToChat(searchQuery)
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        onNavigateToChat(searchQuery)
                    },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AI Search")
                }
                FilledTonalButton(
                    onClick = {
                        focusManager.clearFocus()
                        val randomPrompt = luckyPrompts.random()
                        onNavigateToChat(randomPrompt)
                    },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("I'm Feeling Lucky")
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Installed Apps Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Home Screen Apps",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = CircleShape
                    ) {
                        Text(
                            text = "${installedApps.size}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                TextButton(
                    onClick = {
                        focusManager.clearFocus()
                        onOpenAppStore()
                    }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Install New", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Installed Apps Grid (rendered in rows of 2 columns)
            val chunkedApps = installedApps.chunked(2)
            chunkedApps.forEach { rowApps ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowApps.forEach { app ->
                        Card(
                            onClick = {
                                focusManager.clearFocus()
                                onLaunchApp(app)
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.weight(1f).height(100.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(Color(app.accentColorHex), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getAppIconVector(app.iconName),
                                        contentDescription = app.name,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = app.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = app.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    app.badge?.let { badge ->
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = badge,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (rowApps.size == 1) {
                        // Quick Install card to balance grid
                        Card(
                            onClick = {
                                focusManager.clearFocus()
                                onOpenAppStore()
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.weight(1f).height(100.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Install More", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Install Apps Banner Card
            Card(
                onClick = {
                    focusManager.clearFocus()
                    onOpenAppStore()
                },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Explore & Install New Apps", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("Calculator, Notes, Pomodoro, Dev Sandbox & Web PWAs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Icon(Icons.Filled.ArrowDownward, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel,
    onOpenStorage: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val messages by viewModel.messages.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()
    val sessionTitle by viewModel.sessionTitle.collectAsState()
    val sessionId by viewModel.sessionId.collectAsState()
    val savedBackups by viewModel.savedBackups.collectAsState()
    val exportStatusMessage by viewModel.exportStatusMessage.collectAsState()

    var textState by remember { mutableStateOf("") }
    var showExportDialog by remember { mutableStateOf(false) }
    var showBackupHistoryDialog by remember { mutableStateOf(false) }
    var exportFormat by remember { mutableStateOf(ExportFormat.MARKDOWN) }
    var customExportTitle by remember { mutableStateOf(sessionTitle) }
    var showMenu by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(sessionTitle) {
        customExportTitle = sessionTitle
    }

    LaunchedEffect(Unit) {
        viewModel.refreshSavedBackups(context)
    }

    LaunchedEffect(exportStatusMessage) {
        exportStatusMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearExportStatus()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text("Ultra Chat AI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "$sessionTitle • ${messages.size} msgs",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            actions = {
                // Export & Backup Action Button
                IconButton(onClick = {
                    focusManager.clearFocus()
                    customExportTitle = sessionTitle
                    showExportDialog = true
                }) {
                    Icon(Icons.Filled.Description, contentDescription = "Export Chat Backup")
                }

                // More Menu with Backup History & New Session
                Box {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Filled.History, contentDescription = "Backups & Options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("📁 Export as Markdown (.md)") },
                            onClick = {
                                showMenu = false
                                exportFormat = ExportFormat.MARKDOWN
                                customExportTitle = sessionTitle
                                showExportDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("📄 Export as Plain Text (.txt)") },
                            onClick = {
                                showMenu = false
                                exportFormat = ExportFormat.PLAIN_TEXT
                                customExportTitle = sessionTitle
                                showExportDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("🗄️ View Saved Local Backups (${savedBackups.size})") },
                            onClick = {
                                showMenu = false
                                viewModel.refreshSavedBackups(context)
                                showBackupHistoryDialog = true
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("➕ Start New Session") },
                            onClick = {
                                showMenu = false
                                viewModel.startNewSession()
                                Toast.makeText(context, "Started new private session", Toast.LENGTH_SHORT).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("⚙️ Manage Local Models") },
                            onClick = {
                                showMenu = false
                                onOpenStorage()
                            }
                        )
                    }
                }

                IconButton(onClick = {
                    focusManager.clearFocus()
                    onOpenStorage()
                }) {
                    Icon(Icons.Filled.Settings, contentDescription = "Manage Models")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        // Session Quick Info Banner
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
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
                        text = "100% Offline • On-Device Storage",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(
                    onClick = {
                        customExportTitle = sessionTitle
                        showExportDialog = true
                    },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export Session", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            reverseLayout = false
        ) {
            items(messages) { message ->
                ChatBubble(message)
            }
            if (isTyping) {
                item {
                    TypingIndicator()
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textState,
                onValueChange = { textState = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") },
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (textState.isNotBlank()) {
                            viewModel.sendMessage(textState)
                            textState = ""
                            focusManager.clearFocus()
                        }
                    }
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            FloatingActionButton(
                onClick = {
                    if (textState.isNotBlank()) {
                        viewModel.sendMessage(textState)
                        textState = ""
                        focusManager.clearFocus()
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }

    // EXPORT & LOCAL BACKUP DIALOG
    if (showExportDialog) {
        val previewContent = remember(exportFormat, customExportTitle, messages) {
            when (exportFormat) {
                ExportFormat.MARKDOWN -> viewModel.exportToMarkdown(customExportTitle)
                ExportFormat.PLAIN_TEXT -> viewModel.exportToPlainText(customExportTitle)
            }
        }

        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Chat Session", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Backup this conversation to local device storage as plain text or Markdown.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Title Input
                    OutlinedTextField(
                        value = customExportTitle,
                        onValueChange = { customExportTitle = it },
                        label = { Text("Session Backup Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Format Selection (Markdown vs Plain Text)
                    Text("Select Backup Format:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            onClick = { exportFormat = ExportFormat.MARKDOWN },
                            shape = RoundedCornerShape(10.dp),
                            color = if (exportFormat == ExportFormat.MARKDOWN) MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "📝 Markdown (.md)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (exportFormat == ExportFormat.MARKDOWN) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Formatted Headers & Quotes",
                                    fontSize = 10.sp,
                                    color = if (exportFormat == ExportFormat.MARKDOWN) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            onClick = { exportFormat = ExportFormat.PLAIN_TEXT },
                            shape = RoundedCornerShape(10.dp),
                            color = if (exportFormat == ExportFormat.PLAIN_TEXT) MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "📄 Plain Text (.txt)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (exportFormat == ExportFormat.PLAIN_TEXT) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Universal Transcript",
                                    fontSize = 10.sp,
                                    color = if (exportFormat == ExportFormat.PLAIN_TEXT) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Transcript Preview
                    Text("Preview (${messages.size} messages • ${previewContent.length} chars):", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Box(modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState())) {
                            Text(
                                text = previewContent,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Quick Actions Row (Copy & Share)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(previewContent))
                                Toast.makeText(context, "Copied transcript to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text("📋 Copy", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val intent = viewModel.createTextShareIntent(customExportTitle, exportFormat)
                                context.startActivity(Intent.createChooser(intent, "Share Chat Transcript"))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val savedFile = viewModel.saveSessionToLocalBackup(context, exportFormat, customExportTitle)
                        if (savedFile != null) {
                            showExportDialog = false
                            Toast.makeText(context, "Saved backup to: ${savedFile.name}", Toast.LENGTH_LONG).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save to Local Storage")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // SAVED BACKUPS VIEWER DIALOG
    if (showBackupHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showBackupHistoryDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Local Chat Backups", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Saved in app backup storage directory (${savedBackups.size} files):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (savedBackups.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            color = Color.Transparent
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("📂", fontSize = 32.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("No local chat backups yet.", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Text("Export your current chat to see it here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        savedBackups.forEach { backup ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (backup.format == ExportFormat.MARKDOWN) "📝" else "📄",
                                                fontSize = 14.sp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = backup.name,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${formatSize(backup.sizeBytes)} • ${formatFileDate(backup.lastModified)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                val intent = viewModel.createShareIntent(context, backup.file, backup.format)
                                                context.startActivity(Intent.createChooser(intent, "Share Backup"))
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Filled.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                                        }

                                        IconButton(
                                            onClick = {
                                                viewModel.deleteBackupFile(context, backup)
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBackupHistoryDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.isUser
    val bubbleColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val alignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = bubbleColor,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 0.dp,
                        bottomEnd = if (isUser) 0.dp else 16.dp
                    )
                )
                .padding(12.dp)
                .widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                color = textColor
            )
        }
    }
}

@Composable
fun TypingIndicator() {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = 0.dp,
                    bottomEnd = 16.dp
                )
            )
            .padding(12.dp)
    ) {
        Text(
            text = "AI is thinking...",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

fun formatSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

fun formatFileDate(timestamp: Long): String {
    if (timestamp <= 0) return "Unknown date"
    val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ModelManagerSheet(
    viewModel: ModelDownloadViewModel,
    onDismiss: () -> Unit
) {
    val downloadQueue by viewModel.downloadQueue.collectAsState()
    val localModels by viewModel.localModels.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val totalSize by viewModel.totalSizeBytes.collectAsState()
    
    var urlState by remember { mutableStateOf("") }
    var nameState by remember { mutableStateOf("") }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var showPresets by remember { mutableStateOf(true) }
    var selectedQuantization by remember { mutableStateOf(QuantizationLevel.Q4_BIT) }
    val presetOverrides = remember { mutableStateMapOf<String, QuantizationLevel>() }
    val focusManager = LocalFocusManager.current

    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }

    val activeDownloads = downloadQueue.filter { it.status == DownloadTaskStatus.DOWNLOADING }
    val queuedDownloads = downloadQueue.filter { it.status == DownloadTaskStatus.QUEUED }
    val completedDownloads = downloadQueue.filter { it.status == DownloadTaskStatus.COMPLETED }
    val failedDownloads = downloadQueue.filter { it.status == DownloadTaskStatus.FAILED || it.status == DownloadTaskStatus.CANCELLED }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Model & Storage Hub",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Manage local LLMs, quantization & storage",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }
        }

        // Notification permission hint if not granted on API 33+
        if (notificationPermissionState != null && !notificationPermissionState.status.isGranted) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Background Notifications",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Enable to view download progress in status bar while multitasking.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledTonalButton(
                            onClick = { notificationPermissionState.launchPermissionRequest() },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Enable", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // Storage Overview Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Storage,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Total Model Storage", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = formatSize(totalSize),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${localModels.size} Installed",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (downloadQueue.isNotEmpty()) {
                            Text(
                                text = "${activeDownloads.size + queuedDownloads.size} in queue",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }
        }

        // SECTION: Download Queue (if tasks exist)
        if (downloadQueue.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Download Queue",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Badge(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                                Text(
                                    "${downloadQueue.size}",
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (completedDownloads.isNotEmpty() || failedDownloads.isNotEmpty()) {
                                TextButton(
                                    onClick = { viewModel.clearCompletedTasks() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Clear Done", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            if (activeDownloads.isNotEmpty() || queuedDownloads.isNotEmpty()) {
                                TextButton(
                                    onClick = { viewModel.cancelAllDownloads() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Cancel All", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            items(downloadQueue, key = { it.id }) { task ->
                DownloadTaskCard(
                    task = task,
                    queuePosition = if (task.status == DownloadTaskStatus.QUEUED) {
                        queuedDownloads.indexOf(task) + 1
                    } else 0,
                    onCancel = { viewModel.cancelTask(task.id) },
                    onRetry = { viewModel.retryTask(task.id) },
                    onRemove = { viewModel.removeTask(task.id) }
                )
            }
        }

        // SECTION: Quantization Level Selector Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Quantization Precision",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Optimize memory footprint vs reasoning fidelity",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 4-bit vs 8-bit Segmented Selection Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // 4-bit Toggle Button
                        Surface(
                            onClick = { selectedQuantization = QuantizationLevel.Q4_BIT },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedQuantization == QuantizationLevel.Q4_BIT) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.Transparent
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "⚡ 4-bit (Q4_K_M)",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selectedQuantization == QuantizationLevel.Q4_BIT) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                                Text(
                                    text = "Low RAM / Fast",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = if (selectedQuantization == QuantizationLevel.Q4_BIT) {
                                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }

                        // 8-bit Toggle Button
                        Surface(
                            onClick = { selectedQuantization = QuantizationLevel.Q8_BIT },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedQuantization == QuantizationLevel.Q8_BIT) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.Transparent
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "🎯 8-bit (Q8_0)",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selectedQuantization == QuantizationLevel.Q8_BIT) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                                Text(
                                    text = "High Precision",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = if (selectedQuantization == QuantizationLevel.Q8_BIT) {
                                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dynamic Guidance Message based on active Quantization Level
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedQuantization == QuantizationLevel.Q4_BIT) {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (selectedQuantization == QuantizationLevel.Q4_BIT) {
                                        MaterialTheme.colorScheme.secondary
                                    } else {
                                        MaterialTheme.colorScheme.tertiary
                                    },
                                    modifier = Modifier.size(8.dp)
                                ) {}
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (selectedQuantization == QuantizationLevel.Q4_BIT) {
                                        "⚡ Recommended for Low-Memory Devices (${selectedQuantization.ramRequirement})"
                                    } else {
                                        "🎯 Recommended for Flagship Devices (${selectedQuantization.ramRequirement})"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedQuantization == QuantizationLevel.Q4_BIT) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = selectedQuantization.description,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // SECTION: Quick Presets Gallery with Quantization
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.CloudDownload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Quick Model Presets",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(
                            onClick = {
                                viewModel.addPresetsToQueue(POPULAR_MODEL_PRESETS, selectedQuantization)
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Queue All (${if (selectedQuantization == QuantizationLevel.Q4_BIT) "4-bit" else "8-bit"})", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Text(
                        text = "Tested quantized GGUF models ready for on-device execution.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        POPULAR_MODEL_PRESETS.forEach { preset ->
                            val currentLevel = presetOverrides[preset.id] ?: selectedQuantization
                            val activeVariant = preset.getVariant(currentLevel)

                            val isAlreadyInstalled = localModels.any { it.name.equals(activeVariant.fileName, ignoreCase = true) }
                            val isQueuedOrDownloading = downloadQueue.any { 
                                it.fileName.equals(activeVariant.fileName, ignoreCase = true) && 
                                (it.status == DownloadTaskStatus.QUEUED || it.status == DownloadTaskStatus.DOWNLOADING)
                            }

                            // Check if the other quantization variant is installed
                            val otherLevel = if (currentLevel == QuantizationLevel.Q4_BIT) QuantizationLevel.Q8_BIT else QuantizationLevel.Q4_BIT
                            val otherVariant = preset.getVariant(otherLevel)
                            val isOtherVariantInstalled = localModels.any { it.name.equals(otherVariant.fileName, ignoreCase = true) }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 1.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = preset.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                SuggestionChip(
                                                    onClick = {},
                                                    label = { Text(preset.tag, style = MaterialTheme.typography.labelSmall) },
                                                    modifier = Modifier.height(24.dp)
                                                )
                                            }
                                            Text(
                                                text = preset.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        when {
                                            isAlreadyInstalled -> {
                                                FilledTonalButton(
                                                    onClick = {},
                                                    enabled = false,
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(36.dp)
                                                ) {
                                                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Installed", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                            isQueuedOrDownloading -> {
                                                FilledTonalButton(
                                                    onClick = {},
                                                    enabled = false,
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(36.dp)
                                                ) {
                                                    Text("In Queue", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                            else -> {
                                                Button(
                                                    onClick = {
                                                        viewModel.addToQueue(activeVariant.url, activeVariant.fileName)
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(36.dp)
                                                ) {
                                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Queue", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Per-Model Quantization Selector Chips & Spec Info
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 4-bit / 8-bit interactive chips
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            FilterChip(
                                                selected = currentLevel == QuantizationLevel.Q4_BIT,
                                                onClick = {
                                                    presetOverrides[preset.id] = QuantizationLevel.Q4_BIT
                                                },
                                                label = {
                                                    Text(
                                                        "4-bit (${preset.getVariant(QuantizationLevel.Q4_BIT).estimatedSize})",
                                                        fontSize = 11.sp
                                                    )
                                                },
                                                modifier = Modifier.height(28.dp)
                                            )
                                            FilterChip(
                                                selected = currentLevel == QuantizationLevel.Q8_BIT,
                                                onClick = {
                                                    presetOverrides[preset.id] = QuantizationLevel.Q8_BIT
                                                },
                                                label = {
                                                    Text(
                                                        "8-bit (${preset.getVariant(QuantizationLevel.Q8_BIT).estimatedSize})",
                                                        fontSize = 11.sp
                                                    )
                                                },
                                                modifier = Modifier.height(28.dp)
                                            )
                                        }

                                        // RAM Requirement indicator
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "RAM: ${activeVariant.ramRecommendation}",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (isOtherVariantInstalled && !isAlreadyInstalled) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "ℹ️ ${otherLevel.shortName} variant is currently installed.",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // SECTION: Custom Model Download Input
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Add Custom Model to Queue",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Download direct GGUF / SafeTensors weights from Hugging Face or direct HTTP URLs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = urlState,
                        onValueChange = { urlState = it },
                        label = { Text("Direct Model URL") },
                        placeholder = { Text("https://huggingface.co/.../model.gguf") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = nameState,
                            onValueChange = { nameState = it },
                            label = { Text("Local File Name (optional)") },
                            placeholder = { Text("my_custom_model.gguf") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    if (urlState.isNotBlank()) {
                                        viewModel.addToQueue(urlState, nameState)
                                        urlState = ""
                                        nameState = ""
                                    }
                                }
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { 
                                focusManager.clearFocus()
                                if (urlState.isNotBlank()) {
                                    viewModel.addToQueue(urlState, nameState)
                                    urlState = ""
                                    nameState = ""
                                }
                            },
                            modifier = Modifier.height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = urlState.isNotBlank()
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Queue")
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quantization Helper:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SuggestionChip(
                            onClick = {
                                if (nameState.isNotBlank() && !nameState.contains("q4", ignoreCase = true)) {
                                    val base = nameState.substringBeforeLast(".")
                                    val ext = if (nameState.contains(".")) nameState.substringAfterLast(".") else "gguf"
                                    nameState = "${base}_q4_k_m.$ext"
                                }
                            },
                            label = { Text("Append _q4_k_m (4-bit)", fontSize = 10.sp) },
                            modifier = Modifier.height(26.dp)
                        )
                        SuggestionChip(
                            onClick = {
                                if (nameState.isNotBlank() && !nameState.contains("q8", ignoreCase = true)) {
                                    val base = nameState.substringBeforeLast(".")
                                    val ext = if (nameState.contains(".")) nameState.substringAfterLast(".") else "gguf"
                                    nameState = "${base}_q8_0.$ext"
                                }
                            },
                            label = { Text("Append _q8_0 (8-bit)", fontSize = 10.sp) },
                            modifier = Modifier.height(26.dp)
                        )
                    }
                }
            }
        }

        // SECTION: Installed Models List with Sorting
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Installed Models",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (localModels.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                                Text(
                                    "${localModels.size}",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    
                    Box {
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort Options",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            Text(
                                text = "Sort Models By",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            SortOption.values().forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    leadingIcon = {
                                        if (sortOption == option) {
                                            Icon(
                                                Icons.Filled.Check,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.size(18.dp))
                                        }
                                    },
                                    onClick = {
                                        viewModel.setSortOption(option)
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Quick Sorting Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SortField.values().forEach { field ->
                        val isSelected = sortOption.field == field
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.setSortField(field)
                            },
                            label = { Text(field.label) },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = if (sortOption.order == SortOrder.ASCENDING) {
                                            Icons.Filled.ArrowUpward
                                        } else {
                                            Icons.Filled.ArrowDownward
                                        },
                                        contentDescription = sortOption.order.label,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null
                        )
                    }
                }
            }
        }

        if (localModels.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "No models installed yet. Choose a preset above to begin.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            items(localModels, key = { it.name }) { file ->
                val activeModelName by viewModel.activeModelName.collectAsState()
                val isActive = file.name.equals(activeModelName, ignoreCase = true)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActive) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            RadioButton(
                                selected = isActive,
                                onClick = { viewModel.toggleActiveModel(file.name) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = file.name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (isActive) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        ) {
                                            Text(
                                                text = "ACTIVE",
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${formatSize(file.length())} • ${formatFileDate(file.lastModified())}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = isActive,
                                onCheckedChange = { viewModel.toggleActiveModel(file.name) }
                            )
                            IconButton(onClick = { viewModel.deleteModel(file.name) }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Delete Model",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadTaskCard(
    task: DownloadTask,
    queuePosition: Int,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (task.status) {
                DownloadTaskStatus.DOWNLOADING -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                DownloadTaskStatus.COMPLETED -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                DownloadTaskStatus.FAILED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                DownloadTaskStatus.CANCELLED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                DownloadTaskStatus.QUEUED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    when (task.status) {
                        DownloadTaskStatus.DOWNLOADING -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        DownloadTaskStatus.QUEUED -> {
                            Badge(containerColor = MaterialTheme.colorScheme.secondary) {
                                Text("#$queuePosition", color = MaterialTheme.colorScheme.onSecondary)
                            }
                        }
                        DownloadTaskStatus.COMPLETED -> {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DownloadTaskStatus.FAILED -> {
                            Icon(
                                Icons.Filled.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DownloadTaskStatus.CANCELLED -> {
                            Icon(
                                Icons.Filled.Cancel,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = task.fileName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (task.status) {
                        DownloadTaskStatus.DOWNLOADING, DownloadTaskStatus.QUEUED -> {
                            IconButton(onClick = onCancel, modifier = Modifier.size(28.dp)) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Cancel Download",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        DownloadTaskStatus.FAILED, DownloadTaskStatus.CANCELLED -> {
                            IconButton(onClick = onRetry, modifier = Modifier.size(28.dp)) {
                                Icon(
                                    Icons.Filled.Refresh,
                                    contentDescription = "Retry Download",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Remove",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        DownloadTaskStatus.COMPLETED -> {
                            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Dismiss",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            when (task.status) {
                DownloadTaskStatus.DOWNLOADING -> {
                    if (task.progress >= 0f) {
                        LinearProgressIndicator(
                            progress = { task.progress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val downloadedStr = formatSize(task.downloadedBytes)
                        val totalStr = if (task.totalBytes > 0) formatSize(task.totalBytes) else "..."
                        val percentStr = if (task.progress >= 0f) "${task.progress.toInt()}%" else "Downloading..."
                        val speedStr = if (task.speedBytesPerSec > 0) "${formatSize(task.speedBytesPerSec)}/s" else ""

                        Text(
                            text = "$downloadedStr / $totalStr ($percentStr)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (speedStr.isNotEmpty()) {
                            Text(
                                text = speedStr,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                DownloadTaskStatus.QUEUED -> {
                    Text(
                        text = "Queued — waiting for active downloads to finish",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DownloadTaskStatus.COMPLETED -> {
                    Text(
                        text = "Installed successfully (${formatSize(task.downloadedBytes)}) • Ready to run",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                DownloadTaskStatus.FAILED -> {
                    Text(
                        text = "Failed: ${task.errorMessage ?: "Download interrupted"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                DownloadTaskStatus.CANCELLED -> {
                    Text(
                        text = "Download cancelled by user",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

