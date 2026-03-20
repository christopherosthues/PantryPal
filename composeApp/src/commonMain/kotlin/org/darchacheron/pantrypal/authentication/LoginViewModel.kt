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
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class Login(val username: String, val password: String)

@OptIn(ExperimentalUuidApi::class)
class LoginViewModel(
    private val authenticationService: AuthenticationService,
    private val profileRepository: ProfileRepository,
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
                            val serverUuid = Uuid.parse(loginResponse.user.id)
                            
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
                        }
                        loginState.emit(UiState.success(Login(username, password)))
                        navigator.goToFoodList()
                    } else {
                        loginState.emit(UiState.error(Res.string.login_error))
                    }
                } else {
                    // Local only mode: Try to find local profile by username
                    val existingProfile = profileRepository.getProfileByUsername(username).firstOrNull()
                    
                    if (existingProfile != null) {
                        val inputHash = hashPassword(password)
                        if (existingProfile.passwordHash == inputHash) {
                            loginState.emit(UiState.success(Login(username, password)))
                            navigator.goToFoodList()
                        } else {
                            loginState.emit(UiState.error(Res.string.login_error))
                        }
                    } else {
                        loginState.emit(UiState.error(Res.string.login_error))
                    }
                }
            } catch (exception: Exception) {
                Logger.withTag(loginTag).e(exception) { "Error login user: $username" }
                loginState.emit(UiState.error(Res.string.login_error))
            }
        }
    }

    private fun hashPassword(password: String): String {
        // Dummy hash for demonstration. Use a secure library in production.
        // TODO: use something like scrypt, bcrypt or argon2id
        return password.reversed()
    }

    fun openRegister() {
        navigator.goToRegister()
    }
}
