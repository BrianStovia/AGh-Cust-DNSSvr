# BRST DoT Shield - Native Android DNS-over-TLS Client

A high-performance, ultra-lightweight Native Android DNS-over-TLS (DoT) & DoH client built with **Kotlin**, **Coroutines**, and **Jetpack Compose / Material 3**.

---

## ✨ Fitur Aplikasi

### 1. 🛡️ Mode DNS-over-TLS (RFC 7858 - Port 853)
- **Local TUN VpnService**: Menangkap seluruh paket DNS lokal (UDP port 53) dari semua aplikasi di perangkat Android secara transparan tanpa root.
- **TLS 1.3 Encryption**: Mengenkapsulasi query DNS lokal dengan framing 2-byte prefix biner RFC 7858 langsung ke port 853 server DoT.
- **Protected Sockets**: Menggunakan `vpnService.protect(rawSocket)` sebelum handshake TLS untuk mencegah perulangan routing (*routing loops/deadlocks*).
- **SNI TLS Server Name**: Mendukung Server Name Indication (SNI) kustom untuk validasi sertifikat SSL/TLS.

### 2. ⚡ Presets & Kustom Resolver
- **DoT Kustom**: Masukkan Host/IP, Port TLS (default 853), dan TLS Hostname resolver pribadi Anda.
- **Preset Terpercaya**: Pilihan cepat 1-ketuk untuk:
  - *Cloudflare Anycast DoT* (`1.1.1.1:853` - `one.one.one.one`)
  - *Quad9 Privacy Shield DoT* (`9.9.9.9:853` - `dns.quad9.net`)
  - *AdGuard AdBlock DoT* (`94.140.14.14:853` - `dns.adguard-dns.com`)
  - *Google Public DoT* (`8.8.8.8:853` - `dns.google`)
  - *Mullvad Privacy DoT* (`194.242.2.3:853` - `adblock.dns.mullvad.net`)
- **DoH Fallback Switch**: Pilihan protokol fleksibel untuk beralih antara DoT dan DoH (RFC 8484).

### 3. 🔍 Log Aktivitas Query Lokal
- Pemantauan real-time query DNS yang dikirim dari perangkat.
- Menampilkan nama domain, timestamp, latensi (ms), protokol yang digunakan, dan status keberhasilan.

### 4. 🎛️ Quick Settings Tile Android
- Toggle proteksi DoT langsung dari panel *Quick Settings* status bar Android (Tarik bar notifikasi -> Tambah Tile "BRST DoT").

---

## 🚀 Cara Build di Android Studio

### Prasyarat
- Android Studio (Koala / Ladybug / Flamingo / Hedgehog)
- Android SDK 35 (Android 15) & Min SDK 26 (Android 8.0+)
- JDK 17 atau 21

### Langkah Menjalankan
1. Buka folder `android/` di Android Studio:
   ```
   File -> Open -> Pilih direktori <repo>/android
   ```
2. Tunggu Gradle sync selesai.
3. Hubungkan perangkat Android fisik atau Emulator.
4. Klik tombol **Run 'app'** (`Shift + F10`).

### Build APK via CLI
```sh
cd android
./gradlew assembleDebug
```
Output APK berada di `android/app/build/outputs/apk/debug/app-debug.apk`.
