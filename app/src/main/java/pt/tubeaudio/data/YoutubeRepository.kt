package pt.tubeaudio.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import pt.tubeaudio.model.AudioTrack

class YoutubeRepository {
    private val youtube by lazy { NewPipe.getService("YouTube") }

    suspend fun search(query: String): Result<List<AudioTrack>> = withContext(Dispatchers.IO) {
        runCatching {
            require(query.isNotBlank())
            SearchInfo.getInfo(youtube, query.trim()).relatedItems
                .filterIsInstance<StreamInfoItem>()
                .map {
                    AudioTrack(
                        id = it.url, title = it.name, uploader = it.uploaderName,
                        durationSeconds = it.duration, thumbnailUrl = it.thumbnails.firstOrNull()?.url
                    )
                }
        }
    }

    suspend fun resolveAudio(track: AudioTrack): Result<AudioTrack> = withContext(Dispatchers.IO) {
        runCatching {
            val info = StreamInfo.getInfo(youtube, track.id)
            val stream = info.audioStreams
                .filter { it.url.isNotBlank() }
                .maxByOrNull { it.bitrate }
                ?: error("Nenhum stream de áudio disponível")
            track.copy(
                title = info.name,
                uploader = info.uploaderName,
                durationSeconds = info.length,
                thumbnailUrl = info.thumbnails.firstOrNull()?.url ?: track.thumbnailUrl,
                streamUrl = stream.url
            )
        }
    }
}
