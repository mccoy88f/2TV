package com.twotv.tv.util

data class M3uChannel(
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val groupTitle: String? = null
)

object M3uParser {
    fun isM3uPlaylist(urlOrPath: String, contentHead: String = ""): Boolean {
        val lower = urlOrPath.lowercase()
        return lower.endsWith(".m3u") || lower.contains(".m3u?") || contentHead.contains("#EXTM3U")
    }

    fun parse(content: String): List<M3uChannel> {
        val channels = mutableListOf<M3uChannel>()
        val lines = content.lines()
        var currentName = ""
        var currentLogo: String? = null
        var currentGroup: String? = null

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXTINF:", ignoreCase = true)) {
                // Extract tvg-logo if present
                val logoMatch = Regex("""tvg-logo="([^"]+)"""", RegexOption.IGNORE_CASE).find(trimmed)
                currentLogo = logoMatch?.groupValues?.get(1)

                // Extract group-title if present
                val groupMatch = Regex("""group-title="([^"]+)"""", RegexOption.IGNORE_CASE).find(trimmed)
                currentGroup = groupMatch?.groupValues?.get(1)

                // Extract channel name (after comma)
                val commaIndex = trimmed.lastIndexOf(',')
                currentName = if (commaIndex != -1) {
                    trimmed.substring(commaIndex + 1).trim()
                } else {
                    "Canale ${channels.size + 1}"
                }
            } else if (trimmed.isNotBlank() && !trimmed.startsWith("#")) {
                if (currentName.isBlank()) {
                    currentName = "Canale ${channels.size + 1}"
                }
                channels.add(
                    M3uChannel(
                        name = currentName,
                        streamUrl = trimmed,
                        logoUrl = currentLogo,
                        groupTitle = currentGroup
                    )
                )
                currentName = ""
                currentLogo = null
                currentGroup = null
            }
        }
        return channels
    }
}
