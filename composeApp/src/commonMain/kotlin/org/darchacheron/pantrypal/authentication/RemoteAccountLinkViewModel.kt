package org.darchacheron.pantrypal.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.darchacheron.pantrypal.networking.ConnectionNetworkService
import org.darchacheron.pantrypal.profile.ProfileRepository
import org.darchacheron.pantrypal.profile.RemoteProfile
import org.darchacheron.pantrypal.ui.UiState
import pantrypal.composeapp.generated.resources.Res
import pantrypal.composeapp.generated.resources.remote_login_error_credentials
import pantrypal.composeapp.generated.resources.remote_login_error_generic
import pantrypal.composeapp.generated.resources.remote_login_error_invalid_server_url
import pantrypal.composeapp.generated.resources.remote_login_error_password_empty
import pantrypal.composeapp.generated.resources.remote_login_error_profile_not_found
import pantrypal.composeapp.generated.resources.remote_login_error_server_url_empty
import pantrypal.composeapp.generated.resources.remote_login_error_unreachable
import pantrypal.composeapp.generated.resources.remote_login_error_username_empty
import pantrypal.composeapp.generated.resources.registration_error_password_mismatch
import pantrypal.composeapp.generated.resources.registration_error_invalid_email
import pantrypal.composeapp.generated.resources.remote_login_error_user_exists
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class RemoteAccountLinkViewModel(
    private val authenticationService: AuthenticationService,
    private val profileRepository: ProfileRepository,
    private val preferencesRepository: AuthenticationPreferencesRepository,
    private val connectionNetworkService: ConnectionNetworkService,
) : ViewModel() {
    private val loginTag = "RemoteAccountLink"

    val state = MutableStateFlow(UiState.success(RemoteAccountLink()))

    private fun updateState(block: (RemoteAccountLink) -> RemoteAccountLink) {
        state.update { currentUiState ->
            val currentData = currentUiState.data ?: return@update currentUiState
            val nextData = block(currentData)
            
            val isServerUrlValid = nextData.serverUrl.isNotBlank() && nextData.serverUrlError == null
            val isUsernameValid = nextData.username.isNotBlank() && nextData.usernameError == null
            val isPasswordValid = nextData.password.isNotBlank() && nextData.passwordError == null
            
            val canSubmit = if (nextData.isCreatingNew) {
                isServerUrlValid && isUsernameValid && isPasswordValid &&
                        nextData.email.isNotBlank() && nextData.emailError == null &&
                        nextData.repeatedPassword.isNotBlank() && nextData.repeatedPasswordError == null
            } else {
                isServerUrlValid && isUsernameValid && isPasswordValid
            }

            currentUiState.copy(data = nextData.copy(canSubmit = canSubmit), error = null)
        }
    }

    fun onUsernameChanged(username: String) {
        val error = if (username.isBlank()) Res.string.remote_login_error_username_empty else null
        updateState { it.copy(username = username, usernameError = error) }
    }

    fun onEmailChanged(email: String) {
        val error = if (email.isBlank()) {
            Res.string.remote_login_error_username_empty // TODO Reusing for now
        } else if (!isValidEmail(email)) {
            Res.string.registration_error_invalid_email
        } else {
            null
        }
        updateState { it.copy(email = email, emailError = error) }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-z|A-Z]{2,}$".toRegex()
        return emailRegex.matches(email)
    }

    fun onPasswordChanged(password: String) {
        val error = if (password.isBlank()) Res.string.remote_login_error_password_empty else null
        updateState { current ->
            val repeatedError = if (current.isCreatingNew && current.repeatedPassword.isNotEmpty() && password != current.repeatedPassword) {
                Res.string.registration_error_password_mismatch
            } else {
                null
            }
            current.copy(password = password, passwordError = error, repeatedPasswordError = repeatedError)
        }
    }

    fun onRepeatedPasswordChanged(repeated: String) {
        updateState { current ->
            val error = if (current.password != repeated) Res.string.registration_error_password_mismatch else null
            current.copy(repeatedPassword = repeated, repeatedPasswordError = error)
        }
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

    fun setIsCreatingNew(isCreating: Boolean) {
        updateState { it.copy(isCreatingNew = isCreating) }
    }

    fun submit(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val uiState = state.value
            val data = uiState.data ?: return@launch
            if (!data.canSubmit) return@launch

            val localProfileId = preferencesRepository.authenticationPreferencesFlow.map { it.localProfileId }.firstOrNull() ?: ""
            if (localProfileId.isBlank()) return@launch
            val existingProfile = profileRepository.getProfileById(Uuid.parse(localProfileId)).firstOrNull() ?: return@launch

            try {
                state.emit(UiState.loading())

                if (data.isCreatingNew) {
                    val result = authenticationService.registerUser(data.username, data.email, data.password, data.serverUrl)
                    if (result.isSuccess) {
                        val response = result.getOrNull()
                        if (response != null) {
                            val serverIdFromToken = JwtUtils.getUserIdFromToken(response.tokenResponse.accessToken)
                            val serverUuid = serverIdFromToken?.let { Uuid.parse(it) } ?: Uuid.parse(response.user.id)

                            profileRepository.upsert(existingProfile.copy(
                                isLocalOnly = false,
                                lastSyncedAt = Clock.System.now()
                            ))

                            profileRepository.upsertRemoteProfile(RemoteProfile(
                                localProfileId = existingProfile.id,
                                serverUrl = data.serverUrl,
                                serverId = serverUuid,
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
                } else {
                    val result = authenticationService.loginRemotely(data.username, data.password, data.serverUrl)
                    if (result.isSuccess) {
                        val response = result.getOrNull()
                        if (response != null) {
                            val serverIdFromToken = JwtUtils.getUserIdFromToken(response.tokenResponse.accessToken)
                            val serverUuid = serverIdFromToken?.let { Uuid.parse(it) } ?: Uuid.parse(response.user.id)

                            profileRepository.upsert(existingProfile.copy(
                                isLocalOnly = false,
                                lastSyncedAt = Clock.System.now()
                            ))

                            profileRepository.upsertRemoteProfile(RemoteProfile(
                                localProfileId = existingProfile.id,
                                serverUrl = data.serverUrl,
                                serverId = serverUuid,
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
                }
            } catch (exception: Exception) {
                Logger.withTag(loginTag).e(exception) { "Error submitting remote auth for: ${data.username}" }
                try {
                    authenticationService.logoutRemotely()
                } catch (e: Exception) {
                    Logger.withTag(loginTag).w(e) { "Error logging out user ${data.username}" }
                }
                state.emit(uiState.copy(error = Res.string.remote_login_error_generic))
            }
        }
    }

    private suspend fun handleError(exception: Throwable?, uiState: UiState<RemoteAccountLink>) {
        val errorRes = when (exception) {
            is InvalidCredentialsException -> Res.string.remote_login_error_credentials
            is ProfileNotFoundException -> Res.string.remote_login_error_profile_not_found
            is ServerUnreachableException -> Res.string.remote_login_error_unreachable
            is UserAlreadyExistsException -> Res.string.remote_login_error_user_exists
            else -> Res.string.remote_login_error_generic
        }
        state.emit(uiState.copy(error = errorRes))
    }
}
