package org.darthacheron.pantrypal.shared.util

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.Foundation.NSData
import platform.Foundation.base64EncodedStringWithOptions
import platform.Foundation.create

actual object HashUtils {
    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    actual fun sha256(input: String): String {
        val data = input.encodeToByteArray()
        val digest = ByteArray(CC_SHA256_DIGEST_LENGTH)

        data.usePinned { inputPinned ->
            digest.usePinned { digestPinned ->
                CC_SHA256(
                    inputPinned.addressOf(0).reinterpret<UByteVar>(),
                    data.size.toUInt(),
                    digestPinned.addressOf(0).reinterpret<UByteVar>()
                )
            }
        }

        return digest.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = digest.size.toULong())
                .base64EncodedStringWithOptions(0UL)
        }
    }
}
