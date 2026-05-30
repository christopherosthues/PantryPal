package org.darchacheron.pantrypal.authentication

import org.jetbrains.compose.resources.StringResource

data class Login(
    val username: String,
    val password: String,
    val usernameError: StringResource? = null,
    val passwordError: StringResource? = null,
    val stayLoggedIn: Boolean = false,
    val canLogin: Boolean = false
)
