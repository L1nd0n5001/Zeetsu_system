package com.example.crypto

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.zip.CRC32
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

enum class SaltPosition(val label: String) {
    NONE("Sem Salt"),
    PREFIX("Prefixo (Salt + Texto)"),
    SUFFIX("Sufixo (Texto + Salt)"),
    BOTH("Ambos (Salt + Texto + Salt)")
}

object CryptoEngine {

    fun generateHash(
        input: String,
        algorithm: HashAlgorithm,
        salt: String = "",
        saltPosition: SaltPosition = SaltPosition.NONE,
        secretKey: String = "",
        uppercase: Boolean = false
    ): String {
        val saltedInput = when (saltPosition) {
            SaltPosition.NONE -> input
            SaltPosition.PREFIX -> "$salt$input"
            SaltPosition.SUFFIX -> "$input$salt"
            SaltPosition.BOTH -> "$salt$input$salt"
        }

        val result = when (algorithm) {
            HashAlgorithm.MD5 -> digest("MD5", saltedInput)
            HashAlgorithm.SHA_1 -> digest("SHA-1", saltedInput)
            HashAlgorithm.SHA_224 -> digest("SHA-224", saltedInput)
            HashAlgorithm.SHA_256 -> digest("SHA-256", saltedInput)
            HashAlgorithm.SHA_384 -> digest("SHA-384", saltedInput)
            HashAlgorithm.SHA_512 -> digest("SHA-512", saltedInput)
            HashAlgorithm.NTLM -> ntlm(saltedInput)
            HashAlgorithm.CRC32 -> crc32(saltedInput)
            HashAlgorithm.BASE64 -> encodeBase64(saltedInput)
            HashAlgorithm.ROT13 -> rot13(saltedInput)
            HashAlgorithm.HMAC_SHA256 -> hmac("HmacSHA256", saltedInput, secretKey.ifEmpty { "zeetsu_key" })
        }

        return if (uppercase && algorithm != HashAlgorithm.BASE64) {
            result.uppercase()
        } else {
            result.lowercase()
        }
    }

    private fun digest(algorithmName: String, input: String): String {
        val md = MessageDigest.getInstance(algorithmName)
        val bytes = md.digest(input.toByteArray(StandardCharsets.UTF_8))
        return bytesToHex(bytes)
    }

    fun crc32(input: String): String {
        val crc = CRC32()
        crc.update(input.toByteArray(StandardCharsets.UTF_8))
        return String.format("%08x", crc.value)
    }

    fun encodeBase64(input: String): String {
        return Base64.encodeToString(input.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
    }

    fun decodeBase64(input: String): String? {
        return try {
            val bytes = Base64.decode(input.trim(), Base64.DEFAULT)
            String(bytes, StandardCharsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    fun rot13(input: String): String {
        val sb = StringBuilder()
        for (c in input) {
            when (c) {
                in 'a'..'z' -> {
                    val shifted = ((c - 'a' + 13) % 26) + 'a'.code
                    sb.append(shifted.toChar())
                }
                in 'A'..'Z' -> {
                    val shifted = ((c - 'A' + 13) % 26) + 'A'.code
                    sb.append(shifted.toChar())
                }
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    fun hmac(algorithm: String, input: String, secretKey: String): String {
        val keySpec = SecretKeySpec(secretKey.toByteArray(StandardCharsets.UTF_8), algorithm)
        val mac = Mac.getInstance(algorithm)
        mac.init(keySpec)
        val bytes = mac.doFinal(input.toByteArray(StandardCharsets.UTF_8))
        return bytesToHex(bytes)
    }

    /**
     * NTLM hash is MD4(UTF-16LE(password))
     */
    fun ntlm(password: String): String {
        val utf16Bytes = password.toByteArray(StandardCharsets.UTF_16LE)
        val md4Bytes = md4(utf16Bytes)
        return bytesToHex(md4Bytes)
    }

    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        val digits = "0123456789abcdef"
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = digits[v ushr 4]
            hexChars[i * 2 + 1] = digits[v and 0x0F]
        }
        return String(hexChars)
    }

    /**
     * Native MD4 implementation (RFC 1320)
     */
    fun md4(message: ByteArray): ByteArray {
        val a = intArrayOf(0x67452301)
        val b = intArrayOf(0xefcdab89.toInt())
        val c = intArrayOf(0x98badcfe.toInt())
        val d = intArrayOf(0x10325476)

        val origLen = message.size
        val numBlocks = ((origLen + 8) ushr 6) + 1
        val totalLen = numBlocks shl 6
        val paddingBytes = ByteArray(totalLen - origLen)
        paddingBytes[0] = 0x80.toByte()

        var currentBlock = 0
        val x = IntArray(16)

        fun ff(aa: Int, bb: Int, cc: Int, dd: Int, xk: Int, s: Int): Int {
            val t = aa + ((bb and cc) or (bb.inv() and dd)) + xk
            return (t shl s) or (t ushr (32 - s))
        }

        fun gg(aa: Int, bb: Int, cc: Int, dd: Int, xk: Int, s: Int): Int {
            val t = aa + ((bb and cc) or (bb and dd) or (cc and dd)) + xk + 0x5a827999
            return (t shl s) or (t ushr (32 - s))
        }

        fun hh(aa: Int, bb: Int, cc: Int, dd: Int, xk: Int, s: Int): Int {
            val t = aa + (bb xor cc xor dd) + xk + 0x6ed9eba1
            return (t shl s) or (t ushr (32 - s))
        }

        for (i in 0 until numBlocks) {
            for (j in 0 until 16) {
                val byteIndex = (i shl 6) + (j shl 2)
                var word = 0
                for (k in 0 until 4) {
                    val idx = byteIndex + k
                    val bVal: Int = if (idx < origLen) {
                        message[idx].toInt() and 0xFF
                    } else if (idx < origLen + paddingBytes.size) {
                        paddingBytes[idx - origLen].toInt() and 0xFF
                    } else {
                        val bitLen = (origLen.toLong() * 8)
                        ((bitLen ushr ((idx - totalLen + 8) * 8)) and 0xFF).toInt()
                    }
                    word = word or (bVal shl (k * 8))
                }
                x[j] = word
            }

            var aa = a[0]
            var bb = b[0]
            var cc = c[0]
            var dd = d[0]

            // Round 1
            aa = ff(aa, bb, cc, dd, x[0], 3); dd = ff(dd, aa, bb, cc, x[1], 7)
            cc = ff(cc, dd, aa, bb, x[2], 11); bb = ff(bb, cc, dd, aa, x[3], 19)
            aa = ff(aa, bb, cc, dd, x[4], 3); dd = ff(dd, aa, bb, cc, x[5], 7)
            cc = ff(cc, dd, aa, bb, x[6], 11); bb = ff(bb, cc, dd, aa, x[7], 19)
            aa = ff(aa, bb, cc, dd, x[8], 3); dd = ff(dd, aa, bb, cc, x[9], 7)
            cc = ff(cc, dd, aa, bb, x[10], 11); bb = ff(bb, cc, dd, aa, x[11], 19)
            aa = ff(aa, bb, cc, dd, x[12], 3); dd = ff(dd, aa, bb, cc, x[13], 7)
            cc = ff(cc, dd, aa, bb, x[14], 11); bb = ff(bb, cc, dd, aa, x[15], 19)

            // Round 2
            aa = gg(aa, bb, cc, dd, x[0], 3); dd = gg(dd, aa, bb, cc, x[4], 5)
            cc = gg(cc, dd, aa, bb, x[8], 9); bb = gg(bb, cc, dd, aa, x[12], 13)
            aa = gg(aa, bb, cc, dd, x[1], 3); dd = gg(dd, aa, bb, cc, x[5], 5)
            cc = gg(cc, dd, aa, bb, x[9], 9); bb = gg(bb, cc, dd, aa, x[13], 13)
            aa = gg(aa, bb, cc, dd, x[2], 3); dd = gg(dd, aa, bb, cc, x[6], 5)
            cc = gg(cc, dd, aa, bb, x[10], 9); bb = gg(bb, cc, dd, aa, x[14], 13)
            aa = gg(aa, bb, cc, dd, x[3], 3); dd = gg(dd, aa, bb, cc, x[7], 5)
            cc = gg(cc, dd, aa, bb, x[11], 9); bb = gg(bb, cc, dd, aa, x[15], 13)

            // Round 3
            aa = hh(aa, bb, cc, dd, x[0], 3); dd = hh(dd, aa, bb, cc, x[8], 9)
            cc = hh(cc, dd, aa, bb, x[4], 11); bb = hh(bb, cc, dd, aa, x[12], 15)
            aa = hh(aa, bb, cc, dd, x[2], 3); dd = hh(dd, aa, bb, cc, x[10], 9)
            cc = hh(cc, dd, aa, bb, x[6], 11); bb = hh(bb, cc, dd, aa, x[14], 15)
            aa = hh(aa, bb, cc, dd, x[1], 3); dd = hh(dd, aa, bb, cc, x[9], 9)
            cc = hh(cc, dd, aa, bb, x[5], 11); bb = hh(bb, cc, dd, aa, x[13], 15)
            aa = hh(aa, bb, cc, dd, x[3], 3); dd = hh(dd, aa, bb, cc, x[11], 9)
            cc = hh(cc, dd, aa, bb, x[7], 11); bb = hh(bb, cc, dd, aa, x[15], 15)

            a[0] += aa
            b[0] += bb
            c[0] += cc
            d[0] += dd
        }

        val out = ByteArray(16)
        for (i in 0 until 4) {
            out[i] = (a[0] ushr (i * 8)).toByte()
            out[i + 4] = (b[0] ushr (i * 8)).toByte()
            out[i + 8] = (c[0] ushr (i * 8)).toByte()
            out[i + 12] = (d[0] ushr (i * 8)).toByte()
        }
        return out
    }
}
