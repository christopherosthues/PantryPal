package org.darthacheron.pantrypal.shared.util

import java.security.MessageDigest
import android.util.Base64

actual object HashUtils {
    actual fun sha256(input: String): String {
        val bytes = input.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return Base64.encodeToString(digest, Base64.DEFAULT)
    }
}
