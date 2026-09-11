package com.brst.dns.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brst.dns.R
import com.brst.dns.doh.DohVpnService
import com.brst.dns.ui.theme.*
import com.brst.dns.ui.viewmodel.MainViewModel

@Composable
fun DohVpnScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val isRunning by viewModel.isDohRunning.collectAsState()
    val queryCount by viewModel.dohQueryCount.collectAsState()
    val dohUrl by viewModel.dohUrl.collectAsState()
    val dohClientId by viewModel.dohClientId.collectAsState()

    var customDohUrl by remember { mutableStateOf(dohUrl) }
    var clientId by remember { mutableStateOf(dohClientId) }

    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startDohService(context)
        }
    }

    // Pulse animation when active
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRunning) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BrstBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // --- 1. Header ---
        item {
            Text(
                text = "Mode DoH Android",
                color = BrstTextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Enkripsi DNS-over-HTTPS (RFC 8484) Wire-Format",
                color = BrstTextSecondary,
                fontSize = 12.sp
            )
        }

        // --- 2. Big Animated Connect Button ---
        item {
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = if (isRunning) {
                                listOf(BrstAccent.copy(alpha = 0.3f), Color.Transparent)
                            } else {
                                listOf(BrstPrimary.copy(alpha = 0.1f), Color.Transparent)
                            }
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(
                            if (isRunning) {
                                Brush.linearGradient(listOf(BrstPrimary, BrstAccent))
                            } else {
                                Brush.linearGradient(listOf(BrstSurfaceVariant, BrstSurface))
                            }
                        )
                        .border(
                            2.dp,
                            if (isRunning) BrstAccent else BrstCardBorder,
                            CircleShape
                        )
                        .clickable {
                            if (isRunning) {
                                stopDohService(context)
                            } else {
                                val prepareIntent = VpnService.prepare(context)
                                if (prepareIntent != null) {
                                    vpnLauncher.launch(prepareIntent)
                                } else {
                                    startDohService(context)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_vpn),
                            contentDescription = "DoH Toggle",
                            tint = if (isRunning) BrstBackground else BrstAccent,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isRunning) "TERHUBUNG" else "AKTIFKAN",
                            color = if (isRunning) BrstBackground else BrstTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // --- 3. Live VPN Metrics ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BrstCardBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BrstSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Status Shield", color = BrstTextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isRunning) "Aktif (DoH)" else "Nonaktif",
                            color = if (isRunning) BrstSuccess else BrstTextMuted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(30.dp)
                            .background(BrstCardBorder)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Query Terenkripsi", color = BrstTextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$queryCount",
                            color = BrstAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // --- 4. DoH Configuration & Client ID Tag ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BrstCardBorder, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = BrstSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Konfigurasi Endpoint DoH",
                        color = BrstTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = customDohUrl,
                        onValueChange = { customDohUrl = it },
                        label = { Text("DoH URL Resolver") },
                        placeholder = { Text("https://example.com/dns-query") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrstPrimary,
                            unfocusedBorderColor = BrstCardBorder,
                            focusedTextColor = BrstTextPrimary,
                            unfocusedTextColor = BrstTextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = clientId,
                        onValueChange = { clientId = it },
                        label = { Text("Nama Client ID (Tag Perangkat)") },
                        placeholder = { Text("Contoh: Android-Pixel") },
                        supportingText = {
                            Text(
                                "Query akan dikirim ke /dns-query/{client_id} agar terdata rapi di server BRST",
                                color = BrstTextMuted,
                                fontSize = 10.sp
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrstPrimary,
                            unfocusedBorderColor = BrstCardBorder,
                            focusedTextColor = BrstTextPrimary,
                            unfocusedTextColor = BrstTextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.saveDohSettings(customDohUrl, clientId) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrstPrimary)
                    ) {
                        Text("Simpan Konfigurasi DoH", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- 5. Preset DoH Resolvers ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BrstCardBorder, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = BrstSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Preset Resolver Cepat",
                        color = BrstTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val presets = listOf(
                        "Cloudflare Anycast" to "https://cloudflare-dns.com/dns-query",
                        "Quad9 Swiss Shield" to "https://dns.quad9.net/dns-query",
                        "AdGuard Default" to "https://dns.adguard-dns.com/dns-query",
                        "Google Public DoH" to "https://dns.google/dns-query"
                    )

                    presets.forEach { (name, url) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    customDohUrl = url
                                    viewModel.saveDohSettings(url, clientId)
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = name, color = BrstTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(text = url, color = BrstTextMuted, fontSize = 11.sp)
                            }
                            if (customDohUrl == url) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = BrstAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun startDohService(context: Context) {
    val intent = Intent(context, DohVpnService::class.java).apply {
        action = DohVpnService.ACTION_START
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

private fun stopDohService(context: Context) {
    val intent = Intent(context, DohVpnService::class.java).apply {
        action = DohVpnService.ACTION_STOP
    }
    context.startService(intent)
}
