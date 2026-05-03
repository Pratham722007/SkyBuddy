package com.example.skybuddy.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.skybuddy.ui.theme.OnSurfaceDark
import com.example.skybuddy.ui.theme.OnSurfaceDim
import com.example.skybuddy.ui.theme.PrimaryPurple

/**
 * A composable that renders a subset of Markdown syntax:
 *   - **bold**, *italic*, ***bold-italic***
 *   - `inline code`
 *   - ``` code blocks ```
 *   - # / ## / ### headings
 *   - - / * bullet lists
 *   - 1. numbered lists
 *   - --- horizontal rules
 *   - [link text](url)  (rendered as underlined text)
 *
 * This is a purpose-built lightweight renderer for chat bubbles;
 * it does NOT pull in any third-party Markdown library.
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    textColor: Color = OnSurfaceDark,
    codeBackground: Color = Color(0xFFF1F3F5),
    accentColor: Color = PrimaryPurple
) {
    val blocks = parseMarkdownBlocks(markdown)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Heading -> {
                    val style = when (block.level) {
                        1 -> MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold, fontSize = 17.sp
                        )
                        2 -> MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold, fontSize = 15.sp
                        )
                        else -> MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold, fontSize = 14.sp
                        )
                    }
                    Text(
                        text = parseInlineMarkdown(block.text, textColor, accentColor),
                        style = style,
                        color = textColor,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }

                is MdBlock.CodeBlock -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(codeBackground)
                            .horizontalScroll(rememberScrollState())
                            .padding(10.dp)
                    ) {
                        Text(
                            text = block.code,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            ),
                            color = textColor
                        )
                    }
                }

                is MdBlock.BulletItem -> {
                    Row(
                        modifier = Modifier.padding(start = 8.dp, top = 1.dp, bottom = 1.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.7f))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = parseInlineMarkdown(block.text, textColor, accentColor),
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor
                        )
                    }
                }

                is MdBlock.NumberedItem -> {
                    Row(
                        modifier = Modifier.padding(start = 8.dp, top = 1.dp, bottom = 1.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "${block.number}.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = accentColor.copy(alpha = 0.8f),
                            modifier = Modifier.width(20.dp)
                        )
                        Text(
                            text = parseInlineMarkdown(block.text, textColor, accentColor),
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor
                        )
                    }
                }

                is MdBlock.HorizontalRule -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .height(1.dp)
                            .background(OnSurfaceDim.copy(alpha = 0.25f))
                    )
                }

                is MdBlock.Paragraph -> {
                    if (block.text.isNotBlank()) {
                        Text(
                            text = parseInlineMarkdown(block.text, textColor, accentColor),
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}

// ── Block-level parsing ────────────────────────────────────────────────

private sealed class MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock()
    data class CodeBlock(val code: String) : MdBlock()
    data class BulletItem(val text: String) : MdBlock()
    data class NumberedItem(val number: Int, val text: String) : MdBlock()
    data object HorizontalRule : MdBlock()
    data class Paragraph(val text: String) : MdBlock()
}

private fun parseMarkdownBlocks(markdown: String): List<MdBlock> {
    val lines = markdown.lines()
    val blocks = mutableListOf<MdBlock>()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trimStart()

        // ── Code block (fenced with ```)
        if (trimmed.startsWith("```")) {
            i++
            val codeLines = mutableListOf<String>()
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            if (i < lines.size) i++ // skip closing ```
            blocks.add(MdBlock.CodeBlock(codeLines.joinToString("\n")))
            continue
        }

        // ── Horizontal rule
        if (trimmed.matches(Regex("^-{3,}$")) || trimmed.matches(Regex("^\\*{3,}$"))) {
            blocks.add(MdBlock.HorizontalRule)
            i++
            continue
        }

        // ── Heading
        val headingMatch = Regex("^(#{1,3})\\s+(.*)").find(trimmed)
        if (headingMatch != null) {
            val level = headingMatch.groupValues[1].length
            val text = headingMatch.groupValues[2]
            blocks.add(MdBlock.Heading(level, text))
            i++
            continue
        }

        // ── Bullet list item
        val bulletMatch = Regex("^[-*+]\\s+(.*)").find(trimmed)
        if (bulletMatch != null) {
            blocks.add(MdBlock.BulletItem(bulletMatch.groupValues[1]))
            i++
            continue
        }

        // ── Numbered list item
        val numberedMatch = Regex("^(\\d+)\\.\\s+(.*)").find(trimmed)
        if (numberedMatch != null) {
            blocks.add(MdBlock.NumberedItem(numberedMatch.groupValues[1].toInt(), numberedMatch.groupValues[2]))
            i++
            continue
        }

        // ── Plain paragraph line
        blocks.add(MdBlock.Paragraph(line))
        i++
    }

    return blocks
}

// ── Inline parsing ─────────────────────────────────────────────────────

/**
 * Parses inline markdown (bold, italic, code, links) into an AnnotatedString.
 */
private fun parseInlineMarkdown(
    text: String,
    textColor: Color,
    accentColor: Color
): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        when {
            // ── Inline code: `code`
            text[i] == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end != -1) {
                    withStyle(SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = Color(0xFFF1F3F5),
                        fontSize = 13.sp,
                        color = textColor
                    )) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }

            // ── Bold-italic: ***text***
            text.startsWith("***", i) -> {
                val end = text.indexOf("***", i + 3)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                        append(text.substring(i + 3, end))
                    }
                    i = end + 3
                } else {
                    append("*")
                    i++
                }
            }

            // ── Bold: **text**
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                } else {
                    append("*")
                    i++
                }
            }

            // ── Italic: *text*
            text[i] == '*' -> {
                val end = text.indexOf('*', i + 1)
                if (end != -1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    append("*")
                    i++
                }
            }

            // ── Link: [text](url) — rendered as underlined colored text
            text[i] == '[' -> {
                val closeBracket = text.indexOf(']', i + 1)
                if (closeBracket != -1 && closeBracket + 1 < text.length && text[closeBracket + 1] == '(') {
                    val closeParen = text.indexOf(')', closeBracket + 2)
                    if (closeParen != -1) {
                        val linkText = text.substring(i + 1, closeBracket)
                        withStyle(SpanStyle(
                            color = accentColor,
                            textDecoration = TextDecoration.Underline
                        )) {
                            append(linkText)
                        }
                        i = closeParen + 1
                    } else {
                        append(text[i])
                        i++
                    }
                } else {
                    append(text[i])
                    i++
                }
            }

            else -> {
                append(text[i])
                i++
            }
        }
    }
}
