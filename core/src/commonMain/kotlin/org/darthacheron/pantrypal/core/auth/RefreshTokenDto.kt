package org.darthacheron.pantrypal.core.auth

import kotlinx.serialization.Serializable

@Serializable
data class RefreshTokenDto(val refreshToken: String)