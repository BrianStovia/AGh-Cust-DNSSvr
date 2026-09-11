# BRST DNS Shield - Native Android DoT & DoH Client with Local AdBlocker

A high-performance, ultra-lightweight Native Android **DNS-over-TLS (DoT RFC 7858)** & **DNS-over-HTTPS (DoH RFC 8484)** encrypted client with **On-Device Ad & Tracker Sinkholing**, built with **Kotlin**, **Coroutines**, and **Jetpack Compose / Material 3**.

---

## ✨ Fitur Utama Aplikasi

### 1. 🛡️ Mode DNS-over-TLS (DoT - Port 853)
- **Local TUN VpnService**: Menangkap seluruh paket DNS lokal (UDP port 53) dari semua aplikasi di perangkat Android secara transparan tanpa perlu root.
- **TLS 1.3 Encryption**: Mengenkapsulasi query DNS lokal dengan framing 2-byte prefix biner RFC 7858 langsung ke port 853 server DoT.
- **Protected Sockets**: Menggunakan `vpnService.protect(rawSocket)` sebelum handshake TLS untuk mencegah perulangan routing (*routing loops/deadlocks*).
- **SNI TLS Server Name**: Mendukung Server Name Indication (SNI) kustom untuk validasi sertifikat SSL/TLS.
- **Preset DoT Terpercaya**: Cloudflare (`1.1.1.1:853`), Quad9 (`9.9.9.9:853`), AdGuard (`94.140.14.14:853`), Google (`8.8.8.8:853`), Mullvad (`194.242.2.3:853`).

### 2. 🌐 Mode DNS-over-HTTPS (DoH - Port 443)
- **HTTP/2 Wire Format (RFC 8484)**: Mengirim query DNS biner melalui permintaan HTTPS terenkripsi `application/dns-message`.
- **Bypass Firewall**: Menyamarkan traffic DNS sebagai lalu lintas web standar port 443 sehingga lolos dari blokir port 53 & 853 di jaringan ketat / WiFi publik.
- **Kustom Client Tagging**: Mendukung format URL kustom server BRST / AdGuard Home Anda, contoh: `https://dns.domainanda.com/dns-query/{nama_hp}`.
- **Preset DoH Terpercaya**: Cloudflare DoH, Quad9 DoH, AdGuard DoH, Google DoH, Mullvad DoH.

### 3. 🚫 Local On-Device Ad & Tracker Blocker (Sinkhole 0.0.0.0)
- **Mesin Sinkhole Lokal**: Mencegat domain iklan dan pelacak secara instan di HP tanpa mengirim query ke jaringan (0 ms response).
- **Format Filter Fleksibel**: Mendukung daftar AdGuard (`||domain.com^`), file Hosts (`0.0.0.0 domain.com` / `127.0.0.1 domain.com`), dan daftar domain teks biasa.
- **Preset Filter Populer**: AdGuard DNS Filter, OISD Basic, AdAway Mobile Hosts, HaGeZi Multi PRO Mini.
- **Editor Aturan Kustom**: Masukkan domain blacklist pribadi langsung di aplikasi.
- **Hemat Kuota & Baterai**: Mencegah pemuatan banner iklan dan pelacak analitik dari latar belakang.

### 4. 🔍 Log Aktivitas Query & Pencarian Real-Time
- Pemantauan real-time query DNS yang dikirim dari perangkat.
- Fitur pencarian domain instan dan tab filter: `Semua`, `🚫 Diblokir`, dan `🛡️ Terenkripsi`.
- Menampilkan nama domain, timestamp, latensi (ms) dengan indikator warna, protokol yang aktif (DoT / DoH), dan status `DIBLOKIR`.

### 5. 🎛️ Quick Settings Tile Android
- Toggle proteksi instan langsung dari panel *Quick Settings* status bar Android (Tarik bar notifikasi -> Tambah Tile "BRST DoT/DoH").

---

## 🚀 Cara Menjalankan & Build APK

### Prasyarat
- Android Studio (Ladybug / Koala / Flamingo / Hedgehog)
- Android SDK 35 & Min SDK 26 (Android 8.0+)
- JDK 17 atau 21

### Build APK via CLI
```sh
cd android
./gradlew assembleDebug
```
Output APK berada di `android/app/build/outputs/apk/debug/app-debug.apk`.
