package org.darthacheron.pantrypal.core.auth

import kotlinx.serialization.Serializable

@Serializable
data class RegistrationResponse(
    val tokenResponse: TokenResponse,
    val user: UserResponse
)