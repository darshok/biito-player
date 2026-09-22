package com.biito.player.ui.feature.tracklist

import android.content.ComponentName
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import androidx.media3.common.MediaItem as Media3Item

data class PlaybackUiState(
    val currentMediaItem: MediaItem? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
)

@HiltViewModel
class TrackListViewModel @Inject constructor(
    private val getMediaItemsUseCase: GetMediaItemsUseCase,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _mediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaItems = _mediaItems.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _playbackUiState = MutableStateFlow(PlaybackUiState())
    val playbackUiState = _playbackUiState.asStateFlow()

    private var mediaController: MediaController? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackUiState.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onMediaItemTransition(mediaItem: Media3Item?, reason: Int) {
            mediaItem?.let {
                val currentItem = _mediaItems.value.find { it.id.toString() == mediaItem.mediaId }
                _playbackUiState.update {
                    it.copy(
                        currentMediaItem = currentItem,
                        duration = mediaController?.duration?.coerceAtLeast(0L) ?: 0L
                    )
                }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                _playbackUiState.update {
                    it.copy(duration = mediaController?.duration?.coerceAtLeast(0L) ?: 0L)
                }
            }
        }
        
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.contains(Player.EVENT_PLAYBACK_PARAMETERS_CHANGED) ||
                events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) ||
                events.contains(Player.EVENT_POSITION_DISCONTINUITY)
            ) {
                _playbackUiState.update { it.copy(currentPosition = player.currentPosition) }
            }
        }
    }

    init {
        initializeController()
        startPositionUpdates()
    }

    fun loadMediaItems() {
        viewModelScope.launch {
            getMediaItemsUseCase().collect { items ->
                _mediaItems.value = items
            }
        }
    }

    fun refreshMediaItems() {
        viewModelScope.launch {
            if (_isRefreshing.value) return@launch
            _isRefreshing.value = true

            try {
                withContext(Dispatchers.IO) {
                    val filesToScan = mutableListOf<String>()
                    val dirs = listOf(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    )

                    dirs.filter { it.exists() }.forEach { dir ->
                        dir.walkTopDown()
                            .filter { it.isFile }
                            .filter { it.extension.lowercase() in listOf("mp3", "wav", "flac", "ogg", "m4a", "aac") }
                            .forEach { filesToScan.add(it.absolutePath) }
                    }

                    if (filesToScan.isNotEmpty()) {
                        val latch = CountDownLatch(filesToScan.size)
                        MediaScannerConnection.scanFile(context, filesToScan.toTypedArray(), null) { _, _ ->
                            latch.countDown()
                        }
                        latch.await(3, TimeUnit.SECONDS)
                    }
                }

                _mediaItems.value = getMediaItemsUseCase().first()
            } finally {
                _isRefreshing.value = false
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

            _playbackUiState.update {
                it.copy(
                    isPlaying = controller.isPlaying,
                    currentMediaItem = _mediaItems.value.find { item -> item.id.toString() == controller.currentMediaItem?.mediaId },
                    duration = controller.duration.coerceAtLeast(0L),
                    currentPosition = controller.currentPosition
                )
            }
        }
    }

    private fun startPositionUpdates() {
        viewModelScope.launch {
            _playbackUiState
                .map { it.isPlaying }
                .distinctUntilChanged()
                .collectLatest { isPlaying ->
                    if (isPlaying) {
                        while (true) {
                            mediaController?.let { controller ->
                                _playbackUiState.update { it.copy(currentPosition = controller.currentPosition) }
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

    fun seekTo(position: Long) {
        mediaController?.seekTo(position)
        _playbackUiState.update { it.copy(currentPosition = position) }
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
        mediaController?.removeListener(playerListener)
        mediaController?.release()
    }
}