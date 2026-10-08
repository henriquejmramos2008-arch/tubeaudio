package pt.tubeaudio.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pt.tubeaudio.data.LibraryStore
import pt.tubeaudio.data.LocalPlaylist
import pt.tubeaudio.data.YoutubeRepository
import pt.tubeaudio.model.AudioTrack
import pt.tubeaudio.playback.PlaybackController

enum class LibraryTab { HOME, SEARCH, LIBRARY }
enum class LibrarySection { PLAYLISTS, FAVORITES, HISTORY }

data class PlayerState(
    val query: String = "",
    val loading: Boolean = false,
    val resolving: Boolean = false,
    val results: List<AudioTrack> = emptyList(),
    val favorites: List<AudioTrack> = emptyList(),
    val history: List<AudioTrack> = emptyList(),
    val playlists: List<LocalPlaylist> = emptyList(),
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
    private val playback = PlaybackController(application)
    private val _state = MutableStateFlow(
        PlayerState(favorites = library.favorites(), history = library.history(),
            playlists = library.playlists())
    )
    val state = _state.asStateFlow()
    private var searchJob: Job? = null
    private var playJob: Job? = null

    init {
        viewModelScope.launch {
            playback.status.collect { _state.value = _state.value.copy(playback = it) }
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

    fun play(track: AudioTrack) {
        playJob?.cancel()
        playJob = viewModelScope.launch {
            _state.value = _state.value.copy(resolving = true, error = null, errorDetails = null)
            val result = repo.resolveAudio(track)
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
