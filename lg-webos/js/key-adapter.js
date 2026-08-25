/**
 * 2TV Shared Remote Control Key Adapter (Tizen, webOS, VIDAA)
 * Normalizes remote keycodes across all Web TV platforms.
 */
(function (window) {
    'use strict';

    var KeyAdapter = {
        KEY_RETURN_CODES: [10009, 461, 27, 8], // Tizen (10009), webOS (461), ESC (27), Backspace (8)
        KEY_ENTER_CODES: [13, 32],
        KEY_LEFT: 37,
        KEY_UP: 38,
        KEY_RIGHT: 39,
        KEY_DOWN: 40,
        KEY_PLAY: 415,
        KEY_PAUSE: 19,
        KEY_STOP: 413,
        KEY_FF: 417,
        KEY_RW: 412,

        init: function (onActionCallback) {
            var self = this;

            // Register Tizen TV keys if running on Tizen OS
            if (typeof tizen !== 'undefined' && tizen.tvinputdevice) {
                try {
                    tizen.tvinputdevice.registerKey('MediaPlay');
                    tizen.tvinputdevice.registerKey('MediaPause');
                    tizen.tvinputdevice.registerKey('MediaStop');
                    tizen.tvinputdevice.registerKey('MediaFastForward');
                    tizen.tvinputdevice.registerKey('MediaRewind');
                } catch (e) {}
            }

            // Global Keydown Listener
            window.addEventListener('keydown', function (e) {
                var code = e.keyCode || e.which;

                // Check for RETURN / BACK key across Tizen, webOS, VIDAA
                if (self.KEY_RETURN_CODES.indexOf(code) !== -1) {
                    e.preventDefault();
                    if (onActionCallback) onActionCallback('RETURN', e);
                    return;
                }

                if (self.KEY_ENTER_CODES.indexOf(code) !== -1) {
                    if (onActionCallback) onActionCallback('ENTER', e);
                    return;
                }

                switch (code) {
                    case self.KEY_LEFT:
                        if (onActionCallback) onActionCallback('LEFT', e);
                        break;
                    case self.KEY_RIGHT:
                        if (onActionCallback) onActionCallback('RIGHT', e);
                        break;
                    case self.KEY_UP:
                        if (onActionCallback) onActionCallback('UP', e);
                        break;
                    case self.KEY_DOWN:
                        if (onActionCallback) onActionCallback('DOWN', e);
                        break;
                    case self.KEY_PLAY:
                        if (onActionCallback) onActionCallback('PLAY', e);
                        break;
                    case self.KEY_PAUSE:
                        if (onActionCallback) onActionCallback('PAUSE', e);
                        break;
                    case self.KEY_STOP:
                        if (onActionCallback) onActionCallback('STOP', e);
                        break;
                    case self.KEY_FF:
                        if (onActionCallback) onActionCallback('FF', e);
                        break;
                    case self.KEY_RW:
                        if (onActionCallback) onActionCallback('RW', e);
                        break;
                }
            });
        }
    };

    window.KeyAdapter = KeyAdapter;
})(window);
