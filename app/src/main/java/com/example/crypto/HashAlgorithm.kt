package com.example.crypto

enum class SecurityRating(val label: String, val level: Int) {
    BROKEN("Vulnerável / Quebrado", 1),
    LEGACY("Legado / Fraco", 2),
    SECURE("Seguro (NIST)", 3),
    VERY_SECURE("Alta Segurança", 4),
    CHECKSUM("Checksum / Não-Cripto", 2),
    ENCODING("Codificação / Reversível", 1)
}

enum class HashAlgorithm(
    val id: String,
    val displayName: String,
    val hexLength: Int,
    val bitLength: Int,
    val rating: SecurityRating,
    val description: String,
    val isCryptographic: Boolean = true
) {
    MD5(
        id = "MD5",
        displayName = "MD5",
        hexLength = 32,
        bitLength = 128,
        rating = SecurityRating.BROKEN,
        description = "Algoritmo de 128 bits amplamente usado no md5decrypt. Suscetível a colisões."
    ),
    SHA_1(
        id = "SHA-1",
        displayName = "SHA-1",
        hexLength = 40,
        bitLength = 160,
        rating = SecurityRating.LEGACY,
        description = "160 bits. Quebrado em colisões teóricas pelo Google SHAttered."
    ),
    SHA_224(
        id = "SHA-224",
        displayName = "SHA-224",
        hexLength = 56,
        bitLength = 224,
        rating = SecurityRating.SECURE,
        description = "Variante de 224 bits da família SHA-2."
    ),
    SHA_256(
        id = "SHA-256",
        displayName = "SHA-256",
        hexLength = 64,
        bitLength = 256,
        rating = SecurityRating.SECURE,
        description = "Padrão de indústria SHA-2 (256 bits). Usado em Bitcoin e certificados SSL."
    ),
    SHA_384(
        id = "SHA-384",
        displayName = "SHA-384",
        hexLength = 96,
        bitLength = 384,
        rating = SecurityRating.VERY_SECURE,
        description = "Variante de 384 bits de alta segurança de SHA-2."
    ),
    SHA_512(
        id = "SHA-512",
        displayName = "SHA-512",
        hexLength = 128,
        bitLength = 512,
        rating = SecurityRating.VERY_SECURE,
        description = "Hash de 512 bits de máxima resistência criptográfica."
    ),
    NTLM(
        id = "NTLM",
        displayName = "NTLM (Windows)",
        hexLength = 32,
        bitLength = 128,
        rating = SecurityRating.BROKEN,
        description = "Hash MD4 de string Unicode UTF-16LE usado em senhas do Windows NT."
    ),
    CRC32(
        id = "CRC32",
        displayName = "CRC32",
        hexLength = 8,
        bitLength = 32,
        rating = SecurityRating.CHECKSUM,
        description = "Verificação de redundância cíclica de 32 bits para integridade de dados.",
        isCryptographic = false
    ),
    BASE64(
        id = "BASE64",
        displayName = "Base64",
        hexLength = -1,
        bitLength = 0,
        rating = SecurityRating.ENCODING,
        description = "Esquema de codificação binário para texto ASCII bidirecional.",
        isCryptographic = false
    ),
    ROT13(
        id = "ROT13",
        displayName = "ROT13 / César",
        hexLength = -1,
        bitLength = 0,
        rating = SecurityRating.ENCODING,
        description = "Cifra de substituição simples que rotaciona o alfabeto em 13 posições.",
        isCryptographic = false
    ),
    HMAC_SHA256(
        id = "HMAC-SHA256",
        displayName = "HMAC-SHA256",
        hexLength = 64,
        bitLength = 256,
        rating = SecurityRating.VERY_SECURE,
        description = "Código de autenticação de mensagem hash com chave secreta."
    );

    companion object {
        fun detectPossibleAlgorithms(cleanInput: String): List<HashAlgorithm> {
            val trimmed = cleanInput.trim()
            if (trimmed.isEmpty()) return emptyList()

            val isHex = trimmed.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
            val len = trimmed.length

            val results = mutableListOf<HashAlgorithm>()
            if (isHex) {
                when (len) {
                    8 -> results.add(CRC32)
                    32 -> {
                        results.add(MD5)
                        results.add(NTLM)
                    }
                    40 -> results.add(SHA_1)
                    56 -> results.add(SHA_224)
                    64 -> {
                        results.add(SHA_256)
                        results.add(HMAC_SHA256)
                    }
                    96 -> results.add(SHA_384)
                    128 -> results.add(SHA_512)
                }
            }

            // Also check if valid Base64
            if (trimmed.length % 4 == 0 && trimmed.matches(Regex("^[A-Za-z0-9+/]+={0,2}$"))) {
                results.add(BASE64)
            }

            return results
        }
    }
}
