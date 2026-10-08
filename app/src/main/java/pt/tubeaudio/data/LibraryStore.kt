package pt.tubeaudio.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import pt.tubeaudio.model.AudioTrack

/** Small local library. Resolved stream URLs expire, so they are never persisted. */
class LibraryStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("library", Context.MODE_PRIVATE)

    fun favorites(): List<AudioTrack> = read("favorites")
    fun history(): List<AudioTrack> = read("history")

    fun toggleFavorite(track: AudioTrack): List<AudioTrack> {
        val current = favorites()
        val updated = if (current.any { it.id == track.id }) current.filterNot { it.id == track.id }
            else listOf(track) + current
        write("favorites", updated)
        return updated
    }

    fun recordPlay(track: AudioTrack): List<AudioTrack> {
        val updated = (listOf(track) + history().filterNot { it.id == track.id }).take(100)
        write("history", updated)
        return updated
    }

    private fun read(key: String): List<AudioTrack> = runCatching {
        val array = JSONArray(prefs.getString(key, "[]"))
        (0 until array.length()).mapNotNull { index ->
            runCatching {
                val o = array.getJSONObject(index)
                AudioTrack(
                    id = o.getString("id"), title = o.getString("title"),
                    uploader = o.optString("uploader"),
                    durationSeconds = o.optLong("duration"),
                    thumbnailUrl = o.optString("thumbnail").takeIf { it.isNotBlank() }
                )
            }.getOrNull()
        }
    }.getOrDefault(emptyList())

    private fun write(key: String, tracks: List<AudioTrack>) {
        val array = JSONArray()
        tracks.forEach { track ->
            array.put(JSONObject().apply {
                put("id", track.id)
                put("title", track.title)
                put("uploader", track.uploader)
                put("duration", track.durationSeconds)
                put("thumbnail", track.thumbnailUrl ?: "")
            })
        }
        prefs.edit().putString(key, array.toString()).apply()
    }
}
