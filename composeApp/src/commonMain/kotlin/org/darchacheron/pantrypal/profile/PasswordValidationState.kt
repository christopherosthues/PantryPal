package org.darchacheron.pantrypal.profile

import org.jetbrains.compose.resources.StringResource

data class PasswordValidationState(
    val currentPasswordError: StringResource? = null,
    val newPasswordError: StringResource? = null,
    val repeatPasswordError: StringResource? = null
)