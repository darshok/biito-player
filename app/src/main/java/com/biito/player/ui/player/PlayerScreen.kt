package com.biito.player.ui.player

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.biito.player.domain.model.MediaItem

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val mediaItems by viewModel.mediaItems.collectAsState()

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

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(mediaItems) { item ->
                MediaItemRow(
                    mediaItem = item
                ) { viewModel.playMedia(item) }
            }
        }
    }
}

@Composable
fun MediaItemRow(
    mediaItem: MediaItem,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(mediaItem.title) },
        supportingContent = { Text("${mediaItem.artist} • ${mediaItem.album}") },
        modifier = Modifier.clickable { onClick() }
    )
}