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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.ui.graphics.vector.ImageVector
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.editor.EditorContent
import dev.patrickgold.florisboard.ime.nlp.SuggestionCandidate
import dev.patrickgold.florisboard.ime.nlp.SuggestionProvider
import dev.patrickgold.florisboard.lib.devtools.flogInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * AI suggestion provider that integrates with the on-device AI model to provide
 * context-aware text suggestions based on screen content.
 */
class AiSuggestionProvider : SuggestionProvider {
    override val providerId: String = "org.florisboard.ime.ai.suggestion_provider"
    
    private val _currentSuggestion = MutableStateFlow<AiSuggestionCandidate?>(null)
    val currentSuggestion: StateFlow<AiSuggestionCandidate?> = _currentSuggestion.asStateFlow()
    
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    override suspend fun create() {
        flogInfo { "AiSuggestionProvider created" }
    }

    override suspend fun preload(subtype: Subtype) {
        // No preloading needed for AI suggestions
    }

    override suspend fun suggest(
        subtype: Subtype,
        content: EditorContent,
        maxCandidateCount: Int,
        allowPossiblyOffensive: Boolean,
        isPrivateSession: Boolean
    ): List<SuggestionCandidate> {
        // Return current AI suggestion if available
        val suggestion = _currentSuggestion.value
        return if (suggestion != null && !isPrivateSession) {
            listOf(suggestion)
        } else {
            emptyList()
        }
    }

    override suspend fun notifySuggestionAccepted(subtype: Subtype, candidate: SuggestionCandidate) {
        if (candidate is AiSuggestionCandidate) {
            flogInfo { "AI suggestion accepted: ${candidate.text}" }
            clearSuggestion()
        }
    }

    override suspend fun notifySuggestionReverted(subtype: Subtype, candidate: SuggestionCandidate) {
        if (candidate is AiSuggestionCandidate) {
            flogInfo { "AI suggestion reverted: ${candidate.text}" }
        }
    }

    override suspend fun removeSuggestion(subtype: Subtype, candidate: SuggestionCandidate): Boolean {
        if (candidate is AiSuggestionCandidate) {
            clearSuggestion()
            return true
        }
        return false
    }

    override suspend fun getListOfWords(subtype: Subtype): List<String> {
        return emptyList()
    }

    override suspend fun getFrequencyForWord(subtype: Subtype, word: String): Double {
        return 0.0
    }

    override suspend fun destroy() {
        clearSuggestion()
    }

    override val forcesSuggestionOn: Boolean = true

    /**
     * Sets a new AI suggestion from the model response
     */
    fun setAiSuggestion(text: String, confidence: Double = 0.9) {
        _currentSuggestion.value = AiSuggestionCandidate(
            text = text,
            confidence = confidence,
            sourceProvider = this
        )
        _isProcessing.value = false
        flogInfo { "AI suggestion set: $text" }
    }

    /**
     * Clears the current AI suggestion
     */
    fun clearSuggestion() {
        _currentSuggestion.value = null
        _isProcessing.value = false
    }

    /**
     * Sets the processing state
     */
    fun setProcessing(processing: Boolean) {
        _isProcessing.value = processing
        if (processing) {
            clearSuggestion()
        }
    }
}

/**
 * Specialized suggestion candidate for AI-generated suggestions.
 * Shows with a special icon to indicate it comes from AI.
 */
data class AiSuggestionCandidate(
    override val text: CharSequence,
    override val secondaryText: CharSequence? = "AI Suggestion",
    override val confidence: Double = 0.9,
    override val isEligibleForAutoCommit: Boolean = false,
    override val isEligibleForUserRemoval: Boolean = true,
    override val icon: ImageVector? = Icons.Default.AutoAwesome,
    override val sourceProvider: SuggestionProvider?
) : SuggestionCandidate

/**
 * Singleton instance for global access
 */
object AiSuggestionProviderInstance {
    val provider = AiSuggestionProvider()
}