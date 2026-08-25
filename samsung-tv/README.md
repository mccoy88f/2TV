# 2TV Receiver - Samsung Smart TV (Tizen OS)

Questo modulo contiene l'applicazione nativa per **Samsung Smart TV** (compatibile sia con TV nuove Tizen 5.0 - 8.0+ sia con modelli datati Tizen 2.4 - 4.0 dal 2015 al 2018).

---

## 📦 File di Installazione Pronti

- **Pacchetto Tizen Widget**: [`samsung-tv/2TV-SamsungTV.wgt`](file:///Users/antonello/Sviluppo/2TV/samsung-tv/2TV-SamsungTV.wgt)
- **Script di Build Automatica**: [`samsung-tv/build.sh`](file:///Users/antonello/Sviluppo/2TV/samsung-tv/build.sh)

---

## 📺 Come Installare su Samsung Smart TV

### Metodo 1: Installazione via Chiavetta USB (Modalità Sviluppatore TV)

1. Formatta una chiavetta USB in **FAT32**.
2. Nella radice della chiavetta USB, crea la cartella `userwidget`.
3. Copia il file `2TV-SamsungTV.wgt` (o scompattalo nella cartella `userwidget`).
4. Accendi la Samsung Smart TV e imposta la **Modalità Sviluppatore (Developer Mode)**:
   - Vai in **Smart Hub** -> **App**.
   - Digita sul telecomando la sequenza `1 2 3 4 5`.
   - Attiva l'opzione **Developer Mode** su **ON** ed inserisci l'IP del tuo computer.
   - Riavvia la TV (tenendo premuto il tasto di accensione sul telecomando per 5 secondi).
5. Inserisci la chiavetta USB nella TV: la Samsung Smart TV rileverà l'app `2TV` installandola automaticamente nello Smart Hub!

---

## 🛠️ Metodo 2: Installazione via Tizen Studio / CLI (WiFi)

Se hai installato **Tizen Studio** sul tuo computer:

1. Collega la TV alla stessa rete WiFi del computer.
2. Trova l'IP della TV nelle impostazioni di rete della TV (es. `192.168.1.50`).
3. Connetti la CLI di Tizen alla TV:
   ```bash
   sdb connect 192.168.1.50
   ```
4. Installa il file `.wgt` sulla TV:
   ```bash
   tizen install -n 2TV-SamsungTV.wgt -t <device_name>
   ```

---

## 🎮 Funzionalità Telecomando Samsung

- **Frecce Direzionali (D-Pad)**: Navigazione nello storico dei contenuti ricevuti.
- **Tasto OK / Enter**: Riproduzione del contenuto selezionato.
- **Tasto RETURN / BACK (`10009`)**:
  - Durante la riproduzione: Ferma il video/foto e torna alla schermata di abbinamento col QR Code.
  - Nella schermata principale: Chiede conferma di uscita dall'app.
- **Play / Pause / Stop**: Controlli multimediali per i video.
