package com.example.skybuddy.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.skybuddy.ai.tools.SkyBuddyToolSet
import com.example.skybuddy.di.IoDispatcher
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.tool
import com.google.ai.edge.litertlm.Backend as LiteRtBackend
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiteRtLlmEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val acceleration: AccelerationCompat,
    private val toolSet: SkyBuddyToolSet,
    @IoDispatcher private val io: CoroutineDispatcher
) : LlmEngine {

    private val ready = AtomicBoolean(false)
    private var engine: Engine? = null
    private var conversation: Conversation? = null

    override val isReady: Boolean get() = ready.get()

    fun expectedModelPath(): String = File(context.filesDir, MODEL_FILE).absolutePath

    override fun initialize(preferred: Backend?): Flow<InitState> = flow {
        // CPU for text generation (GPU delegate hangs on a kgsl fence on some device families),
        // but Vision MUST use GPU on devices that don't support advanced CPU vector instructions.
        emit(InitState.Loading(Backend.CPU, InitStage.ProbingDevice))
        val attempt = tryInitialize(Backend.CPU) { stage ->
            emit(InitState.Loading(Backend.CPU, stage))
        }
        if (attempt == null) emit(InitState.Ready(Backend.CPU))
        else emit(InitState.Failed(Backend.CPU, attempt.message ?: "CPU init failed", attempt))
    }.flowOn(io)

    private suspend inline fun tryInitialize(
        backend: Backend,
        crossinline progress: suspend (InitStage) -> Unit
    ): Throwable? = try {
        val modelPath = expectedModelPath()
        val modelFile = File(modelPath)
        if (!modelFile.exists()) {
            throw IllegalStateException("Model file not found at $modelPath")
        }

        progress(InitStage.OpeningModel)

        val textBackend = LiteRtBackend.CPU()
        val visionBackend = if (acceleration.isGpuAvailable()) LiteRtBackend.GPU() else LiteRtBackend.CPU()
        val config = EngineConfig(
            modelPath = modelPath,
            backend = textBackend,
            visionBackend = visionBackend,
            cacheDir = context.cacheDir.absolutePath
        )
        val newEngine = Engine(config)

        progress(InitStage.AllocatingTensors)
        newEngine.initialize()
        engine = newEngine

        progress(InitStage.Warmup)
        val conversationConfig = ConversationConfig(
            systemInstruction = Contents.of(SYSTEM_PROMPT),
            tools = listOf(tool(toolSet))
        )
        conversation = newEngine.createConversation(conversationConfig)
        ready.set(true)
        null
    } catch (t: Throwable) {
        ready.set(false)
        runCatching { engine?.close() }
        engine = null
        conversation = null
        t
    }

    override suspend fun generateText(prompt: String): String = withContext(io) {
        val conv = conversation ?: return@withContext "Error: model not loaded."
        val builder = StringBuilder()
        try {
            conv.sendMessageAsync(prompt).collect { chunk -> builder.append(chunk) }
            builder.toString()
        } catch (t: Throwable) {
            "Error generating response: ${t.message}"
        }
    }

    override suspend fun generateOneOffText(prompt: String): String = withContext(io) {
        var tempEngine: Engine? = null
        var tempConv: Conversation? = null
        val builder = StringBuilder()
        try {
            val config = EngineConfig(
                modelPath = expectedModelPath(),
                backend = LiteRtBackend.CPU(),
                visionBackend = LiteRtBackend.CPU(),
                cacheDir = context.cacheDir.absolutePath
            )
            tempEngine = Engine(config)
            tempEngine.initialize()

            val conversationConfig = ConversationConfig(
                systemInstruction = Contents.of(SYSTEM_PROMPT),
                tools = emptyList() // No tools needed for one-off beacon toast
            )
            tempConv = tempEngine.createConversation(conversationConfig)
            tempConv.sendMessageAsync(prompt).collect { chunk -> builder.append(chunk) }
            builder.toString()
        } catch (t: Throwable) {
            "Error generating response: ${t.message}"
        } finally {
            runCatching { tempConv?.close() }
            runCatching { tempEngine?.close() }
        }
    }

    override suspend fun generateMultimodal(prompt: String, bitmap: Bitmap): String = withContext(io) {
        val conv = conversation ?: return@withContext "Error: model not loaded."
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        val contents = Contents.of(Content.ImageBytes(stream.toByteArray()), Content.Text(prompt))
        val builder = StringBuilder()
        try {
            conv.sendMessageAsync(contents).collect { chunk -> builder.append(chunk) }
            builder.toString()
        } catch (t: Throwable) {
            "Error processing multimodal request: ${t.message}"
        }
    }

    override fun close() {
        ready.set(false)
        runCatching { conversation?.close() }
        runCatching { engine?.close() }
        conversation = null
        engine = null
    }

    companion object {
        private const val TAG = "LiteRtLlmEngine"
        const val MODEL_FILE = "gemma.litertlm"
        private const val SYSTEM_PROMPT =
            "You are SkyBuddy, a helpful, offline airport companion app. Use tools to look up flights, " +
                "save bag descriptions, or save expense receipts when the user provides related images or text. " +
                "Always respond concisely and naturally."
    }
}
