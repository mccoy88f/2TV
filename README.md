# 2TV (To TV) - Ecosistema Multi-Piattaforma Smart TV & PC

**2TV** è un ecosistema completo composto da un'**App Mobile Controller per Cellulare**, un'**App Nativa Android TV** e da una suite **Web TV per Hisense Smart TV (VIDAA OS)**, **Samsung Smart TV (Tizen OS)**, **LG Smart TV (webOS)** e **PC / Mac / Laptop** per condividere e riprodurre in tempo reale qualsiasi contenuto multimediale (video, dirette streaming HLS, playlist IPTV M3U, foto, audio e pagine web) o **file fisici locali** dallo smartphone verso qualsiasi schermo TV o computer in rete locale (WiFi).

---

## 📱📦 Pacchetti Pronti all'Uso & Web Receiver Live

Troverai tutti i pacchetti ed i link pronti per l'installazione diretta:

| Piattaforma Target | Tipo Pacchetto | File o Link Ufficiale |
| :--- | :--- | :--- |
| 📲 **App Mobile Controller** | `.apk` (Android) | [`apks/2TV-Mobile.apk`](apks/2TV-Mobile.apk) |
| 📺 **Android TV Nativa** | `.apk` (ExoPlayer) | [`apks/2TV-AndroidTV.apk`](apks/2TV-AndroidTV.apk) |
| 🌐 **Hisense VIDAA TV & PC / Mac Receiver (Live URL)** | **GitHub Pages** | 🌐 **[`https://mccoy88f.github.io/2TV/vidaa-tv/index.html`](https://mccoy88f.github.io/2TV/vidaa-tv/index.html)** |
| 🟦 **Samsung Smart TV** | `.wgt` (Tizen) | [`samsung-tv/2TV-SamsungTV.wgt`](samsung-tv/2TV-SamsungTV.wgt) |
| 🔴 **LG Smart TV** | `.ipk` (webOS) | [`lg-webos/2TV-LGwebOS.ipk`](lg-webos/2TV-LGwebOS.ipk) |
| 🟢 **Hisense Smart TV (Offline)** | `.zip` (VIDAA) | [`vidaa-tv/2TV-HisenseVIDAA.zip`](vidaa-tv/2TV-HisenseVIDAA.zip) |

---

## 💻🌐 Uso del Web Receiver su PC, Mac e Laptop

L'URL ufficialmente ospitato su GitHub Pages vale sia come **App per Smart TV Hisense VIDAA**, sia come **Ricevitore Multimediale Universale per PC / Mac / Tablet**:

👉 **`https://mccoy88f.github.io/2TV/vidaa-tv/index.html`** *(oppure `https://mccoy88f.github.io/2TV/`)*

1. Apri l'URL da qualsiasi browser su PC o Mac (Chrome, Firefox, Edge, Safari).
2. Premi **F11** per mettere la pagina a schermo intero.
3. Inquadra il QR Code con l'app 2TV dallo smartphone: il tuo PC/Mac diventa all'istante un ricevitore wireless per trasmettere video, foto e canali IPTV M3U!
4. Usa le **Frecce della Tastiera** (`Up`, `Down`, `Left`, `Right`, `Enter`, `Esc`) per navigare esattamente come con il telecomando di una TV.

---

## 🚀 Caratteristiche Principali

### 📺 App TV & Web Receiver (Android TV, Samsung Tizen, LG webOS, Hisense VIDAA, PC)
- **Interfaccia TV 1080p con Zoom Focus (1.10x)**: Ottimizzata per telecomando TV e tastiera PC.
- **Supporto Playlist IPTV M3U**: Analisi automatica dei file `.m3u` con finestra di dialogo **Seleziona Canale TV** per scorrere e riprodurre al volo i canali TV (Rai, Mediaset, ecc.).
- **Barra Superiore con Auto-Hide**: Badge colorati (`STREAM` viola, `FILE` verde, `LINK` blu), pulsante **Lista Canali** e recupero rapido con **Freccia Su** (`D-Pad UP`).
- **Server HTTP & QR Code Integrati**: QR code di accoppiamento a schermo con IP locale e token di sicurezza.
- **Universal Media Player**: Decodifica hardware di video MP4, MKV, dirette streaming HLS `.m3u8` (ExoPlayer su Android TV, `webapis.avplay` su Samsung Tizen, Hls.js / MSE su LG webOS, Hisense VIDAA e PC).

### 📱 App Mobile Controller (`:app`)
- **Material Design 3**: Switch rapido Temi Chiaro (White) e Scuro (Dark).
- **Abbinamento Rapido QR Code**: Scansione integrata con CameraX e ML Kit.
- **Trasferimento File Locali (`POST /api/upload`)**: Caricamento di video e foto dalla memoria dello smartphone alla TV con riproduzione automatica.
- **Menu Condividi di Sistema**: Condivisione da qualsiasi app (YouTube, Chrome, Galleria) con selettore TV immediato.

---

## 🏛️ Architettura del Progetto

```
2TV/
├── apks/                      # APK pronti (2TV-Mobile.apk, 2TV-AndroidTV.apk)
├── android/
│   ├── app/                   # App Mobile Controller (Kotlin + Compose)
│   └── tv/                    # App Nativa Android TV (Kotlin + ExoPlayer)
├── web-tv-core/               # Nucleo Web Condiviso (CSS, Player, M3U Parser, Remote Adapter)
├── samsung-tv/                # App Samsung Smart TV (Tizen OS .wgt)
├── lg-webos/                  # App LG Smart TV (webOS .ipk)
└── vidaa-tv/                  # App Hisense Smart TV & Web PC Receiver (Hosted URL / .zip)
```

---

## 📡 Protocollo Payload REST JSON (`POST /api/play`)

```json
{
  "command": "PLAY",
  "mediaType": "STREAM",
  "url": "https://github.com/Tundrak/IPTV-Italia/raw/main/iptvita.m3u",
  "title": "IPTV Italia Channel List",
  "saveToTv": true
}
```

---

## 🌐 Configurazione Hisense VIDAA App Store

Per pubblicare 2TV su **Hisense VIDAA Developer Portal**:
1. Accedi al VIDAA Dev Portal ed inserisci il nome dell'app: `2TV Receiver`.
2. Nella sezione **App URL / Hosted Web App**, inserisci il seguente link ufficialmente ospitato su GitHub Pages:
   👉 `https://mccoy88f.github.io/2TV/vidaa-tv/index.html`
