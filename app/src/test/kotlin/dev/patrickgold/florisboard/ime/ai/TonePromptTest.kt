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

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TonePromptTest {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    @Nested
    @DisplayName("ToneExample Serialization")
    inner class ToneExampleSerialization {

        @Test
        fun `should serialize and deserialize ToneExample correctly`() {
            val example = ToneExample(
                input = "This is wrong",
                output = "This may need revision"
            )

            val jsonString = json.encodeToString(example)
            val deserialized = json.decodeFromString<ToneExample>(jsonString)

            assertEquals(example.input, deserialized.input)
            assertEquals(example.output, deserialized.output)
        }

        @Test
        fun `should handle special characters in ToneExample`() {
            val example = ToneExample(
                input = "Hello \"user\" & <tag> \n newline",
                output = "Greetings \"user\" & <tag> \n newline"
            )

            val jsonString = json.encodeToString(example)
            val deserialized = json.decodeFromString<ToneExample>(jsonString)

            assertEquals(example.input, deserialized.input)
            assertEquals(example.output, deserialized.output)
        }

        @Test
        fun `should handle empty strings in ToneExample`() {
            val example = ToneExample(
                input = "",
                output = ""
            )

            val jsonString = json.encodeToString(example)
            val deserialized = json.decodeFromString<ToneExample>(jsonString)

            assertEquals("", deserialized.input)
            assertEquals("", deserialized.output)
        }
    }

    @Nested
    @DisplayName("TonePrompt Serialization")
    inner class TonePromptSerialization {

        @Test
        fun `should serialize and deserialize TonePrompt with examples`() {
            val tonePrompt = TonePrompt(
                description = "Professional and polite",
                systemPrompt = "Transform to professional tone",
                instructionTemplate = "Please rewrite: {input}",
                examples = listOf(
                    ToneExample("Hi", "Hello"),
                    ToneExample("Thanks", "Thank you")
                )
            )

            val jsonString = json.encodeToString(tonePrompt)
            val deserialized = json.decodeFromString<TonePrompt>(jsonString)

            assertEquals(tonePrompt.description, deserialized.description)
            assertEquals(tonePrompt.systemPrompt, deserialized.systemPrompt)
            assertEquals(tonePrompt.instructionTemplate, deserialized.instructionTemplate)
            assertEquals(2, deserialized.examples.size)
            assertEquals("Hi", deserialized.examples[0].input)
            assertEquals("Hello", deserialized.examples[0].output)
        }

        @Test
        fun `should handle empty examples list`() {
            val tonePrompt = TonePrompt(
                description = "Direct communication",
                systemPrompt = "Be direct",
                instructionTemplate = "Rewrite: {input}",
                examples = emptyList()
            )

            val jsonString = json.encodeToString(tonePrompt)
            val deserialized = json.decodeFromString<TonePrompt>(jsonString)

            assertEquals(tonePrompt.description, deserialized.description)
            assertTrue(deserialized.examples.isEmpty())
        }

        @Test
        fun `should serialize with correct JSON field names`() {
            val tonePrompt = TonePrompt(
                description = "Test",
                systemPrompt = "System",
                instructionTemplate = "Template",
                examples = emptyList()
            )

            val jsonString = json.encodeToString(tonePrompt)

            assertTrue(jsonString.contains("\"system_prompt\""))
            assertTrue(jsonString.contains("\"instruction_template\""))
            assertFalse(jsonString.contains("\"systemPrompt\""))
            assertFalse(jsonString.contains("\"instructionTemplate\""))
        }

        @Test
        fun `should deserialize from JSON with snake_case fields`() {
            val jsonString = """
            {
                "description": "Professional tone",
                "system_prompt": "Be professional",
                "instruction_template": "Rewrite professionally: {input}",
                "examples": [
                    {
                        "input": "hey",
                        "output": "Hello"
                    }
                ]
            }
            """.trimIndent()

            val tonePrompt = json.decodeFromString<TonePrompt>(jsonString)

            assertEquals("Professional tone", tonePrompt.description)
            assertEquals("Be professional", tonePrompt.systemPrompt)
            assertEquals("Rewrite professionally: {input}", tonePrompt.instructionTemplate)
            assertEquals(1, tonePrompt.examples.size)
        }
    }

    @Nested
    @DisplayName("TonePrompt Map Serialization")
    inner class TonePromptMapSerialization {

        @Test
        fun `should deserialize map of tone prompts`() {
            val jsonString = """
            {
                "work-polite": {
                    "description": "Professional",
                    "system_prompt": "Be professional",
                    "instruction_template": "Rewrite: {input}",
                    "examples": []
                },
                "personal-direct": {
                    "description": "Casual",
                    "system_prompt": "Be casual",
                    "instruction_template": "Casual: {input}",
                    "examples": [
                        {
                            "input": "Hello",
                            "output": "Hey"
                        }
                    ]
                }
            }
            """.trimIndent()

            val toneMap = json.decodeFromString<Map<String, TonePrompt>>(jsonString)

            assertEquals(2, toneMap.size)
            assertTrue(toneMap.containsKey("work-polite"))
            assertTrue(toneMap.containsKey("personal-direct"))

            val workPolite = toneMap["work-polite"]!!
            assertEquals("Professional", workPolite.description)
            assertEquals("Be professional", workPolite.systemPrompt)
            assertEquals(0, workPolite.examples.size)

            val personalDirect = toneMap["personal-direct"]!!
            assertEquals("Casual", personalDirect.description)
            assertEquals(1, personalDirect.examples.size)
        }

        @Test
        fun `should handle empty tone map`() {
            val jsonString = "{}"
            val toneMap = json.decodeFromString<Map<String, TonePrompt>>(jsonString)

            assertNotNull(toneMap)
            assertTrue(toneMap.isEmpty())
        }

        @Test
        fun `should ignore unknown fields in JSON`() {
            val jsonString = """
            {
                "test-tone": {
                    "description": "Test",
                    "system_prompt": "Test prompt",
                    "instruction_template": "Test: {input}",
                    "examples": [],
                    "unknown_field": "should be ignored",
                    "another_unknown": 123
                }
            }
            """.trimIndent()

            val toneMap = json.decodeFromString<Map<String, TonePrompt>>(jsonString)

            assertEquals(1, toneMap.size)
            val testTone = toneMap["test-tone"]!!
            assertEquals("Test", testTone.description)
        }
    }

    @Nested
    @DisplayName("Instruction Template Processing")
    inner class InstructionTemplateProcessing {

        @Test
        fun `should validate template placeholder format`() {
            val validTemplates = listOf(
                "Rewrite: {input}",
                "Transform to professional: {input}",
                "{input} - rewritten",
                "Start {input} End"
            )

            validTemplates.forEach { template ->
                assertTrue(template.contains("{input}"), "Template should contain {input}: $template")
            }
        }

        @Test
        fun `should handle multiple placeholders in template`() {
            val template = "Context: {context} Input: {input} Style: {style}"
            val userInput = "Hello world"

            val processed = template.replace("{input}", userInput)

            assertTrue(processed.contains(userInput))
            assertTrue(processed.contains("{context}"))
            assertTrue(processed.contains("{style}"))
        }

        @Test
        fun `should handle special characters in replacement`() {
            val template = "Rewrite: {input}"
            val specialInputs = listOf(
                "Hello $100",
                "Test & verify",
                "Quote: \"text\"",
                "Path: \\directory\\file",
                "Regex: .*test.*"
            )

            specialInputs.forEach { input ->
                val result = template.replace("{input}", input)
                assertEquals("Rewrite: $input", result)
            }
        }
    }

    @Nested
    @DisplayName("Data Class Properties")
    inner class DataClassProperties {

        @Test
        fun `ToneExample should have correct equals and hashCode`() {
            val example1 = ToneExample("input", "output")
            val example2 = ToneExample("input", "output")
            val example3 = ToneExample("different", "output")

            assertEquals(example1, example2)
            assertEquals(example1.hashCode(), example2.hashCode())
            assertNotEquals(example1, example3)
        }

        @Test
        fun `TonePrompt should have correct equals and hashCode`() {
            val examples = listOf(ToneExample("in", "out"))
            val prompt1 = TonePrompt("desc", "system", "template", examples)
            val prompt2 = TonePrompt("desc", "system", "template", examples)
            val prompt3 = TonePrompt("diff", "system", "template", examples)

            assertEquals(prompt1, prompt2)
            assertEquals(prompt1.hashCode(), prompt2.hashCode())
            assertNotEquals(prompt1, prompt3)
        }

        @Test
        fun `should create copy with modified properties`() {
            val original = TonePrompt(
                description = "Original",
                systemPrompt = "System",
                instructionTemplate = "Template",
                examples = emptyList()
            )

            val modified = original.copy(description = "Modified")

            assertEquals("Modified", modified.description)
            assertEquals(original.systemPrompt, modified.systemPrompt)
            assertEquals(original.instructionTemplate, modified.instructionTemplate)
            assertEquals(original.examples, modified.examples)
        }

        @Test
        fun `should have meaningful toString representations`() {
            val example = ToneExample("in", "out")
            val prompt = TonePrompt("desc", "sys", "temp", listOf(example))

            assertTrue(example.toString().contains("in"))
            assertTrue(example.toString().contains("out"))
            assertTrue(prompt.toString().contains("desc"))
        }
    }
}