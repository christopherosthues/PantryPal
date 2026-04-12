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
import pantrypal.composeapp.generated.resources.login_wrong_username_or_password
import pantrypal.composeapp.generated.resources.registration_error_empty_password
import pantrypal.composeapp.generated.resources.registration_error_empty_username
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class LoginViewModel(
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

    fun login() {
        viewModelScope.launch {
            val uiState = loginState.value
            val data = uiState.data ?: return@launch
            val localUsername = data.username
            val localPassword = data.password

            try {
                loginState.emit(UiState.loading())

                // 1. Try to find local profile
                val existingProfile = profileRepository.getProfileByIdentifier(localUsername).firstOrNull()

                if (existingProfile != null) {
                    if (verifyPassword(localPassword, existingProfile.passwordHash)) {
                        loginLocally(existingProfile, localUsername, localPassword)
                    } else {
                        loginState.emit(uiState.copy(error = Res.string.login_wrong_username_or_password))
                    }
                } else {
                    // No local profile
                    loginState.emit(uiState.copy(error = Res.string.login_wrong_username_or_password))
                }
            } catch (exception: Exception) {
                Logger.withTag(loginTag).e(exception) { "Error login user: $localUsername" }
                loginState.emit(uiState.copy(error = Res.string.login_error))
            }
        }
    }

    private suspend fun loginLocally(profile: Profile, username: String, password: String) {
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
        navigator.goToMain()
    }

    fun openRegister() {
        navigator.goToRegister()
    }
}
