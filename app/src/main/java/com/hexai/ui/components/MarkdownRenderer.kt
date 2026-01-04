package com.hexai.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.hexai.ui.theme.*

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = TextPrimary
) {
    val segments = remember(text) { parseContent(text) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        segments.forEach { segment ->
            when (segment) {
                is ContentSegment.Header -> {
                    HeaderView(level = segment.level, content = segment.content)
                }
                is ContentSegment.Text -> {
                    if (segment.content.isNotBlank()) {
                        Text(
                            text = parseInlineMarkdown(segment.content, textColor),
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor
                        )
                    }
                }
                is ContentSegment.CodeBlock -> {
                    CodeBlockView(code = segment.code, language = segment.language)
                }
                is ContentSegment.LatexBlock -> {
                    LatexView(latex = segment.content, isBlock = true)
                }
                is ContentSegment.LatexInline -> {
                    LatexView(latex = segment.content, isBlock = false)
                }
                is ContentSegment.Table -> {
                    TableView(headers = segment.headers, rows = segment.rows)
                }
            }
        }
    }
}

@Composable
fun HeaderView(
    level: Int,
    content: String,
    modifier: Modifier = Modifier
) {
    val (fontSize, color) = when (level) {
        1 -> 24.sp to NeonCyan
        2 -> 20.sp to NeonMagenta
        3 -> 17.sp to NeonPurple
        4 -> 15.sp to NeonBlue
        else -> 14.sp to NeonGreen
    }

    Text(
        text = content,
        modifier = modifier.padding(vertical = 4.dp),
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
        color = color,
        fontFamily = FontFamily.Monospace
    )
}

@Composable
private fun parseInlineMarkdown(text: String, textColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        val len = text.length

        while (i < len) {
            // Inline LaTeX \( ... \)
            if (i + 1 < len && text[i] == '\\' && text[i + 1] == '(') {
                val endIdx = text.indexOf("\\)", i + 2)
                if (endIdx > i) {
                    val latex = text.substring(i + 2, endIdx)
                    withStyle(SpanStyle(
                        color = NeonPurple,
                        fontFamily = FontFamily.Monospace,
                        background = DarkCard
                    )) {
                        append(" ${formatLatex(latex)} ")
                    }
                    i = endIdx + 2
                    continue
                }
            }

            // Inline LaTeX $...$ (but not $$)
            if (text[i] == '$' && (i + 1 >= len || text[i + 1] != '$')) {
                val endIdx = text.indexOf('$', i + 1)
                if (endIdx > i) {
                    val latex = text.substring(i + 1, endIdx)
                    withStyle(SpanStyle(
                        color = NeonPurple,
                        fontFamily = FontFamily.Monospace,
                        background = DarkCard
                    )) {
                        append(" ${formatLatex(latex)} ")
                    }
                    i = endIdx + 1
                    continue
                }
            }

            // Bold **text**
            if (i + 1 < len && text[i] == '*' && text[i + 1] == '*') {
                val endIdx = text.indexOf("**", i + 2)
                if (endIdx > i) {
                    val content = text.substring(i + 2, endIdx)
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = textColor)) {
                        append(content)
                    }
                    i = endIdx + 2
                    continue
                }
            }

            // Italic *text* (single asterisk, not part of **)
            if (text[i] == '*' && (i == 0 || text[i - 1] != '*') && (i + 1 >= len || text[i + 1] != '*')) {
                val endIdx = findSingleChar(text, '*', i + 1)
                if (endIdx > i) {
                    val content = text.substring(i + 1, endIdx)
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = textColor)) {
                        append(content)
                    }
                    i = endIdx + 1
                    continue
                }
            }

            // Strikethrough ~~text~~
            if (i + 1 < len && text[i] == '~' && text[i + 1] == '~') {
                val endIdx = text.indexOf("~~", i + 2)
                if (endIdx > i) {
                    val content = text.substring(i + 2, endIdx)
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = textColor)) {
                        append(content)
                    }
                    i = endIdx + 2
                    continue
                }
            }

            // Inline code `code`
            if (text[i] == '`' && (i + 1 >= len || text[i + 1] != '`')) {
                val endIdx = text.indexOf('`', i + 1)
                if (endIdx > i) {
                    val code = text.substring(i + 1, endIdx)
                    withStyle(SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = DarkCard,
                        color = NeonMagenta
                    )) {
                        append(" $code ")
                    }
                    i = endIdx + 1
                    continue
                }
            }

            // Regular character
            withStyle(SpanStyle(color = textColor)) {
                append(text[i])
            }
            i++
        }
    }
}

private fun findSingleChar(text: String, char: Char, startIndex: Int): Int {
    var i = startIndex
    while (i < text.length) {
        if (text[i] == char) {
            // Make sure it's not a double
            if (i + 1 < text.length && text[i + 1] == char) {
                i += 2
                continue
            }
            return i
        }
        i++
    }
    return -1
}

@Composable
fun CodeBlockView(
    code: String,
    language: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        color = DarkCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
    ) {
        Column {
            // Header with language and copy button
            Surface(
                color = DarkSurfaceVariant,
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = language?.uppercase() ?: "CODE",
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan
                    )
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("code", code)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy code",
                            modifier = Modifier.size(14.dp),
                            tint = CyberGray300
                        )
                    }
                }
            }

            SelectionContainer {
                Box(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
                    Text(
                        text = highlightSyntax(code, language),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun LatexView(
    latex: String,
    isBlock: Boolean,
    modifier: Modifier = Modifier
) {
    val formattedLatex = remember(latex) { formatLatex(latex) }

    if (isBlock) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(4.dp),
            color = DarkCard.copy(alpha = 0.7f),
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple.copy(alpha = 0.3f))
        ) {
            SelectionContainer {
                Text(
                    text = formattedLatex,
                    modifier = Modifier.padding(12.dp),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = NeonPurple,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    } else {
        Text(
            text = formattedLatex,
            modifier = modifier,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            color = NeonPurple,
            fontWeight = FontWeight.Medium
        )
    }
}

// Content segment types
sealed class ContentSegment {
    data class Header(val level: Int, val content: String) : ContentSegment()
    data class Text(val content: String) : ContentSegment()
    data class CodeBlock(val code: String, val language: String?) : ContentSegment()
    data class LatexBlock(val content: String) : ContentSegment()
    data class LatexInline(val content: String) : ContentSegment()
    data class Table(val headers: List<String>, val rows: List<List<String>>) : ContentSegment()
}

// Parse content into segments
private fun parseContent(text: String): List<ContentSegment> {
    val segments = mutableListOf<ContentSegment>()
    val lines = text.split("\n")
    var i = 0

    while (i < lines.size) {
        val line = lines[i]
        val trimmedLine = line.trim()

        // Code block ```
        if (trimmedLine.startsWith("```")) {
            val language = trimmedLine.removePrefix("```").trim().takeIf { it.isNotEmpty() }
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            segments.add(ContentSegment.CodeBlock(codeLines.joinToString("\n"), language))
            i++
            continue
        }

        // LaTeX block \[...\] (display math)
        if (trimmedLine.startsWith("\\[")) {
            val latexLines = mutableListOf<String>()
            val firstLine = trimmedLine.removePrefix("\\[").trim()
            if (firstLine.endsWith("\\]")) {
                // Single line \[...\]
                segments.add(ContentSegment.LatexBlock(firstLine.removeSuffix("\\]").trim()))
                i++
                continue
            }
            if (firstLine.isNotEmpty()) latexLines.add(firstLine)
            i++
            while (i < lines.size && !lines[i].trim().endsWith("\\]") && !lines[i].trim().startsWith("\\]")) {
                latexLines.add(lines[i])
                i++
            }
            if (i < lines.size) {
                val lastLine = lines[i].trim()
                if (lastLine == "\\]") {
                    // Just the closing delimiter
                } else {
                    latexLines.add(lastLine.removeSuffix("\\]").removePrefix("\\]"))
                }
            }
            segments.add(ContentSegment.LatexBlock(latexLines.joinToString("\n").trim()))
            i++
            continue
        }

        // LaTeX block $$...$$
        if (trimmedLine.startsWith("$$")) {
            val latexLines = mutableListOf<String>()
            val firstLine = trimmedLine.removePrefix("$$")
            if (firstLine.endsWith("$$") && firstLine.length > 2) {
                // Single line $$...$$
                segments.add(ContentSegment.LatexBlock(firstLine.removeSuffix("$$").trim()))
                i++
                continue
            }
            if (firstLine.isNotEmpty() && firstLine != "$$") latexLines.add(firstLine)
            i++
            while (i < lines.size && !lines[i].trim().endsWith("$$") && !lines[i].trim().startsWith("$$")) {
                latexLines.add(lines[i])
                i++
            }
            if (i < lines.size) {
                val lastLine = lines[i].trim()
                if (lastLine == "$$") {
                    // Just the closing delimiter
                } else {
                    latexLines.add(lastLine.removeSuffix("$$"))
                }
            }
            segments.add(ContentSegment.LatexBlock(latexLines.joinToString("\n").trim()))
            i++
            continue
        }

        // Table detection (line starts with | and contains |)
        if (trimmedLine.startsWith("|") && trimmedLine.contains("|")) {
            val tableLines = mutableListOf<String>()
            while (i < lines.size && lines[i].trim().let { it.startsWith("|") || it.contains("|---") || it.contains("| ---") }) {
                tableLines.add(lines[i].trim())
                i++
            }
            if (tableLines.size >= 2) {
                val table = parseTable(tableLines)
                if (table != null) {
                    segments.add(table)
                    continue
                }
            }
            // If not a valid table, treat as text
            i -= tableLines.size
        }

        // Header # ## ### etc.
        val headerMatch = Regex("^(#{1,6})\\s+(.+)$").find(line)
        if (headerMatch != null) {
            val level = headerMatch.groupValues[1].length
            val content = headerMatch.groupValues[2]
            segments.add(ContentSegment.Header(level, content))
            i++
            continue
        }

        // Regular text line (may contain inline LaTeX)
        if (line.isNotEmpty() || (i + 1 < lines.size && lines[i + 1].isNotEmpty())) {
            // Collect consecutive text lines until we hit a block element
            val textLines = mutableListOf<String>()
            while (i < lines.size) {
                val currentLine = lines[i]
                val currentTrimmed = currentLine.trim()
                // Check for block elements
                if (currentTrimmed.startsWith("```") ||
                    currentTrimmed.startsWith("$$") ||
                    currentTrimmed.startsWith("\\[") ||
                    Regex("^#{1,6}\\s+").find(currentLine) != null) {
                    break
                }
                textLines.add(currentLine)
                i++
            }
            if (textLines.isNotEmpty()) {
                segments.add(ContentSegment.Text(textLines.joinToString("\n")))
            }
            continue
        }

        i++
    }

    return segments
}

// Simple syntax highlighting
@Composable
private fun highlightSyntax(code: String, language: String?): AnnotatedString {
    return buildAnnotatedString {
        val keywords = when (language?.lowercase()) {
            "kotlin", "java" -> listOf(
                "fun", "val", "var", "class", "interface", "object", "if", "else", "when",
                "for", "while", "return", "import", "package", "private", "public", "protected",
                "override", "suspend", "data", "sealed", "enum", "companion", "null", "true", "false"
            )
            "python" -> listOf(
                "def", "class", "if", "else", "elif", "for", "while", "return", "import", "from",
                "as", "try", "except", "finally", "with", "lambda", "yield", "None", "True", "False"
            )
            "javascript", "typescript", "js", "ts" -> listOf(
                "function", "const", "let", "var", "if", "else", "for", "while", "return",
                "import", "export", "from", "class", "extends", "async", "await", "null",
                "undefined", "true", "false", "new", "this"
            )
            "rust" -> listOf(
                "fn", "let", "mut", "const", "if", "else", "match", "for", "while", "loop",
                "return", "use", "mod", "pub", "struct", "enum", "impl", "trait", "self",
                "Self", "true", "false", "None", "Some"
            )
            "go" -> listOf(
                "func", "var", "const", "if", "else", "for", "range", "return", "import",
                "package", "type", "struct", "interface", "map", "chan", "go", "defer",
                "nil", "true", "false"
            )
            "c", "cpp", "c++" -> listOf(
                "int", "char", "float", "double", "void", "if", "else", "for", "while",
                "return", "include", "define", "struct", "class", "public", "private",
                "const", "static", "nullptr", "true", "false"
            )
            else -> emptyList()
        }

        val lines = code.split("\n")
        lines.forEachIndexed { lineIndex, line ->
            var idx = 0
            while (idx < line.length) {
                if (line[idx] == '"' || line[idx] == '\'') {
                    val quote = line[idx]
                    val start = idx
                    idx++
                    while (idx < line.length && line[idx] != quote) {
                        if (line[idx] == '\\' && idx + 1 < line.length) idx++
                        idx++
                    }
                    if (idx < line.length) idx++
                    withStyle(SpanStyle(color = NeonGreen)) {
                        append(line.substring(start, idx))
                    }
                    continue
                }

                if (idx + 1 < line.length && line.substring(idx, idx + 2) == "//") {
                    withStyle(SpanStyle(color = CyberGray300)) {
                        append(line.substring(idx))
                    }
                    break
                }

                if (language?.lowercase() == "python" && line[idx] == '#') {
                    withStyle(SpanStyle(color = CyberGray300)) {
                        append(line.substring(idx))
                    }
                    break
                }

                if (line[idx].isDigit()) {
                    val start = idx
                    while (idx < line.length && (line[idx].isDigit() || line[idx] == '.' || line[idx] == 'x' || line[idx] in 'a'..'f' || line[idx] in 'A'..'F')) idx++
                    withStyle(SpanStyle(color = NeonOrange)) {
                        append(line.substring(start, idx))
                    }
                    continue
                }

                if (line[idx].isLetter() || line[idx] == '_') {
                    val start = idx
                    while (idx < line.length && (line[idx].isLetterOrDigit() || line[idx] == '_')) idx++
                    val word = line.substring(start, idx)
                    if (word in keywords) {
                        withStyle(SpanStyle(color = NeonMagenta, fontWeight = FontWeight.Bold)) {
                            append(word)
                        }
                    } else {
                        withStyle(SpanStyle(color = TextPrimary)) {
                            append(word)
                        }
                    }
                    continue
                }

                if (line[idx] in "{}[]().,;:+-*/<>=!&|^~%@") {
                    withStyle(SpanStyle(color = NeonCyan)) {
                        append(line[idx])
                    }
                    idx++
                    continue
                }

                withStyle(SpanStyle(color = TextPrimary)) {
                    append(line[idx])
                }
                idx++
            }

            if (lineIndex < lines.lastIndex) {
                append("\n")
            }
        }
    }
}

// Format LaTeX for display with Unicode symbols
private fun formatLatex(latex: String): String {
    var result = latex

    // Handle \| for norms first (before other processing)
    result = result.replace("\\|", "‖")

    // Handle fractions first (before removing braces)
    val fracRegex = Regex("""\\frac\s*\{([^{}]*(?:\{[^{}]*\}[^{}]*)*)\}\s*\{([^{}]*(?:\{[^{}]*\}[^{}]*)*)\}""")
    while (fracRegex.containsMatchIn(result)) {
        result = fracRegex.replace(result) { match ->
            val num = match.groupValues[1]
            val denom = match.groupValues[2]
            "($num)/($denom)"
        }
    }

    // Handle \vec{} for vectors
    result = result.replace(Regex("""\\vec\s*\{([^}]*)\}""")) { match ->
        "${match.groupValues[1]}\u20D7" // combining arrow above
    }

    // Handle subscripts with braces: _{...} -> subscript
    result = result.replace(Regex("""_\{([^}]+)\}""")) { match ->
        val subscript = match.groupValues[1]
        "₍${toSubscript(subscript)}₎"
    }

    // Handle superscripts with braces: ^{...} -> superscript
    result = result.replace(Regex("""\^\{([^}]+)\}""")) { match ->
        val superscript = match.groupValues[1]
        toSuperscript(superscript)
    }

    // Handle simple subscripts: _x -> subscript (single char)
    result = result.replace(Regex("""_([a-zA-Z0-9])(?![a-zA-Z0-9])""")) { match ->
        toSubscript(match.groupValues[1])
    }

    // Handle simple superscripts: ^x -> superscript (single char)
    result = result.replace(Regex("""\^([a-zA-Z0-9])(?![a-zA-Z0-9])""")) { match ->
        toSuperscript(match.groupValues[1])
    }

    // Handle norms ||x|| (double vertical bars)
    result = result.replace("||", "‖")

    return result
        // Square roots
        .replace(Regex("""\\sqrt\s*\{([^}]*)\}""")) { "√(${it.groupValues[1]})" }
        .replace("\\sqrt", "√")

        // Summation and products
        .replace("\\sum", "Σ")
        .replace("\\prod", "Π")
        .replace("\\int", "∫")
        .replace("\\infty", "∞")

        // Greek letters (lowercase)
        .replace("\\alpha", "α")
        .replace("\\beta", "β")
        .replace("\\gamma", "γ")
        .replace("\\delta", "δ")
        .replace("\\epsilon", "ε")
        .replace("\\varepsilon", "ε")
        .replace("\\zeta", "ζ")
        .replace("\\eta", "η")
        .replace("\\theta", "θ")
        .replace("\\iota", "ι")
        .replace("\\kappa", "κ")
        .replace("\\lambda", "λ")
        .replace("\\mu", "μ")
        .replace("\\nu", "ν")
        .replace("\\xi", "ξ")
        .replace("\\pi", "π")
        .replace("\\rho", "ρ")
        .replace("\\sigma", "σ")
        .replace("\\tau", "τ")
        .replace("\\upsilon", "υ")
        .replace("\\phi", "φ")
        .replace("\\varphi", "φ")
        .replace("\\chi", "χ")
        .replace("\\psi", "ψ")
        .replace("\\omega", "ω")

        // Greek letters (uppercase)
        .replace("\\Gamma", "Γ")
        .replace("\\Delta", "Δ")
        .replace("\\Theta", "Θ")
        .replace("\\Lambda", "Λ")
        .replace("\\Xi", "Ξ")
        .replace("\\Pi", "Π")
        .replace("\\Sigma", "Σ")
        .replace("\\Upsilon", "Υ")
        .replace("\\Phi", "Φ")
        .replace("\\Psi", "Ψ")
        .replace("\\Omega", "Ω")

        // Comparison operators
        .replace("\\neq", "≠")
        .replace("\\ne", "≠")
        .replace("\\leq", "≤")
        .replace("\\le", "≤")
        .replace("\\geq", "≥")
        .replace("\\ge", "≥")
        .replace("\\approx", "≈")
        .replace("\\equiv", "≡")
        .replace("\\sim", "∼")
        .replace("\\propto", "∝")

        // Arithmetic operators
        .replace("\\times", "×")
        .replace("\\div", "÷")
        .replace("\\cdot", "·")
        .replace("\\pm", "±")
        .replace("\\mp", "∓")
        .replace("\\ast", "∗")
        .replace("\\star", "⋆")
        .replace("\\circ", "∘")
        .replace("\\bullet", "•")

        // Arrows
        .replace("\\rightarrow", "→")
        .replace("\\to", "→")
        .replace("\\leftarrow", "←")
        .replace("\\gets", "←")
        .replace("\\leftrightarrow", "↔")
        .replace("\\Rightarrow", "⇒")
        .replace("\\Leftarrow", "⇐")
        .replace("\\Leftrightarrow", "⇔")
        .replace("\\uparrow", "↑")
        .replace("\\downarrow", "↓")
        .replace("\\mapsto", "↦")

        // Set theory
        .replace("\\subset", "⊂")
        .replace("\\supset", "⊃")
        .replace("\\subseteq", "⊆")
        .replace("\\supseteq", "⊇")
        .replace("\\in", "∈")
        .replace("\\notin", "∉")
        .replace("\\cup", "∪")
        .replace("\\cap", "∩")
        .replace("\\emptyset", "∅")
        .replace("\\varnothing", "∅")

        // Logic
        .replace("\\forall", "∀")
        .replace("\\exists", "∃")
        .replace("\\nexists", "∄")
        .replace("\\neg", "¬")
        .replace("\\land", "∧")
        .replace("\\lor", "∨")
        .replace("\\wedge", "∧")
        .replace("\\vee", "∨")

        // Calculus
        .replace("\\partial", "∂")
        .replace("\\nabla", "∇")
        .replace("\\prime", "′")

        // Functions - preserve text
        .replace("\\log", "log")
        .replace("\\ln", "ln")
        .replace("\\sin", "sin")
        .replace("\\cos", "cos")
        .replace("\\tan", "tan")
        .replace("\\cot", "cot")
        .replace("\\sec", "sec")
        .replace("\\csc", "csc")
        .replace("\\arcsin", "arcsin")
        .replace("\\arccos", "arccos")
        .replace("\\arctan", "arctan")
        .replace("\\sinh", "sinh")
        .replace("\\cosh", "cosh")
        .replace("\\tanh", "tanh")
        .replace("\\exp", "exp")
        .replace("\\lim", "lim")
        .replace("\\max", "max")
        .replace("\\min", "min")
        .replace("\\sup", "sup")
        .replace("\\inf", "inf")
        .replace("\\det", "det")
        .replace("\\dim", "dim")
        .replace("\\ker", "ker")
        .replace("\\arg", "arg")

        // Misc symbols
        .replace("\\ldots", "…")
        .replace("\\cdots", "⋯")
        .replace("\\vdots", "⋮")
        .replace("\\ddots", "⋱")
        .replace("\\degree", "°")
        .replace("\\angle", "∠")
        .replace("\\perp", "⊥")
        .replace("\\parallel", "∥")
        .replace("\\triangle", "△")
        .replace("\\square", "□")
        .replace("\\diamond", "◇")
        .replace("\\clubsuit", "♣")
        .replace("\\diamondsuit", "♦")
        .replace("\\heartsuit", "♥")
        .replace("\\spadesuit", "♠")

        // Subscripts and superscripts - show with indicators
        .replace("^{", "^(")
        .replace("_{", "_(")

        // Remove formatting commands but keep content
        .replace(Regex("""\\text\s*\{([^}]*)\}""")) { it.groupValues[1] }
        .replace(Regex("""\\textbf\s*\{([^}]*)\}""")) { it.groupValues[1] }
        .replace(Regex("""\\textit\s*\{([^}]*)\}""")) { it.groupValues[1] }
        .replace(Regex("""\\mathrm\s*\{([^}]*)\}""")) { it.groupValues[1] }
        .replace(Regex("""\\mathbf\s*\{([^}]*)\}""")) { it.groupValues[1] }
        .replace(Regex("""\\mathit\s*\{([^}]*)\}""")) { it.groupValues[1] }
        .replace(Regex("""\\mathcal\s*\{([^}]*)\}""")) { it.groupValues[1] }
        .replace(Regex("""\\mathbb\s*\{([^}]*)\}""")) { it.groupValues[1] }
        .replace(Regex("""\\boldsymbol\s*\{([^}]*)\}""")) { it.groupValues[1] }

        // Remove remaining LaTeX commands
        .replace("\\left", "")
        .replace("\\right", "")
        .replace("\\bigl", "")
        .replace("\\bigr", "")
        .replace("\\Bigl", "")
        .replace("\\Bigr", "")
        .replace("\\biggl", "")
        .replace("\\biggr", "")
        .replace("\\quad", " ")
        .replace("\\qquad", "  ")
        .replace("\\,", " ")
        .replace("\\;", " ")
        .replace("\\:", " ")
        .replace("\\ ", " ")

        // Clean up braces last
        .replace("{", "(")
        .replace("}", ")")

        // Remove any remaining backslashes
        .replace("\\", "")

        // Clean up extra spaces
        .replace(Regex("""\s+"""), " ")
        .trim()
}

// Convert text to Unicode subscript characters
private fun toSubscript(text: String): String {
    val subscriptMap = mapOf(
        '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄',
        '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉',
        'a' to 'ₐ', 'e' to 'ₑ', 'h' to 'ₕ', 'i' to 'ᵢ', 'j' to 'ⱼ',
        'k' to 'ₖ', 'l' to 'ₗ', 'm' to 'ₘ', 'n' to 'ₙ', 'o' to 'ₒ',
        'p' to 'ₚ', 'r' to 'ᵣ', 's' to 'ₛ', 't' to 'ₜ', 'u' to 'ᵤ',
        'v' to 'ᵥ', 'x' to 'ₓ', '+' to '₊', '-' to '₋', '=' to '₌',
        '(' to '₍', ')' to '₎'
    )
    return text.map { subscriptMap[it.lowercaseChar()] ?: it }.joinToString("")
}

// Convert text to Unicode superscript characters
private fun toSuperscript(text: String): String {
    val superscriptMap = mapOf(
        '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
        '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
        'a' to 'ᵃ', 'b' to 'ᵇ', 'c' to 'ᶜ', 'd' to 'ᵈ', 'e' to 'ᵉ',
        'f' to 'ᶠ', 'g' to 'ᵍ', 'h' to 'ʰ', 'i' to 'ⁱ', 'j' to 'ʲ',
        'k' to 'ᵏ', 'l' to 'ˡ', 'm' to 'ᵐ', 'n' to 'ⁿ', 'o' to 'ᵒ',
        'p' to 'ᵖ', 'r' to 'ʳ', 's' to 'ˢ', 't' to 'ᵗ', 'u' to 'ᵘ',
        'v' to 'ᵛ', 'w' to 'ʷ', 'x' to 'ˣ', 'y' to 'ʸ', 'z' to 'ᶻ',
        '+' to '⁺', '-' to '⁻', '=' to '⁼', '(' to '⁽', ')' to '⁾'
    )
    return text.map { superscriptMap[it.lowercaseChar()] ?: it }.joinToString("")
}

// Parse markdown table
private fun parseTable(lines: List<String>): ContentSegment.Table? {
    if (lines.size < 2) return null

    // Parse cells from a table row
    fun parseRow(line: String): List<String> {
        return line.trim()
            .removePrefix("|")
            .removeSuffix("|")
            .split("|")
            .map { it.trim() }
    }

    // Check if line is a separator (|---|---|)
    fun isSeparator(line: String): Boolean {
        val cells = parseRow(line)
        return cells.all { cell ->
            cell.replace("-", "").replace(":", "").isBlank()
        }
    }

    // First line should be headers
    val headers = parseRow(lines[0])
    if (headers.isEmpty()) return null

    // Second line should be separator
    if (lines.size < 2 || !isSeparator(lines[1])) return null

    // Rest are data rows
    val rows = lines.drop(2).map { parseRow(it) }

    return ContentSegment.Table(headers, rows)
}

// Table rendering
@Composable
fun TableView(
    headers: List<String>,
    rows: List<List<String>>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        color = DarkCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
    ) {
        Column {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurfaceVariant)
                    .padding(8.dp)
            ) {
                headers.forEachIndexed { index, header ->
                    Text(
                        text = header,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelMedium,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                    if (index < headers.size - 1) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }

            // Divider
            HorizontalDivider(color = NeonCyan.copy(alpha = 0.3f), thickness = 1.dp)

            // Data rows
            rows.forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (rowIndex % 2 == 0) DarkCard
                            else DarkCard.copy(alpha = 0.7f)
                        )
                        .padding(8.dp)
                ) {
                    row.forEachIndexed { cellIndex, cell ->
                        Text(
                            text = cell,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                        if (cellIndex < row.size - 1) {
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                    // Fill empty cells if row has fewer columns than headers
                    repeat(headers.size - row.size) {
                        Text(
                            text = "",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                    }
                }

                if (rowIndex < rows.size - 1) {
                    HorizontalDivider(color = CyberGray500.copy(alpha = 0.3f), thickness = 0.5.dp)
                }
            }
        }
    }
}
