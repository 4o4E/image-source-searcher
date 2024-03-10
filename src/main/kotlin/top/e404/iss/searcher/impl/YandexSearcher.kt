package top.e404.iss.searcher.impl

import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import top.e404.iss.searcher.SourceSearchResult
import top.e404.iss.searcher.SourceSearcher
import top.e404.iss.searcher.util.parseAsJson
import top.e404.iss.searcher.util.toDocument

object YandexSearcher : SourceSearcher {
    override val type = "yandex"
    private const val YANDEX_DOWNLOAD = "https://yandex.com/images-apphost/image-download"
    private const val YANDEX_SEARCH = "https://yandex.com/images/search"
    override suspend fun search(url2search: String): List<SourceSearchResult> {
        val orig = SourceSearcher.client
            .get(YANDEX_DOWNLOAD) { parameter("url", url2search) }
            .bodyAsText()
            .parseAsJson()
            .jsonObject["url"]!!
            .jsonPrimitive
            .content
            .substringBeforeLast("/")
            .plus("/orig")
        val je = SourceSearcher.client
            .get(YANDEX_SEARCH) {
                parameter("rpt", "imageview")
                parameter("url", orig)
                parameter("format", "json")
                parameter(
                    "request",
                    """{"blocks":[{"block":"extra-content","params":{},"version":2},{"block":"i-global__params:ajax","params":{},"version":2},{"block":"cbir-intent__image-link","params":{},"version":2},{"block":"content_type_search-by-image","params":{},"version":2},{"block":"serp-controller","params":{},"version":2},{"block":"cookies_ajax","params":{},"version":2},{"block":"advanced-search-block","params":{},"version":2}],"metadata":{"bundles":{"lb":"89Vg-?b*G$"},"assets":{"las":"justifier-height=1;justifier-setheight=1;fitimages-height=1;justifier-fitincuts=1;react-with-dom=1;173.0=1;7ddda0.0=1;117.0=1;2a7b71.0=1"},"extraContent":{"names":["i-react-ajax-adapter"]}}}"""
                )
            }
            .bodyAsText()
            .parseAsJson()
            .jsonObject["blocks"]!!
            .jsonArray
            .firstOrNull {
                it.jsonObject["name"]!!.jsonObject["block"]!!.jsonPrimitive.content == "content_type_search-by-image"
            } ?: return emptyList()
        return je.jsonObject["html"]!!
            .jsonPrimitive
            .content
            .toDocument()
            .select(".CbirSites-Items > .CbirSites-Item")
            .map {
                val imageUrl = it.select(".CbirSites-ItemThumb > a").attr("href")
                val size = it.select(".CbirSites-ItemThumb .Thumb-Mark").text().trim()
                val a = it.select(".CbirSites-ItemTitle > a")
                val desc = it.select(".CbirSites-ItemDescription").text()
                Result(imageUrl, a.attr("href"), a.text(), desc, size)
            }
    }

    data class Result(
        override val imageUrl: String,
        override val sourceUrl: String,
        val title: String,
        val desc: String,
        val size: String,
    ) : SourceSearchResult {
        override val extra = listOf(
            "标题: $title",
            "简介: $desc",
            "尺寸: $size",
        )
    }
}