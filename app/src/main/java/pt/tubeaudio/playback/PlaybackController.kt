package pt.tubeaudio.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import pt.tubeaudio.model.AudioTrack

class PlaybackController(context: Context) {
    data class Status(
        val connected: Boolean = false,
        val playing: Boolean = false,
        val durationMs: Long = 0,
        val positionMs: Long = 0
    )
    private val _status = MutableStateFlow(Status())
    val status = _status.asStateFlow()
    private var controller: MediaController? = null
    private var pendingTrack: AudioTrack? = null
    private var released = false
    private val future: ListenableFuture<MediaController> =
        MediaController.Builder(context.applicationContext,
            SessionToken(context, ComponentName(context, PlaybackService::class.java))).buildAsync()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = update(player)
    }

    init {
        future.addListener({
            if (released) return@addListener
            try {
                controller = future.get().also {
                    it.addListener(listener)
                    update(it)
                    pendingTrack?.let(::start)
                    pendingTrack = null
                }
            } catch (_: Exception) {
                _status.value = Status()
            }
        }, androidx.core.content.ContextCompat.getMainExecutor(context))
    }

    private fun update(player: Player) {
        _status.value = Status(
            connected = true,
            playing = player.isPlaying,
            durationMs = player.duration.coerceAtLeast(0),
            positionMs = player.currentPosition.coerceAtLeast(0)
        )
    }

    fun refreshPosition() { controller?.let(::update) }

    fun play(track: AudioTrack) {
        if (released || track.streamUrl == null) return
        if (controller == null) pendingTrack = track else start(track)
    }

    private fun start(track: AudioTrack) {
        val player = controller ?: return
        val metadata = MediaMetadata.Builder().setTitle(track.title).setArtist(track.uploader)
            .setArtworkUri(track.thumbnailUrl?.let(android.net.Uri::parse)).build()
        player.setMediaItem(MediaItem.Builder().setUri(track.streamUrl!!).setMediaMetadata(metadata).build())
        player.prepare()
        player.play()
    }

    fun toggle() { controller?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun seekTo(positionMs: Long) { controller?.seekTo(positionMs) }
    fun release() {
        released = true
        pendingTrack = null
        controller?.removeListener(listener)
        MediaController.releaseFuture(future)
    }
}
