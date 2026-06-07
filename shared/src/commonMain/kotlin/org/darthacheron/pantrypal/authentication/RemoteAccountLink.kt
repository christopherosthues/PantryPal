package org.darthacheron.pantrypal.authentication

import org.jetbrains.compose.resources.StringResource

data class RemoteAccountLink(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val repeatedPassword: String = "",
    val serverUrl: String = "",
    val isCreatingNew: Boolean = false,
    val usernameError: StringResource? = null,
    val emailError: StringResource? = null,
    val passwordError: StringResource? = null,
    val repeatedPasswordError: StringResource? = null,
    val serverUrlError: StringResource? = null,
    val isTestingConnection: Boolean = false,
    val connectionTestSuccess: Boolean? = null,
    val canSubmit: Boolean = false
)
