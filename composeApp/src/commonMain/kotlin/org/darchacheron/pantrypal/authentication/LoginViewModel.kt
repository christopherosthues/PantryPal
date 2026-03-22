package org.darchacheron.pantrypal.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.darchacheron.pantrypal.navigation.Navigator
import org.darchacheron.pantrypal.profile.Profile
import org.darchacheron.pantrypal.profile.ProfileRepository
import org.darchacheron.pantrypal.ui.UiState
import pantrypal.composeapp.generated.resources.Res
import pantrypal.composeapp.generated.resources.login_error
import pantrypal.composeapp.generated.resources.login_wrong_username_or_password
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
data class Login(val username: String, val password: String)

@OptIn(ExperimentalUuidApi::class)
class LoginViewModel(
    private val authenticationService: AuthenticationService,
    private val profileRepository: ProfileRepository,
    private val preferencesRepository: AuthenticationPreferencesRepository,
    private val navigator: Navigator,
) : ViewModel() {
    private val loginTag = "Login"

    val loginState = MutableStateFlow(UiState.success(Login("", "")))

    fun onUsernameChanged(username: String) {
        loginState.value = loginState.value.copy(data = loginState.value.data?.copy(username = username))
    }

    fun onPasswordChanged(password: String) {
        loginState.value = loginState.value.copy(data = loginState.value.data?.copy(password = password))
    }

    fun login() {
        viewModelScope.launch {
            val uiState = loginState.value
            val username = uiState.data?.username ?: ""
            val password = uiState.data?.password ?: ""

            try {
                loginState.emit(UiState.loading())
                
                if (authenticationService.isRemoteEnabled()) {
                    val result = authenticationService.login(username, password)

                    if (result.isSuccess) {
                        val loginResponse = result.getOrNull()
                        if (loginResponse != null) {
                            val serverIdFromToken = JwtUtils.getUserIdFromToken(loginResponse.tokenResponse.accessToken)
                            val serverUuid = serverIdFromToken?.let { Uuid.parse(it) } ?: Uuid.parse(loginResponse.user.id)
                            
                            val existingProfile = profileRepository.getProfileByServerId(serverUuid).firstOrNull()
                            
                            val profile = existingProfile?.copy(
                                username = loginResponse.user.username,
                                email = loginResponse.user.email
                            ) ?: Profile(
                                id = Uuid.generateV7(),
                                serverId = serverUuid,
                                username = loginResponse.user.username,
                                email = loginResponse.user.email,
                                createdAt = Clock.System.now()
                            )
                            profileRepository.upsert(profile)
                            
                            // Mark this profile as current
                            preferencesRepository.updateAccessPreferences(
                                loginResponse.tokenResponse.accessToken,
                                loginResponse.tokenResponse.refreshToken,
                                loginResponse.tokenResponse.expiresIn,
                                loginResponse.tokenResponse.refreshExpiresIn,
                                profile.id.toString()
                            )
                        }
                        loginState.emit(UiState.success(Login(username, password)))
                        navigator.goToMain()
                    } else {
                        loginState.emit(uiState.copy(error = Res.string.login_wrong_username_or_password))
                    }
                } else {
                    // Local only mode: Try to find local profile by username
                    val existingProfile = profileRepository.getProfileByUsername(username).firstOrNull()
                    
                    if (existingProfile != null) {
                        if (verifyPassword(password, existingProfile.passwordHash)) {
                            // Mark this profile as current
                            preferencesRepository.updateAccessPreferences(
                                "", "", 0, 0, existingProfile.id.toString()
                            )
                            loginState.emit(UiState.success(Login(username, password)))
                            navigator.goToMain()
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
