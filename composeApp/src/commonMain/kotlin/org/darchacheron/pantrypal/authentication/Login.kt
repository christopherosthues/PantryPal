package org.darchacheron.pantrypal.authentication

import kotlinx.serialization.Serializable

@Serializable
data class Login(val username: String, val password: String, val isLocalOnly: Boolean = false)