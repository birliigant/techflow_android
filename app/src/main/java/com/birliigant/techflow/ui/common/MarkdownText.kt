package com.birliigant.techflow.ui.common

import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat

private const val UrlAnnotation = "url"

private sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class Bullet(val text: String) : MarkdownBlock
}

@Composable
fun MarkdownText(
    content: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    val text = content.trim()
    if (text.isBlank()) return

    if (looksLikeHtml(text)) {
        HtmlText(
            html = text,
            modifier = modifier,
        )
        return
    }

    val blocks = remember(text) { parseMarkdownBlocks(text) }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> MarkdownClickableText(
                    text = block.text,
                    style = when (block.level) {
                        1 -> MaterialTheme.typography.headlineMedium
                        2 -> MaterialTheme.typography.headlineSmall
                        else -> MaterialTheme.typography.titleLarge
                    }.copy(fontWeight = FontWeight.Bold),
                )

                is MarkdownBlock.Paragraph -> MarkdownClickableText(
                    text = block.text,
                    style = style,
                )

                is MarkdownBlock.Bullet -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MarkdownClickableText(
                        text = "•",
                        style = style.copy(fontWeight = FontWeight.Bold),
                    )
                    MarkdownClickableText(
                        text = block.text,
                        modifier = Modifier.weight(1f),
                        style = style,
                    )
                }
            }
        }
    }
}

@Composable
private fun HtmlText(
    html: String,
    modifier: Modifier = Modifier,
) {
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val linkColor = MaterialTheme.colorScheme.primary.toArgb()
    val bodySizePx = with(androidx.compose.ui.platform.LocalDensity.current) {
        MaterialTheme.typography.bodyLarge.fontSize.toPx()
    }

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            TextView(context).apply {
                movementMethod = LinkMovementMethod.getInstance()
                setTextColor(textColor)
                textSize = bodySizePx / context.resources.displayMetrics.scaledDensity
                setLineSpacing(0f, 1.2f)
                linksClickable = true
                setLinkTextColor(linkColor)
            }
        },
        update = { textView ->
            textView.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
        },
    )
}

@Composable
private fun MarkdownClickableText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle,
) {
    val uriHandler = LocalUriHandler.current
    val linkStyle = SpanStyle(
        color = MaterialTheme.colorScheme.primary,
        textDecoration = TextDecoration.Underline,
    )
    val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
    val codeStyle = SpanStyle(
        fontFamily = FontFamily.Monospace,
        background = MaterialTheme.colorScheme.surfaceVariant,
    )
    val annotated = remember(text, style, linkStyle, boldStyle, codeStyle) {
        buildMarkdownAnnotatedString(
            text = text,
            linkStyle = linkStyle,
            boldStyle = boldStyle,
            codeStyle = codeStyle,
        )
    }

    ClickableText(
        modifier = modifier,
        text = annotated,
        style = style.copy(color = MaterialTheme.colorScheme.onSurface),
        onClick = { offset ->
            annotated
                .getStringAnnotations(UrlAnnotation, offset, offset)
                .firstOrNull()
                ?.item
                ?.let(uriHandler::openUri)
        },
    )
}

private fun looksLikeHtml(text: String): Boolean {
    return text.startsWith("<") && text.contains(">")
}

private fun parseMarkdownBlocks(text: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val paragraph = mutableListOf<String>()

    fun flushParagraph() {
        if (paragraph.isNotEmpty()) {
            blocks += MarkdownBlock.Paragraph(paragraph.joinToString("\n").trim())
            paragraph.clear()
        }
    }

    text.replace("\r\n", "\n")
        .split('\n')
        .forEach { rawLine ->
            val line = rawLine.trimEnd()
            when {
                line.isBlank() -> flushParagraph()
                line.startsWith("#") -> {
                    flushParagraph()
                    val level = line.takeWhile { it == '#' }.length.coerceIn(1, 3)
                    blocks += MarkdownBlock.Heading(level, line.drop(level).trim())
                }

                line.startsWith("- ") || line.startsWith("* ") -> {
                    flushParagraph()
                    blocks += MarkdownBlock.Bullet(line.drop(2).trim())
                }

                Regex("^\\d+\\.\\s+").containsMatchIn(line) -> {
                    flushParagraph()
                    blocks += MarkdownBlock.Bullet(line.replaceFirst(Regex("^\\d+\\.\\s+"), "").trim())
                }

                else -> paragraph += line.trim()
            }
        }

    flushParagraph()
    return blocks
}

private fun buildMarkdownAnnotatedString(
    text: String,
    linkStyle: SpanStyle,
    boldStyle: SpanStyle,
    codeStyle: SpanStyle,
): AnnotatedString {
    val linkRegex = Regex("\\[(.*?)]\\((https?://[^\\s)]+)\\)")
    val rawUrlRegex = Regex("https?://[^\\s)]+")

    return buildAnnotatedString {
        var index = 0
        while (index < text.length) {
            val linkMatch = linkRegex.find(text, index)
            if (linkMatch != null && linkMatch.range.first == index) {
                val label = linkMatch.groupValues[1]
                val url = linkMatch.groupValues[2]
                pushStringAnnotation(UrlAnnotation, url)
                withStyle(linkStyle) { append(label) }
                pop()
                index = linkMatch.range.last + 1
                continue
            }

            val rawUrlMatch = rawUrlRegex.find(text, index)
            if (rawUrlMatch != null && rawUrlMatch.range.first == index) {
                val url = rawUrlMatch.value
                pushStringAnnotation(UrlAnnotation, url)
                withStyle(linkStyle) { append(url) }
                pop()
                index = rawUrlMatch.range.last + 1
                continue
            }

            if (text.startsWith("**", index)) {
                val closeIndex = text.indexOf("**", startIndex = index + 2)
                if (closeIndex > index + 2) {
                    withStyle(boldStyle) {
                        append(text.substring(index + 2, closeIndex))
                    }
                    index = closeIndex + 2
                    continue
                }
            }

            if (text[index] == '`') {
                val closeIndex = text.indexOf('`', startIndex = index + 1)
                if (closeIndex > index + 1) {
                    withStyle(codeStyle) {
                        append(text.substring(index + 1, closeIndex))
                    }
                    index = closeIndex + 1
                    continue
                }
            }

            append(text[index])
            index += 1
        }
    }
}
