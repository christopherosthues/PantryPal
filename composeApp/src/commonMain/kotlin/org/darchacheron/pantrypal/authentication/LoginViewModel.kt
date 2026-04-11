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
import pantrypal.composeapp.generated.resources.login_error_profile_not_found
import pantrypal.composeapp.generated.resources.login_error_server_unreachable
import pantrypal.composeapp.generated.resources.login_wrong_username_or_password
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

    val loginState = MutableStateFlow(UiState.success(Login(username = "", password = "", isLocalOnly = false)))

    fun onUsernameChanged(username: String) {
        loginState.value = loginState.value.copy(data = loginState.value.data?.copy(username = username))
    }

    fun onPasswordChanged(password: String) {
        loginState.value = loginState.value.copy(data = loginState.value.data?.copy(password = password))
    }

    fun onLocalOnlyChanged(isLocalOnly: Boolean) {
        loginState.value = loginState.value.copy(data = loginState.value.data?.copy(isLocalOnly = isLocalOnly))
    }

    fun login(onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            val uiState = loginState.value
            val username = uiState.data?.username ?: ""
            val password = uiState.data?.password ?: ""
            val isLocalOnlyRequested = uiState.data?.isLocalOnly ?: false

            try {
                loginState.emit(UiState.loading())
                
                if (authenticationService.isRemoteEnabled() && !isLocalOnlyRequested) {
                    val result = authenticationService.login(username, password)

                    if (result.isSuccess) {
                        val loginResponse = result.getOrNull()
                        if (loginResponse != null) {
                            val serverIdFromToken = JwtUtils.getUserIdFromToken(loginResponse.tokenResponse.accessToken)
                            val serverUuid = serverIdFromToken?.let { Uuid.parse(it) } ?: Uuid.parse(loginResponse.user.id)
                            
                            val existingProfile = profileRepository.getProfileByServerId(serverUuid).firstOrNull()
                                ?: profileRepository.getProfileByIdentifier(loginResponse.user.username).firstOrNull()
                                ?: profileRepository.getProfileByIdentifier(loginResponse.user.email).firstOrNull()
                            
                            val profile = existingProfile?.copy(
                                serverId = serverUuid,
                                username = loginResponse.user.username,
                                email = loginResponse.user.email,
                                isLocalOnly = false
                            ) ?: Profile(
                                id = Uuid.generateV7(),
                                serverId = serverUuid,
                                username = loginResponse.user.username,
                                email = loginResponse.user.email,
                                createdAt = Clock.System.now(),
                                lastModifiedAt = Clock.System.now(),
                                lastSyncedAt = null,
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
                                serverUrl = authenticationService.getServerUrl()
                            )
                        }
                        loginState.emit(UiState.success(Login(username, password)))
                        if (onSuccess != null) {
                            onSuccess()
                        } else {
                            navigator.goToMain()
                        }
                    } else {
                        val errorRes = when (result.exceptionOrNull()) {
                            is InvalidCredentialsException -> Res.string.login_wrong_username_or_password
                            is ProfileNotFoundException -> Res.string.login_error_profile_not_found
                            is ServerUnreachableException -> Res.string.login_error_server_unreachable
                            else -> Res.string.login_error
                        }
                        loginState.emit(uiState.copy(error = errorRes))
                    }
                } else {
                    // Local only mode: Try to find local profile by username or email
                    val existingProfile = profileRepository.getProfileByIdentifier(username).firstOrNull()
                    
                    if (existingProfile != null) {
                        if (verifyPassword(password, existingProfile.passwordHash)) {
                            // Mark this profile as current
                            preferencesRepository.updateAccessPreferences(
                                accessToken = "",
                                refreshToken = "",
                                expiresIn = 0,
                                refreshExpiresIn = 0,
                                localProfileId = existingProfile.id.toString(),
                                isLoggedInRemotely = false,
                                serverUrl = ""
                            )
                            loginState.emit(UiState.success(Login(username, password)))
                            if (onSuccess != null) {
                                onSuccess()
                            } else {
                                navigator.goToMain()
                            }
                        } else {
                            loginState.emit(uiState.copy(error = Res.string.login_wrong_username_or_password))
                        }
                    } else {
                        loginState.emit(uiState.copy(error = Res.string.login_wrong_username_or_password))
                    }
                }
            } catch (exception: Exception) {
                Logger.withTag(loginTag).e(exception) { "Error login user: $username" }
                loginState.emit(uiState.copy(error = Res.string.login_error))
            }
        }
    }

    fun openRegister() {
        navigator.goToRegister()
    }
}
