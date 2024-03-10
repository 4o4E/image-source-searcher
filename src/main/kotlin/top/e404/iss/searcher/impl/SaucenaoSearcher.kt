package top.e404.iss.searcher.impl

import io.ktor.client.request.*
import io.ktor.client.statement.*
import top.e404.iss.searcher.SourceSearchResult
import top.e404.iss.searcher.SourceSearcher
import top.e404.iss.searcher.util.toDocument

object SaucenaoSearcher : SourceSearcher {
    override val type = "saucenao"

    private const val SAUCENAO_URL = "https://saucenao.com/search.php"

    override suspend fun search(url2search: String): List<SourceSearchResult> = SourceSearcher.client
        .get(SAUCENAO_URL) {
            parameter("db", "999")
            parameter("url", url2search)
        }
        .bodyAsText()
        .toDocument()
        .select("#middle .result tbody > tr")
        .asSequence()
        .flatMap { e ->
            val img = e.select(".resulttableimage .resultimage a img")
            val imageUrl = img.attr("src").let {
                if (it.startsWith("http")) it
                else img.attr("data-src")
            }
            var urls = e.select(".resulttablecontent .resultmiscinfo a").map { it.attr("href") }
            if (urls.isEmpty()) urls = e.select(".resultcontent .resultcontentcolumn a").map { it.attr("href") }
            val similar = e.select(".resultsimilarityinfo").text().replace(Regex("[^\\d.]"), "")
            urls.map {
                Result(imageUrl, it, similar)
            }
        }
        .sortedByDescending { it.similar.toDouble() }
        .toMutableList()

    data class Result(
        override val imageUrl: String,
        override val sourceUrl: String,
        val similar: String,
    ) : SourceSearchResult {

        @Transient
        override val searcher = SaucenaoSearcher

        override val extra = listOf(
            "相似度: $similar"
        )
    }
}