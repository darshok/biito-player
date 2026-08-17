package com.biito.player.domain.model

import android.net.Uri

data class MediaItem(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val contentUri: Uri,
    val artworkUri: Uri? = null
)