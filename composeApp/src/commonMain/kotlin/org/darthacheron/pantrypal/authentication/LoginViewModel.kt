package org.darthacheron.pantrypal.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.darthacheron.pantrypal.navigation.Navigator
import org.darthacheron.pantrypal.profile.Profile
import org.darthacheron.pantrypal.profile.ProfileRepository
import org.darthacheron.pantrypal.ui.UiState
import pantrypal.composeapp.generated.resources.Res
import pantrypal.composeapp.generated.resources.login_error
import pantrypal.composeapp.generated.resources.login_error_empty_password
import pantrypal.composeapp.generated.resources.login_error_empty_username_or_email
import pantrypal.composeapp.generated.resources.login_error_wrong_username_or_password
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class LoginViewModel(
    private val profileRepository: ProfileRepository,
    private val authenticationService: AuthenticationService,
    private val navigator: Navigator,
) : ViewModel() {
    private val loginTag = "Login"

    val loginState = MutableStateFlow(
        UiState.success(
            Login(
                username = "",
                password = "",
                usernameError = null,
                passwordError = null,
                canLogin = false
            )
        )
    )

    private fun updateLogin(block: (Login) -> Login) {
        loginState.update { currentUiState ->
            val currentData = currentUiState.data ?: return@update currentUiState
            val nextData = block(currentData)
            val finalData = nextData.copy(
                canLogin = nextData.usernameError == null && nextData.username.isNotBlank() &&
                        nextData.passwordError == null && nextData.password.isNotBlank()
            )
            currentUiState.copy(data = finalData, error = null)
        }
    }

    fun onUsernameChanged(username: String) {
        val error = if (username.isBlank()) Res.string.login_error_empty_username_or_email else null
        updateLogin { it.copy(username = username, usernameError = error) }
    }

    fun onPasswordChanged(password: String) {
        val error = if (password.isBlank()) Res.string.login_error_empty_password else null
        updateLogin { it.copy(password = password, passwordError = error) }
    }

    fun onStayLoggedInChanged(stayLoggedIn: Boolean) {
        updateLogin { it.copy(stayLoggedIn = stayLoggedIn) }
    }

    fun login() {
        viewModelScope.launch {
            val uiState = loginState.value
            val data = uiState.data ?: return@launch
            val localUsername = data.username
            val localPassword = data.password
            val stayLoggedIn = data.stayLoggedIn

            try {
                loginState.emit(UiState.loading())

                // 1. Try to find local profile
                val existingProfile = profileRepository.getProfileByIdentifier(localUsername).firstOrNull()

                if (existingProfile != null) {
                    if (verifyPassword(localPassword, existingProfile.passwordHash)) {
                        loginLocally(existingProfile, localUsername, localPassword, stayLoggedIn)
                    } else {
                        loginState.emit(uiState.copy(error = Res.string.login_error_wrong_username_or_password))
                    }
                } else {
                    // No local profile
                    loginState.emit(uiState.copy(error = Res.string.login_error_wrong_username_or_password))
                }
            } catch (exception: Exception) {
                Logger.withTag(loginTag).e(exception) { "Error login user: $localUsername" }
                loginState.emit(uiState.copy(error = Res.string.login_error))
            }
        }
    }

    private suspend fun loginLocally(profile: Profile, username: String, password: String, stayLoggedIn: Boolean) {
        authenticationService.loginLocally(profile.id, stayLoggedIn)
        loginState.emit(UiState.success(Login(username, password, stayLoggedIn = stayLoggedIn)))
        navigator.goToMain()
    }

    fun openRegister() {
        navigator.goToRegister()
    }
}
