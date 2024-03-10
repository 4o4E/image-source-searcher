package top.e404.iss.searcher

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.apache.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.TrustSelfSignedStrategy
import org.apache.http.ssl.SSLContextBuilder
import kotlin.reflect.full.memberProperties

interface SourceSearcher {

    val type: String

    suspend fun search(url2search: String): List<SourceSearchResult>

    companion object {
        val noVerify by lazy {
            HttpClient(Apache) {
                engine {
                    proxy = ProxyBuilder.http("http://127.0.0.1:7890")
                }
                engine {
                    customizeClient {
                        setSSLContext(
                            SSLContextBuilder
                                .create()
                                .loadTrustMaterial(TrustSelfSignedStrategy())
                                .build()
                        )
                        setSSLHostnameVerifier(NoopHostnameVerifier())
                    }
                }
                install(HttpTimeout) {
                    connectTimeoutMillis = 15000
                    socketTimeoutMillis = 15000
                    requestTimeoutMillis = 15000
                }
                install(HttpRequestRetry) {
                    retryOnServerErrors(maxRetries = 5)
                    exponentialDelay()
                }
                followRedirects = false
            }
        }
        @Suppress("UNUSED")
        val okhttp by lazy {
            HttpClient(OkHttp) {
                engine {
                    proxy = ProxyBuilder.http("http://127.0.0.1:7890")
                }
                install(HttpTimeout) {
                    connectTimeoutMillis = 15000
                    socketTimeoutMillis = 15000
                    requestTimeoutMillis = 15000
                }
                install(HttpRequestRetry) {
                    retryOnServerErrors(maxRetries = 5)
                    exponentialDelay()
                }
                followRedirects = false
            }
        }

        var clientProvider = object : ClientProvider {
            override val userAgent =
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
            override val client = noVerify
        }
        val client inline get() = clientProvider.client
        val userAgent inline get() = clientProvider.userAgent
    }
}

interface SourceSearchResult {
    /**
     * 搜索结果的图片url
     */
    val imageUrl: String

    /**
     * 搜索结果跳转url
     */
    val sourceUrl: String
}

val SourceSearchResult.extra: Map<String, String?>
    get() {
        return javaClass.kotlin.memberProperties
            .filter { it.name != "imageUrl" }
            .associate { it.name to it.get(this)?.toString() }
    }

interface ClientProvider {
    val client: HttpClient
    val userAgent: String
}