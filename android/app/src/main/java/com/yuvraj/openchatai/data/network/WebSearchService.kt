package com.yuvraj.openchatai.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import java.net.URLEncoder

/** A single web search result passed back to the model as tool output. */
data class WebSearchResult(
    val title: String,
    val url: String,
    val snippet: String,
    /** Extracted readable text from the page itself, when fetchable. */
    val pageContent: String? = null,
)

/**
 * Free web search used as the assistant's research tool.
 * Primary: DuckDuckGo HTML results. Fallback: DuckDuckGo Instant Answer API.
 */
class WebSearchService {

    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient(Android) {
        install(HttpTimeout) {
            requestTimeoutMillis = 20_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 20_000
        }
    }

    suspend fun search(query: String): List<WebSearchResult> {
        val html = runCatching { searchHtml(query) }.getOrElse { e ->
            if (e is CancellationException) throw e
            emptyList()
        }
        val results = html.ifEmpty {
            runCatching { searchInstantAnswers(query) }.getOrElse { e ->
                if (e is CancellationException) throw e
                emptyList()
            }
        }
        return enrichWithPageContent(results)
    }

    /**
     * Fetches the top result pages in parallel and attaches extracted readable
     * text, so the model has real content to summarize instead of bare links.
     */
    private suspend fun enrichWithPageContent(results: List<WebSearchResult>): List<WebSearchResult> =
        coroutineScope {
            results.mapIndexed { index, result ->
                if (index >= PAGES_TO_FETCH) return@mapIndexed async { result }
                async {
                    val text = withTimeoutOrNull(PAGE_FETCH_TIMEOUT_MS) {
                        runCatching { fetchPageText(result.url) }.getOrNull()
                    }
                    if (text.isNullOrBlank()) result else result.copy(pageContent = text)
                }
            }.awaitAll()
        }

    /** Downloads a page and extracts its readable text (scripts/tags stripped). */
    private suspend fun fetchPageText(url: String): String? {
        val response = client.get(url) {
            header(HttpHeaders.UserAgent, DESKTOP_UA)
            header(HttpHeaders.Accept, "text/html")
        }
        if (!response.status.isSuccess()) return null
        val contentType = response.headers[HttpHeaders.ContentType].orEmpty()
        if (contentType.isNotEmpty() && !contentType.contains("html") && !contentType.contains("text")) return null
        val body = response.bodyAsText()
        return extractReadableText(body).takeIf { it.length >= MIN_PAGE_TEXT_LENGTH }
    }

    private fun extractReadableText(html: String): String {
        var text = html
            .replace(Regex("(?is)<script.*?</script>"), " ")
            .replace(Regex("(?is)<style.*?</style>"), " ")
            .replace(Regex("(?is)<noscript.*?</noscript>"), " ")
            .replace(Regex("(?is)<svg.*?</svg>"), " ")
            .replace(Regex("(?is)<header.*?</header>"), " ")
            .replace(Regex("(?is)<footer.*?</footer>"), " ")
            .replace(Regex("(?is)<nav.*?</nav>"), " ")
            .replace(Regex("(?is)<!--.*?-->"), " ")
        // Prefer the <article> or <main> region when present.
        val article = Regex("(?is)<article[^>]*>(.*?)</article>").find(text)?.groupValues?.get(1)
            ?: Regex("(?is)<main[^>]*>(.*?)</main>").find(text)?.groupValues?.get(1)
        if (article != null && article.length > 500) text = article
        text = text
            .replace(Regex("(?i)<(br|/p|/div|/li|/h[1-6]|/tr)[^>]*>"), "\n")
            .replace(Regex("<[^>]*>"), " ")
        return decodeEntities(text)
            .replace(Regex("[ \\t\\x0B\\f\\r]+"), " ")
            .replace(Regex("\\n\\s*\\n+"), "\n")
            .trim()
            .take(MAX_PAGE_TEXT_LENGTH)
    }

    private suspend fun searchHtml(query: String): List<WebSearchResult> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val response = client.get("https://html.duckduckgo.com/html/?q=$encoded") {
            header(HttpHeaders.UserAgent, DESKTOP_UA)
            header(HttpHeaders.Accept, "text/html")
        }
        if (!response.status.isSuccess()) return emptyList()
        val body = response.bodyAsText()

        val titleRegex = Regex(
            "<a[^>]*class=\"result__a\"[^>]*href=\"([^\"]*)\"[^>]*>(.*?)</a>",
            RegexOption.DOT_MATCHES_ALL,
        )
        val snippetRegex = Regex(
            "class=\"result__snippet\"[^>]*>(.*?)</a>",
            RegexOption.DOT_MATCHES_ALL,
        )
        val titles = titleRegex.findAll(body).toList()
        val snippets = snippetRegex.findAll(body).map { stripHtml(it.groupValues[1]) }.toList()

        return titles.mapIndexedNotNull { index, match ->
            val url = resolveUrl(match.groupValues[1]) ?: return@mapIndexedNotNull null
            val title = stripHtml(match.groupValues[2])
            if (title.isBlank()) return@mapIndexedNotNull null
            WebSearchResult(
                title = title,
                url = url,
                snippet = snippets.getOrElse(index) { "" },
            )
        }.take(6)
    }

    private suspend fun searchInstantAnswers(query: String): List<WebSearchResult> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val response = client.get(
            "https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1",
        ) {
            header(HttpHeaders.UserAgent, DESKTOP_UA)
        }
        if (!response.status.isSuccess()) return emptyList()
        val parsed = json.decodeFromString<InstantAnswerResponse>(response.bodyAsText())
        val results = mutableListOf<WebSearchResult>()
        if (parsed.abstractText.isNotBlank()) {
            results += WebSearchResult(
                title = parsed.heading.ifBlank { query },
                url = parsed.abstractUrl.ifBlank { "https://duckduckgo.com/?q=$encoded" },
                snippet = parsed.abstractText,
            )
        }
        parsed.relatedTopics.forEach { topic ->
            val text = topic.text
            val url = topic.firstUrl
            if (!text.isNullOrBlank() && !url.isNullOrBlank()) {
                results += WebSearchResult(
                    title = text.take(80),
                    url = url,
                    snippet = text,
                )
            }
        }
        return results.take(6)
    }

    /** DuckDuckGo wraps result links as //duckduckgo.com/l/?uddg=<encoded-url>. */
    private fun resolveUrl(href: String): String? {
        val decodedHref = decodeEntities(href)
        val uddg = Regex("uddg=([^&]+)").find(decodedHref)?.groupValues?.get(1)
        if (uddg != null) {
            return runCatching { URLDecoder.decode(uddg, "UTF-8") }.getOrNull()
        }
        // Skip DuckDuckGo ad redirect links (y.js) — they aren't real results.
        if (decodedHref.contains("duckduckgo.com/y.js")) return null
        return when {
            decodedHref.startsWith("http") -> decodedHref
            decodedHref.startsWith("//") -> "https:$decodedHref"
            else -> null
        }
    }

    private fun stripHtml(input: String): String =
        decodeEntities(input.replace(Regex("<[^>]*>"), "")).trim()

    private fun decodeEntities(input: String): String = input
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#x27;", "'")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&nbsp;", " ")

    @Serializable
    private data class InstantAnswerResponse(
        @SerialName("AbstractText") val abstractText: String = "",
        @SerialName("AbstractURL") val abstractUrl: String = "",
        @SerialName("Heading") val heading: String = "",
        @SerialName("RelatedTopics") val relatedTopics: List<RelatedTopic> = emptyList(),
    )

    @Serializable
    private data class RelatedTopic(
        @SerialName("Text") val text: String? = null,
        @SerialName("FirstURL") val firstUrl: String? = null,
    )

    private companion object {
        const val DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36"
        const val PAGES_TO_FETCH = 3
        const val PAGE_FETCH_TIMEOUT_MS = 8_000L
        const val MIN_PAGE_TEXT_LENGTH = 200
        const val MAX_PAGE_TEXT_LENGTH = 4_000
    }
}
