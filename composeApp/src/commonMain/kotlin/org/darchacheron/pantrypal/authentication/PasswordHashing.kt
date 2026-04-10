package org.darchacheron.pantrypal.authentication

import org.darthacheron.pantrypal.shared.util.HashUtils

fun hashPassword(password: String?): String? {
    if (password == null) return null
    return HashUtils.sha256(password)
}

fun verifyPassword(password: String?, hashedPassword: String?): Boolean {
    return hashPassword(password) == hashedPassword
}
