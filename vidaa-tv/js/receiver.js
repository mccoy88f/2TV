/**
 * 2TV Shared Receiver State & History Manager (Tizen, webOS, VIDAA)
 */
(function (window) {
    'use strict';

    function TvReceiver() {
        this.appVersion = '2.0.5';
        this.history = [];
        this.tvNickname = 'Samsung Smart TV';
        this.pairingToken = '2TV-' + Math.floor(1000 + Math.random() * 9000);
    }

    TvReceiver.prototype = {
        init: function () {
            this.loadNickname();
            this.loadHistory();
        },

        loadNickname: function () {
            try {
                var stored = localStorage.getItem('2TV_NICKNAME');
                if (stored) this.tvNickname = stored;
            } catch (e) {}
        },

        saveNickname: function (name) {
            if (!name) return;
            this.tvNickname = name;
            try {
                localStorage.setItem('2TV_NICKNAME', name);
            } catch (e) {}
        },

        loadHistory: function () {
            try {
                var stored = localStorage.getItem('2TV_HISTORY');
                if (stored) this.history = JSON.parse(stored);
            } catch (e) {
                this.history = [];
            }
        },

        saveHistory: function () {
            try {
                localStorage.setItem('2TV_HISTORY', JSON.stringify(this.history));
            } catch (e) {}
        },

        addToHistory: function (item) {
            this.history.unshift({
                category: item.category || item.mediaType || 'STREAM',
                url: item.url || item.pathOrUrl || '',
                title: item.title || 'Contenuto Ricevuto',
                timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
            });

            if (this.history.length > 30) this.history.pop();
            this.saveHistory();
        }
    };

    window.TvReceiver = TvReceiver;
})(window);
