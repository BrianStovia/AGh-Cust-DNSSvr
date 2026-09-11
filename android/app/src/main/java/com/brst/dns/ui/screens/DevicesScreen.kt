package com.brst.dns.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brst.dns.data.api.DetectedDevice
import com.brst.dns.ui.theme.*
import com.brst.dns.ui.viewmodel.MainViewModel

@Composable
fun DevicesScreen(viewModel: MainViewModel) {
    val detectedDevices by viewModel.detectedDevices.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshDevices()
    }

    val devices = detectedDevices?.devices ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrstBackground)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Penemuan Perangkat LAN",
                    color = BrstTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Total ${devices.size} perangkat terdeteksi via ARP / DHCP",
                    color = BrstTextSecondary,
                    fontSize = 12.sp
                )
            }

            IconButton(
                onClick = { viewModel.refreshDevices() },
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BrstSurface)
                    .border(1.dp, BrstCardBorder, RoundedCornerShape(12.dp))
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = BrstPrimary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (devices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Devices,
                        contentDescription = "Empty",
                        tint = BrstTextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Belum ada data perangkat terdeteksi di LAN",
                        color = BrstTextMuted,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(devices) { device ->
                    DeviceCard(device = device)
                }
            }
        }
    }
}

@Composable
fun DeviceCard(device: DetectedDevice) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BrstCardBorder, RoundedCornerShape(14.dp)),
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
                    text = if (device.hostname.isNotBlank()) device.hostname else device.ip,
                    color = BrstTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Badge(containerColor = BrstPrimary.copy(alpha = 0.2f)) {
                    Text(
                        text = device.deviceType.uppercase(),
                        color = BrstPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "IP: ${device.ip} • MAC: ${device.mac.ifEmpty { "N/A" }}",
                color = BrstTextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Vendor: ${device.vendor}",
                color = BrstAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
