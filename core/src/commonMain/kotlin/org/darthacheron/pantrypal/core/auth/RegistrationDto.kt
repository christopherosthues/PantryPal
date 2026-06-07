package org.darthacheron.pantrypal.core.auth

import kotlinx.serialization.Serializable

@Serializable
data class RegistrationDto(
    val username: String,
    val email: String,
    val password: String
)