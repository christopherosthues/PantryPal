package org.darchacheron.pantrypal.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.darchacheron.pantrypal.authentication.AuthenticationService
import org.darchacheron.pantrypal.authentication.InvalidCredentialsException
import org.darchacheron.pantrypal.authentication.UserAlreadyExistsException
import org.darchacheron.pantrypal.ui.UiState
import pantrypal.composeapp.generated.resources.Res
import pantrypal.composeapp.generated.resources.profile_error_update
import pantrypal.composeapp.generated.resources.profile_error_username_exists
import pantrypal.composeapp.generated.resources.profile_error_wrong_password

class EditRemoteProfileViewModel(
    private val authenticationService: AuthenticationService
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<Boolean>>(UiState.success(false))
    val uiState: StateFlow<UiState<Boolean>> = _uiState

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email

    private val _oldPassword = MutableStateFlow("")
    val oldPassword: StateFlow<String> = _oldPassword

    private val _newPassword = MutableStateFlow("")
    val newPassword: StateFlow<String> = _newPassword

    private val _repeatNewPassword = MutableStateFlow("")
    val repeatNewPassword: StateFlow<String> = _repeatNewPassword

    fun onUsernameChanged(username: String) {
        _username.value = username
    }

    fun onEmailChanged(email: String) {
        _email.value = email
    }

    fun onOldPasswordChanged(password: String) {
        _oldPassword.value = password
    }

    fun onNewPasswordChanged(password: String) {
        _newPassword.value = password
    }

    fun onRepeatNewPasswordChanged(password: String) {
        _repeatNewPassword.value = password
    }

    fun updateRemoteProfile(serverUrl: String, currentProfile: Profile, onDismiss: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = UiState.loading()
            try {
                val result = authenticationService.updateUser(
                    serverUrl = serverUrl,
                    username = if (_username.value != currentProfile.username) _username.value else null,
                    email = if (_email.value != currentProfile.email) _email.value else null,
                    password = _newPassword.value.takeIf { it.isNotBlank() },
                    currentPassword = _oldPassword.value
                )

                if (result.isSuccess) {
                    _uiState.value = UiState.success(true)
                    onDismiss()
                } else {
                    val exception = result.exceptionOrNull()
                    val errorRes = when (exception) {
                        is UserAlreadyExistsException -> Res.string.profile_error_username_exists
                        is InvalidCredentialsException -> Res.string.profile_error_wrong_password
                        else -> Res.string.profile_error_update
                    }
                    _uiState.value = UiState.error(errorRes)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.error(Res.string.profile_error_update)
            }
        }
    }
}
