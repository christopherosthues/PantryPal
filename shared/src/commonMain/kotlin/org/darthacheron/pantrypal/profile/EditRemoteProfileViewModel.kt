package org.darthacheron.pantrypal.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock
import org.darthacheron.pantrypal.authentication.AuthenticationService
import org.darthacheron.pantrypal.authentication.InvalidCredentialsException
import org.darthacheron.pantrypal.authentication.UserAlreadyExistsException
import org.darthacheron.pantrypal.ui.UiState
import pantrypal.shared.generated.resources.Res
import pantrypal.shared.generated.resources.profile_error_update
import pantrypal.shared.generated.resources.profile_error_username_exists
import pantrypal.shared.generated.resources.profile_error_wrong_password
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class EditRemoteProfileViewModel(
    private val authenticationService: AuthenticationService,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState.success(false))
    val uiState: StateFlow<UiState<Boolean>> = _uiState

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email

    private val _currentPassword = MutableStateFlow("")
    val currentPassword: StateFlow<String> = _currentPassword

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

    fun onCurrentPasswordChanged(password: String) {
        _currentPassword.value = password
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
                val newUsername = _username.value
                val newEmail = _email.value
                val newPassword = _newPassword.value.takeIf { it.isNotBlank() }

                val result = authenticationService.updateUser(
                    serverUrl = serverUrl,
                    username = if (newUsername != currentProfile.username) newUsername else null,
                    email = if (newEmail != currentProfile.email) newEmail else null,
                    password = newPassword,
                    currentPassword = _currentPassword.value.takeIf { it.isNotBlank() }
                )

                if (result.isSuccess) {
                    val remoteProfile = currentProfile.remoteProfiles.firstOrNull { it.serverUrl == serverUrl }
                    if (remoteProfile != null) {
                        profileRepository.upsertRemoteProfile(remoteProfile.copy(
                            username = newUsername,
                            email = newEmail,
                            lastSyncedAt = Clock.System.now()
                        ))
                    }

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
