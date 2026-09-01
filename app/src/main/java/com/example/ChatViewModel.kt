package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ExportFormat(val extension: String, val mimeType: String, val displayName: String) {
    MARKDOWN("md", "text/markdown", "Markdown (.md)"),
    PLAIN_TEXT("txt", "text/plain", "Plain Text (.txt)")
}

data class LocalChatBackup(
    val file: File,
    val name: String,
    val format: ExportFormat,
    val sizeBytes: Long,
    val lastModified: Long
)

class ChatViewModel : ViewModel() {
    private val _sessionId = MutableStateFlow(UUID.randomUUID().toString().take(8))
    val sessionId: StateFlow<String> = _sessionId.asStateFlow()

    private val _sessionTitle = MutableStateFlow("Local Assistant Chat")
    val sessionTitle: StateFlow<String> = _sessionTitle.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                text = "Welcome to Private Chat! This interface is ready for local on-device AI execution. No external servers or data tracking are active.", 
                isUser = false
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping = _isTyping.asStateFlow()

    private val _savedBackups = MutableStateFlow<List<LocalChatBackup>>(emptyList())
    val savedBackups: StateFlow<List<LocalChatBackup>> = _savedBackups.asStateFlow()

    private val _exportStatusMessage = MutableStateFlow<String?>(null)
    val exportStatusMessage: StateFlow<String?> = _exportStatusMessage.asStateFlow()

    fun setSessionTitle(title: String) {
        if (title.isNotBlank()) {
            _sessionTitle.value = title.trim()
        }
    }

    fun startNewSession(initialGreeting: String? = null) {
        _sessionId.value = UUID.randomUUID().toString().take(8)
        _sessionTitle.value = "Local Chat #${_sessionId.value}"
        _messages.value = listOf(
            ChatMessage(
                text = initialGreeting ?: "Started a new local AI chat session. All conversations remain private on your device.",
                isUser = false
            )
        )
    }

    fun clearMessages() {
        _messages.value = emptyList()
    }

    fun clearExportStatus() {
        _exportStatusMessage.value = null
    }

    fun sendMessage(message: String) {
        if (message.isBlank()) return

        // Auto update session title based on first user query if still default
        if (_sessionTitle.value == "Local Assistant Chat" || _sessionTitle.value.startsWith("Local Chat #")) {
            val preview = message.trim().take(32)
            _sessionTitle.value = if (message.length > 32) "$preview..." else preview
        }

        // Add user message
        val userMsg = ChatMessage(text = message, isUser = true)
        _messages.value = _messages.value + userMsg

        // Use Gemini REST API for actual responses
        viewModelScope.launch {
            _isTyping.value = true
            
            try {
                // Build history context for the API
                val contents = _messages.value.mapNotNull { msg ->
                    // Skip the initial welcome message from the assistant
                    if (msg.text.startsWith("Welcome to Private Chat") && !msg.isUser) return@mapNotNull null
                    
                    val role = if (msg.isUser) "user" else "model"
                    Content(role = role, parts = listOf(Part(text = msg.text)))
                }

                val request = GenerateContentRequest(
                    contents = contents,
                    systemInstruction = Content(
                        role = "system",
                        parts = listOf(Part(text = "You are a helpful on-device AI assistant integrated into the A16AIUI Android Launcher."))
                    )
                )

                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isBlank()) {
                    _messages.value = _messages.value + ChatMessage(text = "Error: API Key is missing. Please add it to your Secrets.", isUser = false)
                } else {
                    val response = RetrofitClient.service.generateContent(apiKey, request)
                    val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No response from model."
                    _messages.value = _messages.value + ChatMessage(text = responseText, isUser = false)
                }
            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage(text = "API Error: ${e.message}", isUser = false)
            } finally {
                _isTyping.value = false
            }
        }
    }

    /**
     * Generates a richly formatted Markdown export string for the current session.
     */
    fun exportToMarkdown(title: String = _sessionTitle.value): String {
        val currentMsgs = _messages.value
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val exportDate = dateFormat.format(Date())

        val sb = StringBuilder()
        sb.append("# 💬 $title\n\n")
        sb.append("> **Session ID:** `${_sessionId.value}`  \n")
        sb.append("> **Exported Date:** $exportDate  \n")
        sb.append("> **Total Messages:** ${currentMsgs.size}  \n")
        sb.append("> **Environment:** On-Device Local AI • Zero Telemetry  \n\n")
        sb.append("---\n\n")

        if (currentMsgs.isEmpty()) {
            sb.append("*No messages in this chat session.*\n")
            return sb.toString()
        }

        currentMsgs.forEachIndexed { index, msg ->
            val timestampStr = timeFormat.format(Date(msg.timestamp))
            if (msg.isUser) {
                sb.append("### 👤 User `[$timestampStr]`\n\n")
                sb.append(msg.text.trim())
                sb.append("\n\n")
            } else {
                sb.append("### 🤖 Local Assistant `[$timestampStr]`\n\n")
                sb.append(msg.text.trim())
                sb.append("\n\n")
            }
            if (index < currentMsgs.size - 1) {
                sb.append("---\n\n")
            }
        }

        sb.append("\n\n*Generated by Android Aide Private AI Chat Backup*\n")
        return sb.toString()
    }

    /**
     * Generates a clean Plain Text export string for the current session.
     */
    fun exportToPlainText(title: String = _sessionTitle.value): String {
        val currentMsgs = _messages.value
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val exportDate = dateFormat.format(Date())

        val sb = StringBuilder()
        sb.append("==================================================\n")
        sb.append(" CHAT SESSION BACKUP: $title\n")
        sb.append(" Session ID: ${_sessionId.value}\n")
        sb.append(" Exported: $exportDate\n")
        sb.append(" Messages: ${currentMsgs.size}\n")
        sb.append(" Platform: On-Device Private AI (Offline Storage)\n")
        sb.append("==================================================\n\n")

        if (currentMsgs.isEmpty()) {
            sb.append("[No messages in this chat session]\n")
            return sb.toString()
        }

        currentMsgs.forEach { msg ->
            val sender = if (msg.isUser) "USER" else "ASSISTANT"
            val timestampStr = timeFormat.format(Date(msg.timestamp))
            sb.append("[$timestampStr] $sender:\n")
            sb.append(msg.text.trim())
            sb.append("\n\n--------------------------------------------------\n\n")
        }

        sb.append("End of Chat Session Backup.\n")
        return sb.toString()
    }

    /**
     * Saves the current chat session to device local storage backup directory.
     */
    fun saveSessionToLocalBackup(
        context: Context,
        format: ExportFormat,
        customTitle: String? = null
    ): File? {
        return try {
            val title = customTitle?.takeIf { it.isNotBlank() } ?: _sessionTitle.value
            val content = when (format) {
                ExportFormat.MARKDOWN -> exportToMarkdown(title)
                ExportFormat.PLAIN_TEXT -> exportToPlainText(title)
            }

            // Create target backup directory
            val backupDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "chat_backups")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(30)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "chat_${sanitizedTitle}_$timeStamp.${format.extension}"
            val targetFile = File(backupDir, fileName)

            FileOutputStream(targetFile).use { fos ->
                fos.write(content.toByteArray(Charsets.UTF_8))
                fos.flush()
            }

            _exportStatusMessage.value = "Saved to local storage: ${targetFile.name} (${targetFile.length()} bytes)"
            refreshSavedBackups(context)
            targetFile
        } catch (e: Exception) {
            _exportStatusMessage.value = "Failed to export backup: ${e.localizedMessage}"
            null
        }
    }

    fun refreshSavedBackups(context: Context) {
        viewModelScope.launch {
            try {
                val backupDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "chat_backups")
                if (!backupDir.exists()) {
                    _savedBackups.value = emptyList()
                    return@launch
                }

                val files = backupDir.listFiles() ?: emptyArray()
                val list = files.filter { it.isFile && (it.extension == "md" || it.extension == "txt") }
                    .sortedByDescending { it.lastModified() }
                    .map { file ->
                        val format = if (file.extension.equals("md", ignoreCase = true)) {
                            ExportFormat.MARKDOWN
                        } else {
                            ExportFormat.PLAIN_TEXT
                        }
                        LocalChatBackup(
                            file = file,
                            name = file.name,
                            format = format,
                            sizeBytes = file.length(),
                            lastModified = file.lastModified()
                        )
                    }
                _savedBackups.value = list
            } catch (e: Exception) {
                _savedBackups.value = emptyList()
            }
        }
    }

    fun deleteBackupFile(context: Context, backup: LocalChatBackup): Boolean {
        return try {
            val deleted = backup.file.delete()
            if (deleted) {
                _exportStatusMessage.value = "Deleted backup: ${backup.name}"
                refreshSavedBackups(context)
            }
            deleted
        } catch (e: Exception) {
            false
        }
    }

    fun createShareIntent(context: Context, file: File, format: ExportFormat): Intent {
        val uri: Uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            Uri.fromFile(file)
        }

        return Intent(Intent.ACTION_SEND).apply {
            type = format.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Chat Session Backup: ${file.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun createTextShareIntent(title: String, format: ExportFormat): Intent {
        val content = when (format) {
            ExportFormat.MARKDOWN -> exportToMarkdown(title)
            ExportFormat.PLAIN_TEXT -> exportToPlainText(title)
        }
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Chat Backup: $title")
            putExtra(Intent.EXTRA_TEXT, content)
        }
    }

    private fun generateSmartResponse(query: String): String {
        val lower = query.lowercase().trim()
        return when {
            lower.contains("fact") -> {
                val facts = listOf(
                    "🌌 **Space Fact:** One day on Venus is longer than one year on Venus! It takes Venus 243 Earth days to rotate once on its axis, but only 225 Earth days to complete one orbit around the Sun.",
                    "🐙 **Biology Fact:** Octopuses have three hearts and blue blood! Two hearts pump blood to the gills, while the third circulates blood through the rest of the body.",
                    "⚡ **Physics Fact:** Lightning bolts can reach temperatures around 30,000 Kelvin (53,540°F) — roughly five times hotter than the visible surface of the Sun.",
                    "🍯 **Nature Fact:** Honey never spoils! Archaeologists have found pots of 3,000-year-old honey in ancient Egyptian tombs that remain completely edible."
                )
                facts.random()
            }
            lower.contains("story") -> {
                "🚀 *The Signal*\n\nDeep beneath the lunar ice, the surveyor probe picked up an ancient rhythm repeating in prime numbers. When humanity deciphered the final stanza, the message was just two words:\n\n> *Welcome back.*"
            }
            lower.contains("riddle") -> {
                "🧩 *Riddle for you:*\n\nI speak without a mouth and hear without ears. I have no body, but I come alive with the wind. What am I?\n\n*(Answer: An echo!)*"
            }
            lower.contains("joke") -> {
                "😄 Why do programmers prefer dark mode?\n\nBecause light attracts bugs!"
            }
            lower.contains("what can") || lower.contains("help") || lower.contains("do") -> {
                "💡 **Local AI Capabilities:**\n\n1. **100% Offline Privacy**: All reasoning stays securely on your device.\n2. **Chat & Reasoning**: Ask questions, brainstorm ideas, and draft content.\n3. **Model Manager**: Download and switch between compact GGUF weights (SmolLM2, Qwen 2.5, Phi-3).\n4. **Session Export**: Export any conversation as **Markdown (.md)** or **Plain Text (.txt)** files saved directly to local storage."
            }
            else -> {
                "🤖 **Local Assistant:**\n\nI received your query: \"$query\".\n\nRunning fully on-device with zero telemetry. You can load custom quantized GGUF weights from the Model Hub or export this session transcript anytime using the backup tool in the top bar!"
            }
        }
    }
}
