package com.brst.dns.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brst.dns.data.api.QueryLogItem
import com.brst.dns.ui.theme.*
import com.brst.dns.ui.viewmodel.MainViewModel

@Composable
fun QueryLogScreen(viewModel: MainViewModel) {
    val queryLogs by viewModel.queryLogs.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedItem by remember { mutableStateOf<QueryLogItem?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refreshLogs()
    }

    val allLogs = queryLogs?.data ?: emptyList()
    val filteredLogs = remember(allLogs, searchQuery) {
        if (searchQuery.isBlank()) {
            allLogs
        } else {
            allLogs.filter {
                it.question?.name?.contains(searchQuery, ignoreCase = true) == true ||
                it.client.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrstBackground)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar & Refresh
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari domain atau IP klien...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = BrstTextSecondary)
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrstPrimary,
                    unfocusedBorderColor = BrstCardBorder,
                    focusedTextColor = BrstTextPrimary,
                    unfocusedTextColor = BrstTextPrimary
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { viewModel.refreshLogs() },
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(BrstSurface)
                    .border(1.dp, BrstCardBorder, RoundedCornerShape(14.dp))
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = BrstPrimary)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Menampilkan ${filteredLogs.size} query terakhir",
            color = BrstTextSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isNotBlank()) "Tidak ada query yang cocok dengan pencarian" else "Belum ada log query",
                    color = BrstTextMuted,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredLogs) { item ->
                    QueryLogCard(
                        item = item,
                        onClick = { selectedItem = item }
                    )
                }
            }
        }
    }

    // Action Dialog for unblocking / blocking domain
    selectedItem?.let { item ->
        val domain = item.question?.name ?: ""
        AlertDialog(
            onDismissRequest = { selectedItem = null },
            containerColor = BrstSurface,
            title = {
                Text(
                    text = "Kelola Domain",
                    color = BrstTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = domain,
                        color = BrstAccent,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Klien: ${item.client} (${item.clientProto}) • Status: ${item.reason}",
                        color = BrstTextSecondary,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.unblockDomain(domain)
                        selectedItem = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrstSuccess)
                ) {
                    Text("Buka Blokir (Whitelist)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        viewModel.blockDomain(domain)
                        selectedItem = null
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BrstError)
                ) {
                    Text("Blokir Domain", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun QueryLogCard(
    item: QueryLogItem,
    onClick: () -> Unit
) {
    val isBlocked = item.isBlocked
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isBlocked) BrstError.copy(alpha = 0.3f) else BrstCardBorder,
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BrstSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.question?.name ?: "Unknown domain",
                    color = BrstTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Badge(
                    containerColor = if (isBlocked) BrstError.copy(alpha = 0.2f) else BrstSuccess.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = if (isBlocked) "BLOCKED" else "PROCESSED",
                        color = if (isBlocked) BrstError else BrstSuccess,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${item.client} (${item.question?.type ?: "A"})",
                    color = BrstTextSecondary,
                    fontSize = 11.sp
                )
                Text(
                    text = "${item.elapsedMs} ms",
                    color = BrstTextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}
