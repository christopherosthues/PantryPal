package org.darchacheron.pantrypal.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.darchacheron.pantrypal.navigation.Navigator
import org.darchacheron.pantrypal.profile.Profile
import org.darchacheron.pantrypal.profile.ProfileRepository
import org.darchacheron.pantrypal.ui.UiState
import pantrypal.composeapp.generated.resources.Res
import pantrypal.composeapp.generated.resources.registration_error
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class Registration(
    val userName: String,
    val email: String,
    val password: String,
)

@OptIn(ExperimentalUuidApi::class)
class RegistrationViewModel(
    private val authenticationService: AuthenticationService,
    private val profileRepository: ProfileRepository,
    private val navigator: Navigator
): ViewModel() {
    val registrationState = MutableStateFlow(UiState.success(Registration("", "", "")))

    fun onUserNameChanged(userName: String) {
        registrationState.value = registrationState.value.copy(data = registrationState.value.data?.copy(userName = userName))
    }

    fun onEmailChanged(email: String) {
        registrationState.value = registrationState.value.copy(data = registrationState.value.data?.copy(email = email))
    }

    fun onPasswordChanged(password: String) {
        registrationState.value = registrationState.value.copy(data = registrationState.value.data?.copy(password = password))
    }

    fun register() {
        val uiState = registrationState.value
        val userName = uiState.data?.userName ?: ""
        val email = uiState.data?.email ?: ""
        val password = uiState.data?.password ?: ""

        viewModelScope.launch {
            try {
                registrationState.emit(UiState.loading())
                
                // Check for local conflict first in both modes
                val existingLocal = profileRepository.getProfileByUsername(userName).firstOrNull()
                if (existingLocal != null) {
                    registrationState.emit(UiState.error(Res.string.registration_error))
                    return@launch
                }

                if (authenticationService.isRemoteEnabled()) {
                    val result = authenticationService.registerUser(userName, email, password)

                    if (result.isSuccess && result.getOrDefault(false)) {
                        // Create local profile too for remote registration
                        createLocalProfile(userName, email, password)
                        registrationState.emit(UiState.success(Registration(userName, email, password)))
                        navigator.goToLogin()
                    } else {
                        registrationState.emit(UiState.error(Res.string.registration_error))
                    }
                } else {
                    // Local only mode
                    createLocalProfile(userName, email, password)
                    registrationState.emit(UiState.success(Registration(userName, email, password)))
                    navigator.goToLogin()
                }
            } catch (exception: Exception) {
                registrationState.emit(UiState.error(Res.string.registration_error))
            }
        }
    }

    private suspend fun createLocalProfile(userName: String, email: String, password: String) {
        val profile = Profile(
            id = Uuid.generateV7(),
            serverId = null,
            username = userName,
            email = email,
            passwordHash = hashPassword(password),
            createdAt = Clock.System.now()
        )
        profileRepository.upsert(profile)
    }

    private fun hashPassword(password: String): String {
        // Dummy hash for demonstration. Use a secure library in production.
        // TODO: use something like scrypt, bcrypt or argon2id
        return password.reversed() 
    }
}
