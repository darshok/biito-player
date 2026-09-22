package com.biito.player.ui.feature.player

import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.biito.player.domain.model.MediaItem
import com.biito.player.domain.usecase.GetMediaItemsUseCase
import com.biito.player.media.service.BiitoMediaService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import androidx.media3.common.MediaItem as Media3Item

data class PlayerUiState(
    val currentMediaItem: MediaItem? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val getMediaItemsUseCase: GetMediaItemsUseCase,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private var allMediaItems: List<MediaItem> = emptyList()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState = _uiState.asStateFlow()

    private var mediaController: MediaController? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onMediaItemTransition(mediaItem: Media3Item?, reason: Int) {
            mediaItem?.let {
                val currentItem = allMediaItems.find { item -> item.id.toString() == mediaItem.mediaId }
                _uiState.update {
                    it.copy(
                        currentMediaItem = currentItem,
                        duration = mediaController?.duration?.coerceAtLeast(0L) ?: 0L
                    )
                }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                _uiState.update {
                    it.copy(duration = mediaController?.duration?.coerceAtLeast(0L) ?: 0L)
                }
            }
        }
        
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.contains(Player.EVENT_PLAYBACK_PARAMETERS_CHANGED) ||
                events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) ||
                events.contains(Player.EVENT_POSITION_DISCONTINUITY)
            ) {
                _uiState.update { it.copy(currentPosition = player.currentPosition) }
            }
        }
    }

    init {
        loadMediaItems()
        initializeController()
        startPositionUpdates()
    }

    private fun loadMediaItems() {
        viewModelScope.launch {
            getMediaItemsUseCase().collect { items ->
                allMediaItems = items
                // Update current media item if it was already playing before we loaded the list
                mediaController?.currentMediaItem?.let { currentMedia3Item ->
                    val currentItem = allMediaItems.find { it.id.toString() == currentMedia3Item.mediaId }
                    _uiState.update { it.copy(currentMediaItem = currentItem) }
                }
            }
        }
    }

    private fun initializeController() {
        viewModelScope.launch {
            val sessionToken =
                SessionToken(context, ComponentName(context, BiitoMediaService::class.java))
            val controller = MediaController.Builder(context, sessionToken).buildAsync().await()
            mediaController = controller
            controller.addListener(playerListener)

            _uiState.update {
                it.copy(
                    isPlaying = controller.isPlaying,
                    currentMediaItem = allMediaItems.find { item -> item.id.toString() == controller.currentMediaItem?.mediaId },
                    duration = controller.duration.coerceAtLeast(0L),
                    currentPosition = controller.currentPosition
                )
            }
        }
    }

    private fun startPositionUpdates() {
        viewModelScope.launch {
            _uiState
                .map { it.isPlaying }
                .distinctUntilChanged()
                .collectLatest { isPlaying ->
                    if (isPlaying) {
                        while (true) {
                            mediaController?.let { controller ->
                                _uiState.update { it.copy(currentPosition = controller.currentPosition) }
                            }
                            delay(500.milliseconds)
                        }
                    }
                }
        }
    }

    fun togglePlayPause() {
        mediaController?.let { controller ->
            if (controller.isPlaying) {
                controller.pause()
            } else {
                controller.play()
            }
        }
    }
    
    fun seekToNext() {
        mediaController?.seekToNext()
    }
    
    fun seekToPrevious() {
        mediaController?.seekToPrevious()
    }

    fun seekTo(position: Long) {
        mediaController?.seekTo(position)
        _uiState.update { it.copy(currentPosition = position) }
    }

    override fun onCleared() {
        mediaController?.removeListener(playerListener)
        mediaController?.release()
    }
}