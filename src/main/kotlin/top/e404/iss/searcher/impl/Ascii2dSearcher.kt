package top.e404.iss.searcher.impl

import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.jsoup.nodes.Element
import top.e404.iss.searcher.SourceSearchResult
import top.e404.iss.searcher.SourceSearcher
import top.e404.iss.searcher.util.toDocument

object Ascii2dSearcher : SourceSearcher {
    override val type = "ascii2d"

    enum class Type(val text: String) {
        COLOR("颜色相似"), BOVW("结构相似");
    }

    override suspend fun search(url2search: String): List<SourceSearchResult> {
        val response = SourceSearcher.client.get("https://ascii2d.net/search/url/$url2search")

        val color = response.headers["location"]!!
        val bovw = color.replace("/color/", "/bovw/")
        return coroutineScope {
            listOf(
                async(Dispatchers.IO) {
                    SourceSearcher.client.get(color)
                        .bodyAsText()
                        .toDocument()
                        .select(".item-box")
                        .take(5)
                        .map { it to Type.COLOR }
                        .toMutableList()
                },
                async(Dispatchers.IO) {
                    SourceSearcher.client.get(bovw)
                        .bodyAsText()
                        .toDocument()
                        .select(".item-box")
                        .also { it.removeFirst() }
                        .take(5)
                        .map { it to Type.BOVW }
                }
            ).awaitAll()
        }.flatten().flatMap { (box, type) ->
            val resultImageUrl = box.selectFirst(".image-box img")!!.attr("src").let { "https://ascii2d.net$it" }
            val detailBox = box.select(".detail-box")

            val ff = detailBox.select("font > font")
            if (ff.isNotEmpty()) return@flatMap ff.map { Result(resultImageUrl, it, type) }
            val a = detailBox.select("a")
            if (a.isEmpty()) {
                // if ("登録された詳細" in detailBox.html()) {
                //     val src = detailBox.select(".external").text()
                //     return@mapNotNull Result(imageUrl, src, null, null, null, type)
                // }
                return@flatMap emptyList()
            }

            return@flatMap a.map { Result(resultImageUrl, it, type) }
        }
    }

    data class Result(
        override val imageUrl: String,
        override val sourceUrl: String,
        val title: String,
        val type: Type,
    ) : SourceSearchResult {
        constructor(imageUrl: String, e: Element, type: Type) : this(imageUrl, e.attr("href"), e.text(), type)

        @Transient
        override val searcher = Ascii2dSearcher

        override val extra by lazy {
            listOf(
                "标题: $title",
                "类型: ${type.text}"
            )
        }
    }
}