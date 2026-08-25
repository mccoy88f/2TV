/**
 * 2TV Samsung Smart TV Receiver Main Application Controller
 * Powered by web-tv-core shared modules
 */
(function (window) {
    'use strict';

    function AppController() {
        this.player = null;
        this.receiver = null;
        this.server = null;
        this.focusedGroup = 'HISTORY'; // 'HISTORY', 'CONTROL_BAR', 'MODAL', 'PLAYER'
        this.focusedIndex = 0;
    }

    AppController.prototype = {
        init: function () {
            var self = this;
            console.log('Initializing 2TV Samsung Smart TV App...');

            this.receiver = new TvReceiver();
            this.receiver.init();

            this.player = new UniversalPlayer();

            // Initialize Key Adapter for TV Remote Control
            if (window.KeyAdapter) {
                KeyAdapter.init(function (action, event) {
                    self.handleKeyAction(action, event);
                });
            }

            // Initialize HTTP Receiver Server & QR Code
            if (window.TvServer) {
                this.server = new TvServer(8080, this.receiver.pairingToken, function (payload) {
                    self.onReceiveMedia(payload);
                });
                this.server.init();
            }

            this.setupControlBarListeners();
            this.updateIpDisplay('192.168.1.100', 8080);
            this.renderHistory();
            this.updateFocus();
        },

        setupControlBarListeners: function () {
            var self = this;
            var btnPairings = document.getElementById('btn-manage-pairings');
            var btnHistory = document.getElementById('btn-show-history');

            if (btnPairings) {
                btnPairings.addEventListener('click', function () {
                    self.showToast('Accoppiamenti gestiti dallo smartphone');
                });
            }
            if (btnHistory) {
                btnHistory.addEventListener('click', function () {
                    self.focusedGroup = 'HISTORY';
                    self.focusedIndex = 0;
                    self.updateFocus();
                });
            }
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
                    case 'UP':
                        this.player.showOverlayTemporarily();
                        break;
                }
                return;
            }

            // Modal Navigation
            var modal = document.getElementById('m3u-modal');
            if (modal && modal.classList.contains('active')) {
                if (action === 'RETURN') {
                    modal.classList.remove('active');
                }
                return;
            }

            // Dashboard Grid Navigation
            var historyCards = document.querySelectorAll('.media-card');
            var controlBtns = document.querySelectorAll('#bottom-control-bar .tv-btn');

            if (action === 'RETURN') {
                this.confirmExit();
                return;
            }

            if (this.focusedGroup === 'HISTORY') {
                if (action === 'DOWN') {
                    if (this.focusedIndex < historyCards.length - 1) {
                        this.focusedIndex++;
                    } else {
                        this.focusedGroup = 'CONTROL_BAR';
                        this.focusedIndex = 0;
                    }
                    this.updateFocus();
                } else if (action === 'UP') {
                    if (this.focusedIndex > 0) {
                        this.focusedIndex--;
                        this.updateFocus();
                    }
                } else if (action === 'ENTER') {
                    var selectedItem = this.receiver.history[this.focusedIndex];
                    if (selectedItem) {
                        this.player.playMedia(selectedItem);
                    }
                }
            } else if (this.focusedGroup === 'CONTROL_BAR') {
                if (action === 'UP') {
                    this.focusedGroup = 'HISTORY';
                    this.focusedIndex = historyCards.length > 0 ? 0 : 0;
                    this.updateFocus();
                } else if (action === 'LEFT' && this.focusedIndex > 0) {
                    this.focusedIndex--;
                    this.updateFocus();
                } else if (action === 'RIGHT' && this.focusedIndex < controlBtns.length - 1) {
                    this.focusedIndex++;
                    this.updateFocus();
                } else if (action === 'ENTER') {
                    if (controlBtns[this.focusedIndex]) {
                        controlBtns[this.focusedIndex].click();
                    }
                }
            }
        },

        updateFocus: function () {
            var historyCards = document.querySelectorAll('.media-card');
            var controlBtns = document.querySelectorAll('#bottom-control-bar .tv-btn');

            // Remove all focus
            for (var i = 0; i < historyCards.length; i++) historyCards[i].classList.remove('focused');
            for (var j = 0; j < controlBtns.length; j++) controlBtns[j].classList.remove('focused');

            if (this.focusedGroup === 'HISTORY' && historyCards[this.focusedIndex]) {
                historyCards[this.focusedIndex].classList.add('focused');
                historyCards[this.focusedIndex].scrollIntoView({ behavior: 'smooth', block: 'nearest' });
            } else if (this.focusedGroup === 'CONTROL_BAR' && controlBtns[this.focusedIndex]) {
                controlBtns[this.focusedIndex].classList.add('focused');
            }
        },

        updateIpDisplay: function (ip, port) {
            var ipElement = document.getElementById('tv-ip-display');
            if (ipElement) {
                ipElement.textContent = 'http://' + ip + ':' + port;
            }

            var pairUrl = 'http://' + ip + ':' + port + '?token=' + this.receiver.pairingToken;
            var qrContainer = document.getElementById('qrcode');
            if (qrContainer) qrContainer.innerHTML = '';

            if (typeof QRCode !== 'undefined') {
                new QRCode('qrcode', {
                    text: pairUrl,
                    width: 216,
                    height: 216
                });
            }
        },

        onReceiveMedia: function (payload) {
            this.showToast('Ricevuto: ' + (payload.title || 'Nuovo file'));
            this.receiver.addToHistory(payload);
            this.renderHistory();
            this.player.playMedia(payload);
        },

        renderHistory: function () {
            var container = document.getElementById('history-list');
            if (!container) return;

            if (this.receiver.history.length === 0) {
                container.innerHTML = '<div style="color: var(--text-muted); font-size: 16px; padding: 20px 0;">Nessun elemento nello storico. Inquadra il QR Code dallo smartphone per inviare contenuti.</div>';
                return;
            }

            var html = '';
            for (var i = 0; i < this.receiver.history.length; i++) {
                var item = this.receiver.history[i];
                var icon = '🎬';
                var category = (item.category || item.mediaType || 'STREAM').toUpperCase();

                if (category === 'PHOTO' || category === 'IMAGE') icon = '📸';
                if (category === 'AUDIO') icon = '🎵';
                if (category === 'WEB' || category === 'LINK') icon = '🌐';

                html += '<div class="media-card" data-index="' + i + '">' +
                            '<div class="media-icon">' + icon + '</div>' +
                            '<div class="media-info">' +
                                '<div class="media-name">' + this.escapeHtml(item.title) + '</div>' +
                                '<div class="media-type-tag">' + category + ' • ' + (item.timestamp || '') + '</div>' +
                            '</div>' +
                        '</div>';
            }
            container.innerHTML = html;
            this.updateFocus();
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
