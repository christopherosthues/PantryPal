package org.darthacheron.pantrypal.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.darthacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darthacheron.pantrypal.ui.UiState
import org.darthacheron.pantrypal.core.configuration.ServerDynamicConfiguration
import pantrypal.shared.generated.resources.Res
import pantrypal.shared.generated.resources.settings_error_loading
import pantrypal.shared.generated.resources.settings_error_saving
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class AdminViewModel(
    private val adminNetworkService: AdminNetworkService,
    private val preferencesRepository: AuthenticationPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<ServerDynamicConfiguration>>(UiState.loading())
    val uiState: StateFlow<UiState<ServerDynamicConfiguration>> = _uiState.asStateFlow()

    init {
        loadConfig()
    }

    fun loadConfig() {
        viewModelScope.launch {
            _uiState.value = UiState.loading()
            val prefs = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
            val serverUrl = prefs?.serverUrl
            if (serverUrl.isNullOrBlank()) {
                _uiState.value = UiState.error(Res.string.settings_error_loading)
                return@launch
            }

            adminNetworkService.fetchConfig(serverUrl)
                .onSuccess { _uiState.value = UiState.success(it) }
                .onFailure { _uiState.value = UiState.error(Res.string.settings_error_loading) }
        }
    }

    fun updateConfig(config: ServerDynamicConfiguration) {
        viewModelScope.launch {
            val currentData = _uiState.value.data ?: return@launch
            _uiState.value = UiState.loading(currentData)
            
            val prefs = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
            val serverUrl = prefs?.serverUrl
            if (serverUrl.isNullOrBlank()) {
                _uiState.value = UiState.error(Res.string.settings_error_saving, currentData)
                return@launch
            }

            adminNetworkService.updateConfig(serverUrl, config)
                .onSuccess { _uiState.value = UiState.success(it) }
                .onFailure { _uiState.value = UiState.error(Res.string.settings_error_saving, currentData) }
        }
    }

    fun reloadConfigFromServer() {
        viewModelScope.launch {
            val currentData = _uiState.value.data ?: return@launch
            _uiState.value = UiState.loading(currentData)

            val prefs = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
            val serverUrl = prefs?.serverUrl
            if (serverUrl.isNullOrBlank()) {
                _uiState.value = UiState.error(Res.string.settings_error_loading, currentData)
                return@launch
            }

            adminNetworkService.reloadConfig(serverUrl)
                .onSuccess { _uiState.value = UiState.success(it) }
                .onFailure { _uiState.value = UiState.error(Res.string.settings_error_loading, currentData) }
        }
    }
}
