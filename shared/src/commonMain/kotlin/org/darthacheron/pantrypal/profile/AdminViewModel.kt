package org.darthacheron.pantrypal.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.darthacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darthacheron.pantrypal.core.configuration.ServerDynamicConfiguration
import org.darthacheron.pantrypal.ui.UiState
import pantrypal.shared.generated.resources.Res
import pantrypal.shared.generated.resources.admin_config_load_error
import pantrypal.shared.generated.resources.admin_config_reload_error
import pantrypal.shared.generated.resources.admin_config_reload_success
import pantrypal.shared.generated.resources.admin_config_save_error
import pantrypal.shared.generated.resources.admin_config_save_success
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
                _uiState.value = UiState.error(Res.string.admin_config_load_error)
                return@launch
            }

            adminNetworkService.fetchConfig(serverUrl)
                .onSuccess { _uiState.value = UiState.success(it) }
                .onFailure { _uiState.value = UiState.error(Res.string.admin_config_load_error) }
        }
    }

    fun updateConfig(config: ServerDynamicConfiguration) {
        viewModelScope.launch {
            val prefs = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
            val serverUrl = prefs?.serverUrl ?: return@launch

            _uiState.value = UiState.loading(config)
            adminNetworkService.updateConfig(serverUrl, config)
                .onSuccess {
                    _uiState.value = UiState.success(it, Res.string.admin_config_save_success)
                }
                .onFailure {
                    _uiState.value = UiState.error(Res.string.admin_config_save_error, config)
                }
        }
    }

    fun reloadConfig() {
        viewModelScope.launch {
            val prefs = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
            val serverUrl = prefs?.serverUrl ?: return@launch

            _uiState.value = UiState.loading(_uiState.value.data)
            adminNetworkService.reloadConfig(serverUrl)
                .onSuccess {
                    _uiState.value = UiState.success(it, Res.string.admin_config_reload_success)
                }
                .onFailure {
                    _uiState.value = UiState.error(Res.string.admin_config_reload_error, _uiState.value.data)
                }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
