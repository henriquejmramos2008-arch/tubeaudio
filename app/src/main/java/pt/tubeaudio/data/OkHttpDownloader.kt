package pt.tubeaudio.data

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request as NPRequest
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.util.concurrent.TimeUnit

class OkHttpDownloader : Downloader() {
    private val defaultUserAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"
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
            .header("User-Agent", defaultUserAgent)
        request.headers().forEach { (name, values) ->
            builder.removeHeader(name)
            values.forEach { builder.addHeader(name, it) }
        }
        client.newCall(builder.build()).execute().use {
            if (it.code == 429) throw ReCaptchaException("YouTube pediu verificação", request.url())
            return Response(it.code, it.message, it.headers.toMultimap(),
                it.body.string(), it.request.url.toString())
        }
    }
}
