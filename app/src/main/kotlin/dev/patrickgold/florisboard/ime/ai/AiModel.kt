
package dev.patrickgold.florisboard.ime.ai

import kotlinx.serialization.Serializable

@Serializable
data class AiModel(
    val name: String,
    val modelId: String,
    val modelFile: String,
    val description: String,
    val sizeInBytes: Long,
    val estimatedPeakMemoryInBytes: Long? = null,
    val version: String,
    val llmSupportImage: Boolean? = null,
    val defaultConfig: AiModelDefaultConfig,
    val taskTypes: List<String>
)

@Serializable
data class AiModelDefaultConfig(
    val topK: Int,
    val topP: Float,
    val temperature: Float,
    val maxTokens: Int,
    val accelerators: String
)

@Serializable
data class AiModelAllowlist(
    val models: List<AiModel>
)
