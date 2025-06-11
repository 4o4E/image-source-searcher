package top.e404.iss.searcher.impl

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import top.e404.iss.searcher.SourceSearchResult
import top.e404.iss.searcher.SourceSearcher
import javax.script.ScriptEngineManager

object GoogleSearcher : SourceSearcher {
    private val log = LoggerFactory.getLogger(this::class.java)
    override val type = "google"
    private val manager = ScriptEngineManager()
    private val engine = manager.getEngineByName("nashorn")

    override suspend fun search(url2search: String): List<SourceSearchResult> {
        val obj = withContext(Dispatchers.IO) {
            SourceSearcher.client.get("https://lens.google.com/uploadbyurl") {
                parameter("url", url2search)
                userAgent(SourceSearcher.userAgent)
            }
        }.bodyAsText().substringAfter("; var AF_dataServiceRequests = ").substringBefore("; var AF_initDataChunkQueue")

        val (id1, id2) = (engine.eval(
            """var obj = $obj;
                |id1 = obj['ds:0']['request'][0][0]
                |id2 = obj['ds:0']['request'][1][7][0]
                |id1 + "|" + id2
            """.trimMargin()
        ) as String).split("|")

        val resp = withContext(Dispatchers.IO) {
            SourceSearcher.client.post("https://lens.google.com/_/LensWebStandaloneUi/data/batchexecute") {
                parameter("soc-app", "1")
                parameter("soc-platform", "1")
                parameter("soc-device", "1")
                parameter("rt", "c")
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(
                    "f.req=${
                        """[[["B7fdke","[[\"$id1\",1,1],[null,null,null,null,null,null,[\"\"],[\"$id2\",[null,null,0,0]]],[null,null,null,null,3,[\"en-US\",null,\"US\",\"Europe/Moscow\"],null,null,[null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,1,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,1,null,null,null,null,null,null,null,null,null,null,null,null,1,null,null,null,null,null,1,null,null,null,1,null,null,null,null,null,null,1,1,null,null,null,null,1,null,1,null,null,1],[[null,1,1,1,1,1,1,null,null,null,1,1,1,1,null,null,null,1,null,null,null,null,null,null,null,null,null,1,null,null,null,null,null,null,null,null,1,null,null,null,null,null,null,null,1,null,null,null,null,null,null,null,null,1,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,1,null,null,null,1,null,null,null,1,null,null,null,null,null,null,1,1,1,null,1,1,null,null,null,null,0,0,null,null,null,[5,6,2],null,null,null,1,null,1,null,1,null,null,null,0,null,null,null,null,1,0,0,0,null,null,null,1,null,null,null,null,null,0,0,1,null,null,1,1,null,null,null,null,null,null,1,null,0,null,0]],[[[7]]],null,null,null,26,null,null,null,null,[null,6],[null,16],null,[16],[]],null,null,null,null,null,null,[],null,null,null,null,null,null,[],\"EkcKJDU3NDA3NTNjLWVmNDgtNDViZi04NzExLTQ2ODFlZDkwZGE2MRIfWTlTMkhEQkhiLVFmNEhFeFhiLWI5ZUQ3UEhFNm1SZw==\",null,null,null,null,null,[[null,[]],[null,null,null,null,null,null,[\"\"],[\"\",[null,null,0,0]]]],null,\"\"]",null,"generic"]]]""".encodeURLParameter()
                    }"
                )
                userAgent(SourceSearcher.userAgent)
            }
        }.bodyAsText()

        val content = Json.parseToJsonElement(resp.lines()[3])
            .jsonArray[0]
            .jsonArray[2]
            .jsonPrimitive
            .content
        Json.Default.parseToJsonElement(content)
            .jsonArray[1]
            .jsonArray[0]
            .jsonArray[1]
            .jsonArray[8]
            .jsonArray
            .findContainer()
            ?.let { return it }
        log.warn("从google搜源时无法获取内容, json: $content")
        return emptyList()
    }

    private fun JsonArray.findContainer(): List<Result>? {
        try {
            return map { Result(it.jsonArray) }
        } catch (_: Throwable) {
            filterIsInstance<JsonArray>().forEach {
                return it.findContainer()
            }
        }
        return null
    }

    data class Result(
        override val imageUrl: String,
        override val sourceUrl: String,
        val title: String,
        val desc: String,
        val size: String,
    ) : SourceSearchResult {
        constructor(array: JsonArray) : this(
            array[0].jsonArray[0].jsonPrimitive.content,
            array[2].jsonArray[2].jsonArray[2].jsonPrimitive.content,
            array[1].jsonArray[0].jsonPrimitive.content,
            array[4].jsonPrimitive.content,
            "${array[0].jsonArray[12].jsonPrimitive.content}x${array[0].jsonArray[13].jsonPrimitive.content}",
        )

        @Transient
        override val searcher = GoogleSearcher

        override val extra by lazy {
            listOf(
                "标题: $title",
                "描述: $desc",
                "尺寸: $size",
            )
        }
    }
}