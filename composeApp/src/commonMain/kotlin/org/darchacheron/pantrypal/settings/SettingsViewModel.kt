package org.darchacheron.pantrypal.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.darchacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darchacheron.pantrypal.food.FoodRepository
import org.darchacheron.pantrypal.inventory.InventoryRepository
import org.darchacheron.pantrypal.navigation.Navigator
import org.darchacheron.pantrypal.profile.ProfileRepository
import org.darchacheron.pantrypal.ui.UiState
import pantrypal.composeapp.generated.resources.Res
import pantrypal.composeapp.generated.resources.settings_error_loading
import pantrypal.composeapp.generated.resources.settings_error_saving

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

    fun onThemeModeSelected(mode: ThemeMode) {
        _settingsFlow.update { state ->
            state.data?.let { UiState.success(it.copy(themeMode = mode)) } ?: state
        }
    }

    fun onDataSynchronizationSelected(dataSynchronization: DataSynchronization) {
        val currentSettings = _settingsFlow.value.data ?: return
        if (currentSettings.serverUrl.isBlank() && dataSynchronization != DataSynchronization.NO_SYNCHRONIZATION) {
            return
        }
        _settingsFlow.update { state ->
            state.data?.let { UiState.success(it.copy(dataSynchronization = dataSynchronization)) } ?: state
        }
    }

    fun onServerUrlChanged(serverUrl: String) {
        _settingsFlow.update { state ->
            state.data?.let {
                val urlToSet = serverUrl.trim()
                val newSync = if (urlToSet.isBlank()) DataSynchronization.NO_SYNCHRONIZATION else it.dataSynchronization
                UiState.success(it.copy(serverUrl = urlToSet, dataSynchronization = newSync))
            } ?: state
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun saveSettings(onSuccess: () -> Unit) {
        val currentSettings = settingsFlow.value.data ?: return
        viewModelScope.launch {
            try {
                val serverUrlChanged = currentSettings.serverUrl != _originalSettings.serverUrl
                val syncEnabled = currentSettings.dataSynchronization != DataSynchronization.NO_SYNCHRONIZATION

                settingsRepository.saveSettings(currentSettings)

                if (serverUrlChanged && syncEnabled) {
                    val prefs = authenticationPreferencesRepository.authenticationPreferencesFlow.first()
                    val profileId = prefs.localProfileId
                    if (profileId.isNotEmpty()) {
                        val profile = profileRepository.getProfileById(Uuid.parse(profileId)).first()
                        if (profile?.serverId == null || profile.isLocalOnly || !prefs.isLoggedInRemotely) {
                            // No remote profile, disconnected, or only logged in locally.
                            // Must login/register on the new server.
                            navigator.goToLogin()
                        }
                    } else {
                        navigator.goToLogin()
                    }
                } else if (!serverUrlChanged && syncEnabled && _originalSettings.dataSynchronization == DataSynchronization.NO_SYNCHRONIZATION) {
                    // Sync was just enabled for the same server. Check if we have a remote profile and are logged in.
                    val prefs = authenticationPreferencesRepository.authenticationPreferencesFlow.first()
                    val profileId = prefs.localProfileId
                    if (profileId.isNotEmpty()) {
                        val profile = profileRepository.getProfileById(Uuid.parse(profileId)).first()
                        if (profile?.serverId == null || profile.isLocalOnly || !prefs.isLoggedInRemotely) {
                            navigator.goToLogin()
                        }
                    } else {
                        navigator.goToLogin()
                    }
                }

                onSuccess()
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
