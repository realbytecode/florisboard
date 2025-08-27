/*
 * Copyright (C) 2025 The FlorisBoard Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.patrickgold.florisboard.ime.ai

import android.content.Context
import dev.patrickgold.florisboard.lib.devtools.flogError
import dev.patrickgold.florisboard.lib.devtools.flogInfo
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException

/**
 * Data class representing a single AI prompt configuration
 */
@Serializable
data class PromptConfig(
    val id: String,
    val name: String,
    val description: String,
    val file: String,
    val icon: String,
    val default: Boolean = false
)

/**
 * Data class for the prompts configuration file
 */
@Serializable
data class PromptsConfiguration(
    val prompts: List<PromptConfig>
)

/**
 * Manager class for handling AI prompts
 * Loads prompts from assets and provides them to the AI system
 */
class PromptsManager(private val context: Context) {
    
    private val promptsCache = mutableMapOf<String, String>()
    private var configuration: PromptsConfiguration? = null
    private val json = Json { ignoreUnknownKeys = true }
    
    companion object {
        private const val PROMPTS_DIR = "ai/prompts"
        private const val CONFIG_FILE = "$PROMPTS_DIR/prompts_config.json"
    }
    
    init {
        // Loading a small JSON file is fast, no need to defer
        loadConfiguration()
    }
    
    /**
     * Loads the prompts configuration from assets
     */
    private fun loadConfiguration() {
        try {
            val configJson = context.assets.open(CONFIG_FILE).bufferedReader().use { it.readText() }
            configuration = json.decodeFromString(PromptsConfiguration.serializer(), configJson)
            flogInfo { "Loaded ${configuration?.prompts?.size ?: 0} prompt configurations" }
        } catch (e: Exception) {
            flogError { "Failed to load prompts configuration: ${e.message}" }
            // Fallback configuration
            configuration = PromptsConfiguration(
                prompts = listOf(
                    PromptConfig(
                        id = "response_suggestion",
                        name = "Smart Response",
                        description = "Suggests contextual responses",
                        file = "response_suggestion.txt",
                        icon = "reply",
                        default = true
                    )
                )
            )
        }
    }
    
    /**
     * Gets a prompt by its ID
     * @param promptId The ID of the prompt to retrieve
     * @return The prompt text, or null if not found
     */
    fun getPrompt(promptId: String): String? {
        // Check cache first
        promptsCache[promptId]?.let { return it }
        
        // Find the prompt configuration
        val promptConfig = configuration?.prompts?.find { it.id == promptId }
        if (promptConfig == null) {
            flogError { "Prompt with ID '$promptId' not found in configuration" }
            return null
        }
        
        // Load the prompt from file
        return loadPromptFromFile(promptConfig)
    }
    
    /**
     * Gets the default prompt
     * @return The default prompt text, or a fallback if none is configured
     */
    fun getDefaultPrompt(): String {
        val defaultConfig = configuration?.prompts?.find { it.default }
            ?: configuration?.prompts?.firstOrNull()
        
        if (defaultConfig != null) {
            return loadPromptFromFile(defaultConfig) ?: getFallbackPrompt()
        }
        
        return getFallbackPrompt()
    }
    
    /**
     * Loads a prompt from its file
     * @param config The prompt configuration
     * @return The prompt text, or null if loading fails
     */
    private fun loadPromptFromFile(config: PromptConfig): String? {
        try {
            val promptPath = "$PROMPTS_DIR/${config.file}"
            val promptText = context.assets.open(promptPath).bufferedReader().use { it.readText() }
            
            // Cache the loaded prompt
            promptsCache[config.id] = promptText
            flogInfo { "Loaded prompt '${config.id}' from ${config.file}" }
            
            return promptText
        } catch (e: IOException) {
            flogError { "Failed to load prompt file '${config.file}': ${e.message}" }
            return null
        }
    }
    
    /**
     * Returns a fallback prompt in case of loading failures
     */
    private fun getFallbackPrompt(): String {
        return "Analyze the screenshot and provide a helpful response based on what you see."
    }
    
    /**
     * Gets all available prompt configurations
     * @return List of prompt configurations
     */
    fun getAvailablePrompts(): List<PromptConfig> {
        return configuration?.prompts ?: emptyList()
    }
    
    /**
     * Clears the prompt cache
     */
    fun clearCache() {
        promptsCache.clear()
        flogInfo { "Prompt cache cleared" }
    }
}