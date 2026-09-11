package com.brst.dns.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

@Composable
fun DotLogsScreen(viewModel: DotViewModel) {
    val queries by viewModel.recentQueries.collectAsState()
    val totalCount by viewModel.queryCount.collectAsState()
    val blockedCount by viewModel.blockedCount.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val protocol by viewModel.protocol.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf(0) } // 0: All, 1: Blocked, 2: Encrypted

    val filteredQueries = remember(queries, searchQuery, filterType) {
        queries.filter { item ->
            val matchesSearch = searchQuery.isBlank() || item.domain.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (filterType) {
                1 -> item.blocked
                2 -> !item.blocked
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

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
                        text = "Real-time lalu lintas DNS & pencegatan iklan",
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
                        text = "$totalCount Total",
                        color = BrstAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // --- Status & Blocked Banner ---
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
                        Text(text = "Iklan Dicegat", color = BrstTextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$blockedCount",
                            color = BrstPink,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(BrstCardBorder))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Buffer Log", color = BrstTextSecondary, fontSize = 11.sp)
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

        // --- Search Bar ---
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari domain (misal: google, tiktok, ads)...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = BrstTextMuted)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = BrstTextMuted)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrstAccent,
                    unfocusedBorderColor = BrstCardBorder,
                    focusedTextColor = BrstTextPrimary,
                    unfocusedTextColor = BrstTextPrimary,
                    cursorColor = BrstAccent
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // --- Filter Pills ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Semua", "🚫 Diblokir", "🛡️ Terenkripsi").forEachIndexed { index, label ->
                    val isSel = filterType == index
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSel) BrstPrimary else BrstSurfaceVariant)
                            .clickable { filterType = index }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSel) BrstTextPrimary else BrstTextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // --- Query List / Empty State ---
        if (filteredQueries.isEmpty()) {
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
                            imageVector = Icons.Default.Security,
                            contentDescription = "Empty",
                            tint = BrstTextMuted,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Domain Tidak Ditemukan" else "Belum Ada Query Tercatat",
                            color = BrstTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Coba kata kunci pencarian domain lainnya." else "Aktifkan Shield dan lakukan browsing atau buka aplikasi untuk melihat log query.",
                            color = BrstTextSecondary,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredQueries, key = { it.id }) { item ->
                QueryLogCard(item = item)
            }
        }
    }
}

@Composable
private fun QueryLogCard(item: LocalQueryItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (item.blocked) BrstPink.copy(alpha = 0.4f) else BrstCardBorder,
                RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.blocked) BrstSurfaceVariant.copy(alpha = 0.9f) else BrstSurface
        )
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
                    color = if (item.blocked) BrstPink else BrstTextPrimary,
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
                        text = item.timestamp,
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
                        color = if (item.blocked) BrstPink else BrstAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Badge Status / Latency
            if (item.blocked) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(BrstPink.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "DIBLOKIR",
                        color = BrstPink,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            } else {
                val latencyColor = when {
                    !item.success -> BrstError
                    item.latencyMs < 50 -> BrstSuccess
                    item.latencyMs < 150 -> BrstWarning
                    else -> BrstError
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
}
