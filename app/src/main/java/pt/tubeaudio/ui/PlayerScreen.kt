package pt.tubeaudio.ui

import androidx.compose.foundation.background
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
import androidx.compose.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import pt.tubeaudio.model.AudioTrack

private val ink = Color(0xFF0C101B)
private val card = Color(0xFF191F30)
private val muted = Color(0xFFAAB3C7)
private val accent = Color(0xFFA898FF)
private val mint = Color(0xFF8AE9CA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(vm: PlayerViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    var expandedPlayer by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        containerColor = ink,
        contentColor = Color.White,
        bottomBar = {
            Column(Modifier.background(ink)) {
                state.current?.let { track ->
                    MiniPlayer(track, state, onOpen = { expandedPlayer = true },
                        onToggle = vm::togglePlayback)
                }
                NavigationBar(containerColor = Color(0xFF141A29), contentColor = Color.White) {
                    listOf(
                        Triple(LibraryTab.HOME, "Início", Icons.Default.Home),
                        Triple(LibraryTab.SEARCH, "Pesquisar", Icons.Default.Search),
                        Triple(LibraryTab.LIBRARY, "Biblioteca", Icons.Default.LibraryMusic)
                    ).forEach { (tab, title, icon) ->
                        NavigationBarItem(
                            selected = state.tab == tab, onClick = { vm.select(tab) },
                            icon = { Icon(icon, contentDescription = null) }, label = { Text(title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ink, selectedTextColor = Color.White,
                                indicatorColor = accent, unselectedIconColor = muted,
                                unselectedTextColor = muted)
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            when (state.tab) {
                LibraryTab.HOME -> HomeContent(state, vm)
                LibraryTab.SEARCH -> SearchContent(state, vm)
                LibraryTab.LIBRARY -> LibraryContent(state, vm)
            }
        }
    }
    if (expandedPlayer) state.current?.let { track ->
        ModalBottomSheet(
            onDismissRequest = { expandedPlayer = false },
            containerColor = Color(0xFF171D2D),
            contentColor = Color.White
        ) { PlayerDetails(track, state, vm) }
    }
}

@Composable
private fun Header(title: String, subtitle: String) {
    Spacer(Modifier.height(22.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(accent),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = ink)
        }
        Spacer(Modifier.width(12.dp))
        Text("TubeAudio", style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold, color = Color.White)
    }
    Spacer(Modifier.height(28.dp))
    Text(title, style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold, color = Color.White)
    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = muted)
    Spacer(Modifier.height(22.dp))
}

@Composable
private fun HomeContent(state: PlayerState, vm: PlayerViewModel) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 20.dp)) {
        item {
            Header("Bom regresso", "A tua música, sempre contigo.")
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF6653B7), Color(0xFF284B72))))
                    .padding(24.dp)
            ) {
                Icon(Icons.Default.Headphones, contentDescription = null, tint = mint,
                    modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(34.dp))
                Text("Encontra a próxima\nfaixa favorita",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { vm.select(LibraryTab.SEARCH) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = ink)) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Explorar música")
                }
            }
        }
        if (state.history.isNotEmpty()) {
            item { SectionTitle("Ouvir novamente", "As tuas últimas faixas") }
            items(state.history.take(6), key = { "home-${it.id}" }) { track ->
                TrackRow(track, state.favorites.any { it.id == track.id },
                    onPlay = { vm.play(track) }, onFavorite = { vm.toggleFavorite(track) })
            }
        }
        item {
            SectionTitle("A tua biblioteca", "Acesso rápido")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ShortcutCard("Favoritos", "${state.favorites.size} músicas", Icons.Default.Favorite,
                    Modifier.weight(1f)) {
                    vm.select(LibraryTab.LIBRARY); vm.selectLibrary(LibrarySection.FAVORITES)
                }
                ShortcutCard("Histórico", "${state.history.size} músicas", Icons.Default.History,
                    Modifier.weight(1f)) {
                    vm.select(LibraryTab.LIBRARY); vm.selectLibrary(LibrarySection.HISTORY)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Spacer(Modifier.height(12.dp))
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
        color = Color.White)
    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = muted)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun ShortcutCard(title: String, subtitle: String,
                         icon: androidx.compose.ui.graphics.vector.ImageVector,
                         modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(card).clickable(onClick = onClick)
        .padding(18.dp)) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(18.dp))
        Text(title, color = Color.White, fontWeight = FontWeight.Bold)
        Text(subtitle, color = muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SearchContent(state: PlayerState, vm: PlayerViewModel) {
    val keyboard = LocalSoftwareKeyboardController.current
    Header("Pesquisar", "Músicas, artistas e vídeos num só lugar.")
    OutlinedTextField(
        value = state.query, onValueChange = vm::query, singleLine = true,
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
            focusedBorderColor = accent, unfocusedBorderColor = Color(0xFF3B4357),
            cursorColor = accent, focusedContainerColor = card, unfocusedContainerColor = card),
        placeholder = { Text("O que queres ouvir?", color = muted) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = accent) },
        trailingIcon = {
            IconButton(onClick = { keyboard?.hide(); vm.search() }) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Pesquisar", tint = accent)
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); vm.search() })
    )
    Spacer(Modifier.height(18.dp))
    if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = accent)
    state.error?.let { ErrorCard(it, state.errorDetails, vm::search) }
    if (state.results.isEmpty() && !state.loading && state.error == null) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.MusicNote, contentDescription = null, tint = accent,
                modifier = Modifier.size(50.dp))
            Spacer(Modifier.height(16.dp))
            Text("A música começa aqui", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, color = Color.White)
            Text("Pesquisa por uma música ou um artista.", color = muted)
        }
    } else if (state.results.isNotEmpty()) {
        Text("RESULTADOS", style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold, color = muted)
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 20.dp)) {
            items(state.results, key = { it.id }) { track ->
                TrackRow(track, state.favorites.any { it.id == track.id },
                    onPlay = { vm.play(track) }, onFavorite = { vm.toggleFavorite(track) })
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String, details: String?, retry: () -> Unit) {
    var showDetails by remember(message, details) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)
        .clip(RoundedCornerShape(18.dp)).background(Color(0xFF342430)).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFFFAFAF))
            Spacer(Modifier.width(10.dp))
            Text(message, color = Color.White, modifier = Modifier.weight(1f))
        }
        Row {
            TextButton(onClick = retry) { Text("Tentar novamente", color = mint) }
            if (!details.isNullOrBlank()) {
                TextButton(onClick = { showDetails = !showDetails }) {
                    Text("Detalhes", color = muted)
                }
            }
        }
        if (showDetails) Text(details.orEmpty(), color = muted,
            style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun LibraryContent(state: PlayerState, vm: PlayerViewModel) {
    Header("Biblioteca", "O que guardaste e ouviste.")
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        listOf(LibrarySection.FAVORITES, LibrarySection.HISTORY).forEachIndexed { index, section ->
            SegmentedButton(
                selected = state.librarySection == section,
                onClick = { vm.selectLibrary(section) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                label = { Text(if (section == LibrarySection.FAVORITES) "Favoritos" else "Recentes") },
                icon = { Icon(if (section == LibrarySection.FAVORITES)
                    Icons.Default.Favorite else Icons.Default.History, contentDescription = null,
                    modifier = Modifier.size(18.dp)) })
        }
    }
    Spacer(Modifier.height(20.dp))
    val tracks = if (state.librarySection == LibrarySection.FAVORITES)
        state.favorites else state.history
    if (tracks.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(if (state.librarySection == LibrarySection.FAVORITES)
                "Guarda músicas com o coração para as encontrares aqui."
                else "As músicas que ouvires aparecem aqui.",
                color = muted)
        }
    } else {
        Text("${tracks.size} MÚSICAS", style = MaterialTheme.typography.labelMedium, color = muted)
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 20.dp)) {
            items(tracks, key = { it.id }) { track ->
                TrackRow(track, state.favorites.any { it.id == track.id },
                    onPlay = { vm.play(track) }, onFavorite = { vm.toggleFavorite(track) })
            }
        }
    }
}

@Composable
private fun Artwork(track: AudioTrack, modifier: Modifier = Modifier) {
    if (track.thumbnailUrl != null) {
        AsyncImage(model = track.thumbnailUrl, contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(RoundedCornerShape(14.dp)))
    } else {
        Box(modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xFF353353)),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Default.MusicNote, contentDescription = null, tint = accent)
        }
    }
}

@Composable
private fun TrackRow(track: AudioTrack, favorite: Boolean,
                     onPlay: () -> Unit, onFavorite: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(card)
        .clickable(onClick = onPlay).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Artwork(track, Modifier.size(58.dp))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(track.title, color = Color.White, fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(track.uploader, color = muted, style = MaterialTheme.typography.bodySmall,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onFavorite) {
            Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (favorite) "Remover dos favoritos" else "Adicionar aos favoritos",
                tint = if (favorite) Color(0xFFFF83AA) else muted)
        }
    }
}

@Composable
private fun MiniPlayer(track: AudioTrack, state: PlayerState,
                       onOpen: () -> Unit, onToggle: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
        .clip(RoundedCornerShape(20.dp)).background(Color(0xFF30334F))) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Artwork(track, Modifier.size(48.dp))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(track.title, color = Color.White, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                Text(track.uploader, color = muted, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onToggle) {
                Icon(if (state.playback.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (state.playback.playing) "Pausar" else "Continuar",
                    tint = Color.White)
            }
        }
        val duration = state.playback.durationMs
        LinearProgressIndicator(
            progress = { if (duration > 0)
                (state.playback.positionMs.toFloat() / duration).coerceIn(0f, 1f) else 0f },
            modifier = Modifier.fillMaxWidth().height(3.dp), color = mint,
            trackColor = Color.Transparent)
    }
}

@Composable
private fun PlayerDetails(track: AudioTrack, state: PlayerState, vm: PlayerViewModel) {
    val favorite = state.favorites.any { it.id == track.id }
    val duration = state.playback.durationMs
    var dragging by remember(track.id) { mutableFloatStateOf(-1f) }
    val position = if (duration > 0)
        (state.playback.positionMs.toFloat() / duration).coerceIn(0f, 1f) else 0f
    Column(Modifier.fillMaxWidth().padding(horizontal = 26.dp).padding(bottom = 42.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(12.dp))
        Artwork(track, Modifier.fillMaxWidth().height(260.dp))
        Spacer(Modifier.height(28.dp))
        Text(track.title, color = Color.White, style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(track.uploader, color = muted)
        Spacer(Modifier.height(22.dp))
        Slider(value = if (dragging >= 0f) dragging else position,
            onValueChange = { dragging = it },
            onValueChangeFinished = {
                if (duration > 0 && dragging >= 0f) vm.seekTo((dragging * duration).toLong())
                dragging = -1f
            }, enabled = duration > 0,
            colors = SliderDefaults.colors(thumbColor = mint, activeTrackColor = mint))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(state.playback.positionMs), color = muted)
            Text(formatTime(duration), color = muted)
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.toggleFavorite(track) }) {
                Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (favorite) "Remover dos favoritos" else "Adicionar aos favoritos",
                    tint = if (favorite) Color(0xFFFF83AA) else muted)
            }
            Spacer(Modifier.width(32.dp))
            FilledIconButton(onClick = vm::togglePlayback, modifier = Modifier.size(68.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = accent, contentColor = ink)) {
                Icon(if (state.playback.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (state.playback.playing) "Pausar" else "Continuar",
                    modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.width(80.dp))
        }
    }
}

private fun formatTime(ms: Long): String {
    val seconds = ms.coerceAtLeast(0) / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
