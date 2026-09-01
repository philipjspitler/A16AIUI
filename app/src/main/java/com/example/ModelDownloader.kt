package com.example

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Float) : DownloadState()
    data class Success(val filePath: String) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

class ModelDownloader(private val context: Context) {
    private val client = OkHttpClient()

    fun downloadModel(url: String, fileName: String): Flow<DownloadState> = flow {
        emit(DownloadState.Downloading(0f))
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            val body = response.body ?: throw IOException("Empty body")
            val contentLength = body.contentLength()
            
            // Create a local models directory in internal storage
            val modelsDir = File(context.filesDir, "models").apply { mkdirs() }
            val destFile = File(modelsDir, fileName)

            body.byteStream().use { input ->
                destFile.outputStream().use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesCopied: Long = 0
                    var bytes = input.read(buffer)
                    while (bytes >= 0) {
                        output.write(buffer, 0, bytes)
                        bytesCopied += bytes
                        val progress = if (contentLength > 0) {
                            (bytesCopied.toFloat() / contentLength.toFloat()) * 100f
                        } else {
                            -1f // Indeterminate
                        }
                        emit(DownloadState.Downloading(progress))
                        bytes = input.read(buffer)
                    }
                }
            }
            emit(DownloadState.Success(destFile.absolutePath))
        } catch (e: Exception) {
            emit(DownloadState.Error(e.message ?: "Unknown error occurred"))
        }
    }.flowOn(Dispatchers.IO)
    
    fun getDownloadedModels(): List<File> {
        val modelsDir = File(context.filesDir, "models")
        return modelsDir.listFiles()?.filter { !it.name.contains(".part_") } ?: emptyList()
    }
    
    fun deleteModel(fileName: String): Boolean {
        val file = File(File(context.filesDir, "models"), fileName)
        return if (file.exists()) file.delete() else false
    }
}
