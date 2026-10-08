package pt.tubeaudio.model
data class AudioTrack(
    val id: String,
    val title: String,
    val uploader: String,
    val durationSeconds: Long,
    val thumbnailUrl: String?,
    val streamUrl: String? = null
)
