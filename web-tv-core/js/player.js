/**
 * 2TV Shared Universal Media Player (Tizen, webOS, VIDAA)
 * Supports HTML5 Video, HLS.js (.m3u8), IPTV M3U Playlist Selector, Photo & Web Link Viewers
 */
(function (window) {
    'use strict';

    function UniversalPlayer() {
        this.videoElem = document.getElementById('video-player');
        this.photoViewerElem = document.getElementById('photo-viewer');
        this.photoImgElem = document.getElementById('photo-img');
        this.playerContainerElem = document.getElementById('player-container');
        this.playerOverlayElem = document.getElementById('player-overlay');
        this.playerTitleElem = document.getElementById('player-title');
        this.badgeElem = document.getElementById('player-badge');
        this.btnM3uListElem = document.getElementById('btn-m3u-list');
        this.btnCloseElem = document.getElementById('btn-close-player');

        this.currentType = null; // 'STREAM', 'FILE', 'LINK'
        this.currentM3uChannels = [];
        this.currentM3uTitle = '';
        this.overlayTimeout = null;

        this.init();
    }

    UniversalPlayer.prototype = {
        init: function () {
            var self = this;
            if (this.btnM3uListElem) {
                this.btnM3uListElem.addEventListener('click', function () {
                    if (self.currentM3uChannels.length > 0) {
                        self.showM3uChannelsModal(self.currentM3uTitle, self.currentM3uChannels);
                    }
                });
            }

            if (this.btnCloseElem) {
                this.btnCloseElem.addEventListener('click', function () {
                    self.stopCurrent();
                });
            }
        },

        playMedia: function (payload) {
            var url = (payload.url || payload.pathOrUrl || '').trim();
            var title = payload.title || 'Contenuto 2TV';
            var category = (payload.category || payload.mediaType || 'STREAM').toUpperCase();

            this.stopCurrent();

            // Check if URL is an IPTV M3U Playlist (.m3u)
            if (window.M3uParser && window.M3uParser.isM3uPlaylist(url)) {
                this.fetchAndParseM3u(url, title);
                return;
            }

            if (category === 'PHOTO' || category === 'IMAGE') {
                this.showPhoto(url, title);
            } else if (category === 'WEB' || category === 'LINK') {
                this.showWebPage(url, title);
            } else {
                this.playStream(url, title, category);
            }
        },

        fetchAndParseM3u: function (url, title) {
            var self = this;
            if (window.App) window.App.showToast('Caricamento lista canali IPTV...');

            fetch(url)
                .then(function (res) { return res.text(); })
                .then(function (text) {
                    var channels = window.M3uParser.parse(text);
                    if (channels && channels.length > 0) {
                        self.currentM3uTitle = title;
                        self.currentM3uChannels = channels;
                        self.showM3uChannelsModal(title, channels);
                    } else {
                        self.playStream(url, title, 'STREAM');
                    }
                })
                .catch(function (err) {
                    console.error('Error loading M3U:', err);
                    self.playStream(url, title, 'STREAM');
                });
        },

        showM3uChannelsModal: function (playlistTitle, channels) {
            var self = this;
            var modal = document.getElementById('m3u-modal');
            var titleElem = document.getElementById('m3u-modal-title');
            var bodyElem = document.getElementById('m3u-modal-body');

            if (!modal || !bodyElem) return;

            if (titleElem) {
                titleElem.textContent = playlistTitle + ' (' + channels.length + ' canali)';
            }

            var html = '';
            for (var i = 0; i < channels.length; i++) {
                var ch = channels[i];
                var groupTag = ch.groupTitle ? '[' + ch.groupTitle + '] ' : '';
                html += '<div class="modal-list-item" data-channel-index="' + i + '">' +
                            self.escapeHtml(groupTag + ch.name) +
                        '</div>';
            }
            bodyElem.innerHTML = html;
            modal.classList.add('active');

            // Attach click listeners to channel items
            var items = bodyElem.querySelectorAll('.modal-list-item');
            for (var j = 0; j < items.length; j++) {
                (function (index) {
                    items[index].addEventListener('click', function () {
                        modal.classList.remove('active');
                        var selectedChannel = channels[index];
                        self.playStream(selectedChannel.streamUrl, selectedChannel.name, 'STREAM', true);
                    });
                })(j);
            }
        },

        playStream: function (url, title, category, isM3u) {
            var self = this;
            this.currentType = category || 'STREAM';

            if (this.playerTitleElem) this.playerTitleElem.textContent = title;
            if (this.badgeElem) {
                this.badgeElem.textContent = this.currentType;
                this.badgeElem.className = 'badge-tag badge-' + this.currentType.toLowerCase();
            }

            if (this.btnM3uListElem) {
                if (isM3u || this.currentM3uChannels.length > 0) {
                    this.btnM3uListElem.style.display = 'inline-flex';
                } else {
                    this.btnM3uListElem.style.display = 'none';
                }
            }

            if (this.playerContainerElem) this.playerContainerElem.classList.add('active');
            this.showOverlayTemporarily();

            // Try Samsung native AVPlay or standard HTML5 video
            if (typeof webapis !== 'undefined' && webapis.avplay) {
                try {
                    webapis.avplay.open(url);
                    webapis.avplay.setDisplayRect(0, 0, window.innerWidth, window.innerHeight);
                    webapis.avplay.prepareAsync(function () {
                        webapis.avplay.play();
                    }, function (err) {
                        self.playVideoHtml5(url);
                    });
                    return;
                } catch (e) {}
            }

            this.playVideoHtml5(url);
        },

        playVideoHtml5: function (url) {
            if (!this.videoElem) return;
            this.videoElem.src = url;

            if (url.indexOf('.m3u8') !== -1 && typeof Hls !== 'undefined' && Hls.isSupported()) {
                var hls = new Hls();
                hls.loadSource(url);
                hls.attachMedia(this.videoElem);
                hls.on(Hls.Events.MANIFEST_PARSED, function () {
                    self.videoElem.play();
                });
            } else {
                this.videoElem.play().catch(function (err) {
                    console.log('Autoplay handled:', err);
                });
            }
        },

        showPhoto: function (url, title) {
            if (this.photoImgElem) this.photoImgElem.src = url;
            if (this.photoViewerElem) this.photoViewerElem.classList.add('active');
            if (window.App) window.App.showToast('Foto: ' + title);
        },

        showWebPage: function (url, title) {
            window.open(url, '_blank');
            if (window.App) window.App.showToast('Link: ' + title);
        },

        stopCurrent: function () {
            if (typeof webapis !== 'undefined' && webapis.avplay) {
                try {
                    webapis.avplay.stop();
                    webapis.avplay.close();
                } catch (e) {}
            }

            if (this.videoElem) {
                this.videoElem.pause();
                this.videoElem.src = '';
            }

            if (this.playerContainerElem) this.playerContainerElem.classList.remove('active');
            if (this.photoViewerElem) this.photoViewerElem.classList.remove('active');
            this.currentType = null;
        },

        showOverlayTemporarily: function () {
            var self = this;
            if (!this.playerOverlayElem) return;
            this.playerOverlayElem.classList.remove('hidden');
            if (this.overlayTimeout) clearTimeout(this.overlayTimeout);
            this.overlayTimeout = setTimeout(function () {
                self.playerOverlayElem.classList.add('hidden');
            }, 3500);
        },

        isPlaying: function () {
            return this.currentType !== null;
        },

        escapeHtml: function (str) {
            return (str || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
        }
    };

    window.UniversalPlayer = UniversalPlayer;
})(window);
