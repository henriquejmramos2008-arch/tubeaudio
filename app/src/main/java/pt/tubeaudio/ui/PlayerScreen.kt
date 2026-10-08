package pt.tubeaudio.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import pt.tubeaudio.model.AudioTrack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(vm: PlayerViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showPlayer by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    Scaffold(
        containerColor = Color(0xFF101019),
        bottomBar = {
            Column {
                state.current?.let { track ->
                    Surface(color = Color(0xFF29233D), shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                        Row(Modifier.fillMaxWidth().clickable { showPlayer = true }.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Artwork(track, Modifier.size(48.dp))
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.SemiBold)
                                Text(track.uploader, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = vm::togglePlayback) {
                                Icon(if (state.playback.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (state.playback.playing) "Pausar" else "Continuar")
                            }
                        }
                    }
                }
                NavigationBar(containerColor = Color(0xFF181824)) {
                    listOf(
                        Triple(LibraryTab.SEARCH, "Pesquisar", Icons.Default.Search),
                        Triple(LibraryTab.FAVORITES, "Favoritos", Icons.Default.Favorite),
                        Triple(LibraryTab.HISTORY, "Histórico", Icons.Default.History)
                    ).forEach { (tab, label, icon) ->
                        NavigationBarItem(selected = state.tab == tab, onClick = { vm.select(tab) },
                            icon = { Icon(icon, contentDescription = null) }, label = { Text(label) })
                    }
                }
            }
        }
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).padding(horizontal = 18.dp)) {
            Spacer(Modifier.height(16.dp))
            Text("TubeAudio", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(when (state.tab) {
                LibraryTab.SEARCH -> "Encontra o que queres ouvir"
                LibraryTab.FAVORITES -> "As tuas músicas guardadas"
                LibraryTab.HISTORY -> "Ouvidas recentemente"
            }, color = Color(0xFFB9B3CA))
            Spacer(Modifier.height(22.dp))
            if (state.tab == LibraryTab.SEARCH) {
                OutlinedTextField(value = state.query, onValueChange = vm::query, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
                    placeholder = { Text("Artista, música ou vídeo") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = { IconButton(onClick = { keyboard?.hide(); vm.search() }) {
                        Icon(Icons.Default.Search, contentDescription = "Pesquisar")
                    } },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); vm.search() }))
                Spacer(Modifier.height(12.dp))
            }
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 12.dp))
            }
            if (state.loading || state.resolving) LinearProgressIndicator(Modifier.fillMaxWidth())
            val tracks = when (state.tab) {
                LibraryTab.SEARCH -> state.results
                LibraryTab.FAVORITES -> state.favorites
                LibraryTab.HISTORY -> state.history
            }
            if (tracks.isEmpty() && !state.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(when (state.tab) {
                        LibraryTab.SEARCH -> "Pesquisa uma música para começar."
                        LibraryTab.FAVORITES -> "Toca no coração para guardar músicas."
                        LibraryTab.HISTORY -> "As músicas reproduzidas aparecem aqui."
                    }, color = Color(0xFFB9B3CA))
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tracks, key = { it.id }) { track ->
                        TrackRow(track, state.favorites.any { it.id == track.id },
                            onPlay = { vm.play(track) }, onFavorite = { vm.toggleFavorite(track) })
                    }
                }
            }
        }
    }
    if (showPlayer) state.current?.let { track ->
        ModalBottomSheet(onDismissRequest = { showPlayer = false }) {
            PlayerDetails(track, state, vm)
        }
    }
}

@Composable
private fun Artwork(track: AudioTrack, modifier: Modifier = Modifier) {
    if (track.thumbnailUrl != null) {
        AsyncImage(model = track.thumbnailUrl, contentDescription = null,
            modifier = modifier.clip(RoundedCornerShape(12.dp)))
    } else {
        Box(modifier.clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.MusicNote, contentDescription = null)
        }
    }
}

@Composable
private fun TrackRow(track: AudioTrack, favorite: Boolean, onPlay: () -> Unit, onFavorite: () -> Unit) {
    Surface(color = Color(0xFF1D1C29), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onPlay).padding(10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Artwork(track, Modifier.size(60.dp))
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(track.title, fontWeight = FontWeight.SemiBold, maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
                Text(track.uploader, style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB9B3CA), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onFavorite) {
                Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (favorite) "Remover dos favoritos" else "Adicionar aos favoritos",
                    tint = if (favorite) Color(0xFFFF78A5) else Color(0xFFB9B3CA))
            }
        }
    }
}

@Composable
private fun PlayerDetails(track: AudioTrack, state: PlayerState, vm: PlayerViewModel) {
    val favorite = state.favorites.any { it.id == track.id }
    Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp).padding(bottom = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Artwork(track, Modifier.fillMaxWidth().height(240.dp))
        Spacer(Modifier.height(24.dp))
        Text(track.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(track.uploader, color = Color(0xFFB9B3CA))
        Spacer(Modifier.height(18.dp))
        val duration = state.playback.durationMs
        Slider(value = if (duration > 0) (state.playback.positionMs.toFloat() / duration).coerceIn(0f, 1f) else 0f,
            onValueChange = { if (duration > 0) vm.seekTo((it * duration).toLong()) },
            enabled = duration > 0)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(state.playback.positionMs))
            Text(formatTime(duration))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.toggleFavorite(track) }) {
                Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (favorite) "Remover dos favoritos" else "Adicionar aos favoritos")
            }
            Spacer(Modifier.width(24.dp))
            FilledIconButton(onClick = vm::togglePlayback, modifier = Modifier.size(64.dp)) {
                Icon(if (state.playback.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (state.playback.playing) "Pausar" else "Continuar")
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val seconds = ms.coerceAtLeast(0) / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
