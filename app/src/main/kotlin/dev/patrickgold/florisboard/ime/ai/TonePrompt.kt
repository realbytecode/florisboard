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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data class representing an example for a tone
 */
@Serializable
data class ToneExample(
    val input: String,
    val output: String
)

/**
 * Data class representing a single tone configuration with its prompts
 */
@Serializable
data class TonePrompt(
    val description: String,
    @SerialName("system_prompt")
    val systemPrompt: String,
    @SerialName("instruction_template")
    val instructionTemplate: String,
    val examples: List<ToneExample>
)