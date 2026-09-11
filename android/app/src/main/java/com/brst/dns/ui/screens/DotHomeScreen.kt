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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
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
import com.brst.dns.ui.viewmodel.DotViewModel

@Composable
fun DotHomeScreen(viewModel: DotViewModel) {
    val context = LocalContext.current
    val isRunning by viewModel.isRunning.collectAsState()
    val queryCount by viewModel.queryCount.collectAsState()
    val blockedCount by viewModel.blockedCount.collectAsState()
    val dotHost by viewModel.dotHost.collectAsState()
    val dotPort by viewModel.dotPort.collectAsState()
    val dohUrl by viewModel.dohUrl.collectAsState()
    val protocol by viewModel.protocol.collectAsState()
    val isBlocklistEnabled by viewModel.isBlocklistEnabled.collectAsState()
    val ruleCount by viewModel.blocklistRuleCount.collectAsState()

    val isDoT = protocol.equals("DoT", ignoreCase = true)

    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startVpnService(context)
        }
    }

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
        contentPadding = PaddingValues(top = 20.dp, bottom = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Header ---
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "BRST DNS Shield",
                    color = BrstTextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Enkripsi DoT / DoH + Pemblokir Iklan & Pelacak Lokal",
                    color = BrstTextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // --- 2. Big Animated Connect Button ---
        item {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = if (isRunning) {
                                listOf(BrstAccent.copy(alpha = 0.35f), Color.Transparent)
                            } else {
                                listOf(BrstPrimary.copy(alpha = 0.12f), Color.Transparent)
                            }
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
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
                                stopVpnService(context)
                            } else {
                                val prepareIntent = VpnService.prepare(context)
                                if (prepareIntent != null) {
                                    vpnLauncher.launch(prepareIntent)
                                } else {
                                    startVpnService(context)
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
                            painter = painterResource(id = R.drawable.ic_shield),
                            contentDescription = "Shield Toggle",
                            tint = if (isRunning) BrstBackground else BrstAccent,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isRunning) "TERLINDUNGI" else "HUBUNGKAN",
                            color = if (isRunning) BrstBackground else BrstTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        // --- 3. Live Stats & Target Resolver ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BrstCardBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BrstSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isRunning) BrstSuccess else BrstTextMuted)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isRunning) "Status: Aktif ($protocol)" else "Status: Nonaktif",
                                color = if (isRunning) BrstSuccess else BrstTextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        // Protocol switcher
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(BrstSurfaceVariant)
                                .padding(2.dp)
                        ) {
                            listOf("DoT", "DoH").forEach { proto ->
                                val isSel = protocol.equals(proto, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) BrstPrimary else Color.Transparent)
                                        .clickable { viewModel.setProtocol(proto) }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = proto,
                                        color = if (isSel) BrstTextPrimary else BrstTextMuted,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "Total Query", color = BrstTextSecondary, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$queryCount",
                                color = BrstTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Box(modifier = Modifier.width(1.dp).height(32.dp).background(BrstCardBorder))

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "Iklan Diblokir", color = BrstTextSecondary, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$blockedCount",
                                color = BrstPink,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Box(modifier = Modifier.width(1.dp).height(32.dp).background(BrstCardBorder))

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "Target Resolver", color = BrstTextSecondary, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isDoT) "$dotHost:$dotPort" else "DoH HTTPS",
                                color = BrstAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // --- 4. Local AdBlock Status Card ---
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(BrstPink.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = "AdBlock",
                                tint = BrstPink,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Pemblokir Iklan Lokal",
                                color = BrstTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isBlocklistEnabled) "$ruleCount aturan aktif (0 ms sinkhole)" else "Nonaktif",
                                color = if (isBlocklistEnabled) BrstSuccess else BrstTextMuted,
                                fontSize = 11.sp
                            )
                        }
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
            }
        }

        // --- 5. Security Highlights ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BrstCardBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BrstSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isDoT) Icons.Default.Lock else Icons.Default.Http,
                            contentDescription = "Security",
                            tint = BrstAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isDoT) "DoT Enkripsi TLS 1.3 (Port 853)" else "DoH Enkripsi HTTPS/2 (Port 443)",
                            color = BrstTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Semua query DNS perangkat Anda dienkripsi secara native tanpa log oleh pihak ketiga. Iklan, pelacak, dan spyware otomatis dicegat di perangkat (sinkhole 0.0.0.0) sehingga menghemat kuota dan baterai.",
                        color = BrstTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

private fun startVpnService(context: Context) {
    val intent = Intent(context, DohVpnService::class.java).apply {
        action = DohVpnService.ACTION_START
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

private fun stopVpnService(context: Context) {
    val intent = Intent(context, DohVpnService::class.java).apply {
        action = DohVpnService.ACTION_STOP
    }
    context.startService(intent)
}
