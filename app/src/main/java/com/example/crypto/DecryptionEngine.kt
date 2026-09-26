package com.example.crypto

import com.example.data.HashRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class CrackResult(
    val isSuccess: Boolean,
    val plainText: String?,
    val algorithm: HashAlgorithm,
    val timeTakenMs: Long,
    val checkedCount: Long,
    val source: String,
    val message: String
)

class DecryptionEngine(
    private val repository: HashRepository,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()
) {

    suspend fun decryptHash(
        hashInput: String,
        specifiedAlgo: HashAlgorithm? = null,
        onProgress: ((checked: Long) -> Unit)? = null
    ): CrackResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val targetHash = hashInput.trim().lowercase()

        if (targetHash.isEmpty()) {
            return@withContext CrackResult(
                isSuccess = false,
                plainText = null,
                algorithm = specifiedAlgo ?: HashAlgorithm.MD5,
                timeTakenMs = 0,
                checkedCount = 0,
                source = "Entrada Vazia",
                message = "Por favor, insira um hash válido para decifrar."
            )
        }

        // Determine algorithms to test
        val algosToTest = if (specifiedAlgo != null) {
            listOf(specifiedAlgo)
        } else {
            val detected = HashAlgorithm.detectPossibleAlgorithms(targetHash)
            if (detected.isNotEmpty()) detected else listOf(HashAlgorithm.MD5, HashAlgorithm.SHA_1, HashAlgorithm.SHA_256)
        }

        // 1. Check if Base64 decode
        if (algosToTest.contains(HashAlgorithm.BASE64)) {
            val decoded = CryptoEngine.decodeBase64(targetHash)
            if (decoded != null && decoded.isNotBlank() && decoded != targetHash) {
                return@withContext CrackResult(
                    isSuccess = true,
                    plainText = decoded,
                    algorithm = HashAlgorithm.BASE64,
                    timeTakenMs = System.currentTimeMillis() - startTime,
                    checkedCount = 1,
                    source = "Decodificador Base64",
                    message = "Texto decodificado com sucesso a partir de Base64!"
                )
            }
        }

        // 2. Check Local Database / Vault
        for (algo in algosToTest) {
            val dbMatch = repository.findByHashAndAlgo(targetHash, algo.name)
                ?: repository.findByHash(targetHash)
            if (dbMatch != null) {
                return@withContext CrackResult(
                    isSuccess = true,
                    plainText = dbMatch.plainText,
                    algorithm = algo,
                    timeTakenMs = System.currentTimeMillis() - startTime,
                    checkedCount = 1,
                    source = "Cofre Zeetsu / Cache Local",
                    message = "Encontrado instantaneamente no cache de dados Zeetsu System!"
                )
            }
        }

        var checkedCount = 0L

        // 3. User Custom Wordlist
        val customWords = repository.getCustomWords()
        for (word in customWords) {
            checkedCount++
            for (algo in algosToTest) {
                if (!algo.isCryptographic && algo != HashAlgorithm.CRC32) continue
                val gen = CryptoEngine.generateHash(word, algo)
                if (gen.equals(targetHash, ignoreCase = true)) {
                    return@withContext CrackResult(
                        isSuccess = true,
                        plainText = word,
                        algorithm = algo,
                        timeTakenMs = System.currentTimeMillis() - startTime,
                        checkedCount = checkedCount,
                        source = "Dicionário Personalizado",
                        message = "Sucesso! Encontrado no dicionário personalizado do usuário."
                    )
                }
            }
        }

        // 4. In-Memory Rainbow Dictionary & PIN permutations
        for (candidate in RainbowDictionary.getCandidateSequence()) {
            checkedCount++
            if (checkedCount % 5000 == 0L) {
                onProgress?.invoke(checkedCount)
            }

            for (algo in algosToTest) {
                if (!algo.isCryptographic && algo != HashAlgorithm.CRC32) continue
                val gen = CryptoEngine.generateHash(candidate, algo)
                if (gen.equals(targetHash, ignoreCase = true)) {
                    return@withContext CrackResult(
                        isSuccess = true,
                        plainText = candidate,
                        algorithm = algo,
                        timeTakenMs = System.currentTimeMillis() - startTime,
                        checkedCount = checkedCount,
                        source = "Tabela Rainbow Zeetsu (Alta Velocidade)",
                        message = "Hash quebrado com sucesso na busca rápida por dicionário!"
                    )
                }
            }
        }

        // 5. Online lookup fallback query
        val onlineResult = tryOnlineLookup(targetHash, algosToTest.firstOrNull() ?: HashAlgorithm.MD5)
        if (onlineResult != null) {
            return@withContext CrackResult(
                isSuccess = true,
                plainText = onlineResult,
                algorithm = algosToTest.firstOrNull() ?: HashAlgorithm.MD5,
                timeTakenMs = System.currentTimeMillis() - startTime,
                checkedCount = checkedCount + 1,
                source = "Base de Dados Hash Online",
                message = "Decifrado com sucesso via base global de hashes!"
            )
        }

        val primaryAlgo = algosToTest.firstOrNull() ?: HashAlgorithm.MD5
        return@withContext CrackResult(
            isSuccess = false,
            plainText = null,
            algorithm = primaryAlgo,
            timeTakenMs = System.currentTimeMillis() - startTime,
            checkedCount = checkedCount,
            source = "Busca Concluída",
            message = "Hash não encontrado na tabela rápida de senhas comuns ($checkedCount combinações testadas). Adicione palavras ao dicionário ou tente outro algoritmo."
        )
    }

    private fun tryOnlineLookup(hash: String, algo: HashAlgorithm): String? {
        return try {
            // Test query on free hash reverse service
            val url = "https://hashtoolkit.com/reverse-hash?hash=$hash"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 ZeetsuSystem/1.0")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return null
                    // Parse typical response snippet: <span title="decrypted md5 hash">...</span>
                    val pattern = Regex("res-hash\">([^<]+)<|title=\"decrypted[^>]*>([^<]+)<")
                    val match = pattern.find(body)
                    match?.groupValues?.getOrNull(1)?.ifBlank { null }
                        ?: match?.groupValues?.getOrNull(2)?.ifBlank { null }
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }
}
