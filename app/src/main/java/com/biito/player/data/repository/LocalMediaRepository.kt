package com.biito.player.data.repository

import android.util.Log
import android.content.ContentResolver
import android.content.ContentUris
import android.provider.MediaStore
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
        Log.d("LocalMediaRepository", "Fetching media items...")
        val mediaItems = mutableListOf<MediaItem>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val count = cursor.count
                Log.d("LocalMediaRepository", "Cursor count (with IS_MUSIC filter): $count")
                
                if (count == 0) {
                    // Check if there are any audio items at all without the filter
                    contentResolver.query(collection, arrayOf(MediaStore.Audio.Media._ID), null, null, null)?.use { allCursor ->
                        Log.d("LocalMediaRepository", "Total audio items in MediaStore (without filter): ${allCursor.count}")
                    }
                }

                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn) ?: "Unknown"
                    val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                    val album = cursor.getString(albumColumn) ?: "Unknown Album"
                    val duration = cursor.getLong(durationColumn)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    mediaItems.add(
                        MediaItem(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            duration = duration,
                            contentUri = contentUri
                        )
                    )
                }
            } ?: Log.e("LocalMediaRepository", "Query returned null cursor for $collection")
        } catch (e: SecurityException) {
            Log.e("LocalMediaRepository", "SecurityException: Permission probably not granted yet", e)
        } catch (e: Exception) {
            Log.e("LocalMediaRepository", "Error querying MediaStore", e)
        }
        
        Log.d("LocalMediaRepository", "Emitting ${mediaItems.size} items")
        emit(mediaItems)
    }.flowOn(Dispatchers.IO)
}