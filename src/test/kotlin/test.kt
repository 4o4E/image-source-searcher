package top.e404.iss.searcher.test

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import top.e404.iss.searcher.extra
import top.e404.iss.searcher.impl.*
import java.io.File

class SearchTest {

    private val image2search = "https://avatars.githubusercontent.com/u/12100985"

    private val dir = File("run").also(File::mkdir)

    @Test
    fun testAll() {
        runBlocking(Dispatchers.IO) {
            listOf(
                Ascii2dSearcher,
                GoogleSearcher,
                IqdbSearcher,
                SaucenaoSearcher,
                YandexSearcher
            ).map { searcher ->
                async {
                    dir.resolve(searcher.type + ".md").writeText(buildString {
                        appendLine("# 搜源")
                        appendLine()
                        appendLine("![image]($image2search)")
                        appendLine()
                        searcher.search(image2search).forEachIndexed { index, result ->
                            appendLine("## $index")
                            appendLine()
                            appendLine("![image](${result.imageUrl})")
                            appendLine()
                            result.extra.forEach { (name, value) ->
                                appendLine("### $name")
                                appendLine()
                                appendLine(value)
                                appendLine()
                            }
                        }
                    })
                }
            }.awaitAll()
        }
    }
}