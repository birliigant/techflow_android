package com.birliigant.techflow.ui.common

import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import coil.compose.SubcomposeAsyncImage
import com.birliigant.techflow.data.network.normalizeRemoteUrl

private const val UrlAnnotation = "url"

private sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class ListItem(
        val marker: String,
        val text: String,
        val indentLevel: Int = 0,
    ) : MarkdownBlock
    data class Quote(val text: String) : MarkdownBlock
    data class CodeBlock(val code: String) : MarkdownBlock
    data class Image(val alt: String, val url: String) : MarkdownBlock
    data class Table(val rows: List<List<String>>) : MarkdownBlock
    data object HorizontalRule : MarkdownBlock
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

                is MarkdownBlock.ListItem -> Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = (block.indentLevel * 18).dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MarkdownClickableText(
                        text = block.marker,
                        style = style.copy(fontWeight = FontWeight.Bold),
                    )
                    MarkdownClickableText(
                        text = block.text,
                        modifier = Modifier.weight(1f),
                        style = style,
                    )
                }

                is MarkdownBlock.Quote -> Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = block.text,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        style = style.copy(
                            fontWeight = FontWeight.Medium,
                            color = resolvedTextColor(style),
                        ),
                    )
                }

                is MarkdownBlock.CodeBlock -> Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        text = block.code,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        style = style.copy(
                            fontFamily = FontFamily.Monospace,
                            color = resolvedTextColor(style),
                        ),
                    )
                }

                is MarkdownBlock.Image -> MarkdownImage(
                    url = block.url,
                    alt = block.alt,
                )

                is MarkdownBlock.Table -> MarkdownTable(
                    rows = block.rows,
                    style = style,
                )

                MarkdownBlock.HorizontalRule -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
private fun MarkdownTable(
    rows: List<List<String>>,
    style: TextStyle,
) {
    if (rows.isEmpty()) return

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            rows.forEachIndexed { rowIndex, row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    row.forEach { cell ->
                        Text(
                            text = cell,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            style = style.copy(
                                fontWeight = if (rowIndex == 0) FontWeight.SemiBold else FontWeight.Normal,
                                color = resolvedTextColor(style),
                            ),
                        )
                    }
                }
                if (rowIndex != rows.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                }
            }
        }
    }
}

@Composable
private fun MarkdownImage(
    url: String,
    alt: String,
) {
    val uriHandler = LocalUriHandler.current
    val resolvedUrl = remember(url) { url.toResolvableRemoteUrl() }
    Surface(
        modifier = Modifier.clickable {
            if (resolvedUrl.isNotBlank()) {
                uriHandler.openUri(resolvedUrl)
            }
        },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    ) {
        SubcomposeAsyncImage(
            model = resolvedUrl,
            contentDescription = alt,
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            contentScale = ContentScale.FillWidth,
            loading = {
                Text(
                    text = "图片加载中...",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            error = {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = alt.ifBlank { "图片加载失败" },
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (resolvedUrl.isNotBlank()) {
                        Text(
                            text = "点击打开原图",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
        )
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
        style = style.copy(color = resolvedTextColor(style)),
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
    val imageRegex = Regex("^!\\[(.*?)]\\((.*?)\\)$")
    val imageLinkRegex = Regex("^\\[(.*?)]\\((.*?)\\)$")
    val lines = text.replace("\r\n", "\n").split('\n')
    var index = 0

    fun flushParagraph() {
        if (paragraph.isNotEmpty()) {
            blocks += MarkdownBlock.Paragraph(paragraph.joinToString("\n").trim())
            paragraph.clear()
        }
    }

    while (index < lines.size) {
        val rawLine = lines[index]
        val line = rawLine.trimEnd()
        val trimmed = line.trim()
        when {
            trimmed.isBlank() -> {
                flushParagraph()
                index += 1
            }

            trimmed.startsWith("```") -> {
                flushParagraph()
                val codeLines = mutableListOf<String>()
                index += 1
                while (index < lines.size && !lines[index].trim().startsWith("```")) {
                    codeLines += lines[index]
                    index += 1
                }
                blocks += MarkdownBlock.CodeBlock(codeLines.joinToString("\n").trimEnd())
                if (index < lines.size) index += 1
            }

            isHorizontalRule(trimmed) -> {
                flushParagraph()
                blocks += MarkdownBlock.HorizontalRule
                index += 1
            }

            trimmed.startsWith("|") -> {
                flushParagraph()
                val tableLines = mutableListOf<String>()
                while (index < lines.size && lines[index].trim().startsWith("|")) {
                    tableLines += lines[index].trim()
                    index += 1
                }
                parseMarkdownTable(tableLines)?.let { blocks += it }
            }

            trimmed.startsWith(">") -> {
                flushParagraph()
                val quoteLines = mutableListOf<String>()
                while (index < lines.size && lines[index].trim().startsWith(">")) {
                    quoteLines += lines[index].trim().removePrefix(">").trim()
                    index += 1
                }
                blocks += MarkdownBlock.Quote(quoteLines.joinToString("\n").trim())
            }

            imageRegex.matches(trimmed) -> {
                flushParagraph()
                val match = imageRegex.matchEntire(trimmed) ?: break
                blocks += MarkdownBlock.Image(
                    alt = match.groupValues[1],
                    url = match.groupValues[2],
                )
                index += 1
            }

            imageLinkRegex.matches(trimmed) -> {
                val match = imageLinkRegex.matchEntire(trimmed) ?: break
                val alt = match.groupValues[1]
                val url = match.groupValues[2]
                if (looksLikeImageUrl(url) || looksLikeImageUrl(alt)) {
                    flushParagraph()
                    blocks += MarkdownBlock.Image(
                        alt = alt,
                        url = url,
                    )
                } else {
                    paragraph += trimmed
                }
                index += 1
            }

            trimmed.startsWith("#") -> {
                flushParagraph()
                val level = trimmed.takeWhile { it == '#' }.length.coerceIn(1, 3)
                blocks += MarkdownBlock.Heading(level, trimmed.drop(level).trim())
                index += 1
            }

            isListLine(rawLine) -> {
                flushParagraph()
                parseListItem(rawLine)?.let { blocks += it }
                index += 1
            }

            else -> {
                paragraph += trimmed
                index += 1
            }
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
    val linkRegex = Regex("\\[(.*?)]\\(([^\\s)]+)\\)")
    val rawUrlRegex = Regex("https?://[^\\s)]+")

    return buildAnnotatedString {
        var index = 0
        while (index < text.length) {
            val linkMatch = linkRegex.find(text, index)
            if (linkMatch != null && linkMatch.range.first == index) {
                val label = linkMatch.groupValues[1]
                val url = linkMatch.groupValues[2].toResolvableRemoteUrl()
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

            if (text[index] == '_' || text[index] == '*') {
                val marker = text[index]
                val closeIndex = text.indexOf(marker, startIndex = index + 1)
                if (closeIndex > index + 1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Medium)) {
                        append(text.substring(index + 1, closeIndex))
                    }
                    index = closeIndex + 1
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

private fun parseListItem(rawLine: String): MarkdownBlock.ListItem? {
    val indentSpaces = rawLine.takeWhile { it == ' ' || it == '\t' }
        .fold(0) { acc, c -> acc + if (c == '\t') 4 else 1 }
    val indentLevel = indentSpaces / 4
    val trimmed = rawLine.trimStart()
    val orderedMatch = Regex("^(\\d+)\\.\\s+(.*)$").matchEntire(trimmed)
    if (orderedMatch != null) {
        return MarkdownBlock.ListItem(
            marker = "${orderedMatch.groupValues[1]}.",
            text = orderedMatch.groupValues[2].trim(),
            indentLevel = indentLevel,
        )
    }
    val unorderedMatch = Regex("^[-*+]\\s+(.*)$").matchEntire(trimmed) ?: return null
    return MarkdownBlock.ListItem(
        marker = "•",
        text = unorderedMatch.groupValues[1].trim(),
        indentLevel = indentLevel,
    )
}

private fun isListLine(rawLine: String): Boolean {
    val trimmed = rawLine.trimStart()
    return Regex("^[-*+]\\s+").containsMatchIn(trimmed) ||
        Regex("^\\d+\\.\\s+").containsMatchIn(trimmed)
}

private fun isHorizontalRule(line: String): Boolean {
    return Regex("^([-*_])(?:\\s*\\1){2,}\\s*$").matches(line)
}

private fun parseMarkdownTable(lines: List<String>): MarkdownBlock.Table? {
    if (lines.isEmpty()) return null
    val rows = lines.map { line ->
        line.removePrefix("|")
            .removeSuffix("|")
            .split("|")
            .map { it.trim() }
    }.filter { row -> row.any { it.isNotBlank() } }
    if (rows.isEmpty()) return null

    val filteredRows = if (rows.size > 1 && rows[1].all { cell ->
            cell.isNotBlank() && cell.all { it == '-' || it == ':' }
        }) {
        listOf(rows.first()) + rows.drop(2)
    } else {
        rows
    }
    return MarkdownBlock.Table(filteredRows)
}

@Composable
private fun resolvedTextColor(style: TextStyle): Color {
    return if (style.color != Color.Unspecified) {
        style.color
    } else {
        MaterialTheme.colorScheme.onSurface
    }
}

private fun String.toResolvableRemoteUrl(): String {
    val normalized = normalizeRemoteUrl().trim()
    if (normalized.startsWith("http://image.kid1934.top/")) {
        return normalized.replaceFirst("http://", "https://")
    }
    return normalized
}

private fun looksLikeImageUrl(value: String): Boolean {
    val target = value.lowercase()
    return target.endsWith(".png") ||
        target.endsWith(".jpg") ||
        target.endsWith(".jpeg") ||
        target.endsWith(".gif") ||
        target.endsWith(".webp") ||
        target.endsWith(".svg")
}
