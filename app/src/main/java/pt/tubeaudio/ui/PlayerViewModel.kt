package pt.tubeaudio.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pt.tubeaudio.data.LibraryStore
import pt.tubeaudio.data.LocalPlaylist
import pt.tubeaudio.data.DownloadWorker
import pt.tubeaudio.data.OfflineStore
import pt.tubeaudio.data.YoutubeRepository
import pt.tubeaudio.model.AudioTrack
import pt.tubeaudio.playback.PlaybackController

enum class LibraryTab { HOME, SEARCH, LIBRARY }
enum class LibrarySection { PLAYLISTS, FAVORITES, HISTORY, DOWNLOADS }

data class PlayerState(
    val query: String = "",
    val loading: Boolean = false,
    val resolving: Boolean = false,
    val results: List<AudioTrack> = emptyList(),
    val favorites: List<AudioTrack> = emptyList(),
    val history: List<AudioTrack> = emptyList(),
    val playlists: List<LocalPlaylist> = emptyList(),
    val downloads: List<AudioTrack> = emptyList(),
    val downloadingIds: Set<String> = emptySet(),
    val downloadError: String? = null,
    val queue: List<AudioTrack> = emptyList(),
    val queueIndex: Int = -1,
    val shuffle: Boolean = false,
    val repeatOne: Boolean = false,
    val current: AudioTrack? = null,
    val tab: LibraryTab = LibraryTab.HOME,
    val librarySection: LibrarySection = LibrarySection.FAVORITES,
    val playback: PlaybackController.Status = PlaybackController.Status(),
    val error: String? = null,
    val errorDetails: String? = null
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = YoutubeRepository()
    private val library = LibraryStore(application)
    private val offline = OfflineStore(application)
    private val work = WorkManager.getInstance(application)
    private val playback = PlaybackController(application)
    private val _state = MutableStateFlow(
        PlayerState(favorites = library.favorites(), history = library.history(),
            playlists = library.playlists(), downloads = offline.downloads())
    )
    val state = _state.asStateFlow()
    private var searchJob: Job? = null
    private var playJob: Job? = null

    init {
        viewModelScope.launch {
            work.getWorkInfosByTagFlow(DownloadWorker.TAG).collect { jobs ->
                val pending = jobs.filter { !it.state.isFinished }
                    .mapNotNull { job -> job.tags.firstOrNull { it.startsWith("track:") }
                        ?.removePrefix("track:") }.toSet()
                val failure = jobs.lastOrNull { it.state == WorkInfo.State.FAILED }
                    ?.outputData?.getString("error")
                _state.value = _state.value.copy(downloads = offline.downloads(),
                    downloadingIds = pending, downloadError = failure)
            }
        }
        viewModelScope.launch {
            var handledEnd = false
            playback.status.collect { status ->
                _state.value = _state.value.copy(playback = status)
                if (status.ended && !handledEnd) {
                    handledEnd = true
                    next(fromCompletion = true)
                }
                if (!status.ended) handledEnd = false
            }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(1000)
                playback.refreshPosition()
            }
        }
    }

    fun query(value: String) {
        _state.value = _state.value.copy(query = value, error = null, errorDetails = null)
    }
    fun select(tab: LibraryTab) { _state.value = _state.value.copy(tab = tab, error = null) }
    fun selectLibrary(section: LibrarySection) {
        _state.value = _state.value.copy(librarySection = section)
    }

    fun search() {
        val query = state.value.query.trim()
        if (query.isEmpty()) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null, errorDetails = null,
                tab = LibraryTab.SEARCH)
            val result = repo.search(query)
            if (!isActive) return@launch
            result.onSuccess {
                _state.value = _state.value.copy(loading = false, results = it,
                    error = if (it.isEmpty()) "Não foram encontrados resultados." else null)
            }.onFailure {
                Log.e("TubeAudioSearch", "Falha na pesquisa", it)
                _state.value = _state.value.copy(loading = false,
                    error = searchMessage(it),
                    errorDetails = "${it.javaClass.simpleName}: ${it.message.orEmpty().take(200)}")
            }
        }
    }

    fun play(track: AudioTrack, tracks: List<AudioTrack> = listOf(track)) {
        val queue = tracks.distinctBy { it.id }.ifEmpty { listOf(track) }
        _state.value = _state.value.copy(queue = queue,
            queueIndex = queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0))
        resolveAndPlay(track)
    }

    fun playAll(tracks: List<AudioTrack>) {
        tracks.firstOrNull()?.let { play(it, tracks) }
    }

    fun next(fromCompletion: Boolean = false) {
        val s = state.value
        if (s.queue.isEmpty()) return
        val nextIndex = when {
            fromCompletion && s.repeatOne -> s.queueIndex
            s.shuffle && s.queue.size > 1 ->
                s.queue.indices.filter { it != s.queueIndex }.random()
            s.queueIndex + 1 < s.queue.size -> s.queueIndex + 1
            else -> return
        }
        _state.value = _state.value.copy(queueIndex = nextIndex)
        resolveAndPlay(s.queue[nextIndex])
    }

    fun previous() {
        val s = state.value
        if (s.queueIndex <= 0) {
            seekTo(0)
            return
        }
        val previousIndex = s.queueIndex - 1
        _state.value = s.copy(queueIndex = previousIndex)
        resolveAndPlay(s.queue[previousIndex])
    }

    fun toggleShuffle() {
        _state.value = _state.value.copy(shuffle = !_state.value.shuffle)
    }
    fun toggleRepeatOne() {
        _state.value = _state.value.copy(repeatOne = !_state.value.repeatOne)
    }

    private fun resolveAndPlay(track: AudioTrack) {
        playJob?.cancel()
        playJob = viewModelScope.launch {
            _state.value = _state.value.copy(resolving = true, error = null, errorDetails = null)
            val result = offline.find(track.id)?.let { Result.success(it) } ?: repo.resolveAudio(track)
            if (!isActive) return@launch
            result.onSuccess { resolved ->
                playback.play(resolved)
                _state.value = _state.value.copy(
                    resolving = false, current = resolved, history = library.recordPlay(resolved)
                )
            }.onFailure {
                Log.e("TubeAudioPlayer", "Falha ao resolver áudio", it)
                _state.value = _state.value.copy(resolving = false,
                    error = "Não foi possível reproduzir esta faixa.",
                    errorDetails = "${it.javaClass.simpleName}: ${it.message.orEmpty().take(200)}")
            }
        }
    }

    fun toggleFavorite(track: AudioTrack) {
        _state.value = _state.value.copy(favorites = library.toggleFavorite(track))
    }
    fun createPlaylist(name: String) {
        _state.value = _state.value.copy(playlists = library.createPlaylist(name))
    }
    fun deletePlaylist(id: String) {
        _state.value = _state.value.copy(playlists = library.deletePlaylist(id))
    }
    fun addToPlaylist(id: String, track: AudioTrack) {
        _state.value = _state.value.copy(playlists = library.addToPlaylist(id, track))
    }
    fun removeFromPlaylist(id: String, trackId: String) {
        _state.value = _state.value.copy(playlists = library.removeFromPlaylist(id, trackId))
    }
    fun download(track: AudioTrack) {
        if (offline.find(track.id) != null) return
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf("id" to track.id, "title" to track.title,
                "uploader" to track.uploader, "duration" to track.durationSeconds,
                "thumbnail" to track.thumbnailUrl))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .addTag(DownloadWorker.TAG).addTag("track:${track.id}").build()
        work.enqueueUniqueWork("audio-${OfflineStore.key(track.id)}", ExistingWorkPolicy.KEEP, request)
    }
    fun removeDownload(track: AudioTrack) {
        work.cancelUniqueWork("audio-${OfflineStore.key(track.id)}")
        _state.value = _state.value.copy(downloads = offline.remove(track.id))
    }
    fun togglePlayback() = playback.toggle()
    fun seekTo(positionMs: Long) = playback.seekTo(positionMs)

    private fun searchMessage(error: Throwable): String {
        val name = error.javaClass.simpleName
        return when {
            name.contains("ReCaptcha", true) -> "O YouTube pediu uma verificação. Tenta novamente mais tarde."
            error is java.net.UnknownHostException -> "Sem acesso à Internet. Verifica a ligação."
            error is java.net.SocketTimeoutException -> "A pesquisa demorou demasiado. Tenta novamente."
            name.contains("Extraction", true) || name.contains("Parsing", true) ->
                "O YouTube alterou a pesquisa. Pode ser necessária uma atualização."
            else -> "Não foi possível pesquisar agora. Tenta novamente."
        }
    }

    override fun onCleared() {
        searchJob?.cancel()
        playJob?.cancel()
        playback.release()
        super.onCleared()
    }
}
