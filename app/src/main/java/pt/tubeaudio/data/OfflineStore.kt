package pt.tubeaudio.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import pt.tubeaudio.model.AudioTrack
import java.io.File
import java.security.MessageDigest

/** Audio files live in private app storage; only stable metadata is persisted. */
class OfflineStore(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("offline", Context.MODE_PRIVATE)
    private val directory = File(app.filesDir, "audio").apply { mkdirs() }

    fun downloads(): List<AudioTrack> = synchronized(lock) {
        entries().mapNotNull { entry ->
            val file = File(directory, entry.optString("file"))
            if (!file.isFile || !file.canonicalPath.startsWith(directory.canonicalPath + File.separator))
                return@mapNotNull null
            AudioTrack(entry.getString("id"), entry.optString("title"),
                entry.optString("uploader"), entry.optLong("duration"),
                entry.optString("thumbnail").takeIf { it.isNotBlank() }, Uri.fromFile(file).toString())
        }
    }

    fun find(id: String): AudioTrack? = downloads().firstOrNull { it.id == id }

    fun fileFor(id: String, extension: String): File =
        File(directory, "${key(id)}.$extension")

    fun save(track: AudioTrack, file: File) = synchronized(lock) {
        val items = entries().filterNot { it.optString("id") == track.id }.toMutableList()
        items.add(0, JSONObject().apply {
            put("id", track.id); put("title", track.title); put("uploader", track.uploader)
            put("duration", track.durationSeconds); put("thumbnail", track.thumbnailUrl ?: "")
            put("file", file.name)
        })
        write(items)
    }

    fun remove(id: String): List<AudioTrack> = synchronized(lock) {
        val items = entries()
        items.firstOrNull { it.optString("id") == id }?.optString("file")?.let { name ->
            if (name.matches(Regex("[a-f0-9]{64}\\.(m4a|webm|mp3)"))) File(directory, name).delete()
        }
        write(items.filterNot { it.optString("id") == id })
        downloads()
    }

    private fun entries(): MutableList<JSONObject> = runCatching {
        val array = JSONArray(prefs.getString("tracks", "[]"))
        (0 until array.length()).mapNotNull { array.optJSONObject(it) }.toMutableList()
    }.getOrDefault(mutableListOf())

    private fun write(items: List<JSONObject>) {
        prefs.edit().putString("tracks", JSONArray().apply { items.forEach(::put) }.toString()).commit()
    }

    companion object {
        private val lock = Any()
        fun key(id: String): String = MessageDigest.getInstance("SHA-256")
            .digest(id.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }
}
