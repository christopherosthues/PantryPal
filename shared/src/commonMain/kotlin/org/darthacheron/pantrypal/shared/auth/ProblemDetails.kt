package org.darthacheron.pantrypal.shared.auth

import kotlinx.serialization.Serializable

@Serializable
data class ProblemDetails(
    // TODO: use newer rfc 9467 standard
    val type: String? = null,
    val title: String? = null,
    val status: Int? = null,
    val detail: String? = null,
    val instance: String? = null,
    val errors: Map<String, List<String>>? = null
)