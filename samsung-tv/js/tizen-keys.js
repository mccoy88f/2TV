/**
 * Samsung TV Remote Control Key Handling System
 * Compatible with Tizen 2.4 - 8.0+ & Samsung Smart Remote / Legacy Remotes
 */
(function (window) {
    'use strict';

    var TizenKeys = {
        // Keycodes for Samsung TV
        KEY_LEFT: 37,
        KEY_UP: 38,
        KEY_RIGHT: 39,
        KEY_DOWN: 40,
        KEY_ENTER: 13,
        KEY_RETURN: 10009, // Tizen TV Return/Back button
        KEY_ESC: 27,
        KEY_BACKSPACE: 8,
        KEY_PLAY: 415,
        KEY_PAUSE: 19,
        KEY_STOP: 413,
        KEY_FF: 417,
        KEY_RW: 412,

        init: function (onKeyPressCallback) {
            this.registerTizenKeys();
            window.addEventListener('keydown', function (e) {
                var keyCode = e.keyCode;
                // Intercept Tizen Return/Back button (10009) to prevent backgrounding app unexpectedly
                if (keyCode === TizenKeys.KEY_RETURN || keyCode === TizenKeys.KEY_ESC) {
                    e.preventDefault();
                    if (onKeyPressCallback) onKeyPressCallback('RETURN', e);
                    return;
                }

                switch (keyCode) {
                    case TizenKeys.KEY_LEFT:
                        if (onKeyPressCallback) onKeyPressCallback('LEFT', e);
                        break;
                    case TizenKeys.KEY_RIGHT:
                        if (onKeyPressCallback) onKeyPressCallback('RIGHT', e);
                        break;
                    case TizenKeys.KEY_UP:
                        if (onKeyPressCallback) onKeyPressCallback('UP', e);
                        break;
                    case TizenKeys.KEY_DOWN:
                        if (onKeyPressCallback) onKeyPressCallback('DOWN', e);
                        break;
                    case TizenKeys.KEY_ENTER:
                        if (onKeyPressCallback) onKeyPressCallback('ENTER', e);
                        break;
                    case TizenKeys.KEY_PLAY:
                        if (onKeyPressCallback) onKeyPressCallback('PLAY', e);
                        break;
                    case TizenKeys.KEY_PAUSE:
                        if (onKeyPressCallback) onKeyPressCallback('PAUSE', e);
                        break;
                    case TizenKeys.KEY_STOP:
                        if (onKeyPressCallback) onKeyPressCallback('STOP', e);
                        break;
                    case TizenKeys.KEY_FF:
                        if (onKeyPressCallback) onKeyPressCallback('FF', e);
                        break;
                    case TizenKeys.KEY_RW:
                        if (onKeyPressCallback) onKeyPressCallback('RW', e);
                        break;
                }
            });
        },

        registerTizenKeys: function () {
            if (typeof tizen !== 'undefined' && tizen.tvinputdevice) {
                try {
                    tizen.tvinputdevice.registerKey('MediaPlay');
                    tizen.tvinputdevice.registerKey('MediaPause');
                    tizen.tvinputdevice.registerKey('MediaStop');
                    tizen.tvinputdevice.registerKey('MediaFastForward');
                    tizen.tvinputdevice.registerKey('MediaRewind');
                } catch (err) {
                    console.log('Tizen key registration note:', err.message);
                }
            }
        }
    };

    window.TizenKeys = TizenKeys;
})(window);
