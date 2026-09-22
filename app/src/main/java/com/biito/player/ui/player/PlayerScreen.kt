package com.biito.player.ui.player

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.biito.player.R
import com.biito.player.components.ui.BiitoMediaItemRow
import com.biito.player.components.ui.BiitoPlaybackControlBar
import com.biito.player.components.ui.BiitoTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val mediaItems by viewModel.mediaItems.collectAsState()
    val playbackState by viewModel.playbackUiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.loadMediaItems()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    Scaffold(
        topBar = {
            BiitoTopAppBar(
                title = stringResource(id = R.string.player_screen_title),
            )
        },
        bottomBar = {
            playbackState.currentMediaItem?.let { currentItem ->
                BiitoPlaybackControlBar(
                    title = currentItem.title,
                    artist = currentItem.artist,
                    isPlaying = playbackState.isPlaying,
                    currentPosition = playbackState.currentPosition,
                    duration = playbackState.duration,
                    onTogglePlayPause = viewModel::togglePlayPause,
                    onSeek = viewModel::seekTo
                )
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshMediaItems() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(mediaItems) { item ->
                    BiitoMediaItemRow(
                        title = item.title,
                        artist = item.artist,
                        album = item.album,
                        duration = item.duration,
                        artworkUri = item.artworkUri,
                        isPlaying = playbackState.currentMediaItem?.id == item.id,
                        onClick = { viewModel.playMedia(item) }
                    )
                }
            }
        }
    }
}