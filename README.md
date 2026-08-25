# 2TV (To TV) - App Cellulare, Android TV & Samsung Smart TV

**2TV** è un sistema composto da un'**App Mobile Controller per Cellulare**, un'**App Nativa Android TV** e un'**App Nativa per Samsung Smart TV (Tizen OS)** per condividere e riprodurre in tempo reale qualsiasi contenuto multimediale (video, dirette streaming, foto, audio e pagine web) o **file fisici locali** dallo smartphone verso uno schermo TV in rete locale (WiFi).

---

## 📱📦 File Pronti all'Uso

Troverai i pacchetti pronti per l'installazione diretta nelle rispettive cartelle:
1. 📲 **App Cellulare**: [`apks/2TV-Mobile.apk`](file:///Users/antonello/Sviluppo/2TV/apks/2TV-Mobile.apk)
2. 📺 **App Android TV**: [`apks/2TV-AndroidTV.apk`](file:///Users/antonello/Sviluppo/2TV/apks/2TV-AndroidTV.apk)
3. 🟦 **App Samsung Smart TV (Tizen)**: [`samsung-tv/2TV-SamsungTV.wgt`](file:///Users/antonello/Sviluppo/2TV/samsung-tv/2TV-SamsungTV.wgt)

---

## 🚀 Caratteristiche

### 📺 App Nativa Android TV (`:tv`) & Samsung Smart TV (`samsung-tv`)
- **Interfaccia TV Leanback**: Ottimizzata per schermi grandi e controllabile da telecomando TV (tasto BACK per interrompere la riproduzione e tornare alla Home).
- **Server HTTP integrato (Porta 8080)**: Avvia automaticamente un server locale sulla porta `8080` della TV.
- **QR Code di Abbinamento a Schermo**: Mostrato all'avvio con IP, porta 8080 e token di sicurezza.
- **Player Integrato Universale**: Riproduzione fluida di video MP4, MKV, stream live HLS `.m3u8` (tramite ExoPlayer su Android TV e AVPlay/HTML5 su Samsung TV), audio ed immagini HD.
- **Compatibilità Samsung Smart TV**: Supporta sia TV recenti (Tizen 5.0 - 8.0+) sia TV datate (Tizen 2.4 - 4.0 dal 2015).

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
├── android/
│   ├── app/                   # App Mobile Android (Kotlin + Jetpack Compose)
│   └── tv/                    # App Nativa Android TV (Leanback + ExoPlayer + Ktor Server)
└── samsung-tv/                # App Nativa Samsung Smart TV (Tizen OS Widget .wgt)
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
