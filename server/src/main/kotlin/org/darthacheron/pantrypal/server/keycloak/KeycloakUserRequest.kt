package org.darthacheron.pantrypal.server.keycloak

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
internal data class KeycloakUserRequest(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val username: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val email: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val emailVerified: Boolean? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val enabled: Boolean? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val credentials: List<KeycloakCredential>? = null
)

@Serializable
internal data class KeycloakCredential(
    val type: String,
    val value: String,
    val temporary: Boolean
)
