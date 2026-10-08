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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
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
    var addingTrack by remember { mutableStateOf<AudioTrack?>(null) }
    Scaffold(
        containerColor = ink,
        contentColor = Color.White,
        bottomBar = {
            Column(Modifier.background(ink)) {
                state.current?.let { track ->
                    MiniPlayer(track, state, onOpen = { expandedPlayer = true },
                        onToggle = vm::togglePlayback, onNext = { vm.next() })
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
                LibraryTab.HOME -> HomeContent(state, vm) { addingTrack = it }
                LibraryTab.SEARCH -> SearchContent(state, vm) { addingTrack = it }
                LibraryTab.LIBRARY -> LibraryContent(state, vm) { addingTrack = it }
            }
        }
    }
    if (expandedPlayer) state.current?.let { track ->
        ModalBottomSheet(
            onDismissRequest = { expandedPlayer = false },
            containerColor = Color(0xFF171D2D),
            contentColor = Color.White
        ) { PlayerDetails(track, state, vm) { addingTrack = it } }
    }
    addingTrack?.let { track ->
        PlaylistPicker(track, state, vm, onDismiss = { addingTrack = null })
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
private fun HomeContent(state: PlayerState, vm: PlayerViewModel, onAdd: (AudioTrack) -> Unit) {
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
                    onPlay = { vm.play(track, state.history.take(6)) },
                    onFavorite = { vm.toggleFavorite(track) },
                    onAdd = { onAdd(track) }, onDownload = { vm.download(track) },
                    onDeleteDownload = { vm.removeDownload(track) },
                    downloaded = state.downloads.any { it.id == track.id },
                    downloading = track.id in state.downloadingIds)
            }
        }
        item {
            SectionTitle("A tua biblioteca", "Acesso rápido")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShortcutCard("Playlists", "${state.playlists.size}", Icons.Default.QueueMusic,
                    Modifier.weight(1f)) {
                    vm.select(LibraryTab.LIBRARY); vm.selectLibrary(LibrarySection.PLAYLISTS)
                }
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
private fun SearchContent(state: PlayerState, vm: PlayerViewModel, onAdd: (AudioTrack) -> Unit) {
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
                    onPlay = { vm.play(track, state.results) },
                    onFavorite = { vm.toggleFavorite(track) },
                    onAdd = { onAdd(track) }, onDownload = { vm.download(track) },
                    onDeleteDownload = { vm.removeDownload(track) },
                    downloaded = state.downloads.any { it.id == track.id },
                    downloading = track.id in state.downloadingIds)
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String, details: String?, retry: () -> Unit) {
    var showDetails by remember(message, details) { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
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
        if (showDetails && !details.isNullOrBlank()) TextButton(onClick = {
            clipboard.setText(AnnotatedString(details))
        }) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = mint)
            Spacer(Modifier.width(6.dp))
            Text("Copiar erro", color = mint)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryContent(state: PlayerState, vm: PlayerViewModel, onAdd: (AudioTrack) -> Unit) {
    var selectedPlaylist by rememberSaveable { mutableStateOf<String?>(null) }
    var creating by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }
    LaunchedEffect(selectedPlaylist, state.playlists) {
        if (selectedPlaylist != null && state.playlists.none { it.id == selectedPlaylist })
            selectedPlaylist = null
    }
    Header("Biblioteca", "Músicas e coleções guardadas no telemóvel.")
    if (selectedPlaylist == null) {
        ScrollableTabRow(selectedTabIndex = LibrarySection.entries.indexOf(state.librarySection),
            containerColor = ink, contentColor = accent, edgePadding = 0.dp) {
            LibrarySection.entries.forEachIndexed { index, section ->
                Tab(
                    selected = state.librarySection == section,
                    onClick = { vm.selectLibrary(section) },
                    text = { Text(when (section) {
                        LibrarySection.PLAYLISTS -> "Playlists"
                        LibrarySection.FAVORITES -> "Favoritos"
                        LibrarySection.HISTORY -> "Recentes"
                        LibrarySection.DOWNLOADS -> "Offline"
                    }) })
            }
        }
        Spacer(Modifier.height(20.dp))
    }
    if (state.librarySection == LibrarySection.DOWNLOADS) {
        if (state.downloadingIds.isNotEmpty()) Text(
            "${state.downloadingIds.size} download(s) em curso. Podes sair da app.", color = mint)
        state.downloadError?.let { Text("Falha no download: $it", color = Color(0xFFFFAFAF)) }
        Spacer(Modifier.height(12.dp))
        if (state.downloads.isEmpty()) EmptyLibrary("Descarrega músicas pelo menu ⋮ para ouvir offline.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.downloads, key = { it.id }) { track ->
                TrackRow(track, state.favorites.any { it.id == track.id },
                    onPlay = { vm.play(track, state.downloads) },
                    onFavorite = { vm.toggleFavorite(track) },
                    onAdd = { onAdd(track) }, onDownload = { vm.download(track) },
                    onDeleteDownload = { vm.removeDownload(track) }, downloaded = true)
            }
        }
    } else if (state.librarySection == LibrarySection.PLAYLISTS) {
        val playlist = state.playlists.firstOrNull { it.id == selectedPlaylist }
        if (playlist == null) {
            Button(onClick = { creating = true },
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = ink)) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Nova playlist")
            }
            Spacer(Modifier.height(14.dp))
            if (state.playlists.isEmpty()) {
                EmptyLibrary("Cria uma playlist e junta-lhe as tuas músicas.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.playlists, key = { it.id }) { item ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                            .background(card).clickable { selectedPlaylist = item.id }
                            .padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QueueMusic, contentDescription = null, tint = accent,
                                modifier = Modifier.size(32.dp))
                            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                                Text(item.name, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("${item.tracks.size} músicas", color = muted)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = muted)
                        }
                    }
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selectedPlaylist = null }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    Text(playlist.name, color = Color.White, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge)
                    Text("${playlist.tracks.size} músicas", color = muted)
                }
                IconButton(onClick = { confirmingDelete = true }) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Apagar playlist", tint = muted)
                }
            }
            Spacer(Modifier.height(16.dp))
            if (playlist.tracks.isNotEmpty()) {
                Button(onClick = { vm.playAll(playlist.tracks) },
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = ink)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Reproduzir playlist")
                }
                Spacer(Modifier.height(12.dp))
            }
            if (playlist.tracks.isEmpty()) EmptyLibrary("Adiciona músicas a esta playlist a partir da pesquisa.")
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(playlist.tracks, key = { it.id }) { track ->
                    TrackRow(track, state.favorites.any { it.id == track.id },
                        onPlay = { vm.play(track, playlist.tracks) },
                        onFavorite = { vm.toggleFavorite(track) },
                        onAdd = { onAdd(track) },
                        onRemove = { vm.removeFromPlaylist(playlist.id, track.id) },
                        onDownload = { vm.download(track) },
                        onDeleteDownload = { vm.removeDownload(track) },
                        downloaded = state.downloads.any { it.id == track.id },
                        downloading = track.id in state.downloadingIds)
                }
            }
        }
    } else {
        val tracks = if (state.librarySection == LibrarySection.FAVORITES)
            state.favorites else state.history
        if (tracks.isEmpty()) EmptyLibrary(if (state.librarySection == LibrarySection.FAVORITES)
            "Guarda músicas com o coração para as encontrares aqui."
            else "As músicas que ouvires aparecem aqui.")
        else {
            Text("${tracks.size} MÚSICAS", style = MaterialTheme.typography.labelMedium, color = muted)
            Spacer(Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 20.dp)) {
                items(tracks, key = { it.id }) { track ->
                    TrackRow(track, state.favorites.any { it.id == track.id },
                        onPlay = { vm.play(track, tracks) },
                        onFavorite = { vm.toggleFavorite(track) },
                        onAdd = { onAdd(track) }, onDownload = { vm.download(track) },
                        onDeleteDownload = { vm.removeDownload(track) },
                        downloaded = state.downloads.any { it.id == track.id },
                        downloading = track.id in state.downloadingIds)
                }
            }
        }
    }
    if (creating) CreatePlaylistDialog(
        onDismiss = { creating = false },
        onCreate = { vm.createPlaylist(it); creating = false })
    if (confirmingDelete) AlertDialog(
        onDismissRequest = { confirmingDelete = false },
        title = { Text("Apagar playlist?") },
        text = { Text("As músicas não são apagadas dos favoritos nem do histórico.") },
        confirmButton = { TextButton(onClick = {
            selectedPlaylist?.let(vm::deletePlaylist)
            selectedPlaylist = null; confirmingDelete = false
        }) { Text("Apagar") } },
        dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text("Cancelar") } })
}

@Composable
private fun EmptyLibrary(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = muted)
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
                     onPlay: () -> Unit, onFavorite: () -> Unit,
                     onAdd: () -> Unit, onDownload: () -> Unit,
                     onDeleteDownload: () -> Unit, downloaded: Boolean,
                     downloading: Boolean = false, onRemove: (() -> Unit)? = null) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(card)
        .clickable(onClick = onPlay).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Artwork(track, Modifier.size(58.dp))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(track.title, color = Color.White, fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(track.uploader, color = muted, style = MaterialTheme.typography.bodySmall,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (downloaded || downloading) Text(if (downloaded) "OFFLINE" else "A DESCARREGAR",
                color = mint, style = MaterialTheme.typography.labelSmall)
        }
        IconButton(onClick = onFavorite) {
            Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (favorite) "Remover dos favoritos" else "Adicionar aos favoritos",
                tint = if (favorite) Color(0xFFFF83AA) else muted)
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Mais opções", tint = muted)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("Adicionar à playlist") }, onClick = {
                    menuOpen = false; onAdd()
                }, leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) })
                if (downloaded) DropdownMenuItem(text = { Text("Apagar download") }, onClick = {
                    menuOpen = false; onDeleteDownload()
                }, leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) })
                else if (!downloading) DropdownMenuItem(text = { Text("Descarregar para offline") },
                    onClick = { menuOpen = false; onDownload() },
                    leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) })
                if (onRemove != null) DropdownMenuItem(
                    text = { Text("Remover desta playlist") }, onClick = {
                        menuOpen = false; onRemove()
                    }, leadingIcon = { Icon(Icons.Default.RemoveCircleOutline, contentDescription = null) })
            }
        }
    }
}

@Composable
private fun MiniPlayer(track: AudioTrack, state: PlayerState,
                       onOpen: () -> Unit, onToggle: () -> Unit, onNext: () -> Unit) {
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
            if (state.queueIndex + 1 < state.queue.size) {
                IconButton(onClick = onNext) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Faixa seguinte",
                        tint = Color.White)
                }
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
private fun PlayerDetails(track: AudioTrack, state: PlayerState, vm: PlayerViewModel,
                          onAdd: (AudioTrack) -> Unit) {
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
        if (state.queue.size > 1) {
            Text("FAIXA ${state.queueIndex + 1} DE ${state.queue.size}",
                color = muted, style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = vm::toggleShuffle) {
                Icon(Icons.Default.Shuffle, contentDescription = "Aleatório",
                    tint = if (state.shuffle) mint else muted)
            }
            IconButton(onClick = vm::previous) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Faixa anterior", tint = Color.White)
            }
            FilledIconButton(onClick = vm::togglePlayback, modifier = Modifier.size(68.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = accent, contentColor = ink)) {
                Icon(if (state.playback.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (state.playback.playing) "Pausar" else "Continuar",
                    modifier = Modifier.size(32.dp))
            }
            IconButton(onClick = { vm.next() }) {
                Icon(Icons.Default.SkipNext, contentDescription = "Faixa seguinte", tint = Color.White)
            }
            IconButton(onClick = vm::toggleRepeatOne) {
                Icon(Icons.Default.RepeatOne, contentDescription = "Repetir faixa",
                    tint = if (state.repeatOne) mint else muted)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.toggleFavorite(track) }) {
                Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (favorite) "Remover dos favoritos" else "Adicionar aos favoritos",
                    tint = if (favorite) Color(0xFFFF83AA) else muted)
            }
            Spacer(Modifier.width(24.dp))
            IconButton(onClick = { onAdd(track) }) {
                Icon(Icons.Default.PlaylistAdd, contentDescription = "Adicionar à playlist", tint = muted)
            }
            Spacer(Modifier.width(24.dp))
            val downloaded = state.downloads.any { it.id == track.id }
            IconButton(onClick = { if (!downloaded) vm.download(track) },
                enabled = !downloaded && track.id !in state.downloadingIds) {
                Icon(if (downloaded) Icons.Default.DownloadDone else Icons.Default.Download,
                    contentDescription = if (downloaded) "Disponível offline" else "Descarregar",
                    tint = if (downloaded) mint else muted)
            }
        }
    }
}

@Composable
private fun CreatePlaylistDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova playlist") },
        text = { OutlinedTextField(value = name, onValueChange = { name = it },
            label = { Text("Nome") }, singleLine = true) },
        confirmButton = { TextButton(onClick = { onCreate(name) }, enabled = name.isNotBlank()) {
            Text("Criar")
        } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
private fun PlaylistPicker(track: AudioTrack, state: PlayerState, vm: PlayerViewModel,
                           onDismiss: () -> Unit) {
    var creating by remember { mutableStateOf(false) }
    if (creating) {
        CreatePlaylistDialog(onDismiss = onDismiss, onCreate = { name ->
            vm.createPlaylist(name)
            val created = vm.state.value.playlists.lastOrNull()
            if (created != null) vm.addToPlaylist(created.id, track)
            onDismiss()
        })
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Adicionar à playlist") },
            text = {
                Column {
                    if (state.playlists.isEmpty()) Text("Ainda não tens playlists.")
                    state.playlists.forEach { playlist ->
                        TextButton(onClick = {
                            vm.addToPlaylist(playlist.id, track); onDismiss()
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text(playlist.name, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { creating = true }) {
                Text("Nova playlist")
            } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
    }
}

private fun formatTime(ms: Long): String {
    val seconds = ms.coerceAtLeast(0) / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
