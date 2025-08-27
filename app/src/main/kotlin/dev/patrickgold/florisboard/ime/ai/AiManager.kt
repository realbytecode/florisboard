package dev.patrickgold.florisboard.ime.ai

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.genai.llminference.GraphOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import dev.patrickgold.florisboard.lib.devtools.flogInfo
import kotlinx.serialization.json.Json
import java.io.File

class AiManager(private val context: Context) {
    private var allowlist: AiModelAllowlist? = null
    private var selectedModel: AiModel? = null
    private var llmInference: LlmInference? = null
    private var llmSession: LlmInferenceSession? = null

    fun initialize() {
        flogInfo { "Initializing" }
        loadAllowlist()
        selectedModel = allowlist?.models?.firstOrNull { it.llmSupportImage == true }
        if (selectedModel == null) {
            flogInfo { "No suitable AI model found in allowlist." }
            return
        }
        flogInfo { "Selected AI model: ${selectedModel?.name}" }

        initializeLlmInference()
    }

    private fun initializeLlmInference() {
        flogInfo { "Attempting to initialize LlmInference" }
        val modelName = selectedModel?.modelFile ?: "gemma-3n-E2B-it-int4.task"
        val modelFile = File(context.filesDir, modelName)
        val modelPath = modelFile.absolutePath

        flogInfo { "Checking for model '$modelName' at: $modelPath" }
        if (!modelFile.exists()) {
            flogInfo { "Model file not found at the specified path. Please ensure the model is pushed to the app's internal storage." }
            return
        }
        flogInfo { "Model file found. Proceeding with initialization." }

        try {
            val modelConfig = selectedModel!!.defaultConfig
            flogInfo { "Building LlmInference options with maxTokens: ${modelConfig.maxTokens}" }
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(modelConfig.maxTokens)
                .setMaxNumImages(1)
                .build()
            flogInfo { "LlmInference options built successfully." }

            flogInfo { "Creating LlmInference engine from options..." }
            llmInference = LlmInference.createFromOptions(context, options)
            flogInfo { "LlmInference engine created successfully." }

            flogInfo { "Creating LlmInferenceSession..." }
            llmSession = LlmInferenceSession.createFromOptions(
                llmInference!!,
                LlmInferenceSession.LlmInferenceSessionOptions.builder()
                    .setGraphOptions(
                        GraphOptions.builder()
                            .setEnableVisionModality(true)
                            .build()
                    )
                    .build()
            )
            flogInfo { "LlmInferenceSession created successfully. AI Manager is ready." }
        } catch (e: Exception) {
            flogInfo { "Error initializing LlmInference engine or session: ${e.message}" }
        }
    }

    private fun loadAllowlist() {
        try {
            flogInfo { "Loading AI model allowlist..." }
            val jsonString = context.assets.open("ai_model_allowlist.json").bufferedReader().use { it.readText() }
            val json = Json { ignoreUnknownKeys = true }
            allowlist = json.decodeFromString<AiModelAllowlist>(jsonString)
            flogInfo { "Successfully loaded AI model allowlist." }
        } catch (e: Exception) {
            flogInfo { "Error loading AI model allowlist: ${e.message}" }
        }
    }

    fun getSummary(bitmap: Bitmap, prompt: String, onResult: (String) -> Unit, onError: (String) -> Unit = {}) {
        flogInfo { "getSummary called with prompt: '$prompt' and bitmap size: ${bitmap.width}x${bitmap.height}" }
        if (selectedModel == null) {
            flogInfo { "Cannot get summary: No AI model selected." }
            onError("No AI model selected")
            return
        }
        if (llmInference == null || llmSession == null) {
            flogInfo { "Cannot get summary: LlmInference engine or session not initialized." }
            onError("AI engine not initialized")
            return
        }

        try {
            val fullPrompt = "$prompt <end_of_turn>"
            flogInfo { "Adding query chunk to session: $fullPrompt" }
            llmSession?.addQueryChunk(fullPrompt)
            flogInfo { "Converting Bitmap to MPImage and adding to session." }
            val mpImage = BitmapImageBuilder(bitmap).build()
            llmSession?.addImage(mpImage)
            flogInfo { "Image added. Generating response from LLM..." }

            val fullResponse = StringBuilder()
            llmSession?.generateResponseAsync { partialResult, done ->
                // Partial results can be empty, only log if there's content
                // if (partialResult.isNotBlank()) {
                //    flogInfo { "Partial result: $partialResult" }
                //}
                fullResponse.append(partialResult)
                if (done) {
                    flogInfo { "Successfully received response from LLM." }
                    val response = fullResponse.toString().trim()
                    flogInfo { "Summary from LLM: $response" }
                    onResult(response)
                }
            }
        } catch (e: Exception) {
            flogInfo { "Error during LLM inference: ${e.message}" }
            onError(e.message ?: "Unknown error during inference")
        }
    }
}
