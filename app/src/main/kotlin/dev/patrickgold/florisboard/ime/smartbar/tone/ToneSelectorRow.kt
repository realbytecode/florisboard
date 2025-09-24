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

package dev.patrickgold.florisboard.ime.smartbar.tone

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.keyboardManager
import org.florisboard.lib.snygg.ui.SnyggRow

// Available tone contexts
private val TONE_CONTEXTS = listOf("work", "personal", "social")
// Available tone styles
private val TONE_STYLES = listOf("polite", "direct")

@Composable
fun ToneSelectorRow(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()

    // Get current selections from keyboard manager
    val selectedContext = keyboardManager.selectedToneContext
    val selectedStyle = keyboardManager.selectedToneStyle

    SnyggRow(
        elementName = "smartbar-tone-selector",
        modifier = modifier
            .fillMaxWidth()
            .height(FlorisImeSizing.smartbarHeight),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Context selector button (work/personal/social)
        ToneButton(
            value = selectedContext,
            onClick = {
                val currentIndex = TONE_CONTEXTS.indexOf(selectedContext)
                val nextIndex = (currentIndex + 1) % TONE_CONTEXTS.size
                keyboardManager.selectedToneContext = TONE_CONTEXTS[nextIndex]
            },
            elementName = "tone-context-button",
        )

        // Style selector button (polite/direct)
        ToneButton(
            value = selectedStyle,
            onClick = {
                val currentIndex = TONE_STYLES.indexOf(selectedStyle)
                val nextIndex = (currentIndex + 1) % TONE_STYLES.size
                keyboardManager.selectedToneStyle = TONE_STYLES[nextIndex]
            },
            elementName = "tone-style-button",
        )
    }
}