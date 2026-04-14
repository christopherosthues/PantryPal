package org.darthacheron.pantrypal.shared.auth

import kotlinx.serialization.Serializable

@Serializable
data class RegistrationDto(
    val username: String,
    val email: String,
    val password: String
)