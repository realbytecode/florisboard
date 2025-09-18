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
    // PromptsManager is lightweight - just holds configuration
    private val promptsManager = PromptsManager(context)
    
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
            // Don't throw exception - just return gracefully
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

    /**
     * Generates a contextual AI response for the given screenshot
     * @param bitmap The screenshot to analyze
     * @param promptId The ID of the prompt to use (from prompts_config.json)
     * @param onResult Callback for successful response
     * @param onError Callback for errors
     */
    fun generateResponse(bitmap: Bitmap, promptId: String, onResult: (String) -> Unit, onError: (String) -> Unit = {}) {
        val prompt = promptsManager.getPrompt(promptId) ?: promptsManager.getDefaultPrompt()
        processScreenshotWithPrompt(bitmap, prompt, onResult, onError)
    }
    
    /**
     * Processes a screenshot with a custom prompt to generate AI response
     * @param bitmap The screenshot to analyze
     * @param prompt The custom prompt text
     * @param onResult Callback for successful response
     * @param onError Callback for errors
     */
    private fun processScreenshotWithPrompt(bitmap: Bitmap, prompt: String, onResult: (String) -> Unit, onError: (String) -> Unit = {}) {
        flogInfo { "Processing screenshot with prompt: '${prompt.take(100)}...' and bitmap size: ${bitmap.width}x${bitmap.height}" }
        
        if (selectedModel == null) {
            flogInfo { "Cannot generate response: No AI model selected." }
            onError("No AI model selected")
            return
        }
        
        if (llmInference == null || llmSession == null) {
            flogInfo { "Cannot generate response: LlmInference engine or session not initialized." }
            onError("AI model not loaded. Push model file to app storage.")
            return
        }

        try {
            val fullPrompt = "$prompt <end_of_turn>"
            flogInfo { "Preparing prompt for AI processing" }
            llmSession?.addQueryChunk(fullPrompt)
            flogInfo { "Converting screenshot to MPImage format" }
            val mpImage = BitmapImageBuilder(bitmap).build()
            llmSession?.addImage(mpImage)
            flogInfo { "Generating contextual response..." }

            val fullResponse = StringBuilder()
            llmSession?.generateResponseAsync { partialResult, done ->
                // Partial results can be empty, only log if there's content
                // if (partialResult.isNotBlank()) {
                //    flogInfo { "Partial result: $partialResult" }
                //}
                fullResponse.append(partialResult)
                if (done) {
                    flogInfo { "AI response generation completed" }
                    // Clean up the response by removing any special tokens
                    val response = fullResponse.toString()
                        .trim()
                        .replace("<end_of_turn>", "")
                        .replace("<start_of_turn>", "")
                        .trim()
                    flogInfo { "Generated response: $response" }
                    onResult(response)
                }
            }
        } catch (e: Exception) {
            flogInfo { "Error during LLM inference: ${e.message}" }
            onError(e.message ?: "Unknown error during inference")
        }
    }

    /**
     * Generates a text rewrite using the AI model
     * @param text The text to rewrite
     * @param onResult Callback for successful response
     * @param onError Callback for errors
     */
    fun generateTextResponse(
        text: String,
        onResult: (String) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        flogInfo { "Processing text for rewrite: '${text.take(50)}...'" }

        if (llmInference == null || llmSession == null) {
            onError("Model not loaded")
            return
        }

        try {
            // Simple rewrite prompt
            val prompt = "Rewrite this: $text <end_of_turn>"
            llmSession?.addQueryChunk(prompt)

            val fullResponse = StringBuilder()
            llmSession?.generateResponseAsync { partialResult, done ->
                fullResponse.append(partialResult)
                if (done) {
                    val response = fullResponse.toString()
                        .replace("<end_of_turn>", "")
                        .replace("<start_of_turn>", "")
                        .trim()

                    if (response.isNotBlank()) {
                        flogInfo { "Generated rewrite: $response" }
                        onResult(response)
                    } else {
                        onError("Empty response")
                    }
                }
            }
        } catch (e: Exception) {
            flogInfo { "Error during text rewrite: ${e.message}" }
            onError(e.message ?: "Processing error")
        }
    }
}
