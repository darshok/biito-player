package com.biito.player.domain.usecase

import com.biito.player.domain.model.MediaItem
import com.biito.player.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMediaItemsUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    operator fun invoke(): Flow<List<MediaItem>> {
        return repository.getMediaItems()
    }
}