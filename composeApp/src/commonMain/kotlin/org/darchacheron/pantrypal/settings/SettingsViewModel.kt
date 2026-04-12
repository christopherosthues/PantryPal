package org.darchacheron.pantrypal.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.darchacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darchacheron.pantrypal.food.FoodRepository
import org.darchacheron.pantrypal.inventory.InventoryRepository
import org.darchacheron.pantrypal.navigation.Navigator
import org.darchacheron.pantrypal.profile.ProfileRepository
import org.darchacheron.pantrypal.ui.UiState
import pantrypal.composeapp.generated.resources.Res
import pantrypal.composeapp.generated.resources.settings_error_loading
import pantrypal.composeapp.generated.resources.settings_error_saving
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val foodRepository: FoodRepository,
    private val inventoryRepository: InventoryRepository,
    private val profileRepository: ProfileRepository,
    private val authenticationPreferencesRepository: AuthenticationPreferencesRepository,
    private val navigator: Navigator
) : ViewModel() {
    // TODO: ensure that there is a remote profile for data synchronization

    private val _settingsFlow = MutableStateFlow<UiState<Settings>>(UiState.loading())
    val settingsFlow: StateFlow<UiState<Settings>> = _settingsFlow.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _showLoginDialog = MutableStateFlow(false)
    val showLoginDialog: StateFlow<Boolean> = _showLoginDialog.asStateFlow()

    private val _shouldClose = MutableStateFlow(false)
    val shouldClose: StateFlow<Boolean> = _shouldClose.asStateFlow()

    private var _closeAfterLogin = false

    val isLoggedInRemotely: StateFlow<Boolean> = authenticationPreferencesRepository.authenticationPreferencesFlow
        .map { it.isLoggedInRemotely }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private var _originalSettings: Settings = Settings()

    private val loggerTag = "Settings"

    init {
        viewModelScope.launch {
            settingsRepository.getSettingsFlow()
                .onStart { _settingsFlow.value = UiState.loading() }
                .catch {
                    Logger.withTag(loggerTag).e { "Error loading settings: ${it.message}" }
                    _settingsFlow.value = UiState.error(Res.string.settings_error_loading)
                }
                .collect { settings ->
                    _originalSettings = settings
                    _settingsFlow.value = UiState.success(settings)
                }
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            val prefs = authenticationPreferencesRepository.authenticationPreferencesFlow.first()
            if (!prefs.isLoggedInRemotely) {
                if (prefs.serverUrl.isNotBlank()) {
                    _closeAfterLogin = false
                    _showLoginDialog.value = true
                    return@launch
                }
            }

            _isSyncing.value = true
            try {
                foodRepository.syncWithServer()
                inventoryRepository.syncWithServer()
                profileRepository.syncWithServer()
            } catch (e: Exception) {
                Logger.withTag(loggerTag).e { "Manual sync failed: ${e.message}" }
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun onDismissLoginDialog() {
        _showLoginDialog.value = false
        _closeAfterLogin = false
    }

    fun onLoginSuccess() {
        _showLoginDialog.value = false
        triggerSync()
        if (_closeAfterLogin) {
            _shouldClose.value = true
        }
        _closeAfterLogin = false
    }

    fun onThemeModeSelected(mode: ThemeMode) {
        _settingsFlow.update { state ->
            state.data?.let { UiState.success(it.copy(themeMode = mode)) } ?: state
        }
    }

    fun onDataSynchronizationSelected(dataSynchronization: DataSynchronization) {
        _settingsFlow.update { state ->
            state.data?.let { UiState.success(it.copy(dataSynchronization = dataSynchronization)) } ?: state
        }
    }


    @OptIn(ExperimentalUuidApi::class)
    fun saveSettings(onSuccess: () -> Unit) {
        val currentSettings = settingsFlow.value.data ?: return
        viewModelScope.launch {
            try {
                val syncEnabled = currentSettings.dataSynchronization != DataSynchronization.NO_SYNCHRONIZATION

                settingsRepository.saveSettings(currentSettings)

                var authRequired = false
                if (syncEnabled) {
                    val prefs = authenticationPreferencesRepository.authenticationPreferencesFlow.first()
                    val profileId = prefs.localProfileId
                    val syncJustEnabled = _originalSettings.dataSynchronization == DataSynchronization.NO_SYNCHRONIZATION

                    if (syncJustEnabled) {
                        if (profileId.isNotEmpty()) {
                            val profile = profileRepository.getProfileById(Uuid.parse(profileId)).first()
                            if (profile?.serverId == null || profile.isLocalOnly || !prefs.isLoggedInRemotely) {
                                authRequired = true
                                _closeAfterLogin = true
                                _showLoginDialog.value = true
                            }
                        } else {
                            authRequired = true
                            navigator.goToLogin()
                        }
                    }
                }

                if (!authRequired) {
                    onSuccess()
                }
            } catch (e: Exception) {
                Logger.withTag(loggerTag).e { "Error saving settings: ${e.message}" }
                _settingsFlow.update { it.copy(error = Res.string.settings_error_saving) }
            }
        }
    }

    fun revertChanges() {
        _settingsFlow.value = UiState.success(_originalSettings)
    }

    fun resetToDefaults() {
        _settingsFlow.value = UiState.success(Settings())
    }
}
