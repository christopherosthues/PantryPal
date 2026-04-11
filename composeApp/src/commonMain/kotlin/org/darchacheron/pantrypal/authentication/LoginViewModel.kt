package org.darchacheron.pantrypal.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.darchacheron.pantrypal.navigation.Navigator
import org.darchacheron.pantrypal.profile.Profile
import org.darchacheron.pantrypal.profile.ProfileRepository
import org.darchacheron.pantrypal.ui.UiState
import pantrypal.composeapp.generated.resources.Res
import pantrypal.composeapp.generated.resources.login_error
import pantrypal.composeapp.generated.resources.login_error_invalid_server_url
import pantrypal.composeapp.generated.resources.login_error_profile_not_found
import pantrypal.composeapp.generated.resources.login_error_server_unreachable
import pantrypal.composeapp.generated.resources.login_error_server_url_missing
import pantrypal.composeapp.generated.resources.login_wrong_username_or_password
import pantrypal.composeapp.generated.resources.registration_error_empty_password
import pantrypal.composeapp.generated.resources.registration_error_empty_username
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class LoginViewModel(
    private val authenticationService: AuthenticationService,
    private val profileRepository: ProfileRepository,
    private val preferencesRepository: AuthenticationPreferencesRepository,
    private val navigator: Navigator,
) : ViewModel() {
    private val loginTag = "Login"

    val loginState = MutableStateFlow(UiState.success(Login(username = "", password = "")))

    fun onUsernameChanged(username: String) {
        val error = if (username.isBlank()) Res.string.registration_error_empty_username else null
        loginState.value = loginState.value.copy(data = loginState.value.data?.copy(username = username, usernameError = error))
    }

    fun onPasswordChanged(password: String) {
        val error = if (password.isBlank()) Res.string.registration_error_empty_password else null
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
            Res.string.login_error_server_url_missing
        } else if (!isValidUri(serverUrl)) {
            Res.string.login_error_invalid_server_url
        } else {
            null
        }
        loginState.value = loginState.value.copy(data = loginState.value.data?.copy(serverUrl = serverUrl, serverUrlError = error))
    }

    private fun isValidUri(uri: String): Boolean {
        return try {
            val regex = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]".toRegex()
            regex.matches(uri)
        } catch (e: Exception) {
            false
        }
    }

    fun onRemoteUsernameChanged(remoteUsername: String) {
        val error = if (remoteUsername.isBlank()) Res.string.registration_error_empty_username else null
        loginState.value = loginState.value.copy(data = loginState.value.data?.copy(remoteUsername = remoteUsername, remoteUsernameError = error))
    }

    fun onRemotePasswordChanged(remotePassword: String) {
        val error = if (remotePassword.isBlank()) Res.string.registration_error_empty_password else null
        loginState.value = loginState.value.copy(data = loginState.value.data?.copy(remotePassword = remotePassword, remotePasswordError = error))
    }

    fun login(onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            val uiState = loginState.value
            val data = uiState.data ?: return@launch
            val localUsername = data.username
            val localPassword = data.password

            try {
                loginState.emit(UiState.loading())

                // 1. Try to find local profile
                val existingProfile = profileRepository.getProfileByIdentifier(localUsername).firstOrNull()

                // If profile exists locally, we MUST verify the local password first
                if (existingProfile != null) {
                    if (!verifyPassword(localPassword, existingProfile.passwordHash)) {
                        loginState.emit(uiState.copy(error = Res.string.login_wrong_username_or_password))
                        return@launch
                    }
                }

                // 2. Remote Login logic
                if (data.loginRemotely) {
                    val serverUrl = existingProfile?.serverUrl ?: data.serverUrl

                    if (serverUrl.isBlank()) {
                        loginState.emit(uiState.copy(error = Res.string.login_error_server_url_missing))
                        return@launch
                    }

                    val remoteUsername = if (data.useSameCredentials) localUsername else data.remoteUsername
                    val remotePassword = if (data.useSameCredentials) localPassword else data.remotePassword

                    val authResult = authenticationService.login(remoteUsername, remotePassword, serverUrl)

                    if (authResult.isSuccess) {
                        val loginResponse = authResult.getOrNull()
                        if (loginResponse != null) {
                            val serverIdFromToken = JwtUtils.getUserIdFromToken(loginResponse.tokenResponse.accessToken)
                            val serverUuid = serverIdFromToken?.let { Uuid.parse(it) } ?: Uuid.parse(loginResponse.user.id)

                            val profile = existingProfile?.copy(
                                serverId = serverUuid,
                                username = loginResponse.user.username,
                                email = loginResponse.user.email,
                                serverUrl = serverUrl,
                                isLocalOnly = false
                            ) ?: Profile(
                                serverId = serverUuid,
                                username = loginResponse.user.username,
                                email = loginResponse.user.email,
                                serverUrl = serverUrl,
                                passwordHash = hashPassword(localPassword),
                                createdAt = Clock.System.now(),
                                lastModifiedAt = Clock.System.now(),
                                isLocalOnly = false
                            )
                            profileRepository.upsert(profile)

                            // Mark this profile as current
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
                        if (onSuccess != null) {
                            onSuccess()
                        } else {
                            navigator.goToMain()
                        }
                    } else {
                        val errorRes = when (authResult.exceptionOrNull()) {
                            is InvalidCredentialsException -> Res.string.login_wrong_username_or_password
                            is ProfileNotFoundException -> Res.string.login_error_profile_not_found
                            is ServerUnreachableException -> Res.string.login_error_server_unreachable
                            else -> Res.string.login_error
                        }
                        loginState.emit(uiState.copy(error = errorRes))
                    }
                } else {
                    // Local only login
                    if (existingProfile != null) {
                        // verifyPassword already checked above
                        loginLocally(existingProfile, localUsername, localPassword, onSuccess)
                    } else {
                        // No local profile and no remote login requested
                        loginState.emit(uiState.copy(error = Res.string.login_wrong_username_or_password))
                    }
                }
            } catch (exception: Exception) {
                Logger.withTag(loginTag).e(exception) { "Error login user: $localUsername" }
                loginState.emit(uiState.copy(error = Res.string.login_error))
            }
        }
    }

    private suspend fun loginLocally(profile: Profile, username: String, password: String, onSuccess: (() -> Unit)?) {
        // Mark this profile as current
        preferencesRepository.updateAccessPreferences(
            accessToken = "",
            refreshToken = "",
            expiresIn = 0,
            refreshExpiresIn = 0,
            localProfileId = profile.id.toString(),
            isLoggedInRemotely = false,
            serverUrl = profile.serverUrl ?: ""
        )
        loginState.emit(UiState.success(Login(username, password)))
        if (onSuccess != null) {
            onSuccess()
        } else {
            navigator.goToMain()
        }
    }

    fun openRegister() {
        navigator.goToRegister()
    }
}
