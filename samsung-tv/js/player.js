/**
 * Universal Media Player for Samsung Smart TV (Tizen 2.4 - 8.0+)
 * Wraps HTML5 <video>, Hls.js, Samsung webapis.avplay, and Photo/Audio viewers
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
        this.progressBarFillElem = document.getElementById('progress-bar-fill');

        this.currentType = null; // 'VIDEO', 'PHOTO', 'AUDIO', 'WEB'
        this.isAvPlayMode = false;
        this.overlayTimeout = null;
    }

    UniversalPlayer.prototype = {
        playMedia: function (payload) {
            var type = (payload.mediaType || 'VIDEO').toUpperCase();
            var url = payload.url;
            var title = payload.title || 'Contenuto 2TV';

            this.stopCurrent();
            this.currentType = type;

            if (type === 'PHOTO' || type === 'IMAGE') {
                this.showPhoto(url, title);
            } else if (type === 'VIDEO' || type === 'AUDIO') {
                this.playVideo(url, title);
            } else if (type === 'WEB') {
                this.showWebPage(url, title);
            }
        },

        playVideo: function (url, title) {
            var self = this;
            this.playerTitleElem.textContent = title;
            this.playerContainerElem.classList.add('active');
            this.showOverlayTemporarily();

            // Check if Samsung native webapis.avplay is required (older Tizen 2.4 / 3.0 / 4.0 TVs)
            if (typeof webapis !== 'undefined' && webapis.avplay) {
                try {
                    this.isAvPlayMode = true;
                    webapis.avplay.open(url);
                    webapis.avplay.setDisplayRect(0, 0, window.innerWidth, window.innerHeight);
                    webapis.avplay.prepareAsync(function () {
                        webapis.avplay.play();
                    }, function (err) {
                        console.error('AVPlay prepare error, falling back to HTML5:', err);
                        self.playVideoHtml5(url);
                    });
                    return;
                } catch (e) {
                    console.log('AVPlay error, using HTML5 video:', e.message);
                }
            }

            this.playVideoHtml5(url);
        },

        playVideoHtml5: function (url) {
            var self = this;
            this.isAvPlayMode = false;
            this.videoElem.src = url;

            // Handle HLS .m3u8 live streams with Hls.js if available
            if (url.indexOf('.m3u8') !== -1 && typeof Hls !== 'undefined' && Hls.isSupported()) {
                var hls = new Hls();
                hls.loadSource(url);
                hls.attachMedia(this.videoElem);
                hls.on(Hls.Events.MANIFEST_PARSED, function () {
                    self.videoElem.play();
                });
            } else {
                this.videoElem.play().catch(function (err) {
                    console.log('Video autoplay interrupted:', err);
                });
            }

            this.videoElem.ontimeupdate = function () {
                if (self.videoElem.duration) {
                    var pct = (self.videoElem.currentTime / self.videoElem.duration) * 100;
                    self.progressBarFillElem.style.width = pct + '%';
                }
            };
        },

        showPhoto: function (url, title) {
            this.photoImgElem.src = url;
            this.photoViewerElem.classList.add('active');
            if (window.App) window.App.showToast('Foto: ' + title);
        },

        showWebPage: function (url, title) {
            window.open(url, '_blank');
            if (window.App) window.App.showToast('Apertura link: ' + title);
        },

        togglePlayPause: function () {
            if (this.isAvPlayMode && typeof webapis !== 'undefined') {
                var state = webapis.avplay.getState();
                if (state === 'PLAYING') {
                    webapis.avplay.pause();
                } else {
                    webapis.avplay.play();
                }
            } else if (this.videoElem) {
                if (this.videoElem.paused) {
                    this.videoElem.play();
                } else {
                    this.videoElem.pause();
                }
            }
            this.showOverlayTemporarily();
        },

        seek: function (seconds) {
            if (this.isAvPlayMode && typeof webapis !== 'undefined') {
                try {
                    var curTime = webapis.avplay.getCurrentTime();
                    webapis.avplay.seekTo(curTime + (seconds * 1000));
                } catch (e) {}
            } else if (this.videoElem) {
                this.videoElem.currentTime += seconds;
            }
            this.showOverlayTemporarily();
        },

        stopCurrent: function () {
            if (this.isAvPlayMode && typeof webapis !== 'undefined') {
                try {
                    webapis.avplay.stop();
                    webapis.avplay.close();
                } catch (e) {}
            }

            if (this.videoElem) {
                this.videoElem.pause();
                this.videoElem.src = '';
            }

            this.playerContainerElem.classList.remove('active');
            this.photoViewerElem.classList.remove('active');
            this.currentType = null;
        },

        showOverlayTemporarily: function () {
            var self = this;
            this.playerOverlayElem.classList.remove('hidden');
            if (this.overlayTimeout) clearTimeout(this.overlayTimeout);
            this.overlayTimeout = setTimeout(function () {
                self.playerOverlayElem.classList.add('hidden');
            }, 4000);
        },

        isPlaying: function () {
            return this.currentType !== null;
        }
    };

    window.UniversalPlayer = UniversalPlayer;
})(window);
