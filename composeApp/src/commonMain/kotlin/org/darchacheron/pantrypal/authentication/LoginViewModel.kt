package org.darchacheron.pantrypal.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.darchacheron.pantrypal.navigation.Navigator
import org.darchacheron.pantrypal.ui.UiState
import pantrypal.composeapp.generated.resources.Res
import pantrypal.composeapp.generated.resources.login_error

data class Login(val username: String, val password: String)

class LoginViewModel(
    private val authenticationService: AuthenticationService,
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

    fun login(username: String, password: String) {
        viewModelScope.launch {
            try {
                loginState.emit(UiState.loading())
                val result = authenticationService.login(username, password)

                if (result.isFailure) {
                    // TODO: specific error message
                    loginState.emit(UiState.error(Res.string.login_error))
                    return@launch
                } else if (result.isSuccess) {
                    if (result.getOrDefault(false)) {
                        loginState.emit(UiState.success(Login(username, password)))
                        navigator.goToFoodList()
                    } else {
                        // TODO: error message Login failed
                        loginState.emit(UiState.error(Res.string.login_error))
                    }
                }
            } catch (exception: Exception) {
                Logger.withTag(loginTag).e(exception) { "Error login user: $username" }
                loginState.emit(UiState.error(Res.string.login_error))
            }
        }
    }

    fun openRegister() {
        navigator.goToRegister()
    }
}