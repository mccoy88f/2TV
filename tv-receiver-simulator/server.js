const express = require('express');
const cors = require('cors');
const QRCode = require('qrcode');
const os = require('os');
const path = require('path');
const fs = require('fs');

const app = express();
const PORT = process.env.PORT || 8080;
const PAIRING_TOKEN = '2tv-secret-token-123';

app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

let sseClients = [];
let tvHistory = [];
let currentMedia = null;

function getLocalIp() {
    const interfaces = os.networkInterfaces();
    for (const devName in interfaces) {
        const iface = interfaces[devName];
        for (let i = 0; i < iface.length; i++) {
            const alias = iface[i];
            if (alias.family === 'IPv4' && !alias.internal) {
                return alias.address;
            }
        }
    }
    return '127.0.0.1';
}

function categorizeMedia(url, reportedType) {
    const lower = (url || '').toLowerCase();
    const type = (reportedType || '').toUpperCase();

    if (type === 'IMAGE' || lower.endsWith('.jpg') || lower.endsWith('.jpeg') || lower.endsWith('.png') || lower.endsWith('.webp')) {
        return 'FOTO';
    }
    if (type === 'VIDEO' || type === 'STREAM' || lower.endsWith('.mp4') || lower.endsWith('.mkv') || lower.endsWith('.m3u8')) {
        return 'VIDEO';
    }
    if (type === 'AUDIO' || lower.endsWith('.mp3') || lower.endsWith('.aac') || lower.endsWith('.wav')) {
        return 'AUDIO';
    }
    if (lower.startsWith('http://') || lower.startsWith('https://')) {
        return 'WEB';
    }
    return 'ALTRO';
}

const localIp = getLocalIp();
const qrPayload = JSON.stringify({
    name: "Living Room TV (2TV)",
    ip: localIp,
    port: PORT,
    pairingToken: PAIRING_TOKEN,
    platform: "simulator"
});

let qrDataUrl = '';
QRCode.toDataURL(qrPayload, { margin: 2, scale: 8 }, (err, url) => {
    if (!err) qrDataUrl = url;
});

app.get('/api/events', (req, res) => {
    res.setHeader('Content-Type', 'text/event-stream');
    res.setHeader('Cache-Control', 'no-cache');
    res.setHeader('Connection', 'keep-alive');

    res.write(`data: ${JSON.stringify({ type: 'INIT', qrDataUrl, qrPayload, localIp, port: PORT, currentMedia, tvHistory })}\n\n`);

    sseClients.push(res);
    req.on('close', () => {
        sseClients = sseClients.filter(c => c !== res);
    });
});

function broadcast(data) {
    sseClients.forEach(client => {
        client.write(`data: ${JSON.stringify(data)}\n\n`);
    });
}

app.get('/api/ping', (req, res) => {
    res.json({ success: true, status: 'online', tvName: "Living Room TV (2TV)" });
});

app.get('/api/verify', (req, res) => {
    const token = req.headers['x-pairing-token'];
    if (token === PAIRING_TOKEN || !token) {
        return res.json({ success: true, verified: true });
    }
    return res.status(401).json({ success: false, message: 'Invalid pairing token' });
});

app.post('/api/play', (req, res) => {
    const { command, mediaType, url, title, saveToTv } = req.body;

    if (!url) {
        return res.status(400).json({ success: false, message: 'URL mancante' });
    }

    const category = categorizeMedia(url, mediaType);
    currentMedia = {
        category,
        mediaType: mediaType || category,
        url,
        title: title || url,
        timestamp: Date.now()
    };

    if (saveToTv) {
        tvHistory.unshift({ ...currentMedia, id: Date.now() });
    }

    broadcast({ type: 'PLAY_MEDIA', media: currentMedia, tvHistory });

    res.json({ success: true, message: `Contenuto "${currentMedia.title}" in riproduzione!` });
});

// File upload endpoint for local file transfers
app.post('/api/upload', (req, res) => {
    // Basic mock response for local file upload in simulator
    const sampleTitle = "File Locale Ricevuto";
    const category = "FOTO";
    currentMedia = {
        category,
        mediaType: category,
        url: "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1920&q=80",
        title: sampleTitle,
        timestamp: Date.now()
    };

    tvHistory.unshift({ ...currentMedia, id: Date.now() });
    broadcast({ type: 'PLAY_MEDIA', media: currentMedia, tvHistory });

    res.json({ success: true, message: "File locale ricevuto e avviato sulla TV!" });
});

app.listen(PORT, '0.0.0.0', () => {
    console.log(`[2TV TV SIMULATOR] Avviato su http://${localIp}:${PORT}`);
});
