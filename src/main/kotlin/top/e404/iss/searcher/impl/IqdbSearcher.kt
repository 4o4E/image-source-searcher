package top.e404.iss.searcher.impl

import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.e404.iss.searcher.SourceSearchResult
import top.e404.iss.searcher.SourceSearcher
import top.e404.iss.searcher.util.fixAsUrl
import top.e404.iss.searcher.util.toDocument

object IqdbSearcher : SourceSearcher {
    override val type = "iqdb"
    private const val IQDB_URL = "https://www.iqdb.org/"
    override suspend fun search(url2search: String): List<SourceSearchResult> {
        val resp = withContext(Dispatchers.IO) {
            SourceSearcher.client.submitForm(IQDB_URL, Parameters.build {
                append("url", url2search)
                listOf("1", "2", "3", "4", "5", "6", "11", "13").forEach { append("service[]", it) }
            })
        }.bodyAsText()
        if ("Your image" !in resp || "Best match" !in resp) return emptyList()
        return resp.toDocument()
            .select("#pages div")
            .also { it.removeFirst() }
            .map {
                val pic = it.select(".image img").attr("src").fixAsUrl(true, "www.iqdb.org")
                val imageUrl = it.select(".image a").attr("href").fixAsUrl(true, "www.iqdb.org")
                val td = it.select("td")
                val size = td.eq(2).text().substringBefore(" ")
                val similarity = td.eq(3).text().replace(" similarity", "")
                Result(pic, imageUrl, size, similarity)
            }
    }

    data class Result(
        override val imageUrl: String,
        override val sourceUrl: String,
        val size: String,
        val similar: String,
    ) : SourceSearchResult {
        override val extra = listOf(
            "尺寸: $size",
            "相似度: $similar"
        )
    }
}