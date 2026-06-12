package org.darthacheron.pantrypal.ui

import org.jetbrains.compose.resources.StringResource

data class UiState<T>(
    val data: T? = null,
    val isLoading: Boolean = false,
    val error: StringResource? = null,
    val message: StringResource? = null,
) {
    val hasError: Boolean get() = error != null
    val hasData: Boolean get() = data != null

    companion object {
        fun <T> loading(data: T? = null) = UiState(data = data, isLoading = true)
        fun <T> error(error: StringResource, data: T? = null) = UiState(data = data, error = error)
        fun <T> success(data: T, message: StringResource? = null) = UiState(data = data, message = message)
    }
}
