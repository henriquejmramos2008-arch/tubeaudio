package pt.tubeaudio.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import pt.tubeaudio.model.AudioTrack
import java.util.UUID

data class LocalPlaylist(val id: String, val name: String, val tracks: List<AudioTrack>)

/** Small local library. Resolved stream URLs expire, so they are never persisted. */
class LibraryStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("library", Context.MODE_PRIVATE)

    fun favorites(): List<AudioTrack> = read("favorites")
    fun history(): List<AudioTrack> = read("history")
    fun playlists(): List<LocalPlaylist> = runCatching {
        val array = JSONArray(prefs.getString("playlists", "[]"))
        (0 until array.length()).mapNotNull { index ->
            runCatching {
                val item = array.getJSONObject(index)
                LocalPlaylist(item.getString("id"), item.getString("name"),
                    readTracks(item.getJSONArray("tracks")))
            }.getOrNull()
        }
    }.getOrDefault(emptyList())

    fun createPlaylist(name: String): List<LocalPlaylist> {
        val clean = name.trim().take(60)
        if (clean.isEmpty()) return playlists()
        val updated = playlists() + LocalPlaylist(UUID.randomUUID().toString(), clean, emptyList())
        writePlaylists(updated)
        return updated
    }

    fun deletePlaylist(id: String): List<LocalPlaylist> {
        val updated = playlists().filterNot { it.id == id }
        writePlaylists(updated)
        return updated
    }

    fun addToPlaylist(id: String, track: AudioTrack): List<LocalPlaylist> {
        val updated = playlists().map {
            if (it.id == id && it.tracks.none { saved -> saved.id == track.id })
                it.copy(tracks = it.tracks + track) else it
        }
        writePlaylists(updated)
        return updated
    }

    fun removeFromPlaylist(id: String, trackId: String): List<LocalPlaylist> {
        val updated = playlists().map {
            if (it.id == id) it.copy(tracks = it.tracks.filterNot { saved -> saved.id == trackId })
            else it
        }
        writePlaylists(updated)
        return updated
    }

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
        readTracks(JSONArray(prefs.getString(key, "[]")))
    }.getOrDefault(emptyList())

    private fun readTracks(array: JSONArray): List<AudioTrack> =
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

    private fun write(key: String, tracks: List<AudioTrack>) {
        val array = JSONArray()
        tracks.forEach { array.put(trackJson(it)) }
        prefs.edit().putString(key, array.toString()).apply()
    }

    private fun writePlaylists(playlists: List<LocalPlaylist>) {
        val array = JSONArray()
        playlists.forEach { playlist ->
            val tracks = JSONArray()
            playlist.tracks.forEach { tracks.put(trackJson(it)) }
            array.put(JSONObject().apply {
                put("id", playlist.id)
                put("name", playlist.name)
                put("tracks", tracks)
            })
        }
        prefs.edit().putString("playlists", array.toString()).apply()
    }

    private fun trackJson(track: AudioTrack) = JSONObject().apply {
        put("id", track.id)
        put("title", track.title)
        put("uploader", track.uploader)
        put("duration", track.durationSeconds)
        put("thumbnail", track.thumbnailUrl ?: "")
    }
}
