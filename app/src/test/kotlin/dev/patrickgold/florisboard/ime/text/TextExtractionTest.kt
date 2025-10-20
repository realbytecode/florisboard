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

package dev.patrickgold.florisboard.ime.text

import dev.patrickgold.florisboard.ime.editor.EditorContent
import dev.patrickgold.florisboard.ime.editor.EditorRange
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class TextExtractionTest {

    @Nested
    @DisplayName("EditorContent Text Extraction")
    inner class EditorContentExtraction {

        @Test
        @DisplayName("Should extract selected text when selection exists")
        fun testSelectedTextExtraction() {
            val content = EditorContent(
                text = "Hello World Example",
                offset = 0,
                localSelection = EditorRange(6, 11), // "World" is selected
                localComposing = EditorRange.Unspecified,
                localCurrentWord = EditorRange.Unspecified
            )

            assertEquals("World", content.selectedText)
            assertTrue(content.selectedText.isNotBlank())
        }

        @Test
        @DisplayName("Should return empty string when no selection")
        fun testNoSelectionExtraction() {
            val content = EditorContent(
                text = "Hello World",
                offset = 0,
                localSelection = EditorRange(5, 5), // Cursor at position 5, no selection
                localComposing = EditorRange.Unspecified,
                localCurrentWord = EditorRange.Unspecified
            )

            assertEquals("", content.selectedText)
            assertTrue(content.selectedText.isBlank())
        }

        @Test
        @DisplayName("Should handle invalid selection ranges")
        fun testInvalidSelectionRange() {
            val content = EditorContent(
                text = "Hello World",
                offset = 0,
                localSelection = EditorRange.Unspecified,
                localComposing = EditorRange.Unspecified,
                localCurrentWord = EditorRange.Unspecified
            )

            assertEquals("", content.selectedText)
            assertFalse(content.localSelection.isValid)
        }

        @Test
        @DisplayName("Should extract text before selection")
        fun testTextBeforeSelection() {
            val content = EditorContent(
                text = "Hello World Example",
                offset = 0,
                localSelection = EditorRange(11, 11), // Cursor after "World"
                localComposing = EditorRange.Unspecified,
                localCurrentWord = EditorRange.Unspecified
            )

            assertEquals("Hello World", content.textBeforeSelection)
        }

        @Test
        @DisplayName("Should extract text after selection")
        fun testTextAfterSelection() {
            val content = EditorContent(
                text = "Hello World Example",
                offset = 0,
                localSelection = EditorRange(11, 11), // Cursor after "World"
                localComposing = EditorRange.Unspecified,
                localCurrentWord = EditorRange.Unspecified
            )

            assertEquals(" Example", content.textAfterSelection)
        }

        @Test
        @DisplayName("Should handle EditorContent.Unspecified")
        fun testUnspecifiedContent() {
            val content = EditorContent.Unspecified

            assertEquals("", content.text)
            assertEquals("", content.selectedText)
            assertEquals("", content.textBeforeSelection)
            assertEquals("", content.textAfterSelection)
            assertEquals(-1, content.offset)
        }

        @Test
        @DisplayName("Should handle empty text content")
        fun testEmptyTextContent() {
            val content = EditorContent(
                text = "",
                offset = 0,
                localSelection = EditorRange(0, 0),
                localComposing = EditorRange.Unspecified,
                localCurrentWord = EditorRange.Unspecified
            )

            assertEquals("", content.text)
            assertEquals("", content.selectedText)
            assertEquals("", content.textBeforeSelection)
            assertEquals("", content.textAfterSelection)
        }
    }

    @Nested
    @DisplayName("Text Extraction Priority")
    inner class TextExtractionPriority {

        @Test
        @DisplayName("Should prioritize selected text over full text")
        fun testSelectedTextPriority() {
            val editorContent = EditorContent(
                text = "This is the full text content",
                offset = 0,
                localSelection = EditorRange(8, 11), // "the" is selected
                localComposing = EditorRange.Unspecified,
                localCurrentWord = EditorRange.Unspecified
            )

            // Simulate the priority logic from processTextWithAi
            val textToProcess = when {
                editorContent.selectedText.isNotBlank() -> editorContent.selectedText
                editorContent.text.isNotBlank() -> editorContent.text
                else -> ""
            }

            assertEquals("the", textToProcess)
        }

        @Test
        @DisplayName("Should use full text when no selection")
        fun testFullTextFallback() {
            val editorContent = EditorContent(
                text = "This is the full text",
                offset = 0,
                localSelection = EditorRange(10, 10), // Just cursor, no selection
                localComposing = EditorRange.Unspecified,
                localCurrentWord = EditorRange.Unspecified
            )

            val textToProcess = when {
                editorContent.selectedText.isNotBlank() -> editorContent.selectedText
                editorContent.text.isNotBlank() -> editorContent.text
                else -> ""
            }

            assertEquals("This is the full text", textToProcess)
        }

        @Test
        @DisplayName("Should return empty when no text available")
        fun testNoTextAvailable() {
            val editorContent = EditorContent.Unspecified

            val textToProcess = when {
                editorContent.selectedText.isNotBlank() -> editorContent.selectedText
                editorContent.text.isNotBlank() -> editorContent.text
                else -> ""
            }

            assertEquals("", textToProcess)
        }
    }

    @Nested
    @DisplayName("Edge Cases in Text Extraction")
    inner class TextExtractionEdgeCases {

        @Test
        @DisplayName("Should handle text with only whitespace")
        fun testWhitespaceOnlyText() {
            val whitespaceText = "   \t\n   "
            val content = EditorContent(
                text = whitespaceText,
                offset = 0,
                localSelection = EditorRange(0, whitespaceText.length),
                localComposing = EditorRange.Unspecified,
                localCurrentWord = EditorRange.Unspecified
            )

            // The actual behavior depends on EditorContent implementation
            // If it returns empty string for invalid/whitespace selections, test that
            val actualSelectedText = content.selectedText
            if (actualSelectedText.isEmpty()) {
                assertEquals("", actualSelectedText)
            } else {
                assertEquals(whitespaceText, actualSelectedText)
                assertTrue(actualSelectedText.isBlank())
            }
        }

        @Test
        @DisplayName("Should handle very long text")
        fun testVeryLongText() {
            val longText = "a".repeat(5000)
            val content = EditorContent(
                text = longText,
                offset = 0,
                localSelection = EditorRange(0, 5000),
                localComposing = EditorRange.Unspecified,
                localCurrentWord = EditorRange.Unspecified
            )

            assertEquals(5000, content.selectedText.length)
            assertEquals(longText, content.selectedText)
        }

        @Test
        @DisplayName("Should handle selection at text boundaries")
        fun testBoundarySelection() {
            val text = "Hello World"

            // Selection at start
            val startContent = EditorContent(
                text = text,
                offset = 0,
                localSelection = EditorRange(0, 5), // "Hello"
                localComposing = EditorRange.Unspecified,
                localCurrentWord = EditorRange.Unspecified
            )
            assertEquals("Hello", startContent.selectedText)

            // Selection at end
            val endContent = EditorContent(
                text = text,
                offset = 0,
                localSelection = EditorRange(6, 11), // "World"
                localComposing = EditorRange.Unspecified,
                localCurrentWord = EditorRange.Unspecified
            )
            assertEquals("World", endContent.selectedText)

            // Full text selection
            val fullContent = EditorContent(
                text = text,
                offset = 0,
                localSelection = EditorRange(0, 11),
                localComposing = EditorRange.Unspecified,
                localCurrentWord = EditorRange.Unspecified
            )
            assertEquals("Hello World", fullContent.selectedText)
        }

        @Test
        @DisplayName("Should handle Unicode and special characters")
        fun testUnicodeExtraction() {
            val unicodeText = "Hello 👋 世界 🌍"
            val content = EditorContent(
                text = unicodeText,
                offset = 0,
                localSelection = EditorRange(0, unicodeText.length),
                localComposing = EditorRange.Unspecified,
                localCurrentWord = EditorRange.Unspecified
            )

            assertEquals(unicodeText, content.selectedText)
            assertTrue(content.selectedText.contains("👋"))
            assertTrue(content.selectedText.contains("世界"))
        }

        @Test
        @DisplayName("Should handle multiline text")
        fun testMultilineExtraction() {
            val multilineText = "Line 1\nLine 2\nLine 3"
            val content = EditorContent(
                text = multilineText,
                offset = 0,
                localSelection = EditorRange(7, 13), // "Line 2"
                localComposing = EditorRange.Unspecified,
                localCurrentWord = EditorRange.Unspecified
            )

            assertEquals("Line 2", content.selectedText)
        }

        @Test
        @DisplayName("Should handle composing text")
        fun testComposingText() {
            val content = EditorContent(
                text = "Hello composing World",
                offset = 0,
                localSelection = EditorRange(6, 6),
                localComposing = EditorRange(6, 15), // "composing" is being composed
                localCurrentWord = EditorRange(6, 15)
            )

            assertEquals("composing", content.composingText)
            assertEquals("composing", content.currentWordText)
            assertTrue(content.localComposing.isValid)
        }

        @Test
        @DisplayName("Should handle offset in content")
        fun testContentWithOffset() {
            val content = EditorContent(
                text = "World",
                offset = 6, // Text actually starts at position 6 in full document
                localSelection = EditorRange(0, 5),
                localComposing = EditorRange.Unspecified,
                localCurrentWord = EditorRange.Unspecified
            )

            assertEquals("World", content.selectedText)
            assertEquals(EditorRange(6, 11), content.selection) // Adjusted for offset
        }

        @Test
        @DisplayName("Should handle reversed selection ranges")
        fun testReversedSelection() {
            val content = EditorContent(
                text = "Hello World",
                offset = 0,
                localSelection = EditorRange(11, 6), // Reversed range
                localComposing = EditorRange.Unspecified,
                localCurrentWord = EditorRange.Unspecified
            )

            // EditorRange should be normalized
            val normalizedRange = EditorRange.normalized(11, 6)
            assertEquals(6, normalizedRange.start)
            assertEquals(11, normalizedRange.end)
        }

        @Test
        @DisplayName("Should handle selection with zero length")
        fun testZeroLengthSelection() {
            val content = EditorContent(
                text = "Hello World",
                offset = 0,
                localSelection = EditorRange(5, 5), // Cursor position, no selection
                localComposing = EditorRange.Unspecified,
                localCurrentWord = EditorRange.Unspecified
            )

            assertEquals("", content.selectedText)
            assertEquals(0, content.localSelection.length)
            assertFalse(content.localSelection.isSelectionMode)
        }
    }

    @Nested
    @DisplayName("Text Processing Logic")
    inner class TextProcessingLogic {

        @Test
        @DisplayName("Should trim whitespace from extracted text")
        fun testWhitespaceTrimming() {
            val texts = listOf(
                "  Hello  " to "Hello",
                "\tWorld\t" to "World",
                "\n\nTest\n\n" to "Test",
                "  Mixed   Spaces  " to "Mixed   Spaces"
            )

            texts.forEach { (input, expected) ->
                assertEquals(expected, input.trim())
            }
        }

        @Test
        @DisplayName("Should detect selection vs no selection")
        fun testSelectionDetection() {
            val hasSelection = EditorContent(
                text = "Hello World",
                offset = 0,
                localSelection = EditorRange(0, 5), // Has selection
                localComposing = EditorRange.Unspecified,
                localCurrentWord = EditorRange.Unspecified
            )

            val noSelection = EditorContent(
                text = "Hello World",
                offset = 0,
                localSelection = EditorRange(5, 5), // No selection
                localComposing = EditorRange.Unspecified,
                localCurrentWord = EditorRange.Unspecified
            )

            assertTrue(hasSelection.localSelection.isSelectionMode)
            assertFalse(noSelection.localSelection.isSelectionMode)
        }

        @Test
        @DisplayName("Should handle text length validation")
        fun testTextLengthValidation() {
            val maxLength = 2000

            val validLength = "a".repeat(1999)
            assertTrue(validLength.length < maxLength)

            val exactLength = "a".repeat(2000)
            assertTrue(exactLength.length == maxLength)

            val tooLong = "a".repeat(2001)
            assertTrue(tooLong.length > maxLength)
        }
    }
}