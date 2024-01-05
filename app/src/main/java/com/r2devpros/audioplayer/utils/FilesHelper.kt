package com.r2devpros.audioplayer.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.documentfile.provider.DocumentFile
import com.r2devpros.audioplayer.R

object FilesHelper {
    fun getAudioDuration(context: Context, file: DocumentFile): String {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, file.uri)
        val timeMills = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        return FormatUtils.formatLength(timeMills)
    }

    fun getAudioAuthor(context: Context, file: DocumentFile): String {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, file.uri)
        return retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            ?: context.getString(R.string.txt_unknown)
    }

    fun getAudioArt(context: Context, file: DocumentFile): Bitmap? {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, file.uri)
        val bytes = retriever.embeddedPicture
        val albumArtBitmap: Bitmap? = if (bytes != null) {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } else {
            null
        }
        return albumArtBitmap
    }
}