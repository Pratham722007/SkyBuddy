package com.example.skybuddy.ui.onboarding

import android.content.Context
import com.example.skybuddy.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DownloadProgress {
    data class Active(val bytesRead: Long, val total: Long) : DownloadProgress
    data class Complete(val file: File) : DownloadProgress
    data class Failed(val message: String) : DownloadProgress
}

@Singleton
class ModelDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: OkHttpClient,
    @IoDispatcher private val io: CoroutineDispatcher
) {
    fun targetFile(): File = File(context.filesDir, MODEL_FILE_NAME)

    fun isDownloaded(): Boolean = targetFile().let { it.exists() && it.length() > 0 }

    fun download(url: String, hfToken: String? = null): Flow<DownloadProgress> = flow {
        val request = Request.Builder().url(url).apply {
            if (!hfToken.isNullOrBlank()) header("Authorization", "Bearer $hfToken")
        }.build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            emit(DownloadProgress.Failed("HTTP ${response.code}: ${response.message}"))
            response.close()
            return@flow
        }

        val body = response.body
        if (body == null) {
            emit(DownloadProgress.Failed("Empty response body"))
            response.close()
            return@flow
        }

        val total = body.contentLength()
        val target = targetFile()
        val tempFile = File(context.filesDir, "$MODEL_FILE_NAME.part")

        try {
            body.byteStream().use { input ->
                tempFile.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read = 0L
                    while (true) {
                        val n = input.read(buffer)
                        if (n <= 0) break
                        output.write(buffer, 0, n)
                        read += n
                        emit(DownloadProgress.Active(read, total))
                    }
                }
            }
            if (target.exists()) target.delete()
            if (!tempFile.renameTo(target)) {
                tempFile.copyTo(target, overwrite = true)
                tempFile.delete()
            }
            emit(DownloadProgress.Complete(target))
        } catch (e: Exception) {
            tempFile.delete()
            emit(DownloadProgress.Failed(e.message ?: "Download failed"))
        } finally {
            response.close()
        }
    }.flowOn(io)

    companion object {
        const val MODEL_FILE_NAME = "gemma.litertlm"
        const val DEFAULT_MODEL_URL =
            "https://huggingface.co/google/gemma-3n-E2B-it-litert-lm-preview/resolve/main/gemma-3n-E2B-it-int4.litertlm"
    }
}
