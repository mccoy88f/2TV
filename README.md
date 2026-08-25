# 2TV (To TV) - Ecosistema Multi-Piattaforma Smart TV

**2TV** è un ecosistema completo composto da un'**App Mobile Controller per Cellulare**, un'**App Nativa Android TV** e pacchetti nativi per **Samsung Smart TV (Tizen OS)**, **LG Smart TV (webOS)** ed **Hisense Smart TV (VIDAA OS)** per condividere e riprodurre in tempo reale qualsiasi contenuto multimediale (video, dirette streaming HLS, playlist IPTV M3U, foto e audio) o **file fisici locali** dallo smartphone verso qualsiasi schermo TV in rete locale (Wi-Fi).

---

## 📱📦 Pacchetti Pronti all'Uso (App Native)

Troverai tutti i pacchetti ed i file pronti per l'installazione diretta su smartphone e Smart TV:

| Piattaforma Target | Tipo Pacchetto | File Pacchetto Ufficiale |
| :--- | :--- | :--- |
| 📲 **App Mobile Controller** | `.apk` (Android) | [`apks/2TV-Mobile.apk`](apks/2TV-Mobile.apk) |
| 📺 **Android TV Nativa** | `.apk` (ExoPlayer) | [`apks/2TV-AndroidTV.apk`](apks/2TV-AndroidTV.apk) |
| 🟦 **Samsung Smart TV** | `.wgt` (Tizen OS) | [`samsung-tv/2TV-SamsungTV.wgt`](samsung-tv/2TV-SamsungTV.wgt) |
| 🔴 **LG Smart TV** | `.ipk` (webOS) | [`lg-webos/2TV-LGwebOS.ipk`](lg-webos/2TV-LGwebOS.ipk) |
| 🟨 **Hisense Smart TV** | `.zip` (VIDAA OS) | [`vidaa-tv/2TV-HisenseVIDAA.zip`](vidaa-tv/2TV-HisenseVIDAA.zip) |

---

## 🚀 Caratteristiche Principali

### 📺 App TV Native (Android TV, Samsung Tizen, LG webOS, Hisense VIDAA)
- **Interfaccia TV 1080p Standardizzata ad Android TV**: Ottimizzata per telecomando TV (D-Pad).
- **Server HTTP REST Locale (`http://<IP_TV>:8080`)**: Server Ktor/HTTP nativo in ascolto sulla porta 8080 per ricevere comandi ed il caricamento di file pesanti dallo smartphone.
- **Supporto Playlist IPTV M3U**: Analisi automatica dei file `.m3u` con finestra di dialogo **Seleziona Canale TV** per scorrere e riprodurre al volo i canali TV (Rai, Mediaset, ecc.).
- **Barra Superiore con Auto-Hide**: Badge colorati (`STREAM` viola, `FILE` verde, `LINK` blu), pulsante **Lista Canali** e recupero rapido con **Freccia Su** (`D-Pad UP`).
- **QR Code Integrati**: QR code di accoppiamento a schermo con IP locale e token di sicurezza.
- **Universal Media Player**: Decodifica hardware di video MP4, MKV, dirette streaming HLS `.m3u8` (ExoPlayer su Android TV, `webapis.avplay` su Samsung Tizen, Hls.js / MSE su LG webOS e Hisense VIDAA).

### 📱 App Mobile Controller (`:app`)
- **Material Design 3**: Switch rapido Temi Chiaro (White) e Scuro (Dark).
- **Abbinamento Rapido QR Code**: Scansione integrata con CameraX e ML Kit.
- **Trasferimento File Locali (`POST /api/upload`)**: Caricamento di video e foto dalla memoria dello smartphone alla TV con riproduzione automatica.
- **Menu Condividi di Sistema**: Condivisione da qualsiasi app (YouTube, Chrome, Galleria) con selettore TV immediato.

---

## 🏛️ Architettura del Progetto

```
2TV/
├── apks/                      # APK Android pronti (2TV-Mobile.apk, 2TV-AndroidTV.apk)
├── android/
│   ├── app/                   # App Mobile Controller (Kotlin + Compose)
│   └── tv/                    # App Nativa Android TV (Kotlin + ExoPlayer)
├── web-tv-core/               # Nucleo Web Condiviso (CSS, Player, M3U Parser, Remote Adapter)
├── samsung-tv/                # App Samsung Smart TV (Tizen OS .wgt)
├── lg-webos/                  # App LG Smart TV (webOS .ipk)
└── vidaa-tv/                  # App Hisense Smart TV (VIDAA OS .zip)
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
