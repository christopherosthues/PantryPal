package org.darchacheron.pantrypal.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.darchacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darchacheron.pantrypal.authentication.AuthenticationService
import org.darchacheron.pantrypal.authentication.hashPassword
import org.darchacheron.pantrypal.navigation.Navigator
import org.darchacheron.pantrypal.ui.UiState
import pantrypal.composeapp.generated.resources.Res
import pantrypal.composeapp.generated.resources.profile_error_update
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

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            authenticationPreferencesRepository.authenticationPreferencesFlow.collect { prefs ->
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
                    authenticationService.updateUser(
                        username = updatedProfile.username,
                        email = updatedProfile.email,
                        password = newPassword
                    )
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

    fun deleteProfile() {
        viewModelScope.launch {
            try {
                if (authenticationService.isRemoteEnabled()) {
                    authenticationService.deleteUser()
                }
                profileRepository.delete() 
                authenticationService.logout()
                authenticationPreferencesRepository.clearProfile()
                navigator.goToLogin()
            } catch (e: Exception) {
                // Log error or show message
            }
        }
    }
}
