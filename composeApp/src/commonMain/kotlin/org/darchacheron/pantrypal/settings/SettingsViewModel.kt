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
import org.darchacheron.pantrypal.ui.UiState
import pantrypal.composeapp.generated.resources.Res
import pantrypal.composeapp.generated.resources.settings_error_loading
import pantrypal.composeapp.generated.resources.settings_error_saving

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _settingsFlow = MutableStateFlow<UiState<Settings>>(UiState.loading())
    val settingsFlow: StateFlow<UiState<Settings>> = _settingsFlow.asStateFlow()

    private lateinit var _originalSettings: Settings

    init {
        viewModelScope.launch {
            settingsRepository.getSettingsFlow()
                .onStart { _settingsFlow.value = UiState.loading() }
                .catch {
                    Logger.withTag("Settings").e { "Error loading settings: ${it.message}" }
                    _settingsFlow.value = UiState.error(Res.string.settings_error_loading)
                }
                .collect { settings ->
                    _originalSettings = settings
                    _settingsFlow.value = UiState.success(settings)
                }
        }
    }

    fun onMeasureUnitSelected(unit: MeasureUnit) {
        _settingsFlow.update { state ->
            state.data?.let { UiState.success(it.copy(weightUnit = unit)) } ?: state
        }
    }

    fun onThemeModeSelected(mode: ThemeMode) {
        _settingsFlow.update { state ->
            state.data?.let { UiState.success(it.copy(themeMode = mode)) } ?: state
        }
    }

    fun saveSettings(onSuccess: () -> Unit) {
        val currentSettings = settingsFlow.value.data ?: return
        viewModelScope.launch {
            try {
                settingsRepository.saveSettings(currentSettings)
                onSuccess()
            } catch (e: Exception) {
                Logger.withTag("Settings").e { "Error saving settings: ${e.message}" }
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
