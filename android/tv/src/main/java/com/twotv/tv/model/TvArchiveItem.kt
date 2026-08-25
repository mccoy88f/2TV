package com.twotv.tv.model

import java.util.UUID

enum class MediaCategory {
    STREAM,
    FILE,
    WEB
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
            val type = reportedType?.uppercase() ?: ""
            return when {
                type == "STREAM" || type == "VIDEO" || type == "AUDIO" -> MediaCategory.STREAM
                type == "WEB" || type == "LINK" -> MediaCategory.WEB
                type == "FILE" || type == "IMAGE" || type == "ALTRO" -> MediaCategory.FILE
                else -> {
                    if (urlOrPath.startsWith("http://") || urlOrPath.startsWith("https://")) {
                        MediaCategory.WEB
                    } else {
                        MediaCategory.FILE
                    }
                }
            }
        }
    }
}
