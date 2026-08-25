/**
 * 2TV Samsung Smart TV Receiver Main Application Controller
 * Standardized to Android TV Native UI & Remote Controls 1:1
 */
(function (window) {
    'use strict';

    function AppController() {
        this.player = null;
        this.receiver = null;
        this.server = null;
        this.focusedGroup = 'CONTROL_BAR'; // 'CONTROL_BAR', 'HISTORY_MODAL', 'M3U_MODAL'
        this.focusedIndex = 0;
    }

    AppController.prototype = {
        init: function () {
            var self = this;
            console.log('Initializing 2TV Receiver App (Android TV UI Standardized)...');

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

            this.setupListeners();
            this.updateIpDisplay();
            this.updateFocus();
        },

        setupListeners: function () {
            var self = this;
            var btnPairings = document.getElementById('btn-manage-pairings');
            var btnHistory = document.getElementById('btn-show-history');
            var btnCloseHistory = document.getElementById('btn-close-history-modal');
            var btnCloseM3u = document.getElementById('btn-close-m3u-modal');
            var ipElement = document.getElementById('tv-ip-display');

            if (btnPairings) {
                btnPairings.addEventListener('click', function () {
                    self.showToast('Accoppiamenti gestiti dallo smartphone');
                });
            }

            if (btnHistory) {
                btnHistory.addEventListener('click', function () {
                    self.openHistoryModal();
                });
            }

            if (btnCloseHistory) {
                btnCloseHistory.addEventListener('click', function () {
                    self.closeHistoryModal();
                });
            }

            if (btnCloseM3u) {
                btnCloseM3u.addEventListener('click', function () {
                    var modal = document.getElementById('m3u-modal');
                    if (modal) modal.classList.remove('active');
                    self.focusedGroup = 'CONTROL_BAR';
                    self.updateFocus();
                });
            }

            if (ipElement) {
                ipElement.style.cursor = 'pointer';
                ipElement.title = 'Clicca per impostare l\'indirizzo IP Wi-Fi';
                ipElement.addEventListener('click', function () {
                    self.promptManualIp();
                });
            }
        },

        promptManualIp: function () {
            var currentIp = (this.server && this.server.tvIpAddress) ? this.server.tvIpAddress : '192.168.178.143';
            var newIp = prompt('Inserisci l\'indirizzo IP locale Wi-Fi del tuo dispositivo (es. 192.168.178.143):', currentIp);
            if (newIp && newIp.trim()) {
                var cleaned = newIp.trim();
                try {
                    localStorage.setItem('2TV_MANUAL_IP', cleaned);
                } catch (e) {}
                if (this.server) this.server.setManualIp(cleaned);
                else this.updateIpDisplay(cleaned, 8080);
                this.showToast('IP Wi-Fi salvato: ' + cleaned);
            }
        },

        openHistoryModal: function () {
            var modal = document.getElementById('history-modal');
            if (!modal) return;

            this.renderHistoryModal();
            modal.classList.add('active');
            this.focusedGroup = 'HISTORY_MODAL';
            this.focusedIndex = 0;
            this.updateFocus();
        },

        closeHistoryModal: function () {
            var modal = document.getElementById('history-modal');
            if (modal) modal.classList.remove('active');
            this.focusedGroup = 'CONTROL_BAR';
            this.focusedIndex = 1; // Focus back on History button
            this.updateFocus();
        },

        handleKeyAction: function (action, event) {
            // Player Mode
            if (this.player && this.player.isPlaying()) {
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

            // History Modal Navigation
            var historyModal = document.getElementById('history-modal');
            if (historyModal && historyModal.classList.contains('active')) {
                var historyItems = historyModal.querySelectorAll('.modal-list-item');
                var btnCloseHistory = document.getElementById('btn-close-history-modal');

                if (action === 'RETURN') {
                    this.closeHistoryModal();
                    return;
                }

                if (action === 'DOWN') {
                    if (this.focusedIndex < historyItems.length) {
                        this.focusedIndex++;
                        this.updateFocus();
                    }
                } else if (action === 'UP') {
                    if (this.focusedIndex > 0) {
                        this.focusedIndex--;
                        this.updateFocus();
                    }
                } else if (action === 'ENTER') {
                    if (this.focusedIndex < historyItems.length) {
                        var selectedItem = this.receiver.history[this.focusedIndex];
                        if (selectedItem) {
                            this.closeHistoryModal();
                            this.player.playMedia(selectedItem);
                        }
                    } else if (btnCloseHistory) {
                        this.closeHistoryModal();
                    }
                }
                return;
            }

            // M3U Modal Navigation
            var m3uModal = document.getElementById('m3u-modal');
            if (m3uModal && m3uModal.classList.contains('active')) {
                if (action === 'RETURN') {
                    m3uModal.classList.remove('active');
                    this.focusedGroup = 'CONTROL_BAR';
                    this.updateFocus();
                }
                return;
            }

            // Main Screen Control Bar D-Pad Navigation
            var controlBtns = document.querySelectorAll('#bottom-control-bar .tv-btn');

            if (action === 'RETURN') {
                this.confirmExit();
                return;
            }

            if (this.focusedGroup === 'CONTROL_BAR') {
                if (action === 'LEFT' && this.focusedIndex > 0) {
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
            var controlBtns = document.querySelectorAll('#bottom-control-bar .tv-btn');
            var historyModal = document.getElementById('history-modal');
            var historyItems = historyModal ? historyModal.querySelectorAll('.modal-list-item') : [];
            var btnCloseHistory = document.getElementById('btn-close-history-modal');

            // Remove all focus
            for (var i = 0; i < controlBtns.length; i++) controlBtns[i].classList.remove('focused');
            for (var j = 0; j < historyItems.length; j++) historyItems[j].classList.remove('focused');
            if (btnCloseHistory) btnCloseHistory.classList.remove('focused');

            if (this.focusedGroup === 'CONTROL_BAR' && controlBtns[this.focusedIndex]) {
                controlBtns[this.focusedIndex].classList.add('focused');
            } else if (this.focusedGroup === 'HISTORY_MODAL') {
                if (this.focusedIndex < historyItems.length && historyItems[this.focusedIndex]) {
                    historyItems[this.focusedIndex].classList.add('focused');
                    historyItems[this.focusedIndex].scrollIntoView({ behavior: 'smooth', block: 'nearest' });
                } else if (btnCloseHistory) {
                    btnCloseHistory.classList.add('focused');
                }
            }
        },

        updateIpDisplay: function (detectedIp, detectedPort) {
            var ipElement = document.getElementById('tv-ip-display');
            var savedIp = null;
            try { savedIp = localStorage.getItem('2TV_MANUAL_IP'); } catch (e) {}

            var rawIp = savedIp || detectedIp || (this.server ? this.server.tvIpAddress : null);
            
            // Validate IPv4 private local subnet format
            var isPrivateLocal = rawIp && (this.server ? this.server.isPrivateLocalIp(rawIp) : /^(192\.168\.|10\.|172\.(1[6-9]|2[0-9]|3[01])\.)/.test(rawIp));
            var hostIp = isPrivateLocal ? rawIp : null;
            var hostPort = parseInt(detectedPort || window.location.port || 8080, 10);

            if (ipElement) {
                if (hostIp) {
                    ipElement.textContent = 'IP: ' + hostIp + ':' + hostPort;
                } else {
                    ipElement.textContent = 'IP: Clicca per inserire IP Wi-Fi';
                }
            }

            if (!hostIp) return;

            var token = this.receiver ? this.receiver.pairingToken : '2TV-DEMO';
            var nickname = this.receiver ? this.receiver.tvNickname : '2TV Receiver';
            
            // Local Wi-Fi JSON Payload for 2TV Android Mobile Scanner
            var qrPayload = JSON.stringify({
                name: nickname,
                ip: hostIp,
                port: hostPort,
                pairingToken: token,
                platform: 'web'
            });

            var qrContainer = document.getElementById('qrcode');
            if (qrContainer) {
                qrContainer.innerHTML = '';
                if (typeof QRCode !== 'undefined') {
                    new QRCode('qrcode', {
                        text: qrPayload,
                        width: 144,
                        height: 144
                    });
                }
            }
        },

        onReceiveMedia: function (payload) {
            this.showToast('Ricevuto: ' + (payload.title || 'Nuovo file'));
            this.receiver.addToHistory(payload);
            this.player.playMedia(payload);
        },

        renderHistoryModal: function () {
            var container = document.getElementById('history-modal-body');
            if (!container) return;

            if (this.receiver.history.length === 0) {
                container.innerHTML = '<div style="color: var(--text-muted); font-size: 16px; padding: 20px 0; text-align: center;">Nessun elemento nello storico. Inquadra il QR Code dallo smartphone per inviare contenuti.</div>';
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

                html += '<div class="modal-list-item" data-index="' + i + '">' +
                            '<span>' + icon + ' ' + this.escapeHtml(item.title) + '</span>' +
                            '<span style="font-size: 12px; color: var(--text-muted);">' + category + ' • ' + (item.timestamp || '') + '</span>' +
                        '</div>';
            }
            container.innerHTML = html;
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
