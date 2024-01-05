package com.r2devpros.audioplayer.utils

object FormatUtils {
    fun formatLength(durationInMillis: String?): String {
        try {
            if (durationInMillis.isNullOrEmpty())
                return "00:00"

            val durationInMillisLong = durationInMillis.toLong()
            val seconds = durationInMillisLong / 1000
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60

            return String.format("%02d:%02d", minutes, remainingSeconds)
        } catch (_: Exception) {
            return "0:00"
        }
    }

    fun formatFileName(name: String?): String {
        val indexDot = name?.lastIndexOf(".")
        val nameFormatted =
            if (indexDot != null && indexDot != -1) name.substring(0, indexDot) else name
        return nameFormatted ?: "---"
    }
}