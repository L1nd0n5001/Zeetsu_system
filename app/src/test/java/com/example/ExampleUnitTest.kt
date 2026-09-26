package com.example

import com.example.crypto.CryptoEngine
import com.example.crypto.HashAlgorithm
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testMd5Generation() {
        val hash = CryptoEngine.generateHash("hello", HashAlgorithm.MD5)
        assertEquals("5d41402abc4b2a76b9719d911017c592", hash)
    }

    @Test
    fun testSha1Generation() {
        val hash = CryptoEngine.generateHash("hello", HashAlgorithm.SHA_1)
        assertEquals("aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d", hash)
    }

    @Test
    fun testSha256Generation() {
        val hash = CryptoEngine.generateHash("hello", HashAlgorithm.SHA_256)
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", hash)
    }

    @Test
    fun testNtlmGeneration() {
        val hash = CryptoEngine.generateHash("password", HashAlgorithm.NTLM)
        assertEquals("8846f7eaee8fb117ad06bdd830b7586c", hash)
    }

    @Test
    fun testCrc32Generation() {
        val hash = CryptoEngine.generateHash("hello", HashAlgorithm.CRC32)
        assertEquals("3610a686", hash)
    }

    @Test
    fun testRot13() {
        val encoded = CryptoEngine.rot13("hello")
        assertEquals("uryyb", encoded)
        val decoded = CryptoEngine.rot13(encoded)
        assertEquals("hello", decoded)
    }
}
