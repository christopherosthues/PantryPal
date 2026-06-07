package org.darthacheron.pantrypal.authentication

import org.jetbrains.compose.resources.StringResource

data class Registration(
    val userName: String,
    val email: String,
    val password: String,
    val repeatedPassword: String,
    val userNameError: StringResource? = null,
    val emailError: StringResource? = null,
    val passwordError: StringResource? = null,
    val repeatedPasswordError: StringResource? = null,
    val stayLoggedIn: Boolean = false,
    val canRegister: Boolean = false,
)
