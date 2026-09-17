package com.yuvraj.openchatai.ui.components

/**
 * Converts LaTeX math notation (\( \), \[ \], $ $, $$ $$, \frac, \sqrt, ^, _,
 * greek letters, operators) into readable Unicode text so AI responses with
 * math render cleanly instead of showing raw malformed LaTeX.
 */
object LatexMath {

    private val symbols: Map<String, String> = mapOf(
        "times" to "×", "cdot" to "·", "div" to "÷", "pm" to "±", "mp" to "∓",
        "leq" to "≤", "le" to "≤", "geq" to "≥", "ge" to "≥",
        "neq" to "≠", "ne" to "≠", "approx" to "≈", "equiv" to "≡", "sim" to "∼", "propto" to "∝",
        "infty" to "∞", "partial" to "∂", "nabla" to "∇", "degree" to "°", "circ" to "°",
        "to" to "→", "rightarrow" to "→", "leftarrow" to "←",
        "Rightarrow" to "⇒", "Leftarrow" to "⇐", "leftrightarrow" to "↔", "Leftrightarrow" to "⇔",
        "mapsto" to "↦", "implies" to "⇒", "iff" to "⇔",
        "sum" to "∑", "prod" to "∏", "int" to "∫", "oint" to "∮",
        "cup" to "∪", "cap" to "∩", "subset" to "⊂", "supset" to "⊃",
        "subseteq" to "⊆", "supseteq" to "⊇", "in" to "∈", "notin" to "∉",
        "forall" to "∀", "exists" to "∃", "emptyset" to "∅", "varnothing" to "∅",
        "land" to "∧", "wedge" to "∧", "lor" to "∨", "vee" to "∨", "neg" to "¬", "lnot" to "¬",
        "angle" to "∠", "perp" to "⊥", "parallel" to "∥", "triangle" to "△",
        "ldots" to "…", "cdots" to "⋯", "dots" to "…", "dotsc" to "…", "dotsb" to "⋯",
        "alpha" to "α", "beta" to "β", "gamma" to "γ", "delta" to "δ",
        "epsilon" to "ε", "varepsilon" to "ε", "zeta" to "ζ", "eta" to "η",
        "theta" to "θ", "vartheta" to "ϑ", "iota" to "ι", "kappa" to "κ",
        "lambda" to "λ", "mu" to "μ", "nu" to "ν", "xi" to "ξ", "pi" to "π", "varpi" to "ϖ",
        "rho" to "ρ", "sigma" to "σ", "tau" to "τ", "upsilon" to "υ",
        "phi" to "φ", "varphi" to "φ", "chi" to "χ", "psi" to "ψ", "omega" to "ω",
        "Gamma" to "Γ", "Delta" to "Δ", "Theta" to "Θ", "Lambda" to "Λ", "Xi" to "Ξ",
        "Pi" to "Π", "Sigma" to "Σ", "Upsilon" to "Υ", "Phi" to "Φ", "Psi" to "Ψ", "Omega" to "Ω",
        "prime" to "′", "hbar" to "ℏ", "ell" to "ℓ", "Re" to "ℜ", "Im" to "ℑ",
        "quad" to "  ", "qquad" to "    ",
    )

    private val functionNames: Set<String> = setOf(
        "sin", "cos", "tan", "cot", "sec", "csc", "arcsin", "arccos", "arctan",
        "sinh", "cosh", "tanh", "log", "ln", "lg", "exp", "lim", "min", "max",
        "det", "gcd", "deg", "dim", "mod", "arg", "inf", "sup",
    )

    private val superscripts: Map<Char, Char> = mapOf(
        '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
        '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
        '+' to '⁺', '-' to '⁻', '=' to '⁼', '(' to '⁽', ')' to '⁾',
        'a' to 'ᵃ', 'b' to 'ᵇ', 'c' to 'ᶜ', 'd' to 'ᵈ', 'e' to 'ᵉ', 'f' to 'ᶠ',
        'g' to 'ᵍ', 'h' to 'ʰ', 'i' to 'ⁱ', 'j' to 'ʲ', 'k' to 'ᵏ', 'l' to 'ˡ',
        'm' to 'ᵐ', 'n' to 'ⁿ', 'o' to 'ᵒ', 'p' to 'ᵖ', 'r' to 'ʳ', 's' to 'ˢ',
        't' to 'ᵗ', 'u' to 'ᵘ', 'v' to 'ᵛ', 'w' to 'ʷ', 'x' to 'ˣ', 'y' to 'ʸ', 'z' to 'ᶻ',
        'T' to 'ᵀ',
    )

    private val subscripts: Map<Char, Char> = mapOf(
        '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄',
        '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉',
        '+' to '₊', '-' to '₋', '=' to '₌', '(' to '₍', ')' to '₎',
        'a' to 'ₐ', 'e' to 'ₑ', 'h' to 'ₕ', 'i' to 'ᵢ', 'j' to 'ⱼ', 'k' to 'ₖ',
        'l' to 'ₗ', 'm' to 'ₘ', 'n' to 'ₙ', 'o' to 'ₒ', 'p' to 'ₚ', 'r' to 'ᵣ',
        's' to 'ₛ', 't' to 'ₜ', 'u' to 'ᵤ', 'v' to 'ᵥ', 'x' to 'ₓ',
    )

    private val displayBracketRegex = Regex("\\\\\\[(.+?)\\\\]", RegexOption.DOT_MATCHES_ALL)
    private val displayDollarRegex = Regex("\\$\\$(.+?)\\$\\$", RegexOption.DOT_MATCHES_ALL)
    private val inlineParenRegex = Regex("\\\\\\((.+?)\\\\\\)", RegexOption.DOT_MATCHES_ALL)
    private val inlineDollarRegex = Regex("\\$([^$\\n]{1,160}?)\\$")

    /**
     * Normalizes all LaTeX math in a markdown string to Unicode.
     * Content inside fenced code blocks is left untouched.
     */
    fun normalize(text: String): String {
        if (!text.contains('\\') && !text.contains('$')) return text
        val parts = text.split("```")
        return parts.mapIndexed { index, part ->
            if (index % 2 == 1) part else normalizeSegment(part)
        }.joinToString("```")
    }

    private fun normalizeSegment(segment: String): String {
        var s = segment
        s = displayDollarRegex.replace(s) { m -> "\n\n" + convert(m.groupValues[1].trim()) + "\n\n" }
        s = displayBracketRegex.replace(s) { m -> "\n\n" + convert(m.groupValues[1].trim()) + "\n\n" }
        s = inlineParenRegex.replace(s) { m -> convert(m.groupValues[1].trim()) }
        s = inlineDollarRegex.replace(s) { m ->
            val body = m.groupValues[1]
            if (looksLikeMath(body)) convert(body.trim()) else m.value
        }
        return s
    }

    /** Heuristic so currency like "$5 and $10" is not mistaken for math. */
    private fun looksLikeMath(s: String): Boolean =
        s.contains('\\') || s.contains('^') || s.contains('_') ||
            (s.contains('=') && s.any { it.isLetter() }) ||
            Regex("^[A-Za-z](\\([A-Za-z]\\))?$").matches(s.trim())

    /** Converts a raw LaTeX expression (without delimiters) into Unicode text. */
    fun convert(input: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < input.length) {
            when (val c = input[i]) {
                '\\' -> i = parseCommand(input, i, sb)
                '^' -> i = parseScript(input, i + 1, sb, isSuper = true)
                '_' -> i = parseScript(input, i + 1, sb, isSuper = false)
                '{', '}' -> i++
                '~' -> { sb.append(' '); i++ }
                else -> { sb.append(c); i++ }
            }
        }
        return sb.toString().replace(Regex(" {2,}"), " ").trim()
    }

    private fun parseCommand(input: String, start: Int, sb: StringBuilder): Int {
        var i = start + 1
        if (i >= input.length) return i
        val first = input[i]
        if (!first.isLetter()) {
            when (first) {
                '\\' -> sb.append('\n')
                ',', ';', ':' -> sb.append(' ')
                '!' -> Unit
                else -> sb.append(first)
            }
            return i + 1
        }
        val nameStart = i
        while (i < input.length && input[i].isLetter()) i++
        val name = input.substring(nameStart, i)
        when {
            name == "frac" || name == "dfrac" || name == "tfrac" -> {
                val (num, afterNum) = readArg(input, i)
                val (den, afterDen) = readArg(input, afterNum)
                sb.append(wrapIfComplex(convert(num))).append('/').append(wrapIfComplex(convert(den)))
                return afterDen
            }
            name == "sqrt" -> {
                var j = i
                var index = ""
                if (j < input.length && input[j] == '[') {
                    val close = input.indexOf(']', j)
                    if (close != -1) {
                        index = input.substring(j + 1, close)
                        j = close + 1
                    }
                }
                val (arg, after) = readArg(input, j)
                if (index.isNotEmpty()) sb.append(toScript(convert(index), superscripts, "^"))
                sb.append('√').append(wrapIfComplex(convert(arg)))
                return after
            }
            name in setOf("text", "textbf", "textit", "textrm", "mathrm", "mathbf", "mathit", "mathcal", "mathbb", "mathsf", "operatorname", "boxed", "overline", "underline", "vec", "hat", "bar", "tilde") -> {
                val (arg, after) = readArg(input, i)
                sb.append(convert(arg))
                return after
            }
            name == "begin" || name == "end" -> {
                val (_, after) = readArg(input, i)
                return after
            }
            name in setOf("left", "right", "big", "Big", "bigg", "Bigg", "bigl", "bigr", "Bigl", "Bigr", "displaystyle", "textstyle", "limits", "nolimits") -> {
                if (i < input.length && input[i] == '.') return i + 1
                return i
            }
            name in functionNames -> sb.append(name)
            symbols.containsKey(name) -> sb.append(symbols.getValue(name))
            else -> sb.append(name)
        }
        return i
    }

    private fun parseScript(input: String, argStart: Int, sb: StringBuilder, isSuper: Boolean): Int {
        val (arg, after) = readArg(input, argStart)
        val converted = convert(arg)
        sb.append(toScript(converted, if (isSuper) superscripts else subscripts, if (isSuper) "^" else "_"))
        return after
    }

    private fun toScript(text: String, map: Map<Char, Char>, prefix: String): String {
        if (text.isEmpty()) return ""
        val mapped = text.map { map[it] }
        return if (mapped.all { it != null }) {
            mapped.filterNotNull().joinToString("")
        } else if (text.length == 1) {
            "$prefix$text"
        } else {
            "$prefix($text)"
        }
    }

    /** Reads one argument: a balanced {...} group or a single character. */
    private fun readArg(input: String, start: Int): Pair<String, Int> {
        if (start >= input.length) return "" to start
        if (input[start] != '{') {
            return if (input[start] == '\\') {
                var j = start + 1
                while (j < input.length && input[j].isLetter()) j++
                if (j == start + 1) j++
                input.substring(start, j) to j
            } else {
                input[start].toString() to start + 1
            }
        }
        var depth = 0
        var j = start
        while (j < input.length) {
            when (input[j]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return input.substring(start + 1, j) to j + 1
                }
            }
            j++
        }
        return input.substring(start + 1) to input.length
    }

    private fun wrapIfComplex(s: String): String {
        val simple = s.isNotEmpty() && s.none { it in " +-=" } && s.count { it.isLetterOrDigit() } == s.length
        return if (simple || s.length <= 2) s else "($s)"
    }
}
