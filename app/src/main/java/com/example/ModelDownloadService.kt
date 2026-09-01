package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class ModelDownloadService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isForeground = false

    companion object {
        const val CHANNEL_ID = "model_downloads_channel"
        const val NOTIFICATION_ID = 4040
        const val ACTION_START = "com.example.action.START_DOWNLOADS"
        const val ACTION_CANCEL_ALL = "com.example.action.CANCEL_ALL"

        fun startService(context: Context) {
            val intent = Intent(context, ModelDownloadService::class.java).apply {
                action = ACTION_START
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                // Background start restriction fallback
                e.printStackTrace()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startInitialForeground()
        observeQueue()
    }

    private fun startInitialForeground() {
        val initialNotification = buildWaitingNotification(null, 0)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    initialNotification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, initialNotification)
            }
            isForeground = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Model Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress and background status for AI model downloads"
                setShowBadge(false)
                setSound(null, null)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun observeQueue() {
        serviceScope.launch {
            ModelDownloadQueueManager.getInstance(applicationContext).queueState.collect { queue ->
                val activeTask = queue.firstOrNull { it.status == DownloadTaskStatus.DOWNLOADING }
                val queuedCount = queue.count { it.status == DownloadTaskStatus.QUEUED }

                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                if (activeTask != null) {
                    val notification = buildActiveNotification(activeTask, queuedCount)
                    notificationManager.notify(NOTIFICATION_ID, notification)
                } else if (queuedCount > 0) {
                    val pendingTask = queue.firstOrNull { it.status == DownloadTaskStatus.QUEUED }
                    val notification = buildWaitingNotification(pendingTask, queuedCount)
                    notificationManager.notify(NOTIFICATION_ID, notification)
                } else {
                    // Queue finished or empty
                    val completedCount = queue.count { it.status == DownloadTaskStatus.COMPLETED }
                    val failedCount = queue.count { it.status == DownloadTaskStatus.FAILED }

                    if (completedCount > 0 || failedCount > 0) {
                        val completionNotification = buildCompletionNotification(completedCount, failedCount)
                        notificationManager.notify(NOTIFICATION_ID + 1, completionNotification)
                    }

                    if (isForeground) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            stopForeground(STOP_FOREGROUND_REMOVE)
                        } else {
                            @Suppress("DEPRECATION")
                            stopForeground(true)
                        }
                        isForeground = false
                    }
                    stopSelf()
                }
            }
        }
    }

    private fun buildActiveNotification(task: DownloadTask, queuedCount: Int): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val progressInt = task.progress.toInt().coerceIn(0, 100)
        val isIndeterminate = task.progress < 0f || task.totalBytes <= 0L
        val queueText = if (queuedCount > 0) " (+$queuedCount queued)" else ""

        val downloadedStr = formatByteSize(task.downloadedBytes)
        val totalStr = if (task.totalBytes > 0) formatByteSize(task.totalBytes) else "..."
        val speedStr = if (task.speedBytesPerSec > 0) " • ${formatByteSize(task.speedBytesPerSec)}/s" else ""

        val content = if (isIndeterminate) {
            "Downloading ${task.fileName}$queueText"
        } else {
            "$downloadedStr / $totalStr ($progressInt%)$speedStr$queueText"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Downloading: ${task.fileName}")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(100, if (isIndeterminate) 0 else progressInt, isIndeterminate)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }

    private fun buildWaitingNotification(task: DownloadTask?, queuedCount: Int): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = if (task != null) {
            "Starting ${task.fileName} ($queuedCount in queue)..."
        } else {
            "Queue initializing..."
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AI Model Download Queue")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun buildCompletionNotification(completedCount: Int, failedCount: Int): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (failedCount == 0) "Model Downloads Completed" else "Model Downloads Finished with Warnings"
        val desc = when {
            failedCount == 0 -> "$completedCount model(s) installed and ready for local inference."
            completedCount == 0 -> "$failedCount model download(s) failed."
            else -> "$completedCount installed, $failedCount failed."
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(desc)
            .setSmallIcon(if (failedCount == 0) android.R.drawable.stat_sys_download_done else android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun formatByteSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        return String.format(java.util.Locale.US, "%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL_ALL) {
            ModelDownloadQueueManager.getInstance(applicationContext).cancelAll()
            if (isForeground) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                isForeground = false
            }
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
