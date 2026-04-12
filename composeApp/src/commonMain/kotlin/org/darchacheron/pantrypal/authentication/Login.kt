package org.darchacheron.pantrypal.authentication

import org.jetbrains.compose.resources.StringResource

data class Login(
    val username: String,
    val password: String,
    val serverUrl: String = "",
    val loginRemotely: Boolean = false,
    val useSameCredentials: Boolean = true,
    val remoteUsername: String = "",
    val remotePassword: String = "",
    val usernameError: StringResource? = null,
    val passwordError: StringResource? = null,
    val serverUrlError: StringResource? = null,
    val remoteUsernameError: StringResource? = null,
    val remotePasswordError: StringResource? = null
)
