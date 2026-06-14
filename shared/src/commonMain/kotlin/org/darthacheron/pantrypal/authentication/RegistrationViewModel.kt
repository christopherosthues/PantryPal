package org.darthacheron.pantrypal.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.darthacheron.pantrypal.navigation.Navigator
import org.darthacheron.pantrypal.profile.Profile
import org.darthacheron.pantrypal.profile.ProfileRepository
import org.darthacheron.pantrypal.ui.UiState
import pantrypal.shared.generated.resources.Res
import pantrypal.shared.generated.resources.registration_error
import pantrypal.shared.generated.resources.registration_error_email_exists
import pantrypal.shared.generated.resources.registration_error_empty_email
import pantrypal.shared.generated.resources.registration_error_empty_password
import pantrypal.shared.generated.resources.registration_error_empty_username
import pantrypal.shared.generated.resources.registration_error_invalid_email
import pantrypal.shared.generated.resources.registration_error_password_mismatch
import pantrypal.shared.generated.resources.registration_error_username_exists
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class RegistrationViewModel(
    private val profileRepository: ProfileRepository,
    private val authenticationService: AuthenticationService,
    private val navigator: Navigator
): ViewModel() {
    private val registrationTag = "Registration"
    private var userNameValidationJob: Job? = null
    private var emailValidationJob: Job? = null

    val registrationState = MutableStateFlow(
        UiState.success(
            Registration(
                userName = "",
                email = "",
                password = "",
                repeatedPassword = "",
                userNameError = null,
                emailError = null,
                passwordError = null,
                repeatedPasswordError = null,
                stayLoggedIn = false,
                canRegister = false
            )
        )
    )

    private fun updateRegistration(block: (Registration) -> Registration) {
        registrationState.update { currentUiState ->
            val currentData = currentUiState.data ?: return@update currentUiState
            val nextData = block(currentData)
            val finalData = nextData.copy(
                canRegister = nextData.userNameError == null && nextData.userName.isNotBlank() &&
                        nextData.emailError == null && nextData.email.isNotBlank() &&
                        nextData.passwordError == null && nextData.password.isNotBlank() &&
                        nextData.repeatedPasswordError == null && nextData.repeatedPassword.isNotBlank()
            )
            currentUiState.copy(data = finalData, error = null)
        }
    }

    fun onUserNameChanged(userName: String) {
        userNameValidationJob?.cancel()
        val error = if (userName.isBlank()) Res.string.registration_error_empty_username else null
        updateRegistration { it.copy(userName = userName, userNameError = error) }

        if (error == null) {
            userNameValidationJob = viewModelScope.launch {
                delay(300)
                if (profileRepository.getProfileByUsername(userName).firstOrNull() != null) {
                    updateRegistration { it.copy(userNameError = Res.string.registration_error_username_exists) }
                }
            }
        }
    }

    fun onEmailChanged(email: String) {
        emailValidationJob?.cancel()
        val error = if (email.isBlank()) {
            Res.string.registration_error_empty_email
        } else if (!isValidEmail(email)) {
            Res.string.registration_error_invalid_email
        } else {
            null
        }
        updateRegistration { it.copy(email = email, emailError = error) }

        if (error == null) {
            emailValidationJob = viewModelScope.launch {
                delay(300)
                if (profileRepository.getProfileByEmail(email).firstOrNull() != null) {
                    updateRegistration { it.copy(emailError = Res.string.registration_error_email_exists) }
                }
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-z|A-Z]{2,}$".toRegex()
        return emailRegex.matches(email)
    }

    fun onPasswordChanged(password: String) {
        updateRegistration { current ->
            val passwordError = if (password.isBlank()) Res.string.registration_error_empty_password else null
            val repeatedPasswordError = if (current.repeatedPassword.isNotEmpty() && password != current.repeatedPassword) {
                Res.string.registration_error_password_mismatch
            } else if (current.repeatedPassword.isNotEmpty() && password == current.repeatedPassword) {
                null
            } else {
                current.repeatedPasswordError
            }
            current.copy(
                password = password,
                passwordError = passwordError,
                repeatedPasswordError = repeatedPasswordError
            )
        }
    }

    fun onRepeatPasswordChanged(repeatedPassword: String) {
        updateRegistration { current ->
            val error = if (current.password != repeatedPassword) Res.string.registration_error_password_mismatch else null
            current.copy(repeatedPassword = repeatedPassword, repeatedPasswordError = error)
        }
    }

    fun onStayLoggedInChanged(stayLoggedIn: Boolean) {
        updateRegistration { it.copy(stayLoggedIn = stayLoggedIn) }
    }

    fun goToLogin() {
        navigator.goToLogin()
    }

    fun register() {
        val uiState = registrationState.value
        val registrationData = uiState.data ?: return
        if (!registrationData.canRegister) return

        val userName = registrationData.userName
        val email = registrationData.email
        val password = registrationData.password

        viewModelScope.launch {
            registrationState.emit(UiState.loading())

            createLocalProfile(userName, email, password)
                .onSuccess { localProfile ->
                    authenticationService.loginLocally(localProfile.id, registrationData.stayLoggedIn)
                        .onSuccess {
                            registrationState.emit(UiState.success(registrationData.copy(userName = userName, email = email, password = password)))
                            navigator.goToMain()
                        }
                        .onFailure { exception ->
                            Logger.withTag(registrationTag).e(exception) { "Error login user locally after registration" }
                            registrationState.emit(uiState.copy(error = Res.string.registration_error))
                        }
                }
                .onFailure { exception ->
                    Logger.withTag(registrationTag).e(exception) { "Error registration of user: $userName with email: $email" }
                    val errorRes = when (exception) {
                        is UserAlreadyExistsException -> Res.string.registration_error_username_exists
                        else -> Res.string.registration_error
                    }
                    registrationState.emit(uiState.copy(error = errorRes))
                }
        }
    }

    private suspend fun createLocalProfile(userName: String, email: String, password: String): Result<Profile> {
        val existingProfile = profileRepository.getProfileByIdentifier(userName).firstOrNull()
            ?: profileRepository.getProfileByIdentifier(email).firstOrNull()

        if (existingProfile != null) {
            return Result.failure(UserAlreadyExistsException("Profile already exists locally"))
        }

        return runCatching {
            val now = Clock.System.now()
            val profile = Profile(
                id = Uuid.generateV7(),
                username = userName,
                email = email,
                passwordHash = hashPassword(password),
                createdAt = now,
                lastModifiedAt = now,
                isLocalOnly = true
            )

            profileRepository.upsert(profile).getOrThrow()
            profile
        }
    }
}
