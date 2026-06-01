package org.darchacheron.pantrypal.authentication

import org.jetbrains.compose.resources.StringResource

data class RemoteLogin(
    val username: String = "",
    val password: String = "",
    val serverUrl: String = "",
    val usernameError: StringResource? = null,
    val passwordError: StringResource? = null,
    val serverUrlError: StringResource? = null,
    val canSubmit: Boolean = false
)
