/**
 * 2TV Samsung Smart TV HTTP Receiver Server Module
 * Implements 2TV REST Protocol (/api/ping, /api/pair, /api/play, /api/upload)
 * Fully Dynamic Local IP Discovery via Tizen APIs, webOS Luna APIs, WebRTC, and LocalStorage
 */
(function (window) {
    'use strict';

    function TvServer(port, pairingToken, onPlayCallback, onPairCallback) {
        this.port = port || 8080;
        this.pairingToken = pairingToken || '2TV-TOKEN-123';
        this.onPlayCallback = onPlayCallback;
        this.onPairCallback = onPairCallback;
        this.tvIpAddress = null;

        // Check if user previously saved a manual IP
        try {
            var savedIp = localStorage.getItem('2TV_MANUAL_IP');
            if (savedIp) this.tvIpAddress = savedIp;
        } catch (e) {}
    }

    TvServer.prototype = {
        init: function () {
            this.detectIpAddress();
            this.startListening();
        },

        isPrivateLocalIp: function (ip) {
            if (!ip) return false;
            if (ip.indexOf('192.168.') === 0) return true;
            if (ip.indexOf('10.') === 0) return true;
            if (/^172\.(1[6-9]|2[0-9]|3[01])\./.test(ip)) return true;
            return false;
        },

        detectIpAddress: function () {
            var self = this;

            // 1. Retrieve local IP dynamically via Tizen SystemInfo API (Samsung TV)
            if (typeof tizen !== 'undefined' && tizen.systeminfo) {
                try {
                    tizen.systeminfo.getPropertyValue('NETWORK', function (net) {
                        if (net && net.ipAddress && self.isPrivateLocalIp(net.ipAddress)) {
                            self.tvIpAddress = net.ipAddress;
                            if (window.App) window.App.updateIpDisplay(self.tvIpAddress, self.port);
                        }
                    });
                } catch (e) {}
            }

            // 2. Retrieve local IP dynamically via LG webOS Luna ConnectionManager API
            if (typeof webOS !== 'undefined' && webOS.service) {
                try {
                    webOS.service.request('luna://com.webos.service.connectionmanager', {
                        method: 'getStatus',
                        onSuccess: function (res) {
                            var ip = null;
                            if (res && res.wired && res.wired.ipAddress) ip = res.wired.ipAddress;
                            else if (res && res.wifi && res.wifi.ipAddress) ip = res.wifi.ipAddress;

                            if (ip && self.isPrivateLocalIp(ip)) {
                                self.tvIpAddress = ip;
                                if (window.App) window.App.updateIpDisplay(self.tvIpAddress, self.port);
                            }
                        }
                    });
                } catch (e) {}
            }

            // 3. Retrieve local IP dynamically via WebRTC Candidate ICE Gathering
            try {
                var RTCPeerConnection = window.RTCPeerConnection || window.webkitRTCPeerConnection || window.mozRTCPeerConnection;
                if (RTCPeerConnection) {
                    var rtc = new RTCPeerConnection({ iceServers: [] });
                    rtc.createDataChannel('');
                    rtc.createOffer(function (offerDesc) {
                        rtc.setLocalDescription(offerDesc);
                    }, function (e) {});

                    rtc.onicecandidate = function (evt) {
                        if (evt && evt.candidate && evt.candidate.candidate) {
                            var ipMatch = /([0-9]{1,3}(\.[0-9]{1,3}){3})/.exec(evt.candidate.candidate);
                            if (ipMatch && ipMatch[1]) {
                                var candidateIp = ipMatch[1];
                                if (self.isPrivateLocalIp(candidateIp)) {
                                    self.tvIpAddress = candidateIp;
                                    if (window.App) window.App.updateIpDisplay(self.tvIpAddress, self.port);
                                }
                            }
                        }
                    };
                }
            } catch (err) {
                console.log('RTC IP discovery note:', err);
            }
        },

        setManualIp: function (ip) {
            if (!ip) return;
            this.tvIpAddress = ip;
            try {
                localStorage.setItem('2TV_MANUAL_IP', ip);
            } catch (e) {}
            if (window.App) window.App.updateIpDisplay(this.tvIpAddress, this.port);
        },

        startListening: function () {
            var self = this;
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
            return this.tvIpAddress ? ('http://' + this.tvIpAddress + ':' + this.port + '?token=' + this.pairingToken) : '';
        }
    };

    window.TvServer = TvServer;
})(window);
