package com.biito.player.domain.repository

import com.biito.player.domain.model.MediaItem
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun getMediaItems(): Flow<List<MediaItem>>
}