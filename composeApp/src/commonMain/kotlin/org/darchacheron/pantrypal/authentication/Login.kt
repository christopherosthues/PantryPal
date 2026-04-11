package org.darchacheron.pantrypal.authentication

import kotlinx.serialization.Serializable

import kotlinx.serialization.Transient
import org.jetbrains.compose.resources.StringResource

@Serializable
data class Login(
    val username: String,
    val password: String,
    val serverUrl: String = "",
    val loginRemotely: Boolean = false,
    val useSameCredentials: Boolean = true,
    val remoteUsername: String = "",
    val remotePassword: String = "",
    @Transient val usernameError: StringResource? = null,
    @Transient val passwordError: StringResource? = null,
    @Transient val serverUrlError: StringResource? = null,
    @Transient val remoteUsernameError: StringResource? = null,
    @Transient val remotePasswordError: StringResource? = null
)
