package org.darthacheron.pantrypal.profile

import org.jetbrains.compose.resources.StringResource

data class ProfileValidationState(
    val usernameError: StringResource? = null,
    val emailError: StringResource? = null,
    val serverUrlError: StringResource? = null
)