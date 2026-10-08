package pt.tubeaudio.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pt.tubeaudio.data.LibraryStore
import pt.tubeaudio.data.YoutubeRepository
import pt.tubeaudio.model.AudioTrack
import pt.tubeaudio.playback.PlaybackController

enum class LibraryTab { SEARCH, FAVORITES, HISTORY }

data class PlayerState(
    val query: String = "",
    val loading: Boolean = false,
    val resolving: Boolean = false,
    val results: List<AudioTrack> = emptyList(),
    val favorites: List<AudioTrack> = emptyList(),
    val history: List<AudioTrack> = emptyList(),
    val current: AudioTrack? = null,
    val tab: LibraryTab = LibraryTab.SEARCH,
    val playback: PlaybackController.Status = PlaybackController.Status(),
    val error: String? = null
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = YoutubeRepository()
    private val library = LibraryStore(application)
    private val playback = PlaybackController(application)
    private val _state = MutableStateFlow(
        PlayerState(favorites = library.favorites(), history = library.history())
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

    fun query(value: String) { _state.value = _state.value.copy(query = value, error = null) }
    fun select(tab: LibraryTab) { _state.value = _state.value.copy(tab = tab, error = null) }

    fun search() {
        val query = state.value.query.trim()
        if (query.isEmpty()) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null, tab = LibraryTab.SEARCH)
            val result = repo.search(query)
            if (!isActive) return@launch
            result.onSuccess {
                _state.value = _state.value.copy(loading = false, results = it,
                    error = if (it.isEmpty()) "Não foram encontrados resultados." else null)
            }.onFailure {
                _state.value = _state.value.copy(loading = false,
                    error = "Pesquisa indisponível. Verifica a ligação e tenta novamente.")
            }
        }
    }

    fun play(track: AudioTrack) {
        playJob?.cancel()
        playJob = viewModelScope.launch {
            _state.value = _state.value.copy(resolving = true, error = null)
            val result = repo.resolveAudio(track)
            if (!isActive) return@launch
            result.onSuccess { resolved ->
                playback.play(resolved)
                _state.value = _state.value.copy(
                    resolving = false, current = resolved, history = library.recordPlay(resolved)
                )
            }.onFailure {
                _state.value = _state.value.copy(resolving = false,
                    error = "Não foi possível reproduzir esta faixa. Tenta outra ou volta a tentar.")
            }
        }
    }

    fun toggleFavorite(track: AudioTrack) {
        _state.value = _state.value.copy(favorites = library.toggleFavorite(track))
    }
    fun togglePlayback() = playback.toggle()
    fun seekTo(positionMs: Long) = playback.seekTo(positionMs)

    override fun onCleared() {
        searchJob?.cancel()
        playJob?.cancel()
        playback.release()
        super.onCleared()
    }
}
