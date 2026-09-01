package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class SortField(val label: String) {
    DATE("Date"),
    SIZE("Size"),
    NAME("Alphabetical")
}

enum class SortOrder(val label: String) {
    ASCENDING("Ascending"),
    DESCENDING("Descending")
}

enum class SortOption(val field: SortField, val order: SortOrder, val label: String) {
    DATE_NEWEST(SortField.DATE, SortOrder.DESCENDING, "Date (Newest first)"),
    DATE_OLDEST(SortField.DATE, SortOrder.ASCENDING, "Date (Oldest first)"),
    SIZE_LARGEST(SortField.SIZE, SortOrder.DESCENDING, "Size (Largest first)"),
    SIZE_SMALLEST(SortField.SIZE, SortOrder.ASCENDING, "Size (Smallest first)"),
    NAME_AZ(SortField.NAME, SortOrder.ASCENDING, "Alphabetical (A - Z)"),
    NAME_ZA(SortField.NAME, SortOrder.DESCENDING, "Alphabetical (Z - A)");

    companion object {
        val NAME = NAME_AZ
        val SIZE = SIZE_LARGEST
        val DATE = DATE_NEWEST
    }
}

class ModelDownloadViewModel(application: Application) : AndroidViewModel(application) {
    private val downloader = ModelDownloader(application)
    private val queueManager = ModelDownloadQueueManager.getInstance(application)
    private val prefs = application.getSharedPreferences("model_manager_prefs", android.content.Context.MODE_PRIVATE)

    val downloadQueue: StateFlow<List<DownloadTask>> = queueManager.queueState

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val _localModels = MutableStateFlow<List<File>>(emptyList())
    val localModels: StateFlow<List<File>> = _localModels.asStateFlow()

    private val _installedModelInfos = MutableStateFlow<List<InstalledModelInfo>>(emptyList())
    val installedModelInfos: StateFlow<List<InstalledModelInfo>> = _installedModelInfos.asStateFlow()

    private val _activeModelName = MutableStateFlow<String?>(prefs.getString("active_model_name", null))
    val activeModelName: StateFlow<String?> = _activeModelName.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.DATE_NEWEST)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _totalSizeBytes = MutableStateFlow(0L)
    val totalSizeBytes: StateFlow<Long> = _totalSizeBytes.asStateFlow()

    init {
        refreshModels()

        // Automatically refresh models whenever any queue item completes or is removed
        viewModelScope.launch {
            queueManager.queueState.collect { queue ->
                val active = queue.firstOrNull { it.status == DownloadTaskStatus.DOWNLOADING }
                if (active != null) {
                    _downloadState.value = DownloadState.Downloading(active.progress)
                } else if (queue.any { it.status == DownloadTaskStatus.COMPLETED }) {
                    refreshModels()
                }
            }
        }
    }

    fun startDownload(url: String, fileName: String) {
        addToQueue(url, fileName)
    }

    fun addToQueue(url: String, fileName: String) {
        val validUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else url
        
        var safeFileName = fileName.trim()
        if (safeFileName.isBlank()) {
            safeFileName = validUrl.substringAfterLast("/").substringBefore("?")
            if (safeFileName.isBlank()) safeFileName = "model_${System.currentTimeMillis()}.gguf"
        }
        if (!safeFileName.contains(".")) {
            safeFileName = "$safeFileName.gguf"
        }

        queueManager.enqueue(validUrl, safeFileName)
    }

    fun addPresetsToQueue(presets: List<ModelPreset>, level: QuantizationLevel = QuantizationLevel.Q4_BIT) {
        val items = presets.map { 
            val variant = it.getVariant(level)
            variant.url to variant.fileName 
        }
        queueManager.enqueueMultiple(items)
    }

    fun cancelTask(taskId: String) {
        queueManager.cancelTask(taskId)
    }

    fun retryTask(taskId: String) {
        queueManager.retryTask(taskId)
    }

    fun removeTask(taskId: String) {
        queueManager.removeTask(taskId)
    }

    fun clearCompletedTasks() {
        queueManager.clearCompleted()
        refreshModels()
    }

    fun cancelAllDownloads() {
        queueManager.cancelAll()
    }

    fun resetDownloadState() {
        _downloadState.value = DownloadState.Idle
    }

    fun setActiveModel(fileName: String?) {
        _activeModelName.value = fileName
        prefs.edit().putString("active_model_name", fileName).apply()
        updateInstalledModelInfos()
    }

    fun toggleActiveModel(fileName: String) {
        if (_activeModelName.value.equals(fileName, ignoreCase = true)) {
            setActiveModel(null)
        } else {
            setActiveModel(fileName)
        }
    }

    fun deleteModel(fileName: String) {
        downloader.deleteModel(fileName)
        if (_activeModelName.value.equals(fileName, ignoreCase = true)) {
            setActiveModel(null)
        }
        refreshModels()
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
        refreshModels()
    }

    fun setSortField(field: SortField) {
        val current = _sortOption.value
        val newOption = when (field) {
            SortField.DATE -> if (current.field == SortField.DATE && current.order == SortOrder.DESCENDING) SortOption.DATE_OLDEST else SortOption.DATE_NEWEST
            SortField.SIZE -> if (current.field == SortField.SIZE && current.order == SortOrder.DESCENDING) SortOption.SIZE_SMALLEST else SortOption.SIZE_LARGEST
            SortField.NAME -> if (current.field == SortField.NAME && current.order == SortOrder.ASCENDING) SortOption.NAME_ZA else SortOption.NAME_AZ
        }
        _sortOption.value = newOption
        refreshModels()
    }

    fun toggleSortOrder() {
        val current = _sortOption.value
        val newOption = when (current) {
            SortOption.DATE_NEWEST -> SortOption.DATE_OLDEST
            SortOption.DATE_OLDEST -> SortOption.DATE_NEWEST
            SortOption.SIZE_LARGEST -> SortOption.SIZE_SMALLEST
            SortOption.SIZE_SMALLEST -> SortOption.SIZE_LARGEST
            SortOption.NAME_AZ -> SortOption.NAME_ZA
            SortOption.NAME_ZA -> SortOption.NAME_AZ
        }
        _sortOption.value = newOption
        refreshModels()
    }

    private fun updateInstalledModelInfos() {
        val activeName = _activeModelName.value
        _installedModelInfos.value = _localModels.value.map { file ->
            ModelInfoParser.parse(file, activeName)
        }
    }

    fun refreshModels() {
        val files = downloader.getDownloadedModels()
        _totalSizeBytes.value = files.sumOf { it.length() }
        
        val sortedFiles = when (_sortOption.value) {
            SortOption.NAME_AZ -> files.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            SortOption.NAME_ZA -> files.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.name })
            SortOption.SIZE_LARGEST -> files.sortedByDescending { it.length() }
            SortOption.SIZE_SMALLEST -> files.sortedBy { it.length() }
            SortOption.DATE_NEWEST -> files.sortedByDescending { it.lastModified() }
            SortOption.DATE_OLDEST -> files.sortedBy { it.lastModified() }
        }
        _localModels.value = sortedFiles

        // Auto-select first model as active if none is currently active and models exist
        if (_activeModelName.value == null && sortedFiles.isNotEmpty()) {
            val first = sortedFiles.first().name
            _activeModelName.value = first
            prefs.edit().putString("active_model_name", first).apply()
        } else if (_activeModelName.value != null && sortedFiles.none { it.name.equals(_activeModelName.value, ignoreCase = true) }) {
            // Active model file was deleted
            val fallback = sortedFiles.firstOrNull()?.name
            _activeModelName.value = fallback
            prefs.edit().putString("active_model_name", fallback).apply()
        }

        updateInstalledModelInfos()
    }
}
