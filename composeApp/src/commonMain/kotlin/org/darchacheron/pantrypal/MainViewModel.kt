package org.darchacheron.pantrypal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.darchacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darchacheron.pantrypal.authentication.AuthenticationService
import org.darchacheron.pantrypal.navigation.Navigator
import org.darchacheron.pantrypal.profile.ProfileRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class MainViewModel(
    private val profileRepository: ProfileRepository,
    private val authenticationService: AuthenticationService,
    private val authenticationPreferencesRepository: AuthenticationPreferencesRepository,
    private val navigator: Navigator
) : ViewModel() {

    private val _showRemoteDeletedDialog = MutableStateFlow(false)
    val showRemoteDeletedDialog: StateFlow<Boolean> = _showRemoteDeletedDialog.asStateFlow()

    init {
        viewModelScope.launch {
            profileRepository.remoteAccountDeleted.collect { deleted ->
                if (deleted) {
                    _showRemoteDeletedDialog.value = true
                }
            }
        }
    }

    fun dismissRemoteDeletedDialog() {
        _showRemoteDeletedDialog.value = false
    }

    fun deleteLocalAccount() {
        // TODO: error handling
        viewModelScope.launch {
            profileRepository.delete(remote = false)
            authenticationService.logout()
            _showRemoteDeletedDialog.value = false
            navigator.goToLogin()
        }
    }

    fun keepLocalAccountOnly() {
        // TODO: error handling
        viewModelScope.launch {
            val prefs = authenticationPreferencesRepository.authenticationPreferencesFlow.first()
            if (prefs.localProfileId.isNotBlank()) {
                val profileId = Uuid.parse(prefs.localProfileId)
                profileRepository.getProfileById(profileId).first()?.let { profile ->
                    profileRepository.upsert(profile.copy(serverId = null, isLocalOnly = true))
                }
            }

            // Logout removes remote tokens but keeps localProfileId if we pass it back or change updateAccessPreferences
            authenticationService.logoutRemotely()

            _showRemoteDeletedDialog.value = false
        }
    }
}
