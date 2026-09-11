package com.brst.dns.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brst.dns.data.preferences.AppPreferences
import com.brst.dns.ui.theme.*
import com.brst.dns.ui.viewmodel.MainViewModel

@Composable
fun ServerConfigScreen(viewModel: MainViewModel) {
    val serverUrl by viewModel.serverUrl.collectAsState()
    val authType by viewModel.authType.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()

    var selectedAuthMode by remember(authType) { mutableStateOf(authType) }
    var urlInput by remember(serverUrl) { mutableStateOf(serverUrl) }
    var apiKeyInput by remember(apiKey) { mutableStateOf(apiKey) }
    var userInput by remember(username) { mutableStateOf(username) }
    var passInput by remember(password) { mutableStateOf(password) }
    var showApiKey by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BrstBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Header ---
        item {
            Text(
                text = "Pengaturan Akses Server",
                color = BrstTextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Autentikasi remote controller DNS SERVER BRST",
                color = BrstTextSecondary,
                fontSize = 12.sp
            )
        }

        // --- 2. Auth Mode Tab Switcher ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BrstCardBorder, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BrstSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    // Tab 1: API Key Mode
                    val isApiKeyMode = selectedAuthMode == AppPreferences.AUTH_TYPE_API_KEY
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isApiKeyMode) BrstPrimary else BrstSurface)
                            .clickable { selectedAuthMode = AppPreferences.AUTH_TYPE_API_KEY }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "API Key",
                                tint = if (isApiKeyMode) BrstTextPrimary else BrstTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "API Key (Direkomendasikan)",
                                color = if (isApiKeyMode) BrstTextPrimary else BrstTextSecondary,
                                fontWeight = if (isApiKeyMode) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Tab 2: Username & Password Mode
                    val isBasicMode = selectedAuthMode == AppPreferences.AUTH_TYPE_BASIC
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isBasicMode) BrstPrimary else BrstSurface)
                            .clickable { selectedAuthMode = AppPreferences.AUTH_TYPE_BASIC }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Username",
                                tint = if (isBasicMode) BrstTextPrimary else BrstTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "User & Password",
                                color = if (isBasicMode) BrstTextPrimary else BrstTextSecondary,
                                fontWeight = if (isBasicMode) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // --- 3. Configuration Form ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BrstCardBorder, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = BrstSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("Server Host / IP & Port") },
                        placeholder = { Text("http://192.168.1.1:3000") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrstPrimary,
                            unfocusedBorderColor = BrstCardBorder,
                            focusedTextColor = BrstTextPrimary,
                            unfocusedTextColor = BrstTextPrimary
                        )
                    )

                    if (selectedAuthMode == AppPreferences.AUTH_TYPE_API_KEY) {
                        // --- API Key Input ---
                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            label = { Text("Server API Key / Access Token") },
                            placeholder = { Text("Masukkan API Key atau Password") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showApiKey = !showApiKey }) {
                                    Icon(
                                        imageVector = if (showApiKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle Visibility",
                                        tint = BrstTextSecondary
                                    )
                                }
                            },
                            supportingText = {
                                Text(
                                    "Aplikasi akan mengirimkan header X-API-Key dan Bearer Token secara otomatis",
                                    color = BrstAccent,
                                    fontSize = 11.sp
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrstPrimary,
                                unfocusedBorderColor = BrstCardBorder,
                                focusedTextColor = BrstTextPrimary,
                                unfocusedTextColor = BrstTextPrimary
                            )
                        )
                    } else {
                        // --- Username & Password Inputs ---
                        OutlinedTextField(
                            value = userInput,
                            onValueChange = { userInput = it },
                            label = { Text("Username") },
                            placeholder = { Text("admin") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrstPrimary,
                                unfocusedBorderColor = BrstCardBorder,
                                focusedTextColor = BrstTextPrimary,
                                unfocusedTextColor = BrstTextPrimary
                            )
                        )

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
                                        contentDescription = "Toggle Password",
                                        tint = BrstTextSecondary
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrstPrimary,
                                unfocusedBorderColor = BrstCardBorder,
                                focusedTextColor = BrstTextPrimary,
                                unfocusedTextColor = BrstTextPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            if (selectedAuthMode == AppPreferences.AUTH_TYPE_API_KEY) {
                                viewModel.saveApiKeySettings(urlInput, apiKeyInput)
                            } else {
                                viewModel.saveBasicAuthSettings(urlInput, userInput, passInput)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrstPrimary)
                    ) {
                        Text(
                            text = if (selectedAuthMode == AppPreferences.AUTH_TYPE_API_KEY) "Simpan & Masuk via API Key" else "Simpan & Masuk via Basic Auth",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // --- 4. Info Card ---
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
                        text = "Keunggulan Mode API Key",
                        color = BrstTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Lebih aman dan cepat: Anda cukup memasukkan satu token kunci API tanpa harus menyimpan username dan password teks polos.\n" +
                                "• Mendukung token session hex, password admin, maupun Bearer token.\n" +
                                "• Semua request ke REST API dilindungi secara otomatis melalui header X-API-Key dan Authorization: Bearer.",
                        color = BrstTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
