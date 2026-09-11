package com.brst.dns.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
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

    val isBlocklistEnabled by viewModel.isBlocklistEnabled.collectAsState()
    val blocklistUrl by viewModel.blocklistUrl.collectAsState()
    val customRules by viewModel.customBlockedRules.collectAsState()
    val ruleCount by viewModel.blocklistRuleCount.collectAsState()
    val isUpdatingBlocklist by viewModel.isUpdatingBlocklist.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }

    var hostInput by remember(dotHost) { mutableStateOf(dotHost) }
    var portInput by remember(dotPort) { mutableStateOf(dotPort.toString()) }
    var tlsNameInput by remember(dotTlsName) { mutableStateOf(dotTlsName) }
    var dohUrlInput by remember(dohUrl) { mutableStateOf(dohUrl) }
    var blocklistUrlInput by remember(blocklistUrl) { mutableStateOf(blocklistUrl) }
    var customRulesInput by remember(customRules) { mutableStateOf(customRules) }

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
                text = "Konfigurasi Shield",
                color = BrstTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Atur protokol DoT/DoH, resolver kustom, dan daftar blokir iklan lokal",
                color = BrstTextSecondary,
                fontSize = 12.sp
            )
        }

        // --- 2. 3-Tab Segmented Switcher ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(BrstSurfaceVariant)
                    .padding(3.dp)
            ) {
                // Tab 0: DoT
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedTab == 0) BrstPrimary else Color.Transparent)
                        .clickable { selectedTab = 0 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "DoT (853)",
                        color = if (selectedTab == 0) BrstTextPrimary else BrstTextMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Tab 1: DoH
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedTab == 1) BrstPrimary else Color.Transparent)
                        .clickable { selectedTab = 1 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "DoH (443)",
                        color = if (selectedTab == 1) BrstTextPrimary else BrstTextMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Tab 2: Blocklist
                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedTab == 2) BrstPrimary else Color.Transparent)
                        .clickable { selectedTab = 2 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🚫 Blocklist",
                        color = if (selectedTab == 2) BrstTextPrimary else BrstTextMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // --- 3. Tab Content ---
        when (selectedTab) {
            0 -> {
                // ================= TAB DoT =================
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
            }

            1 -> {
                // ================= TAB DoH =================
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
            }

            2 -> {
                // ================= TAB BLOCKLIST =================
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
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Pemblokir Iklan & Pelacak",
                                        color = BrstTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "$ruleCount aturan aktif di perangkat",
                                        color = if (isBlocklistEnabled) BrstSuccess else BrstTextMuted,
                                        fontSize = 12.sp
                                    )
                                }

                                Switch(
                                    checked = isBlocklistEnabled,
                                    onCheckedChange = { viewModel.setBlocklistEnabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = BrstTextPrimary,
                                        checkedTrackColor = BrstPrimary,
                                        uncheckedThumbColor = BrstTextMuted,
                                        uncheckedTrackColor = BrstSurfaceVariant
                                    )
                                )
                            }

                            HorizontalDivider(color = BrstCardBorder, thickness = 1.dp)

                            Text(
                                text = "URL Sumber Daftar Blokir (Blocklist URL)",
                                color = BrstTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )

                            OutlinedTextField(
                                value = blocklistUrlInput,
                                onValueChange = { blocklistUrlInput = it },
                                label = { Text("URL Filter List (Hosts / AdGuard / txt)") },
                                placeholder = { Text("https://adguardteam.github.io/HostlistsRegistry/assets/filter_1.txt") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = outlinedColors()
                            )

                            Button(
                                onClick = {
                                    viewModel.updateBlocklistFromUrl(blocklistUrlInput)
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isUpdatingBlocklist,
                                colors = ButtonDefaults.buttonColors(containerColor = BrstPrimary)
                            ) {
                                if (isUpdatingBlocklist) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = BrstTextPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Mengunduh & Memproses Aturan...", fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(imageVector = Icons.Default.Download, contentDescription = "Update", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Perbarui Daftar Blokir Sekarang", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Preset Sumber Filter
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
                                text = "Preset Filter Blocklist Populer",
                                color = BrstTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            val filterPresets = listOf(
                                BlocklistPresetItem(
                                    "AdGuard DNS Filter (Rekomendasi)",
                                    "https://adguardteam.github.io/HostlistsRegistry/assets/filter_1.txt",
                                    "Standar emas pemblokir iklan & pelacak mobile global"
                                ),
                                BlocklistPresetItem(
                                    "OISD Basic Filter",
                                    "https://raw.githubusercontent.com/stevenblack/hosts/master/hosts",
                                    "Daftar blokir gabungan StevenBlack anti malware & adware"
                                ),
                                BlocklistPresetItem(
                                    "AdAway Official Mobile Hosts",
                                    "https://adaway.org/hosts.txt",
                                    "Daftar hosts ringan khusus aplikasi Android"
                                ),
                                BlocklistPresetItem(
                                    "HaGeZi Multi PRO Mini",
                                    "https://raw.githubusercontent.com/raghav-arora/Adblock-Filter/main/adguard.txt",
                                    "Filter ultra-ketat untuk privasi pelacakan & telemetri"
                                )
                            )

                            filterPresets.forEach { item ->
                                val isCurrent = blocklistUrlInput == item.url
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isCurrent) BrstPrimary.copy(alpha = 0.15f) else Color.Transparent)
                                        .clickable {
                                            blocklistUrlInput = item.url
                                            viewModel.updateBlocklistFromUrl(item.url)
                                        }
                                        .padding(vertical = 10.dp, horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            color = if (isCurrent) BrstAccent else BrstTextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = item.desc,
                                            color = BrstTextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                    if (isCurrent) {
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

                // Custom User Rules Editor
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
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Aturan Blokir Kustom Pribadi",
                                color = BrstTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Masukkan 1 domain per baris (contoh: tiktok.com atau ||ads.example.com^)",
                                color = BrstTextSecondary,
                                fontSize = 12.sp
                            )

                            OutlinedTextField(
                                value = customRulesInput,
                                onValueChange = { customRulesInput = it },
                                placeholder = { Text("tiktok.com\n||analytics.com^\n0.0.0.0 telemetry.app.com") },
                                modifier = Modifier.fillMaxWidth().height(120.dp),
                                colors = outlinedColors()
                            )

                            Button(
                                onClick = {
                                    viewModel.saveCustomRules(customRulesInput)
                                },
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrstSurfaceVariant)
                            ) {
                                Text("Terapkan Aturan Kustom", color = BrstAccent, fontWeight = FontWeight.Bold)
                            }
                        }
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

data class BlocklistPresetItem(
    val name: String,
    val url: String,
    val desc: String
)
