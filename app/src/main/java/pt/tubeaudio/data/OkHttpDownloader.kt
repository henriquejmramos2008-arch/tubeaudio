package pt.tubeaudio.data

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request as NPRequest
import org.schabi.newpipe.extractor.downloader.Response
import java.util.concurrent.TimeUnit

class OkHttpDownloader : Downloader() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    override fun execute(request: NPRequest): Response {
        val body = request.dataToSend()?.toRequestBody(null)
        val builder = Request.Builder().url(request.url()).method(request.httpMethod(), body)
        request.headers().forEach { (name, values) -> values.forEach { builder.addHeader(name, it) } }
        builder.header("User-Agent", request.headers()["User-Agent"]?.firstOrNull()
            ?: "Mozilla/5.0 (Android) AppleWebKit/537.36 Chrome/140 Mobile Safari/537.36")
        client.newCall(builder.build()).execute().use {
            return Response(it.code, it.message, it.headers.toMultimap(), it.body.string())
        }
    }
}
