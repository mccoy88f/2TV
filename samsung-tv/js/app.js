/**
 * 2TV Samsung Smart TV Receiver Main Application Controller
 * Handles UI state, focus navigation, pairing, history, and key routing
 */
(function (window) {
    'use strict';

    function AppController() {
        this.player = null;
        this.server = null;
        this.history = [];
        this.focusedIndex = 0;
        this.pairingToken = '2TV-' + Math.floor(1000 + Math.random() * 9000);
    }

    AppController.prototype = {
        init: function () {
            var self = this;
            console.log('Initializing 2TV Samsung Smart TV App...');

            this.player = new UniversalPlayer();
            this.loadHistory();

            // Initialize Remote Control Keys
            TizenKeys.init(function (action, event) {
                self.handleKeyAction(action, event);
            });

            // Initialize Server & QR Code
            this.server = new TvServer(8080, this.pairingToken, function (payload) {
                self.onReceiveMedia(payload);
            });
            this.server.init();

            this.updateIpDisplay('192.168.1.100', 8080);
            this.renderHistory();
            this.updateFocus();
        },

        handleKeyAction: function (action, event) {
            if (this.player && this.player.isPlaying()) {
                // Key actions during playback
                switch (action) {
                    case 'RETURN':
                    case 'STOP':
                        this.player.stopCurrent();
                        this.showToast('Riproduzione interrotta');
                        break;
                    case 'ENTER':
                    case 'PLAY':
                    case 'PAUSE':
                        this.player.togglePlayPause();
                        break;
                    case 'LEFT':
                    case 'RW':
                        this.player.seek(-10);
                        break;
                    case 'RIGHT':
                    case 'FF':
                        this.player.seek(10);
                        break;
                }
            } else {
                // Key actions in Dashboard mode (D-pad grid navigation)
                var cards = document.querySelectorAll('.media-card');
                if (cards.length === 0) {
                    if (action === 'RETURN') this.confirmExit();
                    return;
                }

                switch (action) {
                    case 'LEFT':
                    case 'UP':
                        if (this.focusedIndex > 0) {
                            this.focusedIndex--;
                            this.updateFocus();
                        }
                        break;
                    case 'RIGHT':
                    case 'DOWN':
                        if (this.focusedIndex < cards.length - 1) {
                            this.focusedIndex++;
                            this.updateFocus();
                        }
                        break;
                    case 'ENTER':
                        var selectedItem = this.history[this.focusedIndex];
                        if (selectedItem) {
                            this.player.playMedia(selectedItem);
                        }
                        break;
                    case 'RETURN':
                        this.confirmExit();
                        break;
                }
            }
        },

        updateFocus: function () {
            var cards = document.querySelectorAll('.media-card');
            for (var i = 0; i < cards.length; i++) {
                if (i === this.focusedIndex) {
                    cards[i].classList.add('focused');
                    cards[i].scrollIntoView({ behavior: 'smooth', block: 'nearest' });
                } else {
                    cards[i].classList.remove('focused');
                }
            }
        },

        updateIpDisplay: function (ip, port) {
            var ipElement = document.getElementById('tv-ip-display');
            if (ipElement) {
                ipElement.textContent = 'http://' + ip + ':' + port;
            }

            // Generate QR Code
            var pairUrl = 'http://' + ip + ':' + port + '?token=' + this.pairingToken;
            if (typeof QRCode !== 'undefined') {
                new QRCode('qrcode', {
                    text: pairUrl,
                    width: 220,
                    height: 220
                });
            }
        },

        onReceiveMedia: function (payload) {
            this.showToast('Ricevuto: ' + (payload.title || 'Nuovo file'));
            this.addToHistory(payload);
            this.player.playMedia(payload);
        },

        addToHistory: function (item) {
            // Unshift to top of history
            this.history.unshift({
                mediaType: item.mediaType || 'VIDEO',
                url: item.url,
                title: item.title || 'Media Senza Titolo',
                timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
            });

            // Keep max 20 items
            if (this.history.length > 20) this.history.pop();
            this.saveHistory();
            this.renderHistory();
        },

        renderHistory: function () {
            var container = document.getElementById('history-list');
            if (!container) return;

            if (this.history.length === 0) {
                container.innerHTML = '<div style="color: var(--text-muted); font-size: 16px; padding: 20px 0;">Nessun elemento nello storico. Scansiona il QR Code col telefono per inviare contenuti.</div>';
                return;
            }

            var html = '';
            for (var i = 0; i < this.history.length; i++) {
                var item = this.history[i];
                var icon = '🎬';
                if (item.mediaType === 'PHOTO' || item.mediaType === 'IMAGE') icon = '📸';
                if (item.mediaType === 'AUDIO') icon = '🎵';
                if (item.mediaType === 'WEB') icon = '🌐';

                html += '<div class="media-card" data-index="' + i + '">' +
                            '<div class="media-icon">' + icon + '</div>' +
                            '<div class="media-info">' +
                                '<div class="media-name">' + this.escapeHtml(item.title) + '</div>' +
                                '<div class="media-type-tag">' + item.mediaType + ' • ' + (item.timestamp || '') + '</div>' +
                            '</div>' +
                        '</div>';
            }
            container.innerHTML = html;
            this.updateFocus();
        },

        loadHistory: function () {
            try {
                var stored = localStorage.getItem('2TV_SAMSUNG_HISTORY');
                if (stored) {
                    this.history = JSON.parse(stored);
                }
            } catch (e) {
                this.history = [];
            }
        },

        saveHistory: function () {
            try {
                localStorage.setItem('2TV_SAMSUNG_HISTORY', JSON.stringify(this.history));
            } catch (e) {}
        },

        showToast: function (message) {
            var container = document.getElementById('toast-container');
            if (!container) return;

            var toast = document.createElement('div');
            toast.className = 'toast';
            toast.textContent = message;
            container.appendChild(toast);

            setTimeout(function () {
                if (toast.parentNode) toast.parentNode.removeChild(toast);
            }, 3500);
        },

        confirmExit: function () {
            if (confirm('Vuoi davvero uscire dall\'app 2TV?')) {
                if (typeof tizen !== 'undefined' && tizen.application) {
                    tizen.application.getCurrentApplication().exit();
                }
            }
        },

        escapeHtml: function (str) {
            return (str || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
        }
    };

    window.AppController = AppController;
    window.addEventListener('DOMContentLoaded', function () {
        window.App = new AppController();
        window.App.init();
    });
})(window);
