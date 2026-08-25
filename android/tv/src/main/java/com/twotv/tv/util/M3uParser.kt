package com.twotv.tv.util

data class M3uChannel(
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val groupTitle: String? = null
)

object M3uParser {
    fun isM3uPlaylist(urlOrPath: String, contentHead: String = ""): Boolean {
        val lower = urlOrPath.lowercase().trim()
        // Standard IPTV M3U playlists end in .m3u or .m3u?
        if (lower.endsWith(".m3u") || lower.contains(".m3u?")) return true

        // If content is provided, verify it has #EXTINF channel definitions
        if (contentHead.isNotBlank()) {
            return contentHead.contains("#EXTINF:") && !contentHead.contains("#EXT-X-STREAM-INF")
        }

        return false
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
                if (currentName.isNotBlank()) {
                    channels.add(
                        M3uChannel(
                            name = currentName,
                            streamUrl = trimmed,
                            logoUrl = currentLogo,
                            groupTitle = currentGroup
                        )
                    )
                }
                currentName = ""
                currentLogo = null
                currentGroup = null
            }
        }
        return channels
    }
}
