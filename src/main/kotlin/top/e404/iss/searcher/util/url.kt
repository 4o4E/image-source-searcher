package top.e404.iss.searcher.util

import kotlinx.serialization.json.Json
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

@Suppress("NOTHING_TO_INLINE")
inline fun String.fixAsUrl(isHttps: Boolean, host: String) = when {
    startsWith("http") -> this
    startsWith("//") -> if (isHttps) "https:$this" else "http:$this"
    startsWith("/") -> if (isHttps) "https://$host$this" else "http://$host$this"
    else -> this
}

fun String.parseAsJson() = Json.parseToJsonElement(this)

fun String.toDocument(): Document = Jsoup.parse(this)