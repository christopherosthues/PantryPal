package org.darchacheron.pantrypal.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.darchacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darchacheron.pantrypal.authentication.AuthenticationService
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
            if (!profile.isLocalOnly && serverUrl.isNullOrBlank()) {
                validationState = validationState.copy(serverUrlError = Res.string.profile_error_server_url_empty)
                hasValidationError = true
            } else if (!serverUrl.isNullOrBlank() && !isValidUri(serverUrl)) {
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

                val serverUrl = profile.serverUrl
                if (!serverUrl.isNullOrBlank() && profile.serverId != null) {
                    val result = authenticationService.updateUser(
                        serverUrl = serverUrl,
                        username = profile.username,
                        email = profile.email
                    )
                    if (result.isFailure) {
                        val exception = result.exceptionOrNull()
                        
                        if (exception is UserAlreadyExistsException) {
                             // For now we assume username exists if we get a conflict, 
                             // but we could also check email or both if the backend provides info.
                             _profileValidationState.value = ProfileValidationState(
                                 usernameError = Res.string.profile_error_username_exists
                             )
                             _uiState.value = capturedState
                             return@launch
                        }

                        _uiState.value = capturedState.copy(error = Res.string.profile_error_update)
                        return@launch
                    }
                }

                profileRepository.upsert(profile)
                _uiState.value = UiState.success(profile)
            } catch (e: Exception) {
                _uiState.value = capturedState.copy(error = Res.string.profile_error_update)
            }
        }
    }

    fun linkToExistingAccount(serverUrl: String, remotePassword: String) {
        viewModelScope.launch {
            val capturedState = _uiState.value
            val profile = capturedState.data ?: return@launch

            if (serverUrl.isBlank()) {
                _uiState.value = capturedState.copy(error = Res.string.profile_error_server_url_empty)
                return@launch
            }

            try {
                _uiState.value = UiState.loading()
                val result = authenticationService.login(profile.username, remotePassword, serverUrl)
                
                if (result.isSuccess) {
                    val loginResponse = result.getOrNull()
                    if (loginResponse != null) {
                        val serverIdFromToken = JwtUtils.getUserIdFromToken(loginResponse.tokenResponse.accessToken)
                        val serverUuid = serverIdFromToken?.let { Uuid.parse(it) } ?: Uuid.parse(loginResponse.user.id)
                        
                        val updatedProfile = profile.copy(
                            serverId = serverUuid,
                            serverUrl = serverUrl,
                            isLocalOnly = false,
                            lastSyncedAt = Clock.System.now()
                        )
                        profileRepository.upsert(updatedProfile)
                        
                        authenticationPreferencesRepository.updateAccessPreferences(
                            accessToken = loginResponse.tokenResponse.accessToken,
                            refreshToken = loginResponse.tokenResponse.refreshToken,
                            expiresIn = loginResponse.tokenResponse.expiresIn,
                            refreshExpiresIn = loginResponse.tokenResponse.refreshExpiresIn,
                            localProfileId = updatedProfile.id.toString(),
                            isLoggedInRemotely = true,
                            serverUrl = serverUrl
                        )
                        _uiState.value = UiState.success(updatedProfile)
                    }
                } else {
                    _uiState.value = capturedState.copy(error = Res.string.profile_link_error)
                }
            } catch (e: Exception) {
                _uiState.value = capturedState.copy(error = Res.string.profile_link_error)
            }
        }
    }

    fun createNewRemoteAccount(serverUrl: String, remotePassword: String) {
        viewModelScope.launch {
            val capturedState = _uiState.value
            val profile = capturedState.data ?: return@launch

            if (serverUrl.isBlank()) {
                _uiState.value = capturedState.copy(error = Res.string.profile_error_server_url_empty)
                return@launch
            }

            try {
                _uiState.value = UiState.loading()
                val result = authenticationService.registerUser(
                    profile.username,
                    profile.email,
                    remotePassword,
                    serverUrl
                )
                
                if (result.isSuccess) {
                    val registrationResponse = result.getOrNull()
                    if (registrationResponse != null) {
                        val serverIdFromToken = JwtUtils.getUserIdFromToken(registrationResponse.tokenResponse.accessToken)
                        val serverUuid = serverIdFromToken?.let { Uuid.parse(it) } ?: Uuid.parse(registrationResponse.user.id)
                        
                        val updatedProfile = profile.copy(
                            serverId = serverUuid,
                            serverUrl = serverUrl,
                            isLocalOnly = false,
                            lastSyncedAt = Clock.System.now()
                        )
                        profileRepository.upsert(updatedProfile)
                        
                        authenticationPreferencesRepository.updateAccessPreferences(
                            accessToken = registrationResponse.tokenResponse.accessToken,
                            refreshToken = registrationResponse.tokenResponse.refreshToken,
                            expiresIn = registrationResponse.tokenResponse.expiresIn,
                            refreshExpiresIn = registrationResponse.tokenResponse.refreshExpiresIn,
                            localProfileId = updatedProfile.id.toString(),
                            isLoggedInRemotely = true,
                            serverUrl = serverUrl
                        )
                        _uiState.value = UiState.success(updatedProfile)
                    }
                } else {
                    _uiState.value = capturedState.copy(error = Res.string.profile_error_update)
                }
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
                
                val serverUrl = updatedProfile.serverUrl
                if (!serverUrl.isNullOrBlank() && updatedProfile.serverId != null) {
                    authenticationService.updateUser(
                        serverUrl = serverUrl,
                        username = updatedProfile.username,
                        email = updatedProfile.email,
                        password = changeData.new
                    )
                }
                
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

    private fun isValidUri(uri: String): Boolean {
        return try {
            val regex = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]".toRegex()
            regex.matches(uri)
        } catch (e: Exception) {
            false
        }
    }

    fun updateServerUrl(url: String) {
        _uiState.value = _uiState.value.copy(data = _uiState.value.data?.copy(serverUrl = url), error = null)
        val error = if (url.isBlank()) {
            Res.string.profile_error_server_url_empty
        } else if (!isValidUri(url)) {
            Res.string.profile_error_server_url_invalid
        } else {
            null
        }
        _profileValidationState.value = _profileValidationState.value.copy(serverUrlError = error)
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

data class PasswordChange(
    val current: String = "",
    val new: String = "",
    val repeat: String = ""
)

data class ProfileValidationState(
    val usernameError: org.jetbrains.compose.resources.StringResource? = null,
    val emailError: org.jetbrains.compose.resources.StringResource? = null,
    val serverUrlError: org.jetbrains.compose.resources.StringResource? = null
)

data class PasswordValidationState(
    val currentPasswordError: org.jetbrains.compose.resources.StringResource? = null,
    val newPasswordError: org.jetbrains.compose.resources.StringResource? = null,
    val repeatPasswordError: org.jetbrains.compose.resources.StringResource? = null
)
