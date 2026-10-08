package pt.tubeaudio.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import pt.tubeaudio.model.AudioTrack
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

class DownloadWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val id = inputData.getString("id") ?: return@withContext Result.failure()
        val track = AudioTrack(id, inputData.getString("title").orEmpty(),
            inputData.getString("uploader").orEmpty(), inputData.getLong("duration", 0L),
            inputData.getString("thumbnail"))
        val store = OfflineStore(applicationContext)
        if (store.find(id) != null) return@withContext Result.success()
        var part: File? = null
        try {
            setForeground(notification(track.title))
            NewPipeInitializer.initialize(applicationContext)
            val resolved = YoutubeRepository().resolveAudio(track).getOrThrow()
            val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS).build()
            val request = Request.Builder().url(resolved.streamUrl ?: error("URL indisponível")).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                val body = response.body ?: throw IOException("Resposta sem áudio")
                val mime = response.header("Content-Type").orEmpty().lowercase()
                val extension = when {
                    "webm" in mime -> "webm"
                    "mpeg" in mime || "mp3" in mime -> "mp3"
                    else -> "m4a"
                }
                val destination = store.fileFor(id, extension)
                part = File(destination.parentFile, destination.name + ".part")
                body.byteStream().use { input ->
                    part!!.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            coroutineContext.ensureActive()
                            val n = input.read(buffer)
                            if (n < 0) break
                            output.write(buffer, 0, n)
                        }
                    }
                }
                if (part!!.length() == 0L || !part!!.renameTo(destination))
                    throw IOException("Não foi possível guardar o ficheiro")
                store.save(resolved, destination)
            }
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(workDataOf("error" to "${error.javaClass.simpleName}: ${error.message.orEmpty().take(120)}"))
        } finally {
            part?.delete()
        }
    }

    private fun notification(title: String): ForegroundInfo {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(
            "tubeaudio_downloads", "Downloads de áudio", NotificationManager.IMPORTANCE_LOW))
        val built = NotificationCompat.Builder(applicationContext, "tubeaudio_downloads")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("A descarregar áudio")
            .setContentText(title).setOngoing(true).setProgress(0, 0, true).build()
        return if (Build.VERSION.SDK_INT >= 29)
            ForegroundInfo(id.hashCode(), built, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        else ForegroundInfo(id.hashCode(), built)
    }

    companion object { const val TAG = "tubeaudio-download" }
}
