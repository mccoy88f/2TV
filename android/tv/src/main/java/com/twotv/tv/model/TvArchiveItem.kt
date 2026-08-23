package com.twotv.tv.model

import java.util.UUID

enum class MediaCategory {
    FOTO,
    VIDEO,
    AUDIO,
    WEB,
    ALTRO
}

data class TvArchiveItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val pathOrUrl: String,
    val category: MediaCategory,
    val isLocalFile: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun categorize(urlOrPath: String, reportedType: String?): MediaCategory {
            val lower = urlOrPath.lowercase()
            val type = reportedType?.uppercase() ?: ""

            return when {
                type == "IMAGE" || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp") || lower.endsWith(".gif") -> MediaCategory.FOTO
                type == "VIDEO" || type == "STREAM" || lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".webm") || lower.endsWith(".m3u8") -> MediaCategory.VIDEO
                type == "AUDIO" || lower.endsWith(".mp3") || lower.endsWith(".aac") || lower.endsWith(".wav") || lower.endsWith(".flac") -> MediaCategory.AUDIO
                lower.startsWith("http://") || lower.startsWith("https://") -> MediaCategory.WEB
                else -> MediaCategory.ALTRO
            }
        }
    }
}
