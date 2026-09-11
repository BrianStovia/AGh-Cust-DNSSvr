package com.brst.dns.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brst.dns.ui.theme.*
import com.brst.dns.ui.viewmodel.DotViewModel

@Composable
fun DotConfigScreen(viewModel: DotViewModel) {
    val dotHost by viewModel.dotHost.collectAsState()
    val dotPort by viewModel.dotPort.collectAsState()
    val dotTlsName by viewModel.dotTlsServerName.collectAsState()
    val dohUrl by viewModel.dohUrl.collectAsState()
    val activeProtocol by viewModel.protocol.collectAsState()

    var selectedTab by remember(activeProtocol) {
        mutableStateOf(if (activeProtocol.equals("DoH", ignoreCase = true)) 1 else 0)
    }

    var hostInput by remember(dotHost) { mutableStateOf(dotHost) }
    var portInput by remember(dotPort) { mutableStateOf(dotPort.toString()) }
    var tlsNameInput by remember(dotTlsName) { mutableStateOf(dotTlsName) }
    var dohUrlInput by remember(dohUrl) { mutableStateOf(dohUrl) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BrstBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Header ---
        item {
            Text(
                text = "Konfigurasi DoT & DoH",
                color = BrstTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Pilih protokol enkripsi dan server resolver kustom untuk perangkat Anda",
                color = BrstTextSecondary,
                fontSize = 12.sp
            )
        }

        // --- 2. Protocol Segmented Switcher ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(BrstSurfaceVariant)
                    .padding(4.dp)
            ) {
                // Tab DoT
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedTab == 0) BrstPrimary else Color.Transparent)
                        .clickable { selectedTab = 0 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "DoT",
                            tint = if (selectedTab == 0) BrstTextPrimary else BrstTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "DoT (Port 853)",
                            color = if (selectedTab == 0) BrstTextPrimary else BrstTextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                // Tab DoH
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedTab == 1) BrstPrimary else Color.Transparent)
                        .clickable { selectedTab = 1 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Http,
                            contentDescription = "DoH",
                            tint = if (selectedTab == 1) BrstTextPrimary else BrstTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "DoH (HTTPS)",
                            color = if (selectedTab == 1) BrstTextPrimary else BrstTextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // --- 3. Configuration Content Based on Tab ---
        if (selectedTab == 0) {
            // ================= DoT (DNS-over-TLS) =================
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BrstCardBorder, RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = BrstSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Server DNS-over-TLS (DoT)",
                            color = BrstTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        OutlinedTextField(
                            value = hostInput,
                            onValueChange = { hostInput = it },
                            label = { Text("Host / Alamat IP Resolver") },
                            placeholder = { Text("Contoh: 1.1.1.1 atau dns.domainanda.com") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = outlinedColors()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = portInput,
                                onValueChange = { portInput = it },
                                label = { Text("Port TLS") },
                                placeholder = { Text("853") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = outlinedColors()
                            )

                            OutlinedTextField(
                                value = tlsNameInput,
                                onValueChange = { tlsNameInput = it },
                                label = { Text("SNI TLS Hostname") },
                                placeholder = { Text("one.one.one.one") },
                                modifier = Modifier.weight(2f),
                                singleLine = true,
                                colors = outlinedColors()
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                val port = portInput.toIntOrNull() ?: 853
                                viewModel.saveDotConfig(hostInput, port, tlsNameInput)
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrstPrimary)
                        ) {
                            Text("Simpan & Aktifkan Mode DoT", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // DoT Presets
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BrstCardBorder, RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = BrstSurface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Preset DoT Terpercaya",
                            color = BrstTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val dotPresets = listOf(
                            DotPresetItem("Cloudflare Anycast DoT", "1.1.1.1", 853, "one.one.one.one", "Ultra-cepat global Anycast node"),
                            DotPresetItem("Quad9 Privacy Shield DoT", "9.9.9.9", 853, "dns.quad9.net", "Blokir malware & privasi Swiss GDPR"),
                            DotPresetItem("AdGuard AdBlock DoT", "94.140.14.14", 853, "dns.adguard-dns.com", "Blokir iklan & pelacak otomatis"),
                            DotPresetItem("Google Public DoT", "8.8.8.8", 853, "dns.google", "Google Anycast DNS backbone"),
                            DotPresetItem("Mullvad Privacy DoT", "194.242.2.3", 853, "adblock.dns.mullvad.net", "Zero-log Swedia privacy resolver")
                        )

                        dotPresets.forEach { preset ->
                            val isSelected = hostInput == preset.host && activeProtocol.equals("DoT", ignoreCase = true)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) BrstPrimary.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable {
                                        hostInput = preset.host
                                        portInput = preset.port.toString()
                                        tlsNameInput = preset.tlsName
                                        viewModel.applyPreset(preset.name, preset.host, preset.port, preset.tlsName)
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = preset.name,
                                        color = if (isSelected) BrstAccent else BrstTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "${preset.host}:${preset.port} (${preset.tlsName})",
                                        color = BrstTextSecondary,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = preset.desc,
                                        color = BrstTextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = BrstAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            HorizontalDivider(color = BrstCardBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                        }
                    }
                }
            }

            // Android Native Private DNS Guide
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BrstCardBorder, RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = BrstSurface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info",
                                tint = BrstAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "DNS Pribadi Bawaan Android (Native DoT)",
                                color = BrstTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Di Android 9 ke atas, DoT juga dapat digunakan tanpa VPN:\n" +
                                    "1. Buka Pengaturan Android -> Jaringan & Internet -> DNS Pribadi.\n" +
                                    "2. Pilih 'Nama host penyedia DNS pribadi'.\n" +
                                    "3. Masukkan hostname TLS (misal: one.one.one.one atau dns.quad9.net).\n" +
                                    "4. Simpan.",
                            color = BrstTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

        } else {
            // ================= DoH (DNS-over-HTTPS) =================
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BrstCardBorder, RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = BrstSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Server DNS-over-HTTPS (DoH)",
                            color = BrstTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        OutlinedTextField(
                            value = dohUrlInput,
                            onValueChange = { dohUrlInput = it },
                            label = { Text("DoH Endpoint URL (RFC 8484)") },
                            placeholder = { Text("https://cloudflare-dns.com/dns-query") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = outlinedColors()
                        )

                        Text(
                            text = "Mendukung URL kustom server BRST / AdGuard Home Anda, contoh:\nhttps://dns.domainanda.com/dns-query/{nama_hp}",
                            color = BrstTextMuted,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                viewModel.saveDohConfig(dohUrlInput)
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrstPrimary)
                        ) {
                            Text("Simpan & Aktifkan Mode DoH", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // DoH Presets
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BrstCardBorder, RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = BrstSurface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Preset DoH Terpercaya",
                            color = BrstTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val dohPresets = listOf(
                            DohPresetItem("Cloudflare DoH", "https://cloudflare-dns.com/dns-query", "HTTP/2 ultra-cepat global Anycast"),
                            DohPresetItem("Quad9 Secure DoH", "https://dns.quad9.net/dns-query", "Blokir malware & proteksi privasi Swiss"),
                            DohPresetItem("AdGuard AdBlock DoH", "https://dns.adguard-dns.com/dns-query", "Blokir iklan & pelacak otomatis"),
                            DohPresetItem("Google Public DoH", "https://dns.google/dns-query", "Google global backbone resolver"),
                            DohPresetItem("Mullvad Privacy DoH", "https://adblock.dns.mullvad.net/dns-query", "Zero-log Swedia privacy DoH")
                        )

                        dohPresets.forEach { preset ->
                            val isSelected = dohUrlInput == preset.url && activeProtocol.equals("DoH", ignoreCase = true)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) BrstPrimary.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable {
                                        dohUrlInput = preset.url
                                        viewModel.applyDohPreset(preset.name, preset.url)
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = preset.name,
                                        color = if (isSelected) BrstAccent else BrstTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = preset.url,
                                        color = BrstTextSecondary,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = preset.desc,
                                        color = BrstTextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = BrstAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            HorizontalDivider(color = BrstCardBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                        }
                    }
                }
            }

            // DoH Advantage Info
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BrstCardBorder, RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = BrstSurface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info",
                                tint = BrstAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Keunggulan DNS-over-HTTPS (DoH)",
                                color = BrstTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "DoH menyamarkan permintaan DNS sebagai lalu lintas web HTTPS biasa pada Port 443. Hal ini membuat DoH sangat efektif menembus firewall atau jaringan WiFi publik yang memblokir port DNS standar (53) maupun port DoT (853).",
                            color = BrstTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun outlinedColors(): TextFieldColors {
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = BrstAccent,
        unfocusedBorderColor = BrstCardBorder,
        focusedTextColor = BrstTextPrimary,
        unfocusedTextColor = BrstTextPrimary,
        focusedLabelColor = BrstAccent,
        unfocusedLabelColor = BrstTextSecondary,
        cursorColor = BrstAccent
    )
}

data class DotPresetItem(
    val name: String,
    val host: String,
    val port: Int,
    val tlsName: String,
    val desc: String
)

data class DohPresetItem(
    val name: String,
    val url: String,
    val desc: String
)
