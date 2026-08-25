/**
 * 2TV Shared IPTV M3U Playlist Parser (Universal ES5/ES6 compatible)
 * Parses #EXTINF channel names, tvg-logo, group-title, and stream URLs.
 */
(function (window) {
    'use strict';

    var M3uParser = {
        isM3uPlaylist: function (urlOrPath, contentHead) {
            var lower = (urlOrPath || '').toLowerCase().trim();
            if (lower.indexOf('.m3u') !== -1 && lower.indexOf('.m3u8') === -1) return true;
            if (contentHead && contentHead.indexOf('#EXTINF:') !== -1 && contentHead.indexOf('#EXT-X-STREAM-INF') === -1) {
                return true;
            }
            return false;
        },

        parse: function (content) {
            var channels = [];
            if (!content) return channels;

            var lines = content.split(/\r?\n/);
            var currentName = '';
            var currentLogo = null;
            var currentGroup = null;

            for (var i = 0; i < lines.length; i++) {
                var line = lines[i].trim();

                if (line.indexOf('#EXTINF:') === 0 || line.indexOf('#extinf:') === 0) {
                    // Extract tvg-logo
                    var logoMatch = /tvg-logo="([^"]+)"/i.exec(line);
                    currentLogo = logoMatch ? logoMatch[1] : null;

                    // Extract group-title
                    var groupMatch = /group-title="([^"]+)"/i.exec(line);
                    currentGroup = groupMatch ? groupMatch[1] : null;

                    // Extract channel name (after comma)
                    var commaIndex = line.lastIndexOf(',');
                    if (commaIndex !== -1) {
                        currentName = line.substring(commaIndex + 1).trim();
                    } else {
                        currentName = 'Canale ' + (channels.length + 1);
                    }
                } else if (line.length > 0 && line.indexOf('#') !== 0) {
                    if (!currentName) {
                        currentName = 'Canale ' + (channels.length + 1);
                    }
                    channels.push({
                        name: currentName,
                        streamUrl: line,
                        logoUrl: currentLogo,
                        groupTitle: currentGroup
                    });
                    currentName = '';
                    currentLogo = null;
                    currentGroup = null;
                }
            }
            return channels;
        }
    };

    window.M3uParser = M3uParser;
})(window);
