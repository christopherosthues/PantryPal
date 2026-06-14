package org.darthacheron.pantrypal.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.darthacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darthacheron.pantrypal.authentication.AuthenticationService
import org.darthacheron.pantrypal.authentication.JwtUtils
import org.darthacheron.pantrypal.authentication.hashPassword
import org.darthacheron.pantrypal.authentication.verifyPassword
import org.darthacheron.pantrypal.food.FoodRepository
import org.darthacheron.pantrypal.inventory.InventoryRepository
import org.darthacheron.pantrypal.navigation.Navigator
import org.darthacheron.pantrypal.networking.ConnectionNetworkService
import org.darthacheron.pantrypal.settings.DataSynchronization
import org.darthacheron.pantrypal.ui.UiState
import pantrypal.shared.generated.resources.Res
import pantrypal.shared.generated.resources.profile_error_current_password_empty
import pantrypal.shared.generated.resources.profile_error_email_empty
import pantrypal.shared.generated.resources.profile_error_email_invalid
import pantrypal.shared.generated.resources.profile_error_new_password_empty
import pantrypal.shared.generated.resources.profile_error_password_mismatch
import pantrypal.shared.generated.resources.profile_error_update
import pantrypal.shared.generated.resources.profile_error_username_empty
import pantrypal.shared.generated.resources.profile_error_wrong_password
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val foodRepository: FoodRepository,
    private val inventoryRepository: InventoryRepository,
    private val authenticationPreferencesRepository: AuthenticationPreferencesRepository,
    private val authenticationService: AuthenticationService,
    private val connectionNetworkService: ConnectionNetworkService,
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

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin

    private val _persistedServerUrl = MutableStateFlow<String?>(null)
    val persistedServerUrl: StateFlow<String?> = _persistedServerUrl

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection

    private val _connectionTestSuccess = MutableStateFlow<Boolean?>(null)
    val connectionTestSuccess: StateFlow<Boolean?> = _connectionTestSuccess

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _showLoginDialog = MutableStateFlow(false)
    val showLoginDialog: StateFlow<Boolean> = _showLoginDialog.asStateFlow()

    init {
        loadProfile()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    internal fun loadProfile() {
        viewModelScope.launch {
            authenticationPreferencesRepository.authenticationPreferencesFlow
                .collect { prefs ->
                    _isLoggedInRemotely.value = prefs.isLoggedInRemotely
                    _persistedServerUrl.value = prefs.serverUrl
                    _isAdmin.value = JwtUtils.isAdmin(prefs.accessToken)
                }
        }

        viewModelScope.launch {
            authenticationPreferencesRepository.authenticationPreferencesFlow
                .map { it.localProfileId }
                .distinctUntilChanged()
                .flatMapLatest { profileId ->
                    if (profileId.isNotEmpty()) {
                        profileRepository.getProfileById(Uuid.parse(profileId))
                    } else {
                        flowOf(null)
                    }
                }
                .collect { profile ->
                    if (profile != null) {
                        _uiState.value = UiState.success(profile)
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

    fun updateDataSynchronization(dataSynchronization: DataSynchronization) {
        _uiState.value = _uiState.value.copy(
            data = _uiState.value.data?.copy(dataSynchronization = dataSynchronization),
            error = null
        )
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

            if (hasValidationError) {
                _profileValidationState.value = validationState
                return@launch
            }

            _uiState.value = UiState.loading()
            _profileValidationState.value = ProfileValidationState()

            profileRepository.upsert(profile)
                .onSuccess {
                    _uiState.value = UiState.success(profile)
                }
                .onFailure { e ->
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

            _passwordChangeState.value = UiState.loading()
            _passwordValidationState.value = PasswordValidationState()
            val updatedProfile = profile.copy(passwordHash = hashPassword(changeData.new))
            profileRepository.upsert(updatedProfile)
                .onSuccess {
                    _uiState.value = UiState.success(updatedProfile)
                    _passwordChangeState.value = UiState.success(PasswordChange())
                }
                .onFailure { e ->
                    _passwordChangeState.value = capturedPasswordUiState.copy(error = Res.string.profile_error_update)
                }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-z|A-Z]{2,}$".toRegex()
        return emailRegex.matches(email)
    }

    fun testConnection() {
        val serverUrl = _persistedServerUrl.value ?: return
        if (serverUrl.isBlank()) return

        viewModelScope.launch {
            _isTestingConnection.value = true
            _connectionTestSuccess.value = null
            val result = connectionNetworkService.testConnection(serverUrl)
            _connectionTestSuccess.value = result.isSuccess
            _isTestingConnection.value = false
        }
    }

    fun unlinkAccount() {
        viewModelScope.launch {
            val currentProfile = _uiState.value.data ?: return@launch
            val updatedProfile = currentProfile.copy(
                isLocalOnly = true,
                lastSyncedAt = null
            )
            profileRepository.upsert(updatedProfile)
                .onSuccess {
                    // Also ensure remote login state is cleared
                    authenticationService.logoutRemotely()
                    loadProfile()
                }
        }
    }

    fun showLoginDialog() {
        _showLoginDialog.value = true
    }

    fun triggerSync() {
        viewModelScope.launch {
            val prefs = authenticationPreferencesRepository.authenticationPreferencesFlow.first()
            if (!prefs.isLoggedInRemotely) {
                if (prefs.serverUrl.isNotBlank()) {
                    _showLoginDialog.value = true
                    return@launch
                }
            }

            _isSyncing.value = true
            foodRepository.syncWithServer()
            inventoryRepository.syncWithServer()
            profileRepository.syncWithServer()
            _isSyncing.value = false
        }
    }

    fun onDismissLoginDialog() {
        _showLoginDialog.value = false
    }

    fun onLoginSuccess() {
        _showLoginDialog.value = false
        triggerSync()
    }

    fun goToAdmin() {
        navigator.goToAdmin()
    }

    private val _showRemoteProfileDialog = MutableStateFlow(false)
    val showRemoteProfileDialog: StateFlow<Boolean> = _showRemoteProfileDialog

    fun showRemoteProfileDialog() {
        _showRemoteProfileDialog.value = true
    }

    fun dismissRemoteProfileDialog() {
        _showRemoteProfileDialog.value = false
    }

    fun logoutRemote() {
        viewModelScope.launch {
            // TODO: error handling
            authenticationService.logoutRemotely()
                .onSuccess {
                    _isLoggedInRemotely.value = false
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            // TODO: error handling
            authenticationService.logout()
                .onSuccess {
                    navigator.goToLogin()
                }
        }
    }

    private val _showDeleteLocalDialog = MutableStateFlow(false)
    val showDeleteLocalDialog: StateFlow<Boolean> = _showDeleteLocalDialog

    private val _showDeleteRemoteDialog = MutableStateFlow(false)
    val showDeleteRemoteDialog: StateFlow<Boolean> = _showDeleteRemoteDialog

    private val _remoteDeleteUsername = MutableStateFlow("")
    val remoteDeleteUsername: StateFlow<String> = _remoteDeleteUsername

    private val _remoteDeletePassword = MutableStateFlow("")
    val remoteDeletePassword: StateFlow<String> = _remoteDeletePassword

    private val _remoteDeleteError = MutableStateFlow<String?>(null)
    val remoteDeleteError: StateFlow<String?> = _remoteDeleteError

    fun showDeleteLocalDialog() {
        _showDeleteLocalDialog.value = true
    }

    fun dismissDeleteLocalDialog() {
        _showDeleteLocalDialog.value = false
    }

    fun showDeleteRemoteDialog() {
        _showDeleteRemoteDialog.value = true
        _remoteDeleteUsername.value = _uiState.value.data?.username ?: ""
        _remoteDeletePassword.value = ""
        _remoteDeleteError.value = null
    }

    fun dismissDeleteRemoteDialog() {
        _showDeleteRemoteDialog.value = false
    }

    fun onRemoteDeleteUsernameChanged(username: String) {
        _remoteDeleteUsername.value = username
    }

    fun onRemoteDeletePasswordChanged(password: String) {
        _remoteDeletePassword.value = password
    }

    fun deleteLocalProfile() {
        viewModelScope.launch {
            profileRepository.deleteLocal()
                .onSuccess {
                    authenticationService.logout()
                    navigator.goToLogin()
                }
                .onFailure { e ->
                    // Log error
                }
        }
    }

    fun deleteRemoteProfile() {
        viewModelScope.launch {
            val serverUrl = _persistedServerUrl.value ?: return@launch

            if (!_isLoggedInRemotely.value) {
                val loginResult = authenticationService.loginRemotely(
                    _remoteDeleteUsername.value,
                    _remoteDeletePassword.value,
                    serverUrl
                )
                if (loginResult.isFailure) {
                    _remoteDeleteError.value = loginResult.exceptionOrNull()?.message ?: "Login failed"
                    return@launch
                }
            }

            profileRepository.deleteRemote()
                .onSuccess {
                    authenticationService.logoutRemotely()
                    dismissDeleteRemoteDialog()
                    loadProfile() // Reload to reflect local-only state
                }
                .onFailure { e ->
                    _remoteDeleteError.value = e.message ?: "Deletion failed"
                }
        }
    }

    fun canDeleteRemote(): Boolean {
        val profile = _uiState.value.data ?: return false
        return profile.remoteProfiles.isNotEmpty() || _persistedServerUrl.value != null
    }
}
