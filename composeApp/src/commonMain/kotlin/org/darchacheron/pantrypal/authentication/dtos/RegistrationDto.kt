package org.darchacheron.pantrypal.authentication.dtos

import kotlinx.serialization.Serializable

@Serializable
data class RegistrationDto(
    val username: String,
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String
)
