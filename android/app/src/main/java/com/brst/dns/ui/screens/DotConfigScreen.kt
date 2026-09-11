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
import androidx.compose.material.icons.filled.Info
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
        // --- Header ---
        item {
            Text(
                text = "Konfigurasi Server DoT",
                color = BrstTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Pilih resolver DNS-over-TLS (Port 853) atau masukkan server kustom Anda",
                color = BrstTextSecondary,
                fontSize = 12.sp
            )
        }

        // --- Custom Server Form ---
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
                        text = "Server DoT Kustom",
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
                        Text("Simpan Konfigurasi DoT", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- Quick Presets ---
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

                    val presets = listOf(
                        PresetItem("Cloudflare Anycast DoT", "1.1.1.1", 853, "one.one.one.one", "Ultra-cepat global Anycast node"),
                        PresetItem("Quad9 Privacy Shield DoT", "9.9.9.9", 853, "dns.quad9.net", "Blokir malware & privasi Swiss FADP/GDPR"),
                        PresetItem("AdGuard AdBlock DoT", "94.140.14.14", 853, "dns.adguard-dns.com", "Blokir iklan & pelacak otomatis"),
                        PresetItem("Google Public DoT", "8.8.8.8", 853, "dns.google", "Google Anycast DNS backbone"),
                        PresetItem("Mullvad Privacy DoT", "194.242.2.3", 853, "adblock.dns.mullvad.net", "Zero-log Swedia privacy resolver")
                    )

                    presets.forEach { preset ->
                        val isSelected = hostInput == preset.host
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
                        Divider(color = BrstCardBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                    }
                }
            }
        }

        // --- Android Native Private DNS Guide ---
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
                        text = "Di Android 9 ke atas, Anda juga dapat mengaktifkan DoT langsung di sistem tanpa VPN:\n" +
                                "1. Buka Pengaturan Android -> Jaringan & Internet -> DNS Pribadi.\n" +
                                "2. Pilih 'Nama host penyedia DNS pribadi'.\n" +
                                "3. Masukkan hostname TLS (contoh: one.one.one.one atau dns.quad9.net atau domain server Anda).\n" +
                                "4. Klik Simpan.",
                        color = BrstTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
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

data class PresetItem(
    val name: String,
    val host: String,
    val port: Int,
    val tlsName: String,
    val desc: String
)
