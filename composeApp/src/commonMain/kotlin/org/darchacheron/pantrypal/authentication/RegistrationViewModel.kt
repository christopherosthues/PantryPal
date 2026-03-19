package org.darchacheron.pantrypal.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.darchacheron.pantrypal.navigation.Navigator
import org.darchacheron.pantrypal.ui.UiState
import pantrypal.composeapp.generated.resources.Res
import pantrypal.composeapp.generated.resources.registration_error

data class Registration(
    val userName: String,
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String
)

class RegistrationViewModel(
    private val authenticationService: AuthenticationService,
    private val navigator: Navigator
): ViewModel() {
    val registrationState = MutableStateFlow(UiState.success(Registration("", "", "", "", "")))

    fun onUserNameChanged(userName: String) {
        registrationState.value = registrationState.value.copy(data = registrationState.value.data?.copy(userName = userName))
    }

    fun onEmailChanged(email: String) {
        registrationState.value = registrationState.value.copy(data = registrationState.value.data?.copy(email = email))
    }

    fun onPasswordChanged(password: String) {
        registrationState.value = registrationState.value.copy(data = registrationState.value.data?.copy(password = password))
    }

    fun onFirstNameChanged(firstName: String) {
        registrationState.value = registrationState.value.copy(data = registrationState.value.data?.copy(firstName = firstName))
    }

    fun onLastNameChanged(lastName: String) {
        registrationState.value = registrationState.value.copy(data = registrationState.value.data?.copy(lastName = lastName))
    }

    fun register() {
        val uiState = registrationState.value
        val userName = uiState.data?.userName ?: ""
        val email = uiState.data?.email ?: ""
        val password = uiState.data?.password ?: ""
        val firstName = uiState.data?.firstName ?: ""
        val lastName = uiState.data?.lastName ?: ""

        viewModelScope.launch {
            try {
                registrationState.emit(UiState.loading())
                val result = authenticationService.registerUser(userName, email, password, firstName, lastName)

                if (result.isFailure) {
                    registrationState.emit(UiState.error(Res.string.registration_error))
                    return@launch
                } else if (result.isSuccess) {
                    if (result.getOrDefault(false)) {
                        registrationState.emit(
                            UiState.success(
                                Registration(
                                    userName,
                                    email,
                                    password,
                                    firstName,
                                    lastName
                                )
                            )
                        )
                        navigator.goToLogin()
                    } else {
                        registrationState.emit(UiState.error(Res.string.registration_error))
                    }
                }
            } catch (exception: Exception) {
                registrationState.emit(UiState.error(Res.string.registration_error))
            }
        }
    }
}