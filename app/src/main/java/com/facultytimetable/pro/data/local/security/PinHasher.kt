package com.facultytimetable.pro.data.local.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * Offline PIN handling. Only a salted SHA-256 digest is stored in DataStore; the PIN itself
 * never touches disk.
 */
object PinHasher {

    fun newSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun hash(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$salt:$pin".toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun verify(pin: String, salt: String, expectedHash: String): Boolean {
        if (salt.isBlank() || expectedHash.isBlank()) return false
        return MessageDigest.isEqual(
            hash(pin, salt).toByteArray(Charsets.UTF_8),
            expectedHash.toByteArray(Charsets.UTF_8)
        )
    }

    fun isValidPin(pin: String): Boolean = pin.length in 4..8 && pin.all { it.isDigit() }
}
