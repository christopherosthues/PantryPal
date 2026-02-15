package org.darchacheron.pantrypal.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.darchacheron.pantrypal.ui.UiState
import pantrypal.composeapp.generated.resources.Res
import pantrypal.composeapp.generated.resources.settings_error_loading
import pantrypal.composeapp.generated.resources.settings_error_saving

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _saveError = MutableStateFlow<org.jetbrains.compose.resources.StringResource?>(null)

    val uiState: StateFlow<UiState<Settings>> = settingsRepository.getSettingsFlow()
        .map { settings -> UiState.success(settings) }
        .catch { emit(UiState.error(Res.string.settings_error_loading)) }
        .combine(_saveError) { state, error ->
            if (error != null) state.copy(error = error) else state
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UiState.loading()
        )

    fun onMeasureUnitSelected(unit: MeasureUnit) {
        saveSettings { it.copy(weightUnit = unit) }
    }

    fun onThemeModeSelected(mode: ThemeMode) {
        saveSettings { it.copy(themeMode = mode) }
    }

    private fun saveSettings(update: (Settings) -> Settings) {
        viewModelScope.launch {
            try {
                _saveError.update { null }
                val currentSettings = uiState.value.data ?: return@launch
                settingsRepository.saveSettings(update(currentSettings))
            } catch (e: Exception) {
                _saveError.update { Res.string.settings_error_saving }
            }
        }
    }

    fun onErrorDismissed() {
        _saveError.update { null }
    }
}
