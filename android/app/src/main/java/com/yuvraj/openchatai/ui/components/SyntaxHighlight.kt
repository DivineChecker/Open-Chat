package com.yuvraj.openchatai.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle

/**
 * Lightweight syntax highlighter for fenced code blocks. Sequentially tokenizes
 * comments, strings, numbers, annotations and identifiers and colors them with a
 * One Dark-inspired palette. Unstyled text inherits the surrounding Text color.
 */
object SyntaxHighlight {

    private val keywordStyle = SpanStyle(color = Color(0xFFC678DD))
    private val literalStyle = SpanStyle(color = Color(0xFFD19A66))
    private val stringStyle = SpanStyle(color = Color(0xFF98C379))
    private val commentStyle = SpanStyle(color = Color(0xFF7F848E), fontStyle = FontStyle.Italic)
    private val numberStyle = SpanStyle(color = Color(0xFFD19A66))
    private val functionStyle = SpanStyle(color = Color(0xFF61AFEF))
    private val typeStyle = SpanStyle(color = Color(0xFFE5C07B))
    private val annotationStyle = SpanStyle(color = Color(0xFFE5C07B))
    private val tagStyle = SpanStyle(color = Color(0xFFE06C75))
    private val propertyStyle = SpanStyle(color = Color(0xFFE06C75))
    private val attributeStyle = SpanStyle(color = Color(0xFFD19A66))

    private data class LangConfig(
        val keywords: Set<String>,
        val literals: Set<String> = setOf("true", "false", "null"),
        val lineComments: List<String> = listOf("//"),
        val blockComment: Pair<String, String>? = "/*" to "*/",
        val caseInsensitive: Boolean = false,
        val isMarkup: Boolean = false,
    )

    private val kotlinConfig = LangConfig(
        keywords = setOf(
            "fun", "val", "var", "if", "else", "when", "for", "while", "do", "return", "class",
            "object", "interface", "data", "sealed", "enum", "companion", "init", "this", "super",
            "is", "in", "as", "package", "import", "try", "catch", "finally", "throw", "typealias",
            "suspend", "override", "open", "private", "public", "protected", "internal", "lateinit",
            "by", "lazy", "it", "out", "vararg", "where", "constructor", "get", "set", "abstract",
            "final", "annotation", "inline", "noinline", "crossinline", "reified", "operator",
            "infix", "tailrec", "external", "const", "break", "continue",
        ),
    )

    private val javaConfig = LangConfig(
        keywords = setOf(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
            "finally", "float", "for", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
            "throw", "throws", "transient", "try", "void", "volatile", "while", "var", "record",
            "sealed", "permits", "yield",
        ),
    )

    private val pythonConfig = LangConfig(
        keywords = setOf(
            "and", "as", "assert", "async", "await", "break", "class", "continue", "def", "del",
            "elif", "else", "except", "finally", "for", "from", "global", "if", "import", "in",
            "is", "lambda", "nonlocal", "not", "or", "pass", "raise", "return", "try", "while",
            "with", "yield", "match", "case", "self",
        ),
        literals = setOf("True", "False", "None"),
        lineComments = listOf("#"),
        blockComment = null,
    )

    private val jsConfig = LangConfig(
        keywords = setOf(
            "abstract", "any", "as", "async", "await", "boolean", "break", "case", "catch",
            "class", "const", "continue", "debugger", "declare", "default", "delete", "do",
            "else", "enum", "export", "extends", "finally", "for", "from", "function", "get",
            "if", "implements", "import", "in", "instanceof", "interface", "let", "new",
            "number", "of", "package", "private", "protected", "public", "readonly", "return",
            "set", "static", "string", "super", "switch", "this", "throw", "try", "type",
            "typeof", "var", "void", "while", "with", "yield", "never", "unknown", "keyof",
            "namespace", "satisfies",
        ),
        literals = setOf("true", "false", "null", "undefined", "NaN", "Infinity"),
    )

    private val cLikeConfig = LangConfig(
        keywords = setOf(
            "auto", "break", "case", "char", "const", "continue", "default", "do", "double",
            "else", "enum", "extern", "float", "for", "goto", "if", "inline", "int", "long",
            "register", "return", "short", "signed", "sizeof", "static", "struct", "switch",
            "typedef", "union", "unsigned", "void", "volatile", "while", "class", "namespace",
            "new", "delete", "template", "typename", "using", "public", "private", "protected",
            "virtual", "override", "friend", "operator", "this", "bool", "string", "var",
            "async", "await", "foreach", "in", "is", "out", "ref", "readonly", "sealed",
            "abstract", "internal", "base", "interface", "event", "lock", "try", "catch",
            "finally", "throw",
        ),
        literals = setOf("true", "false", "null", "nullptr", "NULL"),
    )

    private val swiftConfig = LangConfig(
        keywords = setOf(
            "as", "associatedtype", "break", "case", "catch", "class", "continue", "default",
            "defer", "deinit", "do", "else", "enum", "extension", "fallthrough", "fileprivate",
            "final", "for", "func", "guard", "if", "import", "in", "indirect", "infix", "init",
            "inout", "internal", "is", "lazy", "let", "mutating", "open", "operator", "optional",
            "override", "postfix", "prefix", "private", "protocol", "public", "repeat",
            "required", "rethrows", "return", "self", "static", "struct", "subscript", "super",
            "switch", "throw", "throws", "try", "typealias", "unowned", "var", "weak", "where",
            "while", "actor", "async", "await", "some", "any",
        ),
        literals = setOf("true", "false", "nil"),
    )

    private val goConfig = LangConfig(
        keywords = setOf(
            "break", "case", "chan", "const", "continue", "default", "defer", "else",
            "fallthrough", "for", "func", "go", "goto", "if", "import", "interface", "map",
            "package", "range", "return", "select", "struct", "switch", "type", "var", "make",
            "new", "append", "len", "cap",
        ),
        literals = setOf("true", "false", "nil", "iota"),
    )

    private val rustConfig = LangConfig(
        keywords = setOf(
            "as", "async", "await", "break", "const", "continue", "crate", "dyn", "else",
            "enum", "extern", "fn", "for", "if", "impl", "in", "let", "loop", "match", "mod",
            "move", "mut", "pub", "ref", "return", "self", "Self", "static", "struct", "super",
            "trait", "type", "unsafe", "use", "where", "while",
        ),
        literals = setOf("true", "false", "None", "Some", "Ok", "Err"),
    )

    private val rubyConfig = LangConfig(
        keywords = setOf(
            "def", "end", "if", "elsif", "else", "unless", "while", "until", "for", "do",
            "begin", "rescue", "ensure", "class", "module", "return", "yield", "self", "and",
            "or", "not", "require", "include", "attr_accessor", "puts", "new", "then", "case",
            "when", "break", "next", "raise", "lambda", "proc",
        ),
        literals = setOf("true", "false", "nil"),
        lineComments = listOf("#"),
        blockComment = null,
    )

    private val sqlConfig = LangConfig(
        keywords = setOf(
            "select", "from", "where", "insert", "into", "values", "update", "delete", "set",
            "create", "table", "alter", "drop", "index", "view", "join", "inner", "left",
            "right", "outer", "on", "group", "by", "order", "having", "limit", "offset",
            "union", "all", "distinct", "as", "and", "or", "not", "primary", "key", "foreign",
            "references", "default", "check", "unique", "constraint", "between", "like", "in",
            "exists", "case", "when", "then", "else", "end", "count", "sum", "avg", "min",
            "max", "if", "database", "add", "column", "varchar", "int", "integer", "text",
            "boolean", "date", "timestamp",
        ),
        literals = setOf("true", "false", "null"),
        lineComments = listOf("--"),
        caseInsensitive = true,
    )

    private val bashConfig = LangConfig(
        keywords = setOf(
            "if", "then", "else", "elif", "fi", "for", "while", "do", "done", "case", "esac",
            "in", "function", "return", "exit", "echo", "local", "export", "readonly",
            "source", "set", "unset", "shift", "break", "continue", "until", "select",
            "declare", "eval", "exec", "printf", "read", "cd", "sudo",
        ),
        lineComments = listOf("#"),
        blockComment = null,
    )

    private val jsonConfig = LangConfig(
        keywords = emptySet(),
        lineComments = listOf("//"),
    )

    private val markupConfig = LangConfig(
        keywords = emptySet(),
        lineComments = emptyList(),
        blockComment = "<!--" to "-->",
        isMarkup = true,
    )

    private val cssConfig = LangConfig(
        keywords = setOf("important", "media", "keyframes", "import", "supports", "font-face"),
        lineComments = emptyList(),
    )

    private val hashConfig = LangConfig(
        keywords = emptySet(),
        lineComments = listOf("#"),
        blockComment = null,
    )

    private val genericConfig = LangConfig(
        keywords = emptySet(),
        lineComments = listOf("//", "#"),
    )

    private fun resolve(language: String): LangConfig = when (language) {
        "kotlin", "kt", "kts", "gradle" -> kotlinConfig
        "java" -> javaConfig
        "python", "py", "python3" -> pythonConfig
        "javascript", "js", "jsx", "typescript", "ts", "tsx", "node", "dart", "php" -> jsConfig
        "json", "jsonc", "json5" -> jsonConfig
        "html", "xml", "svg", "vue", "markup" -> markupConfig
        "css", "scss", "less" -> cssConfig
        "sql", "mysql", "postgres", "postgresql", "sqlite", "plsql" -> sqlConfig
        "bash", "sh", "shell", "zsh", "console", "terminal", "shellscript" -> bashConfig
        "c", "cpp", "c++", "h", "hpp", "cc", "csharp", "cs", "objc", "objective-c" -> cLikeConfig
        "swift" -> swiftConfig
        "go", "golang" -> goConfig
        "rust", "rs" -> rustConfig
        "ruby", "rb" -> rubyConfig
        "yaml", "yml", "toml", "ini", "properties", "dockerfile", "makefile", "r" -> hashConfig
        else -> genericConfig
    }

    private fun nextNonSpace(code: String, from: Int): Char? {
        var j = from
        while (j < code.length && code[j] == ' ') j++
        return code.getOrNull(j)
    }

    /** Produces a colored [AnnotatedString] for [code] in the given [language]. */
    fun highlight(code: String, language: String): AnnotatedString {
        val lang = resolve(language.trim().lowercase())
        return buildAnnotatedString {
            var i = 0
            val n = code.length
            var inTag = false
            while (i < n) {
                val c = code[i]

                val block = lang.blockComment
                if (block != null && code.startsWith(block.first, i)) {
                    val end = code.indexOf(block.second, i + block.first.length)
                    val stop = if (end == -1) n else end + block.second.length
                    withStyle(commentStyle) { append(code.substring(i, stop)) }
                    i = stop
                    continue
                }

                val lineComment = lang.lineComments.firstOrNull { code.startsWith(it, i) }
                if (lineComment != null) {
                    val eol = code.indexOf('\n', i).let { if (it == -1) n else it }
                    withStyle(commentStyle) { append(code.substring(i, eol)) }
                    i = eol
                    continue
                }

                if (c == '"' || c == '\'' || c == '`') {
                    val triple = i + 2 < n && code[i + 1] == c && code[i + 2] == c
                    val delim = if (triple) "$c$c$c" else c.toString()
                    var j = i + delim.length
                    while (j < n) {
                        if (code[j] == '\\') {
                            j += 2
                            continue
                        }
                        if (code.startsWith(delim, j)) {
                            j += delim.length
                            break
                        }
                        j++
                    }
                    val end = minOf(j, n)
                    val token = code.substring(i, end)
                    if (nextNonSpace(code, end) == ':') {
                        withStyle(propertyStyle) { append(token) }
                    } else {
                        withStyle(stringStyle) { append(token) }
                    }
                    i = end
                    continue
                }

                if (lang.isMarkup && c == '<') {
                    append('<')
                    i++
                    if (i < n && code[i] == '/') {
                        append('/')
                        i++
                    }
                    val start = i
                    while (i < n && (code[i].isLetterOrDigit() || code[i] == '-' || code[i] == ':')) i++
                    if (i > start) withStyle(tagStyle) { append(code.substring(start, i)) }
                    inTag = true
                    continue
                }
                if (lang.isMarkup && c == '>') {
                    append('>')
                    inTag = false
                    i++
                    continue
                }

                if (c.isDigit()) {
                    val prev = if (i > 0) code[i - 1] else ' '
                    if (!prev.isLetterOrDigit() && prev != '_') {
                        var j = i
                        while (j < n && (code[j].isLetterOrDigit() || code[j] == '.' || code[j] == '_')) j++
                        withStyle(numberStyle) { append(code.substring(i, j)) }
                        i = j
                        continue
                    }
                }

                if (c == '@' && i + 1 < n && code[i + 1].isLetter()) {
                    var j = i + 1
                    while (j < n && (code[j].isLetterOrDigit() || code[j] == '_' || code[j] == '.')) j++
                    withStyle(annotationStyle) { append(code.substring(i, j)) }
                    i = j
                    continue
                }

                if (c.isLetter() || c == '_') {
                    var j = i
                    while (j < n && (code[j].isLetterOrDigit() || code[j] == '_')) j++
                    val word = code.substring(i, j)
                    val cmp = if (lang.caseInsensitive) word.lowercase() else word
                    when {
                        inTag -> withStyle(attributeStyle) { append(word) }
                        cmp in lang.literals -> withStyle(literalStyle) { append(word) }
                        cmp in lang.keywords -> withStyle(keywordStyle) { append(word) }
                        nextNonSpace(code, j) == '(' -> withStyle(functionStyle) { append(word) }
                        word.first().isUpperCase() && word.length > 1 -> withStyle(typeStyle) { append(word) }
                        else -> append(word)
                    }
                    i = j
                    continue
                }

                append(c)
                i++
            }
        }
    }
}
