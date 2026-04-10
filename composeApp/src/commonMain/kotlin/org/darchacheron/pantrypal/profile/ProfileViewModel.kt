package org.darchacheron.pantrypal.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.darchacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darchacheron.pantrypal.authentication.AuthenticationService
import org.darchacheron.pantrypal.authentication.JwtUtils
import org.darchacheron.pantrypal.authentication.hashPassword
import org.darchacheron.pantrypal.navigation.Navigator
import org.darchacheron.pantrypal.ui.UiState
import pantrypal.composeapp.generated.resources.Res
import pantrypal.composeapp.generated.resources.profile_error_update
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

    private val _isLoggedInRemotely = MutableStateFlow(false)
    val isLoggedInRemotely: StateFlow<Boolean> = _isLoggedInRemotely

    init {
        loadProfile()
    }

    private fun loadProfile() {
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
        val currentProfile = _uiState.value.data ?: return
        _uiState.value = UiState.success(currentProfile.copy(username = username))
    }

    fun updateEmail(email: String) {
        val currentProfile = _uiState.value.data ?: return
        _uiState.value = UiState.success(currentProfile.copy(email = email))
    }

    fun saveProfile(newPassword: String? = null) {
        viewModelScope.launch {
            val profile = _uiState.value.data ?: return@launch
            try {
                var updatedProfile = profile
                if (!newPassword.isNullOrBlank()) {
                    updatedProfile = profile.copy(passwordHash = hashPassword(newPassword))
                }
                
                profileRepository.upsert(updatedProfile)
                
                if (authenticationService.isRemoteEnabled()) {
                    if (updatedProfile.serverId == null) {
                        // User enabled sync for the first time or was local-only.
                        // We need to register the user on the server.
                        val result = authenticationService.registerUser(
                            updatedProfile.username,
                            updatedProfile.email,
                            newPassword ?: "" // TODO: handle case where password is not provided but sync is enabled
                        )
                        if (result.isSuccess) {
                            val registrationResponse = result.getOrNull()
                            val serverUuid = registrationResponse?.let {
                                val serverIdFromToken = JwtUtils.getUserIdFromToken(it.tokenResponse.accessToken)
                                serverIdFromToken?.let { id -> Uuid.parse(id) } ?: Uuid.parse(it.user.id)
                            }
                            updatedProfile = updatedProfile.copy(serverId = serverUuid, lastSyncedAt = Clock.System.now())
                            profileRepository.upsert(updatedProfile)

                            // Also update preferences to mark as logged in remotely
                            registrationResponse?.let {
                                authenticationPreferencesRepository.updateAccessPreferences(
                                    accessToken = it.tokenResponse.accessToken,
                                    refreshToken = it.tokenResponse.refreshToken,
                                    expiresIn = it.tokenResponse.expiresIn,
                                    refreshExpiresIn = it.tokenResponse.refreshExpiresIn,
                                    localProfileId = updatedProfile.id.toString(),
                                    isLoggedInRemotely = true,
                                    serverUrl = authenticationService.getServerUrl()
                                )
                            }
                        }
                    } else {
                        authenticationService.updateUser(
                            username = updatedProfile.username,
                            email = updatedProfile.email,
                            password = newPassword
                        )
                    }
                }
                
                _uiState.value = UiState.success(updatedProfile)
            } catch (e: Exception) {
                _uiState.value = UiState.error(Res.string.profile_error_update)
            }
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
            try {
                if (authenticationService.isRemoteEnabled()) {
                    authenticationService.deleteUser(deleteRemote)
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
