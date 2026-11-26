package com.example.warehousescan.feature.usermanagement

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehousescan.core.storage.UserDao
import com.example.warehousescan.core.storage.UserEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

sealed interface UserManagementUiState {
    data class Content(
        val users: List<UserEntity> = emptyList(),
        val errorMessage: String? = null,
        val successMessage: String? = null,
        val editingUser: UserEntity? = null,
        val isAddingOrEditing: Boolean = false
    ) : UserManagementUiState
}

@HiltViewModel
class UserManagementViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userDao: UserDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<UserManagementUiState>(UserManagementUiState.Content())
    val uiState: StateFlow<UserManagementUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userDao.getAllUsers().collect { list ->
                val current = _uiState.value
                if (current is UserManagementUiState.Content) {
                    _uiState.value = current.copy(users = list)
                }
            }
        }
    }

    fun startAddUser() {
        val current = _uiState.value as? UserManagementUiState.Content ?: return
        _uiState.value = current.copy(
            isAddingOrEditing = true,
            editingUser = UserEntity(userId = "", password = "", displayName = "", email = "", role = "USER", active = true),
            errorMessage = null,
            successMessage = null
        )
    }

    fun startEditUser(user: UserEntity) {
        val current = _uiState.value as? UserManagementUiState.Content ?: return
        _uiState.value = current.copy(
            isAddingOrEditing = true,
            editingUser = user,
            errorMessage = null,
            successMessage = null
        )
    }

    fun cancelEdit() {
        val current = _uiState.value as? UserManagementUiState.Content ?: return
        _uiState.value = current.copy(isAddingOrEditing = false, editingUser = null)
    }

    fun saveUser(userId: String, pass: String, displayName: String, email: String, role: String, active: Boolean) {
        val current = _uiState.value as? UserManagementUiState.Content ?: return
        if (userId.isBlank() || pass.isBlank() || email.isBlank()) {
            _uiState.value = current.copy(errorMessage = "All fields (userId, password, email) are required.")
            return
        }
        if (!email.contains("@") || !email.contains(".")) {
            _uiState.value = current.copy(errorMessage = "Invalid email format.")
            return
        }

        viewModelScope.launch {
            val existing = userDao.getUserByUserId(userId)
            val editing = current.editingUser

            // Check unique userId
            if (existing != null && (editing == null || editing.id != existing.id)) {
                _uiState.value = current.copy(errorMessage = "User ID already exists: $userId")
                return@launch
            }

            if (editing != null && editing.id != 0L) {
                // Update
                val updated = editing.copy(
                    userId = userId,
                    password = pass,
                    displayName = displayName,
                    email = email,
                    role = role,
                    active = active,
                    updatedAt = System.currentTimeMillis()
                )
                userDao.updateUser(updated)
            } else {
                // Insert
                val newUser = UserEntity(
                    userId = userId,
                    password = pass,
                    displayName = displayName.ifBlank { userId },
                    email = email,
                    role = role,
                    active = active
                )
                userDao.insertUser(newUser)
            }

            _uiState.value = current.copy(isAddingOrEditing = false, editingUser = null, successMessage = "User saved successfully.")
        }
    }

    fun deleteUser(user: UserEntity) {
        val current = _uiState.value as? UserManagementUiState.Content ?: return
        viewModelScope.launch {
            if (user.role == "ADMIN" && user.active) {
                val adminCount = userDao.getActiveAdminCount()
                if (adminCount <= 1) {
                    _uiState.value = current.copy(errorMessage = "Cannot delete the last active admin!")
                    return@launch
                }
            }
            userDao.deleteUser(user.userId)
            _uiState.value = current.copy(successMessage = "User deleted.")
        }
    }

    fun toggleActive(user: UserEntity) {
        val current = _uiState.value as? UserManagementUiState.Content ?: return
        viewModelScope.launch {
            if (user.role == "ADMIN" && user.active) {
                val adminCount = userDao.getActiveAdminCount()
                if (adminCount <= 1) {
                    _uiState.value = current.copy(errorMessage = "Cannot deactivate the last active admin!")
                    return@launch
                }
            }
            val updated = user.copy(active = !user.active, updatedAt = System.currentTimeMillis())
            userDao.updateUser(updated)
        }
    }

    fun importUsers(uri: Uri, replaceAll: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value as? UserManagementUiState.Content ?: return@launch
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val text = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).readText()
                inputStream?.close()

                @Serializable
                data class ImportUserDto(
                    val userId: String,
                    val password: String,
                    val displayName: String,
                    val email: String,
                    val role: String,
                    val active: Boolean
                )
                @Serializable
                data class ImportDto(val users: List<ImportUserDto>)

                val json = Json { ignoreUnknownKeys = true }
                val imported = json.decodeFromString<ImportDto>(text)

                if (replaceAll) {
                    // Clear existing users and insert new
                    for (u in current.users) {
                        userDao.deleteUser(u.userId)
                    }
                }

                for (u in imported.users) {
                    userDao.insertUser(
                        UserEntity(
                            userId = u.userId,
                            password = u.password,
                            displayName = u.displayName,
                            email = u.email,
                            role = u.role,
                            active = u.active
                        )
                    )
                }
                _uiState.value = current.copy(successMessage = "Successfully imported ${imported.users.size} users.")
            } catch (e: Exception) {
                _uiState.value = current.copy(errorMessage = "Import failed: ${e.localizedMessage}")
            }
        }
    }

    fun exportUsersJson(): String {
        val current = _uiState.value as? UserManagementUiState.Content ?: return ""
        @Serializable
        data class ExportUserDto(
            val userId: String,
            val password: String,
            val displayName: String,
            val email: String,
            val role: String,
            val active: Boolean
        )
        @Serializable
        data class ExportDto(val users: List<ExportUserDto>)

        val dtos = current.users.map {
            ExportUserDto(it.userId, it.password, it.displayName, it.email, it.role, it.active)
        }
        return Json { prettyPrint = true }.encodeToString(ExportDto(dtos))
    }
}
