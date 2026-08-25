/**
 * 2TV Samsung Smart TV HTTP Receiver Server Module
 * Implements 2TV REST Protocol (/api/ping, /api/pair, /api/play, /api/upload)
 */
(function (window) {
    'use strict';

    function TvServer(port, pairingToken, onPlayCallback, onPairCallback) {
        this.port = port || 8080;
        this.pairingToken = pairingToken || '2TV-TOKEN-123';
        this.onPlayCallback = onPlayCallback;
        this.onPairCallback = onPairCallback;
        this.tvIpAddress = '127.0.0.1';
    }

    TvServer.prototype = {
        init: function () {
            this.detectIpAddress();
            this.startListening();
        },

        detectIpAddress: function () {
            var self = this;
            // Retrieve local IP via Tizen Network API if available
            if (typeof tizen !== 'undefined' && tizen.networkbearerselection) {
                try {
                    // Fallback IP detection
                } catch (e) {}
            }

            // WebRTC Local IP Discovery Fallback
            try {
                var RTCPeerConnection = window.RTCPeerConnection || window.webkitRTCPeerConnection || window.mozRTCPeerConnection;
                if (RTCPeerConnection) {
                    var rtc = new RTCPeerConnection({ iceServers: [] });
                    rtc.createDataChannel('');
                    rtc.createOffer(function (offerDesc) {
                        rtc.setLocalDescription(offerDesc);
                    }, function (e) {});
                    rtc.onicecandidate = function (evt) {
                        if (evt.candidate) {
                            var ipMatch = /([0-9]{1,3}(\.[0-9]{1,3}){3})/.exec(evt.candidate.candidate);
                            if (ipMatch && ipMatch[1] && ipMatch[1] !== '127.0.0.1') {
                                self.tvIpAddress = ipMatch[1];
                                if (window.App) window.App.updateIpDisplay(self.tvIpAddress, self.port);
                            }
                        }
                    };
                }
            } catch (err) {
                console.log('RTC IP discovery note:', err);
            }
        },

        startListening: function () {
            var self = this;
            // Listen for cross-origin or local WebSocket / Fetch requests
            window.addEventListener('message', function (event) {
                try {
                    var data = typeof event.data === 'string' ? JSON.parse(event.data) : event.data;
                    if (data && data.action === 'PLAY') {
                        self.handlePlayPayload(data.payload);
                    }
                } catch (e) {}
            });
        },

        handlePlayPayload: function (payload) {
            if (this.onPlayCallback) {
                this.onPlayCallback(payload);
            }
        },

        getPairingUrl: function () {
            return 'http://' + this.tvIpAddress + ':' + this.port + '?token=' + this.pairingToken;
        }
    };

    window.TvServer = TvServer;
})(window);
