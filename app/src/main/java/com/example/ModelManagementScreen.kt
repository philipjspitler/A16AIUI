package com.example

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

/**
 * Dedicated Model Management Screen.
 * Displays:
 * 1. Active Model Banner & quick toggle switch
 * 2. Real-time Storage & Disk Usage breakdowns with total storage bar
 * 3. Installed models with version tags, quantization, RAM requirements, disk usage, and Active Toggle switch
 * 4. Model presets download & direct custom model queueing
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ModelManagementScreen(
    viewModel: ModelDownloadViewModel,
    onBack: () -> Unit
) {
    val downloadQueue by viewModel.downloadQueue.collectAsState()
    val installedModelInfos by viewModel.installedModelInfos.collectAsState()
    val activeModelName by viewModel.activeModelName.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val totalSize by viewModel.totalSizeBytes.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Installed & Active, 1 = Presets & Downloads
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var selectedQuantization by remember { mutableStateOf(QuantizationLevel.Q4_BIT) }
    val presetOverrides = remember { mutableStateMapOf<String, QuantizationLevel>() }

    var urlState by remember { mutableStateOf("") }
    var nameState by remember { mutableStateOf("") }
    var showCustomInput by remember { mutableStateOf(false) }
    var modelToDelete by remember { mutableStateOf<InstalledModelInfo?>(null) }

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

    val activeModelInfo = installedModelInfos.firstOrNull { it.isActive }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Model Management",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (activeModelInfo != null) "Active: ${activeModelInfo.displayName}" else "No active model selected",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (activeModelInfo != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Home")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshModels() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh Models")
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
            // Top Tab Navigation Bar
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Installed (${installedModelInfos.size})", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (downloadQueue.isNotEmpty()) "Get Models (${downloadQueue.size})" else "Get Models",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ACTIVE MODEL HIGHLIGHT HERO CARD (Shown on both tabs)
                item {
                    ActiveModelHeroCard(
                        activeModel = activeModelInfo,
                        totalInstalled = installedModelInfos.size,
                        onSwitchClick = { selectedTab = 0 }
                    )
                }

                // STORAGE & DISK USAGE OVERVIEW CARD
                item {
                    StorageDiskUsageCard(
                        totalSizeBytes = totalSize,
                        installedModels = installedModelInfos,
                        activeDownloadsCount = activeDownloads.size + queuedDownloads.size
                    )
                }

                if (selectedTab == 0) {
                    // TAB 0: INSTALLED MODELS & ACTIVE TOGGLES
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Installed Model Versions",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                                    Text(
                                        "${installedModelInfos.size}",
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            // Sort dropdown button
                            Box {
                                TextButton(onClick = { sortMenuExpanded = true }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Sort,
                                        contentDescription = "Sort Options",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(sortOption.field.label, fontSize = 12.sp)
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
                    }

                    // Quick Filter chips
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SortField.values().forEach { field ->
                                val isSelected = sortOption.field == field
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setSortField(field) },
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

                    if (installedModelInfos.isEmpty()) {
                        item {
                            EmptyModelsInstalledCard(onBrowsePresets = { selectedTab = 1 })
                        }
                    } else {
                        items(installedModelInfos, key = { it.fileName }) { modelInfo ->
                            InstalledModelCard(
                                modelInfo = modelInfo,
                                onToggleActive = { viewModel.toggleActiveModel(modelInfo.fileName) },
                                onDelete = { modelToDelete = modelInfo }
                            )
                        }
                    }

                } else {
                    // TAB 1: PRESETS, QUANTIZATION & DOWNLOAD QUEUE
                    if (downloadQueue.isNotEmpty()) {
                        item {
                            DownloadQueueSection(
                                downloadQueue = downloadQueue,
                                queuedDownloads = queuedDownloads,
                                activeDownloads = activeDownloads,
                                completedDownloads = completedDownloads,
                                failedDownloads = failedDownloads,
                                viewModel = viewModel
                            )
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

                    // Quantization Selector Card
                    item {
                        QuantizationConfigCard(
                            selectedQuantization = selectedQuantization,
                            onSelectQuantization = { selectedQuantization = it }
                        )
                    }

                    // Quick Model Presets
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Model Presets",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = {
                                    viewModel.addPresetsToQueue(POPULAR_MODEL_PRESETS, selectedQuantization)
                                }
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Queue All (${selectedQuantization.shortName.take(5)})", fontSize = 12.sp)
                            }
                        }
                    }

                    items(POPULAR_MODEL_PRESETS, key = { it.id }) { preset ->
                        val currentLevel = presetOverrides[preset.id] ?: selectedQuantization
                        val activeVariant = preset.getVariant(currentLevel)

                        val isAlreadyInstalled = installedModelInfos.any { it.fileName.equals(activeVariant.fileName, ignoreCase = true) }
                        val isQueuedOrDownloading = downloadQueue.any { 
                            it.fileName.equals(activeVariant.fileName, ignoreCase = true) && 
                            (it.status == DownloadTaskStatus.QUEUED || it.status == DownloadTaskStatus.DOWNLOADING)
                        }

                        PresetModelCard(
                            preset = preset,
                            currentLevel = currentLevel,
                            activeVariant = activeVariant,
                            isAlreadyInstalled = isAlreadyInstalled,
                            isQueuedOrDownloading = isQueuedOrDownloading,
                            onSelectLevel = { level -> presetOverrides[preset.id] = level },
                            onQueueDownload = { viewModel.addToQueue(activeVariant.url, activeVariant.fileName) }
                        )
                    }

                    // Custom URL / HuggingFace Downloader
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Direct Custom Download",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Add custom GGUF model files from Hugging Face or URL",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { showCustomInput = !showCustomInput }) {
                                        Icon(
                                            if (showCustomInput) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                            contentDescription = "Toggle custom input"
                                        )
                                    }
                                }

                                AnimatedVisibility(visible = showCustomInput) {
                                    Column(modifier = Modifier.padding(top = 12.dp)) {
                                        OutlinedTextField(
                                            value = urlState,
                                            onValueChange = { urlState = it },
                                            label = { Text("Model Direct URL (.gguf)") },
                                            placeholder = { Text("https://huggingface.co/.../model.gguf") },
                                            modifier = Modifier.fillMaxWidth(),
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
                                                label = { Text("File Name (optional)") },
                                                placeholder = { Text("custom_model_q4_k_m.gguf") },
                                                modifier = Modifier.weight(1f),
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
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // CONFIRM DELETE DIALOG
    if (modelToDelete != null) {
        val target = modelToDelete!!
        AlertDialog(
            onDismissRequest = { modelToDelete = null },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Model File?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Are you sure you want to delete this model file from your device storage?")
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(target.displayName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text(target.fileName, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                            Text("Disk Space Reclaimed: ${target.formattedSize}", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                        }
                    }
                    if (target.isActive) {
                        Text(
                            "⚠️ This is currently your active model. If deleted, active inference will be disabled until you select another model.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteModel(target.fileName)
                        modelToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete (${target.formattedSize})")
                }
            },
            dismissButton = {
                TextButton(onClick = { modelToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Hero Card highlighting the currently active on-device model with status indicator.
 */
@Composable
private fun ActiveModelHeroCard(
    activeModel: InstalledModelInfo?,
    totalInstalled: Int,
    onSwitchClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (activeModel != null) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = if (activeModel != null) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(10.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (activeModel != null) "ACTIVE LOCAL MODEL" else "NO ACTIVE MODEL",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        color = if (activeModel != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (activeModel != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(2.dp)
                ) {
                    Text(
                        text = if (activeModel != null) "ONLINE (ON-DEVICE)" else "STANDBY",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 9.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (activeModel != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeModel.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                            ) {
                                Text(
                                    text = "Version: ${activeModel.versionTag}",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                            ) {
                                Text(
                                    text = activeModel.quantization,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                            ) {
                                Text(
                                    text = activeModel.formattedSize,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    FilledTonalButton(
                        onClick = onSwitchClick,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Switch", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Text(
                    text = "Select an installed model below to set it as the primary engine for Chat, AI Assistant, and Code Aide.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

/**
 * Storage & Disk Usage Card detailing model footprint and breakdown.
 */
@Composable
private fun StorageDiskUsageCard(
    totalSizeBytes: Long,
    installedModels: List<InstalledModelInfo>,
    activeDownloadsCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.Storage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Disk Usage & Footprint", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("${installedModels.size} models loaded on device", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatSize(totalSizeBytes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    if (activeDownloadsCount > 0) {
                        Text(
                            text = "+$activeDownloadsCount downloading",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            if (installedModels.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                // Storage usage multi-segment bar visualization
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        val palette = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary,
                            MaterialTheme.colorScheme.tertiary,
                            MaterialTheme.colorScheme.error,
                            Color(0xFF9C27B0),
                            Color(0xFFFF9800)
                        )
                        val total = if (totalSizeBytes > 0) totalSizeBytes.toFloat() else 1f

                        installedModels.forEachIndexed { index, model ->
                            val weight = (model.sizeBytes.toFloat() / total).coerceAtLeast(0.02f)
                            Box(
                                modifier = Modifier
                                    .weight(weight)
                                    .fillMaxHeight()
                                    .background(palette[index % palette.size])
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Mini model disk tags
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    installedModels.take(3).forEach { model ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (model.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(6.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${model.displayName.take(10)}: ${model.formattedSize}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual installed model card with version tag, disk usage, active status indicator,
 * and prominent Switch Active Model toggle.
 */
@Composable
private fun InstalledModelCard(
    modelInfo: InstalledModelInfo,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (modelInfo.isActive) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "BorderColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (modelInfo.isActive) 2.dp else 1.dp,
                color = if (modelInfo.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (modelInfo.isActive) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header with Model Title & Active Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = modelInfo.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Text(
                                text = modelInfo.versionTag,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Text(
                        text = modelInfo.fileName,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 10.sp
                    )
                }

                // ACTIVE TOGGLE SWITCH
                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (modelInfo.isActive) "Active" else "Inactive",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (modelInfo.isActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (modelInfo.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Switch(
                            checked = modelInfo.isActive,
                            onCheckedChange = { onToggleActive() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Metadata Chips (Quantization, RAM, Disk Size, Last Modified)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Memory, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(modelInfo.quantization, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.SdStorage, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(modelInfo.formattedSize, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Text(
                        text = "RAM: ${modelInfo.ramRequirement}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Actions & Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Saved: ${formatFileDate(modelInfo.lastModified)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!modelInfo.isActive) {
                        TextButton(
                            onClick = onToggleActive,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Set as Active", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete model",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Empty state card when no models are downloaded yet.
 */
@Composable
private fun EmptyModelsInstalledCard(onBrowsePresets: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Filled.CloudDownload,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text("No Models Installed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Download quantized GGUF weights from our tested presets to enable fast on-device inference.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onBrowsePresets, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Browse Model Presets")
            }
        }
    }
}

/**
 * Quantization selection card.
 */
@Composable
private fun QuantizationConfigCard(
    selectedQuantization: QuantizationLevel,
    onSelectQuantization: (QuantizationLevel) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Quantization Precision", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Select global default quantization level for queueing downloads", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    onClick = { onSelectQuantization(QuantizationLevel.Q4_BIT) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedQuantization == QuantizationLevel.Q4_BIT) MaterialTheme.colorScheme.primary else Color.Transparent,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "⚡ 4-bit (Q4_K_M)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (selectedQuantization == QuantizationLevel.Q4_BIT) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Low RAM / High Speed",
                            fontSize = 10.sp,
                            color = if (selectedQuantization == QuantizationLevel.Q4_BIT) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    onClick = { onSelectQuantization(QuantizationLevel.Q8_BIT) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedQuantization == QuantizationLevel.Q8_BIT) MaterialTheme.colorScheme.primary else Color.Transparent,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🎯 8-bit (Q8_0)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (selectedQuantization == QuantizationLevel.Q8_BIT) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "High Reasoning Fidelity",
                            fontSize = 10.sp,
                            color = if (selectedQuantization == QuantizationLevel.Q8_BIT) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Preset Model Card with quantization variant selectors.
 */
@Composable
private fun PresetModelCard(
    preset: ModelPreset,
    currentLevel: QuantizationLevel,
    activeVariant: QuantizedVariant,
    isAlreadyInstalled: Boolean,
    isQueuedOrDownloading: Boolean,
    onSelectLevel: (QuantizationLevel) -> Unit,
    onQueueDownload: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = preset.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        SuggestionChip(
                            onClick = {},
                            label = { Text(preset.tag, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp) },
                            modifier = Modifier.height(22.dp)
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
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Installed", fontSize = 11.sp)
                        }
                    }
                    isQueuedOrDownloading -> {
                        FilledTonalButton(
                            onClick = {},
                            enabled = false,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("In Queue", fontSize = 11.sp)
                        }
                    }
                    else -> {
                        Button(
                            onClick = onQueueDownload,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Queue", fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = currentLevel == QuantizationLevel.Q4_BIT,
                        onClick = { onSelectLevel(QuantizationLevel.Q4_BIT) },
                        label = { Text("4-bit (${preset.getVariant(QuantizationLevel.Q4_BIT).estimatedSize})", fontSize = 10.sp) },
                        modifier = Modifier.height(26.dp)
                    )
                    FilterChip(
                        selected = currentLevel == QuantizationLevel.Q8_BIT,
                        onClick = { onSelectLevel(QuantizationLevel.Q8_BIT) },
                        label = { Text("8-bit (${preset.getVariant(QuantizationLevel.Q8_BIT).estimatedSize})", fontSize = 10.sp) },
                        modifier = Modifier.height(26.dp)
                    )
                }

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
        }
    }
}

/**
 * Queue section header and controls.
 */
@Composable
private fun DownloadQueueSection(
    downloadQueue: List<DownloadTask>,
    queuedDownloads: List<DownloadTask>,
    activeDownloads: List<DownloadTask>,
    completedDownloads: List<DownloadTask>,
    failedDownloads: List<DownloadTask>,
    viewModel: ModelDownloadViewModel
) {
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
                Text("${downloadQueue.size}", color = MaterialTheme.colorScheme.onTertiaryContainer)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (completedDownloads.isNotEmpty() || failedDownloads.isNotEmpty()) {
                TextButton(
                    onClick = { viewModel.clearCompletedTasks() },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Clear Done", fontSize = 11.sp)
                }
            }
            if (activeDownloads.isNotEmpty() || queuedDownloads.isNotEmpty()) {
                TextButton(
                    onClick = { viewModel.cancelAllDownloads() },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Cancel All", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }
            }
        }
    }
}
