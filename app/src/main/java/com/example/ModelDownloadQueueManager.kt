package com.example

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class ModelDownloadQueueManager private constructor(private val appContext: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val _queueState = MutableStateFlow<List<DownloadTask>>(emptyList())
    val queueState: StateFlow<List<DownloadTask>> = _queueState.asStateFlow()

    private val queueMutex = Mutex()
    private var workerJob: Job? = null
    private var currentCall: Call? = null
    private var currentActiveTaskId: String? = null

    companion object {
        @Volatile
        private var INSTANCE: ModelDownloadQueueManager? = null

        fun getInstance(context: Context): ModelDownloadQueueManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ModelDownloadQueueManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun enqueue(url: String, fileName: String): DownloadTask {
        val task = DownloadTask(url = url.trim(), fileName = fileName.trim())
        scope.launch {
            queueMutex.withLock {
                _queueState.value = _queueState.value + task
            }
            startServiceIfRequired()
            ensureWorkerRunning()
        }
        return task
    }

    fun enqueueMultiple(items: List<Pair<String, String>>): List<DownloadTask> {
        val newTasks = items.map { (url, name) ->
            DownloadTask(url = url.trim(), fileName = name.trim())
        }
        if (newTasks.isEmpty()) return emptyList()

        scope.launch {
            queueMutex.withLock {
                _queueState.value = _queueState.value + newTasks
            }
            startServiceIfRequired()
            ensureWorkerRunning()
        }
        return newTasks
    }

    fun cancelTask(taskId: String) {
        scope.launch {
            queueMutex.withLock {
                if (currentActiveTaskId == taskId) {
                    currentCall?.cancel()
                }
                _queueState.value = _queueState.value.map { task ->
                    if (task.id == taskId) {
                        task.copy(status = DownloadTaskStatus.CANCELLED)
                    } else task
                }
            }
        }
    }

    fun retryTask(taskId: String) {
        scope.launch {
            queueMutex.withLock {
                _queueState.value = _queueState.value.map { task ->
                    if (task.id == taskId) {
                        task.copy(
                            status = DownloadTaskStatus.QUEUED,
                            progress = 0f,
                            downloadedBytes = 0L,
                            speedBytesPerSec = 0L,
                            errorMessage = null
                        )
                    } else task
                }
            }
            startServiceIfRequired()
            ensureWorkerRunning()
        }
    }

    fun removeTask(taskId: String) {
        scope.launch {
            queueMutex.withLock {
                if (currentActiveTaskId == taskId) {
                    currentCall?.cancel()
                }
                _queueState.value = _queueState.value.filterNot { it.id == taskId }
            }
        }
    }

    fun clearCompleted() {
        scope.launch {
            queueMutex.withLock {
                _queueState.value = _queueState.value.filter {
                    it.status == DownloadTaskStatus.QUEUED || it.status == DownloadTaskStatus.DOWNLOADING
                }
            }
        }
    }

    fun cancelAll() {
        scope.launch {
            queueMutex.withLock {
                currentCall?.cancel()
                _queueState.value = _queueState.value.map {
                    if (it.status == DownloadTaskStatus.QUEUED || it.status == DownloadTaskStatus.DOWNLOADING) {
                        it.copy(status = DownloadTaskStatus.CANCELLED)
                    } else it
                }
            }
        }
    }

    private fun startServiceIfRequired() {
        try {
            ModelDownloadService.startService(appContext)
        } catch (_: Exception) {
            // Service startup best effort
        }
    }

    private fun ensureWorkerRunning() {
        if (workerJob?.isActive == true) return
        workerJob = scope.launch {
            processQueue()
        }
    }

    private suspend fun processQueue() {
        while (currentCoroutineContext().isActive && scope.isActive) {
            val nextTask = queueMutex.withLock {
                _queueState.value.firstOrNull { it.status == DownloadTaskStatus.QUEUED }
            } ?: break

            currentActiveTaskId = nextTask.id
            updateTask(nextTask.id) { it.copy(status = DownloadTaskStatus.DOWNLOADING, progress = 0f) }

            executeDownload(nextTask)

            currentActiveTaskId = null
            currentCall = null
        }
    }

    private suspend fun executeDownload(task: DownloadTask) {
        val modelsDir = File(appContext.filesDir, "models").apply { mkdirs() }
        val destFile = File(modelsDir, task.fileName)
        val tempFile = File(modelsDir, "${task.fileName}.part_${task.id.take(6)}")

        try {
            val request = Request.Builder()
                .url(task.url)
                .build()

            val call = client.newCall(request)
            currentCall = call

            val response = withContext(Dispatchers.IO) {
                call.execute()
            }

            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}: ${response.message}")
            }

            val body = response.body ?: throw IOException("Empty response body from server")
            val contentLength = body.contentLength()

            updateTask(task.id) {
                it.copy(totalBytes = contentLength)
            }

            var downloadedBytes = 0L
            var lastSpeedCheckTime = System.currentTimeMillis()
            var bytesSinceLastCheck = 0L
            var currentSpeed = 0L

            body.byteStream().use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    val buffer = ByteArray(32 * 1024)
                    var read: Int

                    while (inputStream.read(buffer).also { read = it } != -1) {
                        if (!currentCoroutineContext().isActive || currentActiveTaskId != task.id) {
                            throw CancellationException("Download cancelled by user")
                        }

                        outputStream.write(buffer, 0, read)
                        downloadedBytes += read
                        bytesSinceLastCheck += read

                        val now = System.currentTimeMillis()
                        val delta = now - lastSpeedCheckTime
                        if (delta >= 600) {
                            currentSpeed = (bytesSinceLastCheck * 1000L) / delta
                            bytesSinceLastCheck = 0L
                            lastSpeedCheckTime = now

                            val progress = if (contentLength > 0) {
                                ((downloadedBytes.toDouble() / contentLength.toDouble()) * 100.0).toFloat().coerceIn(0f, 100f)
                            } else {
                                -1f
                            }

                            updateTask(task.id) {
                                it.copy(
                                    progress = progress,
                                    downloadedBytes = downloadedBytes,
                                    totalBytes = contentLength,
                                    speedBytesPerSec = currentSpeed
                                )
                            }
                        }
                    }
                    outputStream.flush()
                }
            }

            // Successfully finished stream -> Rename temp file to final destination
            if (destFile.exists()) {
                destFile.delete()
            }
            if (!tempFile.renameTo(destFile)) {
                // If rename fails, copy and delete
                tempFile.copyTo(destFile, overwrite = true)
                tempFile.delete()
            }

            updateTask(task.id) {
                it.copy(
                    status = DownloadTaskStatus.COMPLETED,
                    progress = 100f,
                    downloadedBytes = destFile.length(),
                    totalBytes = destFile.length(),
                    speedBytesPerSec = 0L,
                    errorMessage = null
                )
            }

        } catch (ce: CancellationException) {
            tempFile.delete()
            updateTask(task.id) {
                it.copy(
                    status = DownloadTaskStatus.CANCELLED,
                    speedBytesPerSec = 0L,
                    errorMessage = "Cancelled"
                )
            }
        } catch (e: Exception) {
            tempFile.delete()
            val isCancelled = currentActiveTaskId != task.id || e.message?.contains("Socket closed", ignoreCase = true) == true
            updateTask(task.id) {
                it.copy(
                    status = if (isCancelled) DownloadTaskStatus.CANCELLED else DownloadTaskStatus.FAILED,
                    speedBytesPerSec = 0L,
                    errorMessage = if (isCancelled) "Cancelled" else (e.localizedMessage ?: "Download error occurred")
                )
            }
        }
    }

    private suspend fun updateTask(taskId: String, transform: (DownloadTask) -> DownloadTask) {
        queueMutex.withLock {
            _queueState.value = _queueState.value.map {
                if (it.id == taskId) transform(it) else it
            }
        }
    }
}
