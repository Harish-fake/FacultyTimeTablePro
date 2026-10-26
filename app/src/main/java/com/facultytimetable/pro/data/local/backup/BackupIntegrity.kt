package com.facultytimetable.pro.data.local.backup

import java.security.MessageDigest

/**
 * Small, Android-free helper used to sign and verify JSON snapshots.
 *
 * Keeping this in its own object makes the integrity logic directly testable on the JVM and
 * guarantees that the same algorithm is used when a backup is written and when it is read.
 */
object BackupIntegrity {

    fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }

    fun checksumOf(json: String): String = sha256(json.toByteArray(Charsets.UTF_8))
}
