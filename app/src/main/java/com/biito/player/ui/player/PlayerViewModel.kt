package com.biito.player.ui.player

import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.biito.player.domain.model.MediaItem
import com.biito.player.domain.usecase.GetMediaItemsUseCase
import com.biito.player.media.service.BiitoMediaService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val getMediaItemsUseCase: GetMediaItemsUseCase,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _mediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaItems = _mediaItems.asStateFlow()

    private var mediaController: MediaController? = null

    init {
        initializeController()
    }

    fun loadMediaItems() {
        viewModelScope.launch {
            getMediaItemsUseCase().collect { items ->
                _mediaItems.value = items
            }
        }
    }

    private fun initializeController() {
        viewModelScope.launch {
            val sessionToken = SessionToken(context, ComponentName(context, BiitoMediaService::class.java))
            mediaController = MediaController.Builder(context, sessionToken).buildAsync().await()
        }
    }

    fun playMedia(mediaItem: MediaItem) {
        mediaController?.let { controller ->
            val media3Item = Media3Item.Builder()
                .setMediaId(mediaItem.id.toString())
                .setUri(mediaItem.contentUri)
                .build()
            controller.setMediaItem(media3Item)
            controller.prepare()
            controller.play()
        }
    }

    override fun onCleared() {
        mediaController?.release()
        super.onCleared()
    }
}