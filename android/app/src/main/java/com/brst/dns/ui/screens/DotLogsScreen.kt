package com.brst.dns.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brst.dns.data.model.LocalQueryItem
import com.brst.dns.ui.theme.*
import com.brst.dns.ui.viewmodel.DotViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DotLogsScreen(viewModel: DotViewModel) {
    val queries by viewModel.recentQueries.collectAsState()
    val totalCount by viewModel.queryCount.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val protocol by viewModel.protocol.collectAsState()

    val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BrstBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- Header ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Log Aktivitas Query",
                        color = BrstTextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Real-time lalu lintas DNS terenkripsi perangkat",
                        color = BrstTextSecondary,
                        fontSize = 12.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(BrstSurfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "$totalCount Query",
                        color = BrstAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // --- Status Banner ---
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
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Status Shield", color = BrstTextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isRunning) "AKTIF ($protocol)" else "NONAKTIF",
                            color = if (isRunning) BrstSuccess else BrstTextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(BrstCardBorder))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Terekam di Buffer", color = BrstTextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${queries.size} item",
                            color = BrstTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // --- Query List / Empty State ---
        if (queries.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BrstCardBorder, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BrstSurface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ListAlt,
                            contentDescription = "Empty",
                            tint = BrstTextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Belum Ada Query Tercatat",
                            color = BrstTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Aktifkan DoT Shield dan lakukan browsing atau buka aplikasi untuk melihat rekaman query lokal di sini.",
                            color = BrstTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(queries, key = { it.id }) { item ->
                QueryLogCard(item = item, timeFormatter = timeFormatter)
            }
        }
    }
}

@Composable
private fun QueryLogCard(item: LocalQueryItem, timeFormatter: SimpleDateFormat) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BrstCardBorder, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BrstSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.domain,
                    color = BrstTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = timeFormatter.format(Date(item.timestamp)),
                        color = BrstTextMuted,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "•",
                        color = BrstTextMuted,
                        fontSize = 10.sp
                    )
                    Text(
                        text = item.protocol,
                        color = BrstAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Latency Badge
            val latencyColor = when {
                !item.success -> BrstDanger
                item.latencyMs < 50 -> BrstSuccess
                item.latencyMs < 150 -> BrstWarning
                else -> BrstDanger
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(latencyColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (item.success) "${item.latencyMs} ms" else "ERR",
                    color = latencyColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
