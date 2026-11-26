package com.example.warehousescan.feature.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehousescan.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RegisterUiState {
    data object Idle : RegisterUiState
    data object Loading : RegisterUiState
    data class Success(val username: String) : RegisterUiState
    data class Error(val message: String) : RegisterUiState
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun register(
        username: String,
        displayName: String,
        email: String,
        password: String,
        confirmPass: String
    ) {
        val trimmedUser = username.trim()
        val trimmedEmail = email.trim()

        if (trimmedUser.isBlank()) {
            _uiState.value = RegisterUiState.Error("Username is required")
            return
        }
        if (trimmedEmail.isBlank() || !trimmedEmail.contains("@")) {
            _uiState.value = RegisterUiState.Error("Please enter a valid email address")
            return
        }
        if (password.isBlank()) {
            _uiState.value = RegisterUiState.Error("Password is required")
            return
        }
        if (password != confirmPass) {
            _uiState.value = RegisterUiState.Error("Passwords do not match")
            return
        }

        viewModelScope.launch {
            _uiState.value = RegisterUiState.Loading
            val result = authRepository.register(
                username = trimmedUser,
                password = password,
                displayName = displayName.ifBlank { trimmedUser },
                email = trimmedEmail
            )
            if (result.isSuccess) {
                _uiState.value = RegisterUiState.Success(trimmedUser)
            } else {
                _uiState.value = RegisterUiState.Error(
                    result.exceptionOrNull()?.message ?: "Registration failed"
                )
            }
        }
    }
}
