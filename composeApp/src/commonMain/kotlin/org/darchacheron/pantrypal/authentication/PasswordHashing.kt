package org.darchacheron.pantrypal.authentication

fun hashPassword(password: String): String {
    return password
}

fun verifyPassword(password: String?, hashedPassword: String?): Boolean {
    return password == hashedPassword
}

