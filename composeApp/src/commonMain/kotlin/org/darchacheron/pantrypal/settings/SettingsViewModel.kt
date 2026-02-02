package org.darchacheron.pantrypal.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Success(val settings: Settings) : SettingsUiState
    data class Error(val message: String) : SettingsUiState
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = settingsRepository.getSettingsFlow()
        .map { settings -> SettingsUiState.Success(settings) as SettingsUiState }
        .catch { emit(SettingsUiState.Error(it.message ?: "Unknown error")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsUiState.Loading
        )

    fun onMeasureUnitSelected(unit: MeasureUnit) {
        viewModelScope.launch {
            val currentSettings = (uiState.value as? SettingsUiState.Success)?.settings ?: return@launch
            settingsRepository.saveSettings(currentSettings.copy(weightUnit = unit))
        }
    }

    fun onThemeModeSelected(mode: ThemeMode) {
        viewModelScope.launch {
            val currentSettings = (uiState.value as? SettingsUiState.Success)?.settings ?: return@launch
            settingsRepository.saveSettings(currentSettings.copy(themeMode = mode))
        }
    }
}
