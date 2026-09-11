package com.brst.dns.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brst.dns.ui.theme.*
import com.brst.dns.ui.viewmodel.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val serverUrl by viewModel.serverUrl.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()

    val dnsConfig by viewModel.dnsConfig.collectAsState()
    val filteringStatus by viewModel.filteringStatus.collectAsState()
    val safeBrowsing by viewModel.safeBrowsingEnabled.collectAsState()
    val parental by viewModel.parentalEnabled.collectAsState()
    val safeSearch by viewModel.safeSearchEnabled.collectAsState()
    val gameMode by viewModel.gameModeStatus.collectAsState()
    val rebindConfig by viewModel.rebindConfig.collectAsState()
    val odohConfig by viewModel.odohConfig.collectAsState()
    val telegramConfig by viewModel.telegramConfig.collectAsState()
    val blockedServices by viewModel.blockedServices.collectAsState()

    var activeCategory by remember { mutableStateOf("Semua") }

    // Form inputs state
    var urlInput by remember(serverUrl) { mutableStateOf(serverUrl) }
    var userInput by remember(username) { mutableStateOf(username) }
    var passInput by remember(password) { mutableStateOf(password) }
    var showPassword by remember { mutableStateOf(false) }

    var upstreamsInput by remember(dnsConfig) {
        mutableStateOf(dnsConfig?.upstreamDns?.joinToString("\n") ?: "https://dns.quad9.net/dns-query\ntls://1.1.1.1")
    }
    var bootstrapsInput by remember(dnsConfig) {
        mutableStateOf(dnsConfig?.bootstrapDns?.joinToString("\n") ?: "9.9.9.9\n1.1.1.1")
    }
    var upstreamModeInput by remember(dnsConfig) {
        mutableStateOf(dnsConfig?.upstreamMode ?: "load_balance")
    }
    var dnssecInput by remember(dnsConfig) {
        mutableStateOf(dnsConfig?.dnssecEnabled ?: false)
    }
    var ecsInput by remember(dnsConfig) {
        mutableStateOf(dnsConfig?.ednsCsEnabled ?: false)
    }
    var optimisticInput by remember(dnsConfig) {
        mutableStateOf(dnsConfig?.cacheOptimistic ?: false)
    }

    var userRulesInput by remember(filteringStatus) {
        mutableStateOf(filteringStatus?.userRules?.joinToString("\n") ?: "")
    }

    var tgTokenInput by remember(telegramConfig) { mutableStateOf(telegramConfig?.token ?: "") }
    var tgChatIdInput by remember(telegramConfig) { mutableStateOf(telegramConfig?.adminChatId ?: "") }

    LaunchedEffect(Unit) {
        viewModel.loadSettings()
    }

    val categories = listOf("Semua", "Koneksi", "DNS Upstream", "Proteksi", "Game QoS", "Anti-Rebind", "ODoH", "Blokir Aplikasi", "Telegram", "Aturan Kustom")

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
                text = "Pusat Pengaturan Server",
                color = BrstTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Konfigurasi penuh server DNS SERVER BRST langsung dari ponsel",
                color = BrstTextSecondary,
                fontSize = 12.sp
            )
        }

        // --- Category Chip Bar ---
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    val selected = activeCategory == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selected) BrstPrimary else BrstSurface)
                            .border(1.dp, if (selected) BrstPrimary else BrstCardBorder, RoundedCornerShape(20.dp))
                            .clickable { activeCategory = cat }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = cat,
                            color = if (selected) BrstTextPrimary else BrstTextSecondary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // --- 1. KONEKSI & KREDENSIAL ---
        if (activeCategory == "Semua" || activeCategory == "Koneksi") {
            item {
                SettingsSectionCard(
                    title = "Koneksi Server",
                    icon = Icons.Default.Link,
                    iconColor = BrstPrimary
                ) {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("Server Host / IP & Port") },
                        placeholder = { Text("http://192.168.1.1:3000") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = outlinedColors()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = userInput,
                        onValueChange = { userInput = it },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = outlinedColors()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = passInput,
                        onValueChange = { passInput = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle",
                                    tint = BrstTextSecondary
                                )
                            }
                        },
                        colors = outlinedColors()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.saveServerSettings(urlInput, userInput, passInput) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrstPrimary)
                    ) {
                        Text("Simpan & Hubungkan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- 2. DNS & UPSTREAM RESOLVERS ---
        if (activeCategory == "Semua" || activeCategory == "DNS Upstream") {
            item {
                SettingsSectionCard(
                    title = "DNS & Upstream Resolvers",
                    icon = Icons.Default.Dns,
                    iconColor = BrstAccent
                ) {
                    Text("Server DNS Upstream (Satu per baris):", color = BrstTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = upstreamsInput,
                        onValueChange = { upstreamsInput = it },
                        placeholder = { Text("https://dns.quad9.net/dns-query\ntls://1.1.1.1") },
                        modifier = Modifier.fillMaxWidth().height(110.dp),
                        colors = outlinedColors()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Bootstrap DNS (Resolusi nama DoH/DoT):", color = BrstTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = bootstrapsInput,
                        onValueChange = { bootstrapsInput = it },
                        placeholder = { Text("9.9.9.9\n1.1.1.1") },
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        colors = outlinedColors()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Mode Upstream Selector
                    Text("Mode Query Upstream:", color = BrstTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("load_balance" to "Load Balance", "parallel" to "Paralel (Cepat)", "fastest_addr" to "IP Tercepat").forEach { (mode, label) ->
                            val isSel = upstreamModeInput == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) BrstAccent else BrstSurfaceVariant)
                                    .clickable { upstreamModeInput = mode }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSel) BrstBackground else BrstTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingToggleRow(
                        label = "DNSSEC (Validasi Kriptografi DNS)",
                        checked = dnssecInput,
                        onCheckedChange = { dnssecInput = it }
                    )

                    SettingToggleRow(
                        label = "EDNS Client Subnet (ECS Geo-Routing)",
                        checked = ecsInput,
                        onCheckedChange = { ecsInput = it }
                    )

                    SettingToggleRow(
                        label = "Optimistic Caching (Sajikan Cache Kadaluarsa)",
                        checked = optimisticInput,
                        onCheckedChange = { optimisticInput = it }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val upList = upstreamsInput.lines().map { it.trim() }.filter { it.isNotEmpty() }
                            val bootList = bootstrapsInput.lines().map { it.trim() }.filter { it.isNotEmpty() }
                            viewModel.updateDnsConfig(
                                upstreams = upList,
                                bootstraps = bootList,
                                upstreamMode = upstreamModeInput,
                                dnssec = dnssecInput,
                                ecs = ecsInput,
                                rateLimit = dnsConfig?.ratelimit ?: 20,
                                optimistic = optimisticInput
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrstAccent)
                    ) {
                        Text("Simpan Pengaturan DNS", color = BrstBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- 3. PROTEKSI & SAFE BROWSING ---
        if (activeCategory == "Semua" || activeCategory == "Proteksi") {
            item {
                SettingsSectionCard(
                    title = "Proteksi & Safe Search",
                    icon = Icons.Default.Shield,
                    iconColor = BrstSuccess
                ) {
                    SettingToggleRow(
                        label = "Blokir Iklan & Pelacak (Ad/Tracker Filter)",
                        checked = filteringStatus?.enabled ?: true,
                        onCheckedChange = { viewModel.updateFiltering(it, filteringStatus?.interval ?: 24) }
                    )

                    SettingToggleRow(
                        label = "SafeBrowsing (Perlindungan Malware & Phishing)",
                        checked = safeBrowsing,
                        onCheckedChange = { viewModel.toggleSafeBrowsing(it) }
                    )

                    SettingToggleRow(
                        label = "Parental Control (Blokir Konten Dewasa)",
                        checked = parental,
                        onCheckedChange = { viewModel.toggleParental(it) }
                    )

                    SettingToggleRow(
                        label = "SafeSearch (Paksa SafeSearch Google, YouTube, Bing)",
                        checked = safeSearch,
                        onCheckedChange = { viewModel.toggleSafeSearch(it) }
                    )
                }
            }
        }

        // --- 4. SMART GAME MODE QOS ---
        if (activeCategory == "Semua" || activeCategory == "Game QoS") {
            item {
                SettingsSectionCard(
                    title = "Smart Game Mode QoS",
                    icon = Icons.Default.Gamepad,
                    iconColor = BrstPurple
                ) {
                    Text(
                        text = "Optimasi latensi query DNS dan perutean prioritas untuk domain server game populer.",
                        color = BrstTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SettingToggleRow(
                        label = "Aktifkan Smart Game Mode QoS",
                        checked = gameMode?.enabled == true,
                        onCheckedChange = { viewModel.updateGameMode(it) }
                    )
                }
            }
        }

        // --- 5. ANTI-DNS REBINDING ---
        if (activeCategory == "Semua" || activeCategory == "Anti-Rebind") {
            item {
                SettingsSectionCard(
                    title = "Anti-DNS Rebinding Shield",
                    icon = Icons.Default.Security,
                    iconColor = BrstWarning
                ) {
                    Text(
                        text = "Mencegah serangan DNS Rebinding yang dapat mengekspos perangkat di jaringan lokal.",
                        color = BrstTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SettingToggleRow(
                        label = "Aktifkan Proteksi Anti-Rebind",
                        checked = rebindConfig?.enabled ?: true,
                        onCheckedChange = {
                            viewModel.updateRebindConfig(it, rebindConfig?.strictMode ?: false, rebindConfig?.whitelistedDomains ?: emptyList())
                        }
                    )
                    SettingToggleRow(
                        label = "Mode Ketat (Strict Mode)",
                        checked = rebindConfig?.strictMode ?: false,
                        onCheckedChange = {
                            viewModel.updateRebindConfig(rebindConfig?.enabled ?: true, it, rebindConfig?.whitelistedDomains ?: emptyList())
                        }
                    )
                }
            }
        }

        // --- 6. OBLIVIOUS DOH (ODOH) ---
        if (activeCategory == "Semua" || activeCategory == "ODoH") {
            item {
                SettingsSectionCard(
                    title = "Oblivious DoH (ODoH) Shield",
                    icon = Icons.Default.VpnLock,
                    iconColor = BrstAccent
                ) {
                    Text(
                        text = "Enkripsi dua lapis memisahkan IP server Anda dari target DNS resolver (Zero-Knowledge Privacy).",
                        color = BrstTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SettingToggleRow(
                        label = "Aktifkan ODoH Relay",
                        checked = odohConfig?.enabled == true,
                        onCheckedChange = {
                            viewModel.updateODoHConfig(it, odohConfig?.preset ?: "cloudflare", odohConfig?.relayUrl ?: "", odohConfig?.targetUrl ?: "")
                        }
                    )
                }
            }
        }

        // --- 7. BLOKIR APLIKASI (BLOCKED SERVICES) ---
        if (activeCategory == "Semua" || activeCategory == "Blokir Aplikasi") {
            item {
                SettingsSectionCard(
                    title = "Blokir Layanan & Aplikasi",
                    icon = Icons.Default.Block,
                    iconColor = BrstError
                ) {
                    Text(
                        text = "Blokir akses aplikasi & platform tertentu di seluruh jaringan dengan 1-ketuk:",
                        color = BrstTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val popularServices = listOf(
                        "tiktok" to "TikTok",
                        "youtube" to "YouTube",
                        "netflix" to "Netflix",
                        "instagram" to "Instagram",
                        "facebook" to "Facebook",
                        "steam" to "Steam",
                        "discord" to "Discord",
                        "roblox" to "Roblox",
                        "spotify" to "Spotify",
                        "twitch" to "Twitch",
                        "twitter" to "X (Twitter)",
                        "whatsapp" to "WhatsApp"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        popularServices.forEach { (id, name) ->
                            val isBlocked = blockedServices.contains(id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isBlocked) BrstError.copy(alpha = 0.15f) else BrstSurfaceVariant)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = name,
                                    color = if (isBlocked) BrstError else BrstTextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Switch(
                                    checked = isBlocked,
                                    onCheckedChange = { viewModel.toggleBlockedService(id, it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = BrstError,
                                        checkedTrackColor = BrstError.copy(alpha = 0.4f),
                                        uncheckedThumbColor = BrstTextMuted,
                                        uncheckedTrackColor = BrstSurface
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 8. TELEGRAM BOT CONTROL ---
        if (activeCategory == "Semua" || activeCategory == "Telegram") {
            item {
                SettingsSectionCard(
                    title = "Integrasi Bot Telegram",
                    icon = Icons.Default.Send,
                    iconColor = BrstPrimary
                ) {
                    SettingToggleRow(
                        label = "Aktifkan Bot Telegram",
                        checked = telegramConfig?.enabled == true,
                        onCheckedChange = {
                            viewModel.updateTelegramConfig(it, tgTokenInput, tgChatIdInput)
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = tgTokenInput,
                        onValueChange = { tgTokenInput = it },
                        label = { Text("Bot API Token") },
                        placeholder = { Text("123456:ABC-DEF...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = outlinedColors()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = tgChatIdInput,
                        onValueChange = { tgChatIdInput = it },
                        label = { Text("Admin Chat ID") },
                        placeholder = { Text("987654321") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = outlinedColors()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            viewModel.updateTelegramConfig(telegramConfig?.enabled == true, tgTokenInput, tgChatIdInput)
                        },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrstPrimary)
                    ) {
                        Text("Simpan Konfigurasi Telegram", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- 9. ATURAN KUSTOM ---
        if (activeCategory == "Semua" || activeCategory == "Aturan Kustom") {
            item {
                SettingsSectionCard(
                    title = "Aturan Kustom (User Filter Rules)",
                    icon = Icons.Default.Code,
                    iconColor = BrstAccent
                ) {
                    Text(
                        text = "Tambahkan aturan pemblokiran kustom format AdGuard (contoh: ||iklan.com^ atau @@||domain.com^ untuk whitelist). Satu aturan per baris.",
                        color = BrstTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = userRulesInput,
                        onValueChange = { userRulesInput = it },
                        placeholder = { Text("||example-ad.com^\n@@||allowed-domain.com^") },
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        colors = outlinedColors()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val rules = userRulesInput.lines().map { it.trim() }.filter { it.isNotEmpty() }
                            viewModel.updateUserRules(rules)
                        },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrstAccent)
                    ) {
                        Text("Simpan Aturan Kustom", color = BrstBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BrstCardBorder, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = BrstSurface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    color = BrstTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
fun SettingToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = BrstTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = BrstTextPrimary,
                checkedTrackColor = BrstPrimary,
                uncheckedThumbColor = BrstTextMuted,
                uncheckedTrackColor = BrstSurfaceVariant
            )
        )
    }
}

@Composable
fun outlinedColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BrstPrimary,
    unfocusedBorderColor = BrstCardBorder,
    focusedTextColor = BrstTextPrimary,
    unfocusedTextColor = BrstTextPrimary
)
