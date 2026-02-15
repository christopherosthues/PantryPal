package org.darchacheron.pantrypal.ui

import org.jetbrains.compose.resources.StringResource

data class UiState<T>(
    val data: T? = null,
    val isLoading: Boolean = false,
    val error: StringResource? = null,
) {
    val hasError: Boolean get() = error != null
    val hasData: Boolean get() = data != null

    companion object {
        fun <T> loading() = UiState<T>(isLoading = true)
        fun <T> error(error: StringResource) = UiState<T>(error = error)
        fun <T> success(data: T) = UiState(data = data)
    }
}
