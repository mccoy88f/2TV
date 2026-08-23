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
                // PDF, Documents, APKs, Zip, Other Files -> ALTRO (Opened with system default apps)
                lower.endsWith(".pdf") || lower.endsWith(".doc") || lower.endsWith(".docx") || 
                lower.endsWith(".apk") || lower.endsWith(".txt") || lower.endsWith(".zip") || 
                lower.endsWith(".rar") || type == "FILE" || type == "ALTRO" -> MediaCategory.ALTRO

                // Photos & Images
                type == "IMAGE" || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || 
                lower.endsWith(".png") || lower.endsWith(".webp") || lower.endsWith(".gif") || lower.endsWith(".bmp") -> MediaCategory.FOTO

                // Audio
                type == "AUDIO" || lower.endsWith(".mp3") || lower.endsWith(".aac") || 
                lower.endsWith(".wav") || lower.endsWith(".flac") || lower.endsWith(".ogg") -> MediaCategory.AUDIO

                // Video Streams & Files
                lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".webm") || 
                lower.endsWith(".m3u8") || lower.endsWith(".ts") || lower.endsWith(".avi") -> MediaCategory.VIDEO

                // Web Pages
                type == "WEB" -> MediaCategory.WEB
                lower.startsWith("http://") || lower.startsWith("https://") -> {
                    if (type == "STREAM" || type == "VIDEO") MediaCategory.VIDEO else MediaCategory.WEB
                }

                else -> MediaCategory.ALTRO
            }
        }
    }
}
