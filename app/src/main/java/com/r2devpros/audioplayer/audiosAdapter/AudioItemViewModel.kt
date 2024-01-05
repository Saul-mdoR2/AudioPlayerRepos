package com.r2devpros.audioplayer.audiosAdapter

import android.graphics.Bitmap
import androidx.documentfile.provider.DocumentFile
import com.r2devpros.audioplayer.utils.FormatUtils

class AudioItemViewModel {
    var file: DocumentFile? = null

    val name: String
        get() = FormatUtils.formatFileName(file?.name)

    var duration: String = "0:00"

    var author: String = "---"

    var imageCover: Bitmap? = null
}