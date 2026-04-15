package org.darchacheron.pantrypal.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.darchacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darchacheron.pantrypal.authentication.AuthenticationService
import org.darchacheron.pantrypal.authentication.InvalidCredentialsException
import org.darchacheron.pantrypal.authentication.JwtUtils
import org.darchacheron.pantrypal.authentication.UserAlreadyExistsException
import org.darchacheron.pantrypal.authentication.hashPassword
import org.darchacheron.pantrypal.authentication.verifyPassword
import org.darchacheron.pantrypal.navigation.Navigator
import org.darchacheron.pantrypal.ui.UiState
import pantrypal.composeapp.generated.resources.Res
import pantrypal.composeapp.generated.resources.profile_error_current_password_empty
import pantrypal.composeapp.generated.resources.profile_error_email_empty
import pantrypal.composeapp.generated.resources.profile_error_email_invalid
import pantrypal.composeapp.generated.resources.profile_error_new_password_empty
import pantrypal.composeapp.generated.resources.profile_error_password_mismatch
import pantrypal.composeapp.generated.resources.profile_error_server_url_empty
import pantrypal.composeapp.generated.resources.profile_error_server_url_invalid
import pantrypal.composeapp.generated.resources.profile_error_update
import pantrypal.composeapp.generated.resources.profile_error_username_empty
import pantrypal.composeapp.generated.resources.profile_error_username_exists
import pantrypal.composeapp.generated.resources.profile_error_wrong_password
import pantrypal.composeapp.generated.resources.profile_link_error
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val authenticationPreferencesRepository: AuthenticationPreferencesRepository,
    private val authenticationService: AuthenticationService,
    private val navigator: Navigator
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<Profile>>(UiState.loading())
    val uiState: StateFlow<UiState<Profile>> = _uiState

    private val _profileValidationState = MutableStateFlow(ProfileValidationState())
    val profileValidationState: StateFlow<ProfileValidationState> = _profileValidationState

    private val _passwordChangeState = MutableStateFlow(UiState.success(PasswordChange()))
    val passwordChangeState: StateFlow<UiState<PasswordChange>> = _passwordChangeState

    private val _passwordValidationState = MutableStateFlow(PasswordValidationState())
    val passwordValidationState: StateFlow<PasswordValidationState> = _passwordValidationState

    private val _isLoggedInRemotely = MutableStateFlow(false)
    val isLoggedInRemotely: StateFlow<Boolean> = _isLoggedInRemotely

    private val _persistedServerUrl = MutableStateFlow<String?>(null)
    val persistedServerUrl: StateFlow<String?> = _persistedServerUrl

    init {
        loadProfile()
    }

    internal fun loadProfile() {
        viewModelScope.launch {
            authenticationPreferencesRepository.authenticationPreferencesFlow.collect { prefs ->
                _isLoggedInRemotely.value = prefs.isLoggedInRemotely
                val profileId = prefs.localProfileId
                if (profileId.isNotEmpty()) {
                    profileRepository.getProfileById(Uuid.parse(profileId)).collect { profile ->
                        if (profile != null) {
                            _persistedServerUrl.value = profile.serverUrl
                            _uiState.value = UiState.success(profile)
                        }
                    }
                }
            }
        }
    }

    fun updateUsername(username: String) {
        _uiState.value = _uiState.value.copy(data = _uiState.value.data?.copy(username = username), error = null)
        val error = if (username.isBlank()) Res.string.profile_error_username_empty else null
        _profileValidationState.value = _profileValidationState.value.copy(usernameError = error)
    }

    fun updateEmail(email: String) {
        _uiState.value = _uiState.value.copy(data = _uiState.value.data?.copy(email = email), error = null)
        val error = if (email.isBlank()) {
            Res.string.profile_error_email_empty
        } else if (!isValidEmail(email)) {
            Res.string.profile_error_email_invalid
        } else {
            null
        }
        _profileValidationState.value = _profileValidationState.value.copy(emailError = error)
    }

    fun onCurrentPasswordChanged(current: String) {
        _passwordChangeState.value = _passwordChangeState.value.copy(
            data = _passwordChangeState.value.data?.copy(current = current)
        )
        val error = if (current.isBlank()) Res.string.profile_error_current_password_empty else null
        _passwordValidationState.value = _passwordValidationState.value.copy(currentPasswordError = error)
    }

    fun onNewPasswordChanged(new: String) {
        _passwordChangeState.value = _passwordChangeState.value.copy(
            data = _passwordChangeState.value.data?.copy(new = new)
        )
        val newPasswordError = if (new.isBlank()) Res.string.profile_error_new_password_empty else null
        
        val repeat = _passwordChangeState.value.data?.repeat ?: ""
        val repeatPasswordError = if (repeat.isNotEmpty() && new != repeat) {
            Res.string.profile_error_password_mismatch
        } else {
            null
        }

        _passwordValidationState.value = _passwordValidationState.value.copy(
            newPasswordError = newPasswordError,
            repeatPasswordError = repeatPasswordError
        )
    }

    fun onRepeatPasswordChanged(repeat: String) {
        _passwordChangeState.value = _passwordChangeState.value.copy(
            data = _passwordChangeState.value.data?.copy(repeat = repeat)
        )
        val new = _passwordChangeState.value.data?.new ?: ""
        val error = if (new != repeat) Res.string.profile_error_password_mismatch else null
        _passwordValidationState.value = _passwordValidationState.value.copy(repeatPasswordError = error)
    }

    fun saveProfile() {
        viewModelScope.launch {
            val capturedState = _uiState.value
            val profile = capturedState.data ?: return@launch

            var hasValidationError = false
            var validationState = ProfileValidationState()

            if (profile.username.isBlank()) {
                validationState = validationState.copy(usernameError = Res.string.profile_error_username_empty)
                hasValidationError = true
            }

            val serverUrl = profile.serverUrl
            if (!serverUrl.isNullOrBlank() && !isValidUri(serverUrl)) {
                validationState = validationState.copy(serverUrlError = Res.string.profile_error_server_url_invalid)
                hasValidationError = true
            }

            if (hasValidationError) {
                _profileValidationState.value = validationState
                return@launch
            }

            try {
                _uiState.value = UiState.loading()
                _profileValidationState.value = ProfileValidationState()

                val profileToSave = if (profile.serverUrl?.isBlank() == true) {
                    profile.copy(serverUrl = null)
                } else {
                    profile
                }

                profileRepository.upsert(profileToSave)
                _uiState.value = UiState.success(profileToSave)
            } catch (e: Exception) {
                _uiState.value = capturedState.copy(error = Res.string.profile_error_update)
            }
        }
    }

    fun changePassword() {
        viewModelScope.launch {
            val capturedUiState = _uiState.value
            val profile = capturedUiState.data ?: return@launch
            
            val capturedPasswordUiState = _passwordChangeState.value
            val changeData = capturedPasswordUiState.data ?: return@launch
            
            var hasValidationError = false
            var validationState = PasswordValidationState()

            if (changeData.current.isBlank()) {
                validationState = validationState.copy(currentPasswordError = Res.string.profile_error_current_password_empty)
                hasValidationError = true
            } else if (!verifyPassword(changeData.current, profile.passwordHash)) {
                validationState = validationState.copy(currentPasswordError = Res.string.profile_error_wrong_password)
                hasValidationError = true
            }
            
            if (changeData.new.isBlank()) {
                validationState = validationState.copy(newPasswordError = Res.string.profile_error_new_password_empty)
                hasValidationError = true
            }

            if (changeData.new != changeData.repeat) {
                validationState = validationState.copy(repeatPasswordError = Res.string.profile_error_password_mismatch)
                hasValidationError = true
            }
            
            if (hasValidationError) {
                _passwordValidationState.value = validationState
                return@launch
            }

            try {
                _passwordChangeState.value = UiState.loading()
                _passwordValidationState.value = PasswordValidationState()
                val updatedProfile = profile.copy(passwordHash = hashPassword(changeData.new))
                profileRepository.upsert(updatedProfile)
                
                _uiState.value = UiState.success(updatedProfile)
                _passwordChangeState.value = UiState.success(PasswordChange())
            } catch (e: Exception) {
                _passwordChangeState.value = capturedPasswordUiState.copy(error = Res.string.profile_error_update)
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-z|A-Z]{2,}$".toRegex()
        return emailRegex.matches(email)
    }

    fun isValidUri(uri: String): Boolean {
        return try {
            val regex = "^https?://[-a-zA-Z0-9+&@/%~_|!:,.;]*[-a-zA-Z0-9+&@/%=~_|]".toRegex()
            regex.matches(uri)
        } catch (e: Exception) {
            false
        }
    }

    fun updateServerUrl(url: String) {
        _uiState.value = _uiState.value.copy(data = _uiState.value.data?.copy(serverUrl = url), error = null)
        val error = if (url.isNotEmpty() && !isValidUri(url)) {
            Res.string.profile_error_server_url_invalid
        } else {
            null
        }
        _profileValidationState.value = _profileValidationState.value.copy(serverUrlError = error)
    }

    private val _remoteProfileEditState = MutableStateFlow<UiState<Boolean>>(UiState.success(false))
    val remoteProfileEditState: StateFlow<UiState<Boolean>> = _remoteProfileEditState

    private val _showRemoteProfileDialog = MutableStateFlow(false)
    val showRemoteProfileDialog: StateFlow<Boolean> = _showRemoteProfileDialog

    fun showRemoteProfileDialog() {
        _showRemoteProfileDialog.value = true
        _remoteProfileEditState.value = UiState.success(false)
    }

    fun dismissRemoteProfileDialog() {
        _showRemoteProfileDialog.value = false
    }

    fun updateRemoteProfile(username: String, email: String, newPassword: String?, currentPassword: String) {
        viewModelScope.launch {
            val profile = _uiState.value.data ?: return@launch
            val serverUrl = profile.serverUrl ?: return@launch

            _remoteProfileEditState.value = UiState.loading()
            try {
                // We need to verify current password first or the backend needs it for sensitive changes
                // If backend requires current password for any update, we include it.
                // Based on AuthenticationService.updateUser, it doesn't take current password yet.
                // Let's assume we might need to re-authenticate or the backend handles it via token.
                // However, the requirement said "password with repeat password and old password".
                // I should check if updateUser in AuthenticationService should be updated to include current password if needed.
                
                val result = authenticationService.updateUser(
                    serverUrl = serverUrl,
                    username = if (username != profile.username) username else null,
                    email = if (email != profile.email) email else null,
                    password = newPassword,
                    currentPassword = currentPassword
                )

                if (result.isSuccess) {
                    _remoteProfileEditState.value = UiState.success(true)
                    // We don't update the local profile username/email here 
                    // because they can be different from the remote ones.
                    _showRemoteProfileDialog.value = false
                } else {
                    val exception = result.exceptionOrNull()
                    val errorRes = when (exception) {
                        is UserAlreadyExistsException -> Res.string.profile_error_username_exists
                        is InvalidCredentialsException -> Res.string.profile_error_wrong_password
                        else -> Res.string.profile_error_update
                    }
                    _remoteProfileEditState.value = UiState.error(errorRes)
                }
            } catch (e: Exception) {
                _remoteProfileEditState.value = UiState.error(Res.string.profile_error_update)
            }
        }
    }

    fun logoutRemote() {
        viewModelScope.launch {
            authenticationService.logout()
            authenticationPreferencesRepository.updateAccessPreferences(
                accessToken = "",
                refreshToken = "",
                expiresIn = 0,
                refreshExpiresIn = 0,
                localProfileId = _uiState.value.data?.id.toString(),
                isLoggedInRemotely = false,
                serverUrl = _uiState.value.data?.serverUrl ?: ""
            )
            _isLoggedInRemotely.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            authenticationService.logout()
            authenticationPreferencesRepository.clearProfile()
            navigator.goToLogin()
        }
    }

    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog

    fun showDeleteDialog() {
        _showDeleteDialog.value = true
    }

    fun canDeleteRemote(): Boolean {
        val profile = _uiState.value.data ?: return false
        return profile.serverId != null && !profile.isLocalOnly
    }

    fun dismissDeleteDialog() {
        _showDeleteDialog.value = false
    }

    fun deleteProfile(deleteRemote: Boolean) {
        viewModelScope.launch {
            val profile = _uiState.value.data
            val serverUrl = profile?.serverUrl
            try {
                if (deleteRemote && !serverUrl.isNullOrBlank()) {
                    authenticationService.deleteUser(serverUrl, deleteRemote)
                }
                profileRepository.delete(deleteRemote)
                authenticationService.logout()
                authenticationPreferencesRepository.clearProfile()
                navigator.goToLogin()
            } catch (e: Exception) {
                // Log error or show message
            }
        }
    }
}
