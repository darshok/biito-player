package com.biito.player.ui.feature.tracklist

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.biito.player.LocalSharedTransitionScope
import com.biito.player.R
import com.biito.player.components.ui.BiitoMediaItemRow
import com.biito.player.components.ui.BiitoPlaybackControlBar
import com.biito.player.components.ui.BiitoTopAppBar
import com.biito.player.navigation.SharedTransitionKeys

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackListScreen(
    onNavigateToPlayer: () -> Unit,
    viewModel: TrackListViewModel = hiltViewModel(),
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

    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalNavAnimatedContentScope.current

    Scaffold(
        topBar = {
            BiitoTopAppBar(
                title = stringResource(id = R.string.track_list_screen_title),
            )
        },
        bottomBar = {
            playbackState.currentMediaItem?.let { currentItem ->
                
                val modifier = if (sharedTransitionScope != null) {
                    with(sharedTransitionScope) {
                        Modifier
                            .sharedBounds(
                                sharedContentState = rememberSharedContentState(key = SharedTransitionKeys.PLAYER_BOUNDS),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                            .clickable { onNavigateToPlayer() }
                    }
                } else {
                    Modifier.clickable { onNavigateToPlayer() }
                }

                BiitoPlaybackControlBar(
                    title = currentItem.title,
                    artist = currentItem.artist,
                    isPlaying = playbackState.isPlaying,
                    currentPosition = playbackState.currentPosition,
                    duration = playbackState.duration,
                    onTogglePlayPause = viewModel::togglePlayPause,
                    onSeek = viewModel::seekTo,
                    modifier = modifier
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