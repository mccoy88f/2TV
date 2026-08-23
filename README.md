<<<<<<< HEAD
# 2TV
Send to any TV any content, fast, easy and secure.
=======
# 2TV (To TV) - Sistema di Condivisione Contenuti e File su Smart TV

**2TV** è un sistema multi-piattaforma per condividere e riprodurre in tempo reale contenuti multimediali (video, dirette streaming, immagini, audio e pagine web) o **file locali fisici** da uno smartphone/tablet Android direttamente su uno schermo Smart TV (Android TV, LG webOS, Samsung Tizen) presente nella stessa rete locale (LAN/WiFi).

---

## 🚀 Funzionalità

### 📺 App Nativa Android TV (`:tv`) & Simulatore TV
- **Interfaccia TV Leanback**: Ottimizzata per schermi grandi e navigabile da telecomando TV.
- **Server HTTP Embedded (Ktor Server CIO)**: Avvia automaticamente un server locale sulla porta `8080` della TV.
- **QR Code di Abbinamento a Schermo**: Generato dinamicamente con ZXing (IP, porta 8080 e token di sicurezza).
- **Player Integrato ExoPlayer (Media3)**: Riproduzione ad alte prestazioni di video MP4, MKV, stream live HLS `.m3u8`, audio ed immagini HD.
- **Catalogazione Automatica dell'Archivio TV**:
  - 📸 **Foto**: Immagini JPG, PNG, WebP, GIF.
  - 🎬 **Video**: Video e dirette streaming HLS.
  - 🎵 **Audio**: File e stream musicali.
  - 🌐 **Web**: Pagine e link web.
  - 📁 **Altro**: File generici.
- **Gestione Manuale dell'Archivio**: Possibilità di riprodurre, riorganizzare o eliminare manualmente gli elementi salvati sulla memoria della TV.

### 📱 App Android Mobile Controller (`:app`)
- **Material Design 3**: Temi Chiaro (White) e Scuro (Dark) con pulsante di switch rapido.
- **Abbinamento Locale QR Code**: Scansione con **CameraX** + **ML Kit Barcode Scanning**.
- **Trasferimento File Fisici (`POST /api/upload`)**: Invio di file locali (video o foto dalla Galleria/memoria del telefono) tramite upload HTTP alla TV con riproduzione automatica a fine caricamento.
- **Menù Condividi con Selettore TV (`TvSelectionShareDialog`)**: Condividendo un link o file da qualsiasi app Android (YouTube, Chrome, Galleria, Gestore File), si apre un dialog per scegliere la TV di destinazione con 1 tap.

---

## 💻 Struttura del Progetto

```
2TV/
├── android/
│   ├── app/                   # App Mobile Android (Kotlin + Jetpack Compose)
│   └── tv/                    # App Nativa Android TV (Leanback + ExoPlayer + Ktor Server)
│
├── tv-receiver-simulator/     # Simulatore Web/Node per Smart TV
│   ├── public/index.html      # UI Lettore TV + Filtri Categorie (Foto, Video, Audio, Web, Altro)
│   └── server.js              # Server HTTP / REST API / SSE
│
└── README.md
```

---

## 📡 Protocollo Payload JSON

### Inviare URL / Streaming: `POST /api/play`
```json
{
  "command": "PLAY",
  "mediaType": "VIDEO",
  "url": "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
  "title": "Big Buck Bunny Demo",
  "saveToTv": true
}
```

### Inviare File Locale Fisico: `POST /api/upload`
Invia una richiesta `multipart/form-data` con i campi `mediaType`, `title`, `saveToTv` e `file`.
>>>>>>> c732c15 (Initial release of 2TV: Android Mobile Controller, Android TV Native Receiver, and Smart TV Simulator)
