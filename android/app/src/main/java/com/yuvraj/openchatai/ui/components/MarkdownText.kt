package com.yuvraj.openchatai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yuvraj.openchatai.ui.theme.CodeBackground

private sealed interface MdBlock {
    data class Paragraph(val text: String) : MdBlock
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Code(val language: String, val code: String) : MdBlock
    data class Bullets(val items: List<String>) : MdBlock
    data class Numbered(val items: List<String>) : MdBlock
    data class Quote(val text: String) : MdBlock
}

private val numberedRegex = Regex("^\\d+[.)]\\s+")

private fun parseBlocks(markdown: String): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    val lines = markdown.lines()
    val paragraph = StringBuilder()

    fun flushParagraph() {
        if (paragraph.isNotBlank()) blocks += MdBlock.Paragraph(paragraph.toString().trim())
        paragraph.clear()
    }

    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trimStart()
        when {
            trimmed.startsWith("```") -> {
                flushParagraph()
                val language = trimmed.removePrefix("```").trim()
                val code = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                    code.appendLine(lines[i])
                    i++
                }
                blocks += MdBlock.Code(language, code.toString().trimEnd())
                i++
            }
            trimmed.startsWith("#") -> {
                flushParagraph()
                val level = trimmed.takeWhile { it == '#' }.length.coerceAtMost(4)
                blocks += MdBlock.Heading(level, trimmed.dropWhile { it == '#' }.trim())
                i++
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                flushParagraph()
                val items = mutableListOf<String>()
                while (i < lines.size) {
                    val t = lines[i].trimStart()
                    if (t.startsWith("- ") || t.startsWith("* ")) {
                        items += t.drop(2).trim()
                        i++
                    } else break
                }
                blocks += MdBlock.Bullets(items)
            }
            numberedRegex.containsMatchIn(trimmed) -> {
                flushParagraph()
                val items = mutableListOf<String>()
                while (i < lines.size) {
                    val t = lines[i].trimStart()
                    if (numberedRegex.containsMatchIn(t)) {
                        items += t.replaceFirst(numberedRegex, "").trim()
                        i++
                    } else break
                }
                blocks += MdBlock.Numbered(items)
            }
            trimmed.startsWith(">") -> {
                flushParagraph()
                val quote = StringBuilder()
                while (i < lines.size && lines[i].trimStart().startsWith(">")) {
                    if (quote.isNotEmpty()) quote.append('\n')
                    quote.append(lines[i].trimStart().removePrefix(">").trim())
                    i++
                }
                blocks += MdBlock.Quote(quote.toString())
            }
            line.isBlank() -> {
                flushParagraph()
                i++
            }
            else -> {
                if (paragraph.isNotEmpty()) paragraph.append('\n')
                paragraph.append(line)
                i++
            }
        }
    }
    flushParagraph()
    return blocks
}

private val inlinePattern = Regex(
    "(\\*\\*(.+?)\\*\\*)|(\\*([^*\\n]+?)\\*)|(`([^`\\n]+)`)|(\\[([^\\]]+)]\\(([^)\\s]+)\\))",
)

private fun buildInline(
    text: String,
    linkColor: Color,
    codeBackground: Color,
    codeColor: Color,
): AnnotatedString = buildAnnotatedString {
    var last = 0
    for (match in inlinePattern.findAll(text)) {
        if (match.range.first > last) append(text.substring(last, match.range.first))
        val bold = match.groups[2]
        val italic = match.groups[4]
        val code = match.groups[6]
        val linkText = match.groups[8]
        val linkUrl = match.groups[9]
        when {
            bold != null -> withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(bold.value) }
            italic != null -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(italic.value) }
            code != null -> withStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = codeBackground,
                    color = codeColor,
                    fontSize = 13.5.sp,
                ),
            ) { append(" ${code.value} ") }
            linkText != null && linkUrl != null -> withLink(
                LinkAnnotation.Url(
                    url = linkUrl.value,
                    styles = TextLinkStyles(
                        style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                    ),
                ),
            ) { append(linkText.value) }
        }
        last = match.range.last + 1
    }
    if (last < text.length) append(text.substring(last))
}

/**
 * Lightweight markdown renderer supporting headings, bold/italic, inline code,
 * fenced code blocks with copy, bullet/numbered lists, quotes and links.
 */
@Composable
fun MarkdownText(markdown: String, modifier: Modifier = Modifier) {
    val blocks = remember(markdown) { parseBlocks(LatexMath.normalize(markdown)) }
    val colors = MaterialTheme.colorScheme
    val inlineCodeBg = colors.surfaceContainerHighest

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Paragraph -> Text(
                    text = buildInline(block.text, colors.primary, inlineCodeBg, colors.onPrimaryContainer),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onBackground,
                    lineHeight = 24.sp,
                )
                is MdBlock.Heading -> Text(
                    text = buildInline(block.text, colors.primary, inlineCodeBg, colors.onPrimaryContainer),
                    style = when (block.level) {
                        1 -> MaterialTheme.typography.titleLarge
                        2 -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.titleSmall
                    },
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground,
                )
                is MdBlock.Code -> CodeBlock(language = block.language, code = block.code)
                is MdBlock.Bullets -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    block.items.forEach { item ->
                        Row {
                            Text(
                                text = "•  ",
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.primary,
                            )
                            Text(
                                text = buildInline(item, colors.primary, inlineCodeBg, colors.onPrimaryContainer),
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.onBackground,
                                lineHeight = 22.sp,
                            )
                        }
                    }
                }
                is MdBlock.Numbered -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    block.items.forEachIndexed { index, item ->
                        Row {
                            Text(
                                text = "${index + 1}.  ",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.primary,
                            )
                            Text(
                                text = buildInline(item, colors.primary, inlineCodeBg, colors.onPrimaryContainer),
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.onBackground,
                                lineHeight = 22.sp,
                            )
                        }
                    }
                }
                is MdBlock.Quote -> Row {
                    Column(
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .background(colors.primary, RoundedCornerShape(2.dp))
                            .size(width = 3.dp, height = 20.dp),
                    ) {}
                    Text(
                        text = buildInline(block.text, colors.primary, inlineCodeBg, colors.onPrimaryContainer),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        fontStyle = FontStyle.Italic,
                    )
                }
            }
        }
    }
}

@Composable
private fun CodeBlock(language: String, code: String) {
    val clipboard = LocalClipboardManager.current
    val highlighted = remember(language, code) { SyntaxHighlight.highlight(code, language) }
    Surface(
        color = CodeBackground,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(start = 14.dp, end = 4.dp),
            ) {
                Text(
                    text = language.ifBlank { "code" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                )
                IconButton(onClick = { clipboard.setText(AnnotatedString(code)) }) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = "Copy code",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
            Text(
                text = highlighted,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(14.dp),
            )
        }
    }
}
