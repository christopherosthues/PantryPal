package org.darthacheron.pantrypal.shared.util

expect object HashUtils {
    fun sha256(input: String): String
}
