package com.facultytimetable.pro.data.local.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class BackupIntegrityTest {

    @Test
    fun `checksum is deterministic for the same payload`() {
        val json = """{"departments":[{"id":1,"code":"CSE"}],"sections":[{"id":1}]}"""

        assertEquals(BackupIntegrity.checksumOf(json), BackupIntegrity.checksumOf(json))
        assertEquals(64, BackupIntegrity.checksumOf(json).length)
    }

    @Test
    fun `checksum detects any payload change`() {
        val original = """{"departments":[{"id":1,"code":"CSE"}]}"""
        val edited = """{"departments":[{"id":1,"code":"ECE"}]}"""

        assertNotEquals(BackupIntegrity.checksumOf(original), BackupIntegrity.checksumOf(edited))
    }

    @Test
    fun `checksum matches the known sha256 value`() {
        // Well-known SHA-256 of "abc".
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            BackupIntegrity.checksumOf("abc")
        )
    }
}
