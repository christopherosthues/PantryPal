package org.darchacheron.pantrypal.authentication.dtos

import kotlinx.serialization.Serializable

@Serializable
data class RefreshTokenDto(val refreshToken: String)
