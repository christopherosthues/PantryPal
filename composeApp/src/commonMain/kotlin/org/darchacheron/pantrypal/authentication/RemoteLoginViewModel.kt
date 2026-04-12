package org.darchacheron.pantrypal.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.darchacheron.pantrypal.profile.ProfileRepository
import org.darchacheron.pantrypal.ui.UiState
import org.jetbrains.compose.resources.StringResource
import pantrypal.composeapp.generated.resources.Res
import pantrypal.composeapp.generated.resources.remote_login_error_credentials
import pantrypal.composeapp.generated.resources.remote_login_error_generic
import pantrypal.composeapp.generated.resources.remote_login_error_invalid_server_url
import pantrypal.composeapp.generated.resources.remote_login_error_password_empty
import pantrypal.composeapp.generated.resources.remote_login_error_profile_not_found
import pantrypal.composeapp.generated.resources.remote_login_error_server_url_empty
import pantrypal.composeapp.generated.resources.remote_login_error_unreachable
import pantrypal.composeapp.generated.resources.remote_login_error_username_empty
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class RemoteLogin(
    val username: String,
    val password: String,
    val serverUrl: String = "",
    val loginRemotely: Boolean = false,
    val useSameCredentials: Boolean = true,
    val remoteUsername: String = "",
    val remotePassword: String = "",
    val usernameError: StringResource? = null,
    val passwordError: StringResource? = null,
    val serverUrlError: StringResource? = null,
    val remoteUsernameError: StringResource? = null,
    val remotePasswordError: StringResource? = null,
    val canLogin: Boolean = false
)

@OptIn(ExperimentalUuidApi::class)
class RemoteLoginViewModel(
    private val authenticationService: AuthenticationService,
    private val profileRepository: ProfileRepository,
    private val preferencesRepository: AuthenticationPreferencesRepository,
) : ViewModel() {
    private val loginTag = "RemoteLogin"

    val loginState = MutableStateFlow(UiState.success(RemoteLogin(username = "", password = "", loginRemotely = true)))

    fun onUsernameChanged(username: String) {
        val error = if (username.isBlank()) Res.string.remote_login_error_username_empty else null
        loginState.value = loginState.value.copy(data = loginState.value.data?.copy(username = username, usernameError = error))
    }

    fun onPasswordChanged(password: String) {
        val error = if (password.isBlank()) Res.string.remote_login_error_password_empty else null
        loginState.value = loginState.value.copy(data = loginState.value.data?.copy(password = password, passwordError = error))
    }

    fun onLoginRemotelyChanged(loginRemotely: Boolean) {
        loginState.value = loginState.value.copy(data = loginState.value.data?.copy(loginRemotely = loginRemotely))
    }

    fun onUseSameCredentialsChanged(useSameCredentials: Boolean) {
        loginState.value = loginState.value.copy(data = loginState.value.data?.copy(useSameCredentials = useSameCredentials))
    }

    fun onServerUrlChanged(serverUrl: String) {
        val error = if (serverUrl.isBlank()) {
            Res.string.remote_login_error_server_url_empty
        } else if (!isValidUri(serverUrl)) {
            Res.string.remote_login_error_invalid_server_url
        } else {
            null
        }
        loginState.value = loginState.value.copy(data = loginState.value.data?.copy(serverUrl = serverUrl, serverUrlError = error))
    }

    private fun isValidUri(uri: String): Boolean {
        return try {
            val regex = "^(https?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]".toRegex()
            regex.matches(uri)
        } catch (e: Exception) {
            false
        }
    }

    fun onRemoteUsernameChanged(remoteUsername: String) {
        val error = if (remoteUsername.isBlank()) Res.string.remote_login_error_username_empty else null
        loginState.value = loginState.value.copy(data = loginState.value.data?.copy(remoteUsername = remoteUsername, remoteUsernameError = error))
    }

    fun onRemotePasswordChanged(remotePassword: String) {
        val error = if (remotePassword.isBlank()) Res.string.remote_login_error_password_empty else null
        loginState.value = loginState.value.copy(data = loginState.value.data?.copy(remotePassword = remotePassword, remotePasswordError = error))
    }

    fun login(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val uiState = loginState.value
            val data = uiState.data ?: return@launch
            
            val localProfileId = preferencesRepository.authenticationPreferencesFlow.map { it.localProfileId }.firstOrNull() ?: ""
            if (localProfileId.isBlank()) return@launch
            val existingProfile = profileRepository.getProfileById(Uuid.parse(localProfileId)).firstOrNull() ?: return@launch

            val serverUrl = if (data.loginRemotely) data.serverUrl else existingProfile.serverUrl ?: ""

            if (serverUrl.isBlank()) {
                loginState.emit(uiState.copy(error = Res.string.remote_login_error_server_url_empty))
                return@launch
            }

            val remoteUsername = if (data.useSameCredentials) data.username else data.remoteUsername
            val remotePassword = if (data.useSameCredentials) data.password else data.remotePassword

            try {
                loginState.emit(UiState.loading())

                val authResult = authenticationService.login(remoteUsername, remotePassword, serverUrl)

                if (authResult.isSuccess) {
                    val loginResponse = authResult.getOrNull()
                    if (loginResponse != null) {
                        val serverIdFromToken = JwtUtils.getUserIdFromToken(loginResponse.tokenResponse.accessToken)
                        val serverUuid = serverIdFromToken?.let { Uuid.parse(it) } ?: Uuid.parse(loginResponse.user.id)

                        val profile = existingProfile.copy(
                            serverId = serverUuid,
                            username = loginResponse.user.username,
                            email = loginResponse.user.email,
                            serverUrl = serverUrl,
                            isLocalOnly = false
                        )
                        profileRepository.upsert(profile)

                        preferencesRepository.updateAccessPreferences(
                            accessToken = loginResponse.tokenResponse.accessToken,
                            refreshToken = loginResponse.tokenResponse.refreshToken,
                            expiresIn = loginResponse.tokenResponse.expiresIn,
                            refreshExpiresIn = loginResponse.tokenResponse.refreshExpiresIn,
                            localProfileId = profile.id.toString(),
                            isLoggedInRemotely = true,
                            serverUrl = profile.serverUrl ?: ""
                        )
                    }
                    loginState.emit(UiState.success(data))
                    onSuccess()
                } else {
                    val errorRes = when (authResult.exceptionOrNull()) {
                        is InvalidCredentialsException -> Res.string.remote_login_error_credentials
                        is ProfileNotFoundException -> Res.string.remote_login_error_profile_not_found
                        is ServerUnreachableException -> Res.string.remote_login_error_unreachable
                        else -> Res.string.remote_login_error_generic
                    }
                    loginState.emit(uiState.copy(error = errorRes))
                }
            } catch (exception: Exception) {
                Logger.withTag(loginTag).e(exception) { "Error remote login user: ${data.username}" }
                loginState.emit(uiState.copy(error = Res.string.remote_login_error_generic))
            }
        }
    }

}
