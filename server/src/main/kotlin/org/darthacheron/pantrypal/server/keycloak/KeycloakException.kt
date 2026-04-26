package org.darthacheron.pantrypal.server.keycloak

import io.ktor.http.HttpStatusCode

class KeycloakException(val status: HttpStatusCode, override val message: String) : Exception(message)