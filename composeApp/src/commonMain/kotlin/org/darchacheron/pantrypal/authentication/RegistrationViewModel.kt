package org.darchacheron.pantrypal.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.darchacheron.pantrypal.navigation.Navigator
import org.darchacheron.pantrypal.profile.Profile
import org.darchacheron.pantrypal.profile.ProfileRepository
import org.darchacheron.pantrypal.ui.UiState
import pantrypal.composeapp.generated.resources.Res
import pantrypal.composeapp.generated.resources.registration_error
import pantrypal.composeapp.generated.resources.registration_error_email_exists
import pantrypal.composeapp.generated.resources.registration_error_empty_email
import pantrypal.composeapp.generated.resources.registration_error_empty_password
import pantrypal.composeapp.generated.resources.registration_error_empty_username
import pantrypal.composeapp.generated.resources.registration_error_password_mismatch
import pantrypal.composeapp.generated.resources.registration_error_invalid_email
import pantrypal.composeapp.generated.resources.registration_error_invalid_server_url
import pantrypal.composeapp.generated.resources.registration_error_username_exists
import pantrypal.composeapp.generated.resources.registration_error_server_url_missing
import pantrypal.composeapp.generated.resources.login_error_server_unreachable
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class RegistrationViewModel(
    private val authenticationService: AuthenticationService,
    private val profileRepository: ProfileRepository,
    private val preferencesRepository: AuthenticationPreferencesRepository,
    private val navigator: Navigator
): ViewModel() {
    private val registrationTag = "Registration"

    val registrationState = MutableStateFlow(
        UiState.success(
            Registration(
                userName = "",
                email = "",
                password = "",
                repeatedPassword = "",
                registerRemotely = true,
                userNameError = null,
                emailError = null,
                passwordError = null,
                repeatedPasswordError = null,
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
                        nextData.repeatedPasswordError == null && nextData.repeatedPassword.isNotBlank() &&
                        (nextData.serverUrlError == null || !nextData.registerRemotely)
            )
            currentUiState.copy(data = finalData, error = null)
        }
    }

    fun onUserNameChanged(userName: String) {
        viewModelScope.launch {
            val error = if (userName.isBlank()) Res.string.registration_error_empty_username
            else if (profileRepository.getProfileByUsername(userName).firstOrNull() != null) Res.string.registration_error_username_exists
            else null
            updateRegistration { it.copy(userName = userName, userNameError = error) }
        }
    }

    fun onEmailChanged(email: String) {
        viewModelScope.launch {
            val error = if (email.isBlank()) {
                Res.string.registration_error_empty_email
            } else if (!isValidEmail(email)) {
                Res.string.registration_error_invalid_email
            } else if (profileRepository.getProfileByEmail(email).firstOrNull() != null) {
                Res.string.registration_error_email_exists
            } else {
                null
            }
            updateRegistration { it.copy(email = email, emailError = error) }
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

    fun onRegisterRemotelyChanged(registerRemotely: Boolean) {
        updateRegistration { it.copy(registerRemotely = registerRemotely) }
    }

    fun onServerUrlChanged(serverUrl: String) {
        val error = if (serverUrl.isBlank()) {
            Res.string.registration_error_server_url_missing
        } else if (!isValidUri(serverUrl)) {
            Res.string.registration_error_invalid_server_url
        } else {
            null
        }
        updateRegistration { it.copy(serverUrl = serverUrl, serverUrlError = error) }
    }

    private fun isValidUri(uri: String): Boolean {
        return try {
            val regex = "^(https?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]".toRegex()
            regex.matches(uri)
        } catch (e: Exception) {
            false
        }
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
        val registerRemotely = registrationData.registerRemotely

        viewModelScope.launch {
            try {
                registrationState.emit(UiState.loading())

                if (registerRemotely) {
                    val result = authenticationService.registerUser(userName, email, password, registrationData.serverUrl)

                    if (result.isSuccess && result.getOrNull() != null) {
                        val registrationResponse = result.getOrNull()!!
                        val serverUuid = run {
                            val serverIdFromToken = JwtUtils.getUserIdFromToken(registrationResponse.tokenResponse.accessToken)
                            serverIdFromToken?.let { id -> Uuid.parse(id) } ?: Uuid.parse(registrationResponse.user.id)
                        }

                        // Create/Update local profile for remote registration
                        val localProfile = createLocalProfile(userName, email, password, serverUuid, registrationData.serverUrl)
                        
                        // Set remote login state
                        preferencesRepository.updateAccessPreferences(
                            accessToken = registrationResponse.tokenResponse.accessToken,
                            refreshToken = registrationResponse.tokenResponse.refreshToken,
                            expiresIn = registrationResponse.tokenResponse.expiresIn,
                            refreshExpiresIn = registrationResponse.tokenResponse.refreshExpiresIn,
                            localProfileId = localProfile.id.toString(),
                            isLoggedInRemotely = true,
                            serverUrl = registrationData.serverUrl
                        )
                        
                        registrationState.emit(UiState.success(registrationData.copy(userName = userName, email = email, password = password)))
                        navigator.goToMain()
                    } else {
                        registrationState.emit(uiState.copy(error = Res.string.registration_error))
                    }
                } else {
                    // Local only mode
                    val localProfile = createLocalProfile(userName, email, password)
                    preferencesRepository.updateAccessPreferences(
                        accessToken = "",
                        refreshToken = "",
                        expiresIn = 0,
                        refreshExpiresIn = 0,
                        localProfileId = localProfile.id.toString(),
                        isLoggedInRemotely = false,
                        serverUrl = ""
                    )

                    registrationState.emit(UiState.success(registrationData.copy(userName = userName, email = email, password = password)))
                    navigator.goToMain()
                }
            } catch (exception: Exception) {
                Logger.withTag(registrationTag).e(exception) { "Error registration of user: $userName with email: $email" }
                registrationState.emit(uiState.copy(error = Res.string.registration_error))
            }
        }
    }

    private suspend fun createLocalProfile(userName: String, email: String, password: String, serverId: Uuid? = null, serverUrl: String? = null): Profile {
        val now = Clock.System.now()
        
        val existingProfile = (if (serverId != null) profileRepository.getProfileByServerId(serverId).firstOrNull() else null)
            ?: profileRepository.getProfileByIdentifier(userName).firstOrNull()
            ?: profileRepository.getProfileByIdentifier(email).firstOrNull()

        val profile = existingProfile?.copy(
            serverId = serverId,
            username = userName,
            email = email,
            passwordHash = hashPassword(password),
            serverUrl = serverUrl,
            lastModifiedAt = now,
            lastSyncedAt = if (serverId != null) now else null,
            isLocalOnly = serverId == null
        ) ?: Profile(
            id = Uuid.generateV7(),
            serverId = serverId,
            username = userName,
            email = email,
            passwordHash = hashPassword(password),
            serverUrl = serverUrl,
            createdAt = now,
            lastModifiedAt = now,
            lastSyncedAt = if (serverId != null) now else null,
            isLocalOnly = serverId == null
        )

        profileRepository.upsert(profile)
        return profile
    }
}
