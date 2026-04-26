package org.darthacheron.pantrypal.server.keycloak

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class KeycloakErrorResponse(
    val error: String,
    @SerialName("error_description") val errorDescription: String? = null
)