package org.darthacheron.pantrypal.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.darthacheron.pantrypal.networking.ConnectionNetworkService
import org.darthacheron.pantrypal.profile.ProfileRepository
import org.darthacheron.pantrypal.profile.RemoteProfile
import org.darthacheron.pantrypal.ui.UiState
import pantrypal.composeapp.generated.resources.Res
import pantrypal.composeapp.generated.resources.remote_login_error_credentials
import pantrypal.composeapp.generated.resources.remote_login_error_generic
import pantrypal.composeapp.generated.resources.remote_login_error_invalid_server_url
import pantrypal.composeapp.generated.resources.remote_login_error_password_empty
import pantrypal.composeapp.generated.resources.remote_login_error_profile_not_found
import pantrypal.composeapp.generated.resources.remote_login_error_server_url_empty
import pantrypal.composeapp.generated.resources.remote_login_error_unreachable
import pantrypal.composeapp.generated.resources.remote_login_error_username_empty
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class RemoteLoginViewModel(
    private val authenticationService: AuthenticationService,
    private val profileRepository: ProfileRepository,
    private val preferencesRepository: AuthenticationPreferencesRepository,
    private val connectionNetworkService: ConnectionNetworkService,
) : ViewModel() {
    private val loginTag = "RemoteLogin"

    val state = MutableStateFlow(UiState.success(RemoteLogin()))

    private fun updateState(block: (RemoteLogin) -> RemoteLogin) {
        state.update { currentUiState ->
            val currentData = currentUiState.data ?: return@update currentUiState
            val nextData = block(currentData)
            
            val isServerUrlValid = nextData.serverUrl.isNotBlank() && nextData.serverUrlError == null
            val isUsernameValid = nextData.username.isNotBlank() && nextData.usernameError == null
            val isPasswordValid = nextData.password.isNotBlank() && nextData.passwordError == null
            
            val canSubmit = isServerUrlValid && isUsernameValid && isPasswordValid

            currentUiState.copy(data = nextData.copy(canSubmit = canSubmit), error = null)
        }
    }

    fun onUsernameChanged(username: String) {
        val error = if (username.isBlank()) Res.string.remote_login_error_username_empty else null
        updateState { it.copy(username = username, usernameError = error) }
    }

    fun onPasswordChanged(password: String) {
        val error = if (password.isBlank()) Res.string.remote_login_error_password_empty else null
        updateState { it.copy(password = password, passwordError = error) }
    }

    fun onServerUrlChanged(serverUrl: String) {
        val error = if (serverUrl.isBlank()) {
            Res.string.remote_login_error_server_url_empty
        } else if (!isValidUri(serverUrl)) {
            Res.string.remote_login_error_invalid_server_url
        } else {
            null
        }
        updateState { it.copy(serverUrl = serverUrl, serverUrlError = error, connectionTestSuccess = null) }
    }

    private fun isValidUri(uri: String): Boolean {
        return try {
            val regex = "^https?://[-a-zA-Z0-9+&@/%~_|!:,.;]*[-a-zA-Z0-9+&@/%=~_|]".toRegex()
            regex.matches(uri)
        } catch (e: Exception) {
            false
        }
    }

    fun testConnection() {
        val serverUrl = state.value.data?.serverUrl ?: return
        if (serverUrl.isBlank()) return

        viewModelScope.launch {
            updateState { it.copy(isTestingConnection = true, connectionTestSuccess = null) }
            val result = connectionNetworkService.testConnection(serverUrl)
            updateState { it.copy(isTestingConnection = false, connectionTestSuccess = result.isSuccess) }
        }
    }

    fun login(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val uiState = state.value
            val data = uiState.data ?: return@launch
            if (!data.canSubmit) return@launch

            val localProfileId = preferencesRepository.authenticationPreferencesFlow.map { it.localProfileId }.firstOrNull() ?: ""
            if (localProfileId.isBlank()) return@launch
            val existingProfile = profileRepository.getProfileById(Uuid.parse(localProfileId)).firstOrNull() ?: return@launch

            try {
                state.emit(UiState.loading())

                val result = authenticationService.loginRemotely(data.username, data.password, data.serverUrl)
                if (result.isSuccess) {
                    val response = result.getOrNull()
                    if (response != null) {
                        // Update profile sync timestamp
                        profileRepository.upsert(existingProfile.copy(
                            lastSyncedAt = Clock.System.now()
                        ))

                        profileRepository.upsertRemoteProfile(RemoteProfile(
                            localProfileId = existingProfile.id,
                            serverUrl = data.serverUrl,
                            serverId = Uuid.parse(response.user.id),
                            username = response.user.username,
                            email = response.user.email,
                            lastSyncedAt = Clock.System.now()
                        ))
                    }
                    state.emit(UiState.success(data))
                    onSuccess()
                } else {
                    handleError(result.exceptionOrNull(), uiState)
                }
            } catch (exception: Exception) {
                Logger.withTag(loginTag).e(exception) { "Error logging in remotely: ${data.username}" }
                state.emit(uiState.copy(error = Res.string.remote_login_error_generic))
            }
        }
    }

    private suspend fun handleError(exception: Throwable?, uiState: UiState<RemoteLogin>) {
        val errorRes = when (exception) {
            is InvalidCredentialsException -> Res.string.remote_login_error_credentials
            is ProfileNotFoundException -> Res.string.remote_login_error_profile_not_found
            is ServerUnreachableException -> Res.string.remote_login_error_unreachable
            else -> Res.string.remote_login_error_generic
        }
        state.emit(uiState.copy(error = errorRes))
    }
}
