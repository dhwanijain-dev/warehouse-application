package com.example.warehousescan.feature.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.warehousescan.core.designsystem.LoadingButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onBack: () -> Unit,
    onNavigateUserManagement: () -> Unit,
    isAdminUser: Boolean = true,
    expectedPin: String = "1234",
    viewModel: AdminViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var pinEntered by remember { mutableStateOf("") }
    var pinVerified by remember { mutableStateOf(isAdminUser) }

    if (!pinVerified) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Admin PIN Required") },
                    navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Enter Admin PIN", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = pinEntered,
                    onValueChange = { pinEntered = it },
                    label = { Text("PIN") },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    if (pinEntered == expectedPin) {
                        pinVerified = true
                    }
                }) {
                    Text("Verify PIN")
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Settings & Control") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                },
                actions = {
                    TextButton(onClick = onNavigateUserManagement) {
                        Text("Users")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is AdminUiState.Content -> {
                var baseUrl by remember { mutableStateOf(state.baseUrl) }
                var authProvider by remember { mutableStateOf(state.authProvider) }
                var emailProvider by remember { mutableStateOf(state.emailProvider) }
                var timeoutMinutes by remember { mutableStateOf(state.timeoutMinutes.toString()) }
                var duplicatePolicy by remember { mutableStateOf(state.duplicatePolicy) }
                var includeLocationLines by remember { mutableStateOf(state.includeLocationLines) }
                var adminPin by remember { mutableStateOf(state.adminPin) }
                var smtpHost by remember { mutableStateOf(state.smtpHost) }
                var smtpPort by remember { mutableStateOf(state.smtpPort.toString()) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Environment: ${state.environment}", style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = { Text("Base API URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = authProvider,
                        onValueChange = { authProvider = it },
                        label = { Text("Auth Provider (local, api, fake)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = emailProvider,
                        onValueChange = { emailProvider = it },
                        label = { Text("Email Provider (intent, saveonly, smtp, api, fake)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = timeoutMinutes,
                        onValueChange = { timeoutMinutes = it },
                        label = { Text("Session Timeout (minutes, 0 = disabled)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = duplicatePolicy,
                        onValueChange = { duplicatePolicy = it },
                        label = { Text("Duplicate Scan Policy (reject, warn, allow)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = includeLocationLines,
                            onCheckedChange = { includeLocationLines = it }
                        )
                        Text("Include Location Lines in TXT File")
                    }

                    OutlinedTextField(
                        value = adminPin,
                        onValueChange = { adminPin = it },
                        label = { Text("Admin PIN") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = smtpHost,
                        onValueChange = { smtpHost = it },
                        label = { Text("SMTP Host") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = smtpPort,
                        onValueChange = { smtpPort = it },
                        label = { Text("SMTP Port") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LoadingButton(
                            text = "Test Connection",
                            isLoading = state.isTesting,
                            onClick = {
                                viewModel.updateSettings(baseUrl, authProvider, emailProvider, timeoutMinutes.toIntOrNull() ?: 0, duplicatePolicy, includeLocationLines, adminPin, smtpHost, smtpPort.toIntOrNull() ?: 587)
                                viewModel.testConnection()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                viewModel.updateSettings(baseUrl, authProvider, emailProvider, timeoutMinutes.toIntOrNull() ?: 0, duplicatePolicy, includeLocationLines, adminPin, smtpHost, smtpPort.toIntOrNull() ?: 587)
                                viewModel.applySettings(onBack)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        ) {
                            Text("Apply")
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.resetSettings() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text("Reset to Default")
                    }

                    if (!state.testResult.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.testResult,
                            color = if (state.testResult.contains("Successful", ignoreCase = true))
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}
