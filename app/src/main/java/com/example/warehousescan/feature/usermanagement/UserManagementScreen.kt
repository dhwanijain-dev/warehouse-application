package com.example.warehousescan.feature.usermanagement

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.warehousescan.core.storage.UserEntity
import com.example.warehousescan.core.designsystem.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    onBack: () -> Unit,
    viewModel: UserManagementViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showImportDialog by remember { mutableStateOf(false) }
    var replaceAllOnImport by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importUsers(uri, replaceAllOnImport)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Management") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.startAddUser() }) {
                        Text("Add User")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is UserManagementUiState.Content -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                ) {
                    if (!state.errorMessage.isNullOrBlank()) {
                        ErrorBanner(message = state.errorMessage!!) {}
                    }
                    if (!state.successMessage.isNullOrBlank()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(text = state.successMessage!!, modifier = Modifier.padding(12.dp))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                replaceAllOnImport = false
                                showImportDialog = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Import Users")
                        }
                        OutlinedButton(
                            onClick = {
                                val jsonStr = viewModel.exportUsersJson()
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/json"
                                    putExtra(Intent.EXTRA_SUBJECT, "Users Export")
                                    putExtra(Intent.EXTRA_TEXT, jsonStr)
                                }
                                context.startActivity(Intent.createChooser(intent, "Export Users"))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Export Users")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(state.users) { user ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${user.displayName} (${user.userId})", style = MaterialTheme.typography.titleMedium)
                                        Text("Email: ${user.email} | Role: ${user.role} | Active: ${user.active}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Row {
                                        TextButton(onClick = { viewModel.startEditUser(user) }) {
                                            Text("Edit")
                                        }
                                        TextButton(onClick = { viewModel.toggleActive(user) }) {
                                            Text(if (user.active) "Deactivate" else "Activate")
                                        }
                                        TextButton(onClick = { viewModel.deleteUser(user) }) {
                                            Text("Delete", color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (state.isAddingOrEditing && state.editingUser != null) {
                        UserEditDialog(
                            user = state.editingUser!!,
                            onSave = { uid, pwd, name, email, role, active ->
                                viewModel.saveUser(uid, pwd, name, email, role, active)
                            },
                            onDismiss = { viewModel.cancelEdit() }
                        )
                    }
                }
            }
        }

        if (showImportDialog) {
            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                title = { Text("Import Users Mode") },
                text = { Text("Choose how to import users from JSON:") },
                confirmButton = {
                    Button(onClick = {
                        replaceAllOnImport = true
                        showImportDialog = false
                        filePickerLauncher.launch("application/json")
                    }) {
                        Text("Replace All")
                    }
                },
                dismissButton = {
                    Button(onClick = {
                        replaceAllOnImport = false
                        showImportDialog = false
                        filePickerLauncher.launch("application/json")
                    }) {
                        Text("Merge")
                    }
                }
            )
        }
    }
}

@Composable
fun UserEditDialog(
    user: UserEntity,
    onSave: (String, String, String, String, String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var userId by remember { mutableStateOf(user.userId) }
    var password by remember { mutableStateOf(user.password) }
    var displayName by remember { mutableStateOf(user.displayName) }
    var email by remember { mutableStateOf(user.email) }
    var role by remember { mutableStateOf(user.role) }
    var active by remember { mutableStateOf(user.active) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (user.id == 0L) "Add User" else "Edit User") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = userId, onValueChange = { userId = it }, label = { Text("User ID") }, singleLine = true)
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, singleLine = true)
                OutlinedTextField(value = displayName, onValueChange = { displayName = it }, label = { Text("Display Name") }, singleLine = true)
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Role: ")
                    RadioButton(selected = role == "ADMIN", onClick = { role = "ADMIN" })
                    Text("Admin")
                    Spacer(modifier = Modifier.width(8.dp))
                    RadioButton(selected = role == "USER", onClick = { role = "USER" })
                    Text("User")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = active, onCheckedChange = { active = it })
                    Text("Active")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(userId, password, displayName, email, role, active) }) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
