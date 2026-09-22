package com.biito.player.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import com.biito.player.domain.model.MediaItem
import com.biito.player.domain.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class LocalMediaRepository @Inject constructor(
    private val contentResolver: ContentResolver,
) : MediaRepository {

    override fun getMediaItems(): Flow<List<MediaItem>> = flow {
        val mediaItems = mutableListOf<MediaItem>()
        val queryUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            contentResolver.query(
                queryUri,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                if (cursor.count == 0) {
                    // Check if there are any audio items at all without the filter
                    contentResolver.query(
                        queryUri,
                        arrayOf(MediaStore.Audio.Media._ID),
                        null,
                        null,
                        null
                    )?.use { allCursor ->
                        Log.d(
                            "LocalMediaRepository",
                            "Total audio items in MediaStore (without filter): ${allCursor.count}"
                        )
                    }
                }

                while (cursor.moveToNext()) {
                    val id =
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                    val title =
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
                            ?: "Unknown"
                    val artist =
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
                            ?: "Unknown Artist"
                    val album =
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM))
                            ?: "Unknown Album"
                    val duration =
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
                    val albumId =
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))

                    val contentUri =
                        ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                    val artworkUri = ContentUris.withAppendedId(
                        "content://media/external/audio/albumart".toUri(),
                        albumId
                    )

                    mediaItems.add(
                        MediaItem(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            duration = duration,
                            contentUri = contentUri,
                            artworkUri = artworkUri
                        )
                    )
                }
            } ?: Log.e("LocalMediaRepository", "Query returned null cursor for $queryUri")
        } catch (e: SecurityException) {
            Log.e(
                "LocalMediaRepository",
                "SecurityException: Permission probably not granted yet",
                e
            )
        } catch (e: Exception) {
            Log.e("LocalMediaRepository", "Error querying MediaStore", e)
        }
        emit(mediaItems)
    }.flowOn(Dispatchers.IO)
}