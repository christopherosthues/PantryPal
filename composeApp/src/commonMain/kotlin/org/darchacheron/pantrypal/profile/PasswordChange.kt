package org.darchacheron.pantrypal.profile

data class PasswordChange(
    val current: String = "",
    val new: String = "",
    val repeat: String = ""
)