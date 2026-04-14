package org.darthacheron.pantrypal.shared.auth

import kotlinx.serialization.Serializable

@Serializable
data class RegistrationResponse(
    val tokenResponse: TokenResponse,
    val user: UserResponse
)