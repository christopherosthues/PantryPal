package org.darchacheron.pantrypal.authentication

fun hashPassword(password: String?): String? {
    // TODO: Use something like BCrypt or Argon2
    return password
}

fun verifyPassword(password: String?, hashedPassword: String?): Boolean {
    return hashPassword(password) == hashedPassword
}

