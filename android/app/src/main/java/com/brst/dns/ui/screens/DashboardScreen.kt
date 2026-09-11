package com.brst.dns.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brst.dns.R
import com.brst.dns.ui.theme.*
import com.brst.dns.ui.viewmodel.MainViewModel

@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val status by viewModel.serverStatus.collectAsState()
    val stats by viewModel.serverStats.collectAsState()
    val gameMode by viewModel.gameModeStatus.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BrstBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Server Protection Hero Card ---
        item {
            val isProtected = status?.protectionEnabled == true
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isProtected) BrstPrimary.copy(alpha = 0.5f) else BrstError.copy(alpha = 0.5f),
                        RoundedCornerShape(20.dp)
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = BrstSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(if (isProtected) BrstSuccess else BrstError)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isProtected) "PROTEKSI AKTIF" else "PROTEKSI DIJEDA",
                                color = if (isProtected) BrstSuccess else BrstError,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Switch(
                            checked = isProtected,
                            onCheckedChange = { viewModel.toggleProtection(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BrstTextPrimary,
                                checkedTrackColor = BrstPrimary,
                                uncheckedThumbColor = BrstTextMuted,
                                uncheckedTrackColor = BrstSurfaceVariant
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "DNS SERVER BRST",
                        color = BrstTextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Versi: ${status?.version ?: "v0.108.x"} • Port DNS: ${status?.dnsPort ?: 53}",
                        color = BrstTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // --- 2. Live Metrics 2x2 Grid ---
        item {
            Text(
                text = "Statistik Real-time",
                color = BrstTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Total Query",
                        value = "${stats?.numDnsQueries ?: 0}",
                        subText = "24 Jam Terakhir",
                        color = BrstPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Diblokir",
                        value = "${stats?.numBlockedFiltering ?: 0}",
                        subText = "Ancaman & Iklan",
                        color = BrstAccent,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Rasio Blokir",
                        value = String.format("%.1f%%", stats?.blockPercentage ?: 0.0),
                        subText = "Tingkat Efektivitas",
                        color = BrstPurple,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Rata-rata Respon",
                        value = String.format("%.1f ms", (stats?.avgProcessingTime ?: 0.0) * 1000),
                        subText = "Latensi Resolver",
                        color = BrstSuccess,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // --- 3. Quick Action Controls ---
        item {
            Text(
                text = "Kendali Cepat & Optimasi",
                color = BrstTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Smart Game Mode QoS Button
                val isGameActive = gameMode?.enabled == true
                Button(
                    onClick = { viewModel.toggleGameMode() },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .border(
                            1.dp,
                            if (isGameActive) BrstAccent else BrstCardBorder,
                            RoundedCornerShape(14.dp)
                        ),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isGameActive) BrstSurfaceVariant else BrstSurface
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_gamepad),
                            contentDescription = "Game Mode",
                            tint = if (isGameActive) BrstAccent else BrstTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isGameActive) "Game QoS: ON" else "Game QoS: OFF",
                            color = if (isGameActive) BrstAccent else BrstTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }

                // RAM & Cache Maintenance Button
                Button(
                    onClick = { viewModel.runMaintenance() },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .border(1.dp, BrstCardBorder, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrstSurface)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_bolt),
                            contentDescription = "Maintenance",
                            tint = BrstWarning,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "RAM Cleaner",
                            color = BrstTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // --- 4. Top Blocked Domains Preview ---
        item {
            val topBlocked = stats?.topBlockedDomains ?: emptyList()
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BrstCardBorder, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = BrstSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Domain Paling Sering Diblokir",
                            color = BrstTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        IconButton(onClick = { viewModel.refreshAll() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = BrstPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (topBlocked.isEmpty()) {
                        Text(
                            text = "Belum ada domain yang diblokir dalam periode ini.",
                            color = BrstTextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        topBlocked.take(5).forEach { item ->
                            item.forEach { (domain, count) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = domain,
                                        color = BrstTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Badge(containerColor = BrstError.copy(alpha = 0.2f)) {
                                        Text(
                                            text = "$count blokir",
                                            color = BrstError,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subText: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.border(1.dp, BrstCardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrstSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, color = BrstTextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = color,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subText, color = BrstTextMuted, fontSize = 10.sp)
        }
    }
}
