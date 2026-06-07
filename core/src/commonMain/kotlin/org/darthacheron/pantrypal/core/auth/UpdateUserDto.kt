package org.darthacheron.pantrypal.core.auth

import kotlinx.serialization.Serializable

@Serializable
data class UpdateUserDto(
    val username: String? = null,
    val email: String? = null,
    val password: String? = null,
    val currentPassword: String? = null
)