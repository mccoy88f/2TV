# 2TV (To TV) - App Cellulare & App Nativa Android TV

**2TV** è un sistema composto da un'**App Mobile Controller per Cellulare** e un'**App Nativa per Android TV** per condividere e riprodurre in tempo reale qualsiasi contenuto multimediale (video, dirette streaming, foto, audio e pagine web) o **file fisici locali** dallo smartphone verso uno schermo TV in rete locale (WiFi).

---

## 📱📦 File APK Pronti all'Uso

Troverai i file `.apk` pronti per l'installazione diretta nella cartella [`apks/`](file:///Users/antonello/Sviluppo/2TV/apks):
1. 📲 **App Cellulare**: [`apks/2TV-Mobile.apk`](file:///Users/antonello/Sviluppo/2TV/apks/2TV-Mobile.apk)
2. 📺 **App Android TV**: [`apks/2TV-AndroidTV.apk`](file:///Users/antonello/Sviluppo/2TV/apks/2TV-AndroidTV.apk)

---

## 🚀 Caratteristiche

### 📺 App Nativa Android TV (`:tv`)
- **Interfaccia TV Leanback**: Ottimizzata per schermi grandi e controllabile da telecomando TV (tasto BACK per interrompere la riproduzione e tornare alla Home).
- **Server HTTP integrato (Ktor CIO)**: Avvia automaticamente un server locale sulla porta `8080` della TV.
- **QR Code di Abbinamento a Schermo**: Mostrato all'avvio con IP, porta 8080 e token di sicurezza.
- **Player Integrato ExoPlayer (Media3)**: Riproduzione fluida di video MP4, MKV, stream live HLS `.m3u8`, audio ed immagini HD.
- **Catalogazione Automatica**:
  - 📸 **Foto**: Immagini JPG, PNG, WebP, GIF.
  - 🎬 **Video**: Video e dirette streaming HLS.
  - 🎵 **Audio**: File e stream musicali.
  - 🌐 **Web**: Pagine e link web.
  - 📁 **Altro**: File generici.

### 📱 App Mobile Controller per Cellulare (`:app`)
- **Material Design 3**: Temi Chiaro (White) e Scuro (Dark) con switch rapido.
- **Abbinamento Locale QR Code**: Scansione integrata con **CameraX** + **ML Kit Barcode Scanning**.
- **Trasferimento File Locali (`POST /api/upload`)**: Caricamento di file video/foto dalla memoria del telefono verso la TV con riproduzione automatica a fine upload.
- **Selettore TV da Condivisione (`TvSelectionShareDialog`)**: Condividendo un link o file da YouTube, Chrome, Galleria o Gestore File, si apre un dialog per scegliere la TV di destinazione con 1 tap.

---

## 💻 Struttura del Progetto

```
2TV/
├── apks/                      # APK pronti (2TV-Mobile.apk, 2TV-AndroidTV.apk)
└── android/
    ├── app/                   # App Mobile Android (Kotlin + Jetpack Compose)
    └── tv/                    # App Nativa Android TV (Leanback + ExoPlayer + Ktor Server)
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
Richiesta `multipart/form-data` con i campi `mediaType`, `title`, `saveToTv` e `file`.
