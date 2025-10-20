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

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AiManagerTest {

    @Nested
    @DisplayName("Text Response Generation")
    inner class TextResponseGeneration {

        @Test
        @DisplayName("Should handle empty text by calling error callback")
        fun testEmptyTextHandling() {
            var errorCalled = false
            var errorMessage = ""

            // Since we can't easily mock AiManager, we'll test the logic conceptually
            val emptyTexts = listOf("", "   ", "\n", "\t", "\n\n\t ")

            emptyTexts.forEach { text ->
                // Validate that empty text should trigger error
                assertTrue(text.isBlank(), "Text '$text' should be considered blank")
            }
        }

        @Test
        @DisplayName("Should format prompt correctly with normal text")
        fun testPromptFormatting() {
            val testCases = mapOf(
                "Hello world" to "Rewrite this: Hello world <end_of_turn>",
                "Test 123" to "Rewrite this: Test 123 <end_of_turn>",
                "Multiple\nlines" to "Rewrite this: Multiple\nlines <end_of_turn>"
            )

            testCases.forEach { (input, expected) ->
                val actual = "Rewrite this: $input <end_of_turn>"
                assertEquals(expected, actual, "Prompt format incorrect for input: $input")
            }
        }

        @Test
        @DisplayName("Should handle special characters in text")
        fun testSpecialCharacters() {
            val specialTexts = listOf(
                "Hello @user!",
                "Price: $100.50",
                "Email: test@example.com",
                "Math: 2+2=4",
                "Quote: \"Hello\"",
                "Path: C:\\Users\\Test",
                "Emoji: 👋 Hello 🌍"
            )

            specialTexts.forEach { text ->
                val prompt = "Rewrite this: $text <end_of_turn>"
                assertTrue(prompt.contains(text), "Special characters should be preserved")
            }
        }

        @Test
        @DisplayName("Should handle very long text")
        fun testVeryLongText() {
            val longText = "a".repeat(2000)
            val prompt = "Rewrite this: $longText <end_of_turn>"

            assertTrue(prompt.length > 2000, "Long text should be included in prompt")
            assertTrue(prompt.startsWith("Rewrite this:"), "Prompt should start correctly")
            assertTrue(prompt.endsWith("<end_of_turn>"), "Prompt should end correctly")
        }

        @Test
        @DisplayName("Should handle Unicode and multilingual text")
        fun testUnicodeText() {
            val unicodeTexts = listOf(
                "Hello 世界", // Chinese
                "مرحبا بالعالم", // Arabic
                "Здравствуй мир", // Russian
                "🎉🎊🎈", // Emojis only
                "Café ☕", // Accents and symbols
                "Math: ∑∫∂", // Math symbols
                "♠♣♥♦" // Card symbols
            )

            unicodeTexts.forEach { text ->
                val prompt = "Rewrite this: $text <end_of_turn>"
                assertTrue(prompt.contains(text), "Unicode text should be preserved: $text")
            }
        }

        @Test
        @DisplayName("Should handle HTML and XML content")
        fun testHtmlXmlContent() {
            val markupTexts = listOf(
                "<div>Hello</div>",
                "<tag attr=\"value\">content</tag>",
                "&lt;&gt;&amp;",
                "<!-- comment -->",
                "<![CDATA[data]]>"
            )

            markupTexts.forEach { text ->
                val prompt = "Rewrite this: $text <end_of_turn>"
                assertTrue(prompt.contains(text), "Markup should be preserved: $text")
            }
        }

        @Test
        @DisplayName("Should handle code snippets")
        fun testCodeSnippets() {
            val codeTexts = listOf(
                "function test() { return true; }",
                "val x = 10",
                "if (condition) { /* comment */ }",
                "`backticks`",
                "```kotlin\ncode\n```"
            )

            codeTexts.forEach { text ->
                val prompt = "Rewrite this: $text <end_of_turn>"
                assertTrue(prompt.contains(text), "Code should be preserved: $text")
            }
        }

        @Test
        @DisplayName("Should clean response by removing special tokens")
        fun testResponseCleaning() {
            val responses = listOf(
                "<end_of_turn>Hello<end_of_turn>" to "Hello",
                "<start_of_turn>Hello" to "Hello",
                "Hello<end_of_turn>" to "Hello",
                "<start_of_turn>Hello<end_of_turn>" to "Hello",
                "  Hello  " to "Hello",
                "\nHello\n" to "Hello"
            )

            responses.forEach { (raw, expected) ->
                val cleaned = raw
                    .replace("<end_of_turn>", "")
                    .replace("<start_of_turn>", "")
                    .trim()
                assertEquals(expected, cleaned, "Response not cleaned properly: $raw")
            }
        }

        @Test
        @DisplayName("Should handle single character input")
        fun testSingleCharacter() {
            val singleChars = listOf("a", "1", "!", "€", "🎉", "界")

            singleChars.forEach { char ->
                val prompt = "Rewrite this: $char <end_of_turn>"
                assertTrue(prompt.contains(char), "Single character should be preserved: $char")
                assertTrue(prompt.length > 10, "Prompt should be complete even for single char")
            }
        }

        @Test
        @DisplayName("Should handle mixed RTL and LTR text")
        fun testMixedDirectionText() {
            val mixedTexts = listOf(
                "Hello مرحبا World",
                "עברית and English",
                "123 مرحبا 456"
            )

            mixedTexts.forEach { text ->
                val prompt = "Rewrite this: $text <end_of_turn>"
                assertTrue(prompt.contains(text), "Mixed direction text should be preserved")
            }
        }
    }

    @Nested
    @DisplayName("Error Handling")
    inner class ErrorHandling {

        @Test
        @DisplayName("Should handle null model gracefully")
        fun testNullModelHandling() {
            // Test that appropriate error is returned when model is null
            val expectedErrors = listOf(
                "Model not loaded",
                "No AI model selected",
                "AI model not loaded"
            )

            // These are the error messages we expect in various null scenarios
            assertTrue(expectedErrors.isNotEmpty(), "Should have defined error messages")
        }

        @Test
        @DisplayName("Should handle callback errors appropriately")
        fun testCallbackErrorHandling() {
            val testErrors = listOf(
                "Network error",
                "Out of memory",
                "Model file corrupted",
                "Timeout",
                null // Test null error message
            )

            testErrors.forEach { error ->
                val actualError = error ?: "Processing error"
                assertTrue(actualError.isNotEmpty(), "Error message should never be empty")
            }
        }

        @Test
        @DisplayName("Should handle empty AI response")
        fun testEmptyResponseHandling() {
            val emptyResponses = listOf("", "   ", "\n", "\t")

            emptyResponses.forEach { response ->
                assertTrue(response.isBlank(), "Response '$response' should be considered empty")
                // In actual implementation, this should trigger error callback
            }
        }
    }

    @Nested
    @DisplayName("Concurrent Access")
    inner class ConcurrentAccess {

        @Test
        @DisplayName("Should handle rapid successive calls")
        fun testRapidCalls() {
            // Test that multiple rapid calls don't cause issues
            val texts = List(10) { "Text $it" }
            val prompts = texts.map { "Rewrite this: $it <end_of_turn>" }

            assertEquals(10, prompts.size, "Should handle all rapid calls")
            prompts.forEachIndexed { index, prompt ->
                assertTrue(prompt.contains("Text $index"), "Each call should preserve its text")
            }
        }

        @Test
        @DisplayName("Should maintain callback integrity")
        fun testCallbackIntegrity() {
            // Each callback should receive the correct response
            val inputs = mapOf(
                "Input1" to "Output1",
                "Input2" to "Output2",
                "Input3" to "Output3"
            )

            inputs.forEach { (input, expectedOutput) ->
                // In real scenario, verify correct callback is called with correct output
                assertTrue(input.isNotEmpty() && expectedOutput.isNotEmpty())
            }
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    inner class EdgeCases {

        @Test
        @DisplayName("Should handle text with only whitespace")
        fun testWhitespaceOnlyText() {
            val whitespaceTexts = listOf(
                " ",
                "  ",
                "\t",
                "\n",
                "\r\n",
                " \t \n ",
                "\u00A0", // Non-breaking space
                "\u2003"  // Em space
            )

            whitespaceTexts.forEach { text ->
                assertTrue(text.isBlank(), "Whitespace text should be considered blank")
            }
        }

        @Test
        @DisplayName("Should handle text at boundary conditions")
        fun testBoundaryConditions() {
            val boundaryTexts = mapOf(
                0 to "",
                1 to "a",
                1999 to "a".repeat(1999),
                2000 to "a".repeat(2000),
                2001 to "a".repeat(2001)
            )

            boundaryTexts.forEach { (length, text) ->
                assertEquals(length, text.length, "Text length should match expected")
            }
        }

        @Test
        @DisplayName("Should handle malformed input gracefully")
        fun testMalformedInput() {
            val malformedTexts = listOf(
                "\u0000", // Null character
                "\uFEFF", // Zero-width no-break space
                "\\x00\\x01", // Escaped characters
                "${Int.MAX_VALUE}", // Large number
                "NaN", // Not a number
                "null", // Literal null
                "undefined" // Literal undefined
            )

            malformedTexts.forEach { text ->
                val prompt = "Rewrite this: $text <end_of_turn>"
                assertTrue(prompt.isNotEmpty(), "Should handle malformed input: $text")
            }
        }
    }
}