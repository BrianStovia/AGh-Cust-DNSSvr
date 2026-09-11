# DNS SERVER BRST - Android App & DoH Client

A modern Native Android application built with **Kotlin** and **Jetpack Compose / Material 3** designed for:
1. **Remote Server Management**: Control and monitor your **DNS SERVER BRST** (AdGuard Home fork) instance.
2. **Local Android DoH Client (VpnService)**: Encrypt and route all Android DNS queries using DNS-over-HTTPS (RFC 8484) wire format directly to your server.

---

## ✨ Fitur Aplikasi

### 1. 🛡️ Mode DoH Android (DNS-over-HTTPS Client)
- **Local TUN VpnService**: Menangkap paket UDP port 53 dari seluruh aplikasi di Android secara transparan.
- **RFC 8484 Wire-Format HTTP/2**: Mengenkapsulasi query DNS biner menjadi permintaan HTTPS terenkripsi ke endpoint `/dns-query` server BRST Anda.
- **Client Tagging**: Dukungan format URL `/dns-query/{client_id}` sehingga nama perangkat (misal: `Android-Pixel-Rey`) langsung teridentifikasi rapi di *Query Log* server AdGuard Home.
- **Quick Settings Tile**: Toggle aktifkan/matikan mode DoH langsung dari *Quick Settings Panel* status bar Android.
- **Preset Publik**: Pilihan cepat beralih ke preset Cloudflare Anycast, Quad9 Swiss Shield, AdGuard, atau Google DoH.

### 2. ⚡ Server Dashboard & Remote Control
- **Status & Proteksi**: Saklar instan untuk mengaktifkan atau menjeda proteksi DNS server.
- **Live Metrics**: Pemantauan jumlah query 24 jam, query diblokir, persentase efektivitas pemblokiran, dan rata-rata latensi upstream.
- **Smart Game Mode QoS Toggle**: Tombol cepat untuk mengaktifkan mode prioritas rendah-latensi game online.
- **RAM & DB Auto-Maintenance**: Tombol eksekusi Garbage Collection dan pembersihan cache server dari jarak jauh.

### 3. 🔍 Live Query Log & Filter Management
- Pemantauan riwayat query DNS secara *real-time*.
- Pencarian dan filter berdasarkan domain atau IP klien.
- Aksi 1-ketuk untuk **Buka Blokir (Whitelist)** atau **Blokir Domain (Blacklist)** langsung dari ponsel.

### 4. 📱 Penemuan Perangkat LAN
- Menampilkan daftar perangkat lokal yang terdeteksi via ARP / DHCP pada server beserta nama vendor MAC dan tipe perangkat.

---

## 🚀 Cara Build & Menjalankan di Android Studio

### Prasyarat
- Android Studio Ladybug / Koala / Hedgehog (atau versi terbaru)
- Android SDK 35 (Android 15) & Min SDK 26 (Android 8.0+)
- JDK 17 atau 21

### Langkah Build
1. Buka folder `android/` di Android Studio:
   ```sh
   File -> Open -> Pilih direktori <repo>/android
   ```
2. Biarkan Gradle melakukan sinkronisasi dependensi.
3. Sambungkan perangkat Android fisik (via USB Debugging atau Wireless Debugging) atau jalankan Android Emulator.
4. Klik **Run 'app'** (`Shift + F10`) untuk memasang dan menjalankan aplikasi di perangkat Anda.

### Build APK via Terminal (CLI)
Jika menggunakan Gradle wrapper di terminal:
```sh
cd android
./gradlew assembleDebug
```
File APK hasil build akan berada di: `android/app/build/outputs/apk/debug/app-debug.apk`.
