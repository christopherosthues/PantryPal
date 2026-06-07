package org.darthacheron.pantrypal.core.util

expect object HashUtils {
    fun sha256(input: String): String
}
