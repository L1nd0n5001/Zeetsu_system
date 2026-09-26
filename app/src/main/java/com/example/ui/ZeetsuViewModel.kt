package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.CrackResult
import com.example.crypto.CryptoEngine
import com.example.crypto.DecryptionEngine
import com.example.crypto.HashAlgorithm
import com.example.crypto.SaltPosition
import com.example.crypto.SecurityRating
import com.example.data.HashEntity
import com.example.data.HashRepository
import com.example.data.ZeetsuDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ZeetsuTab(val title: String) {
    DECRYPT("Decifrar"),
    ENCRYPT("Criar Hash"),
    IDENTIFY("Identificar"),
    COMPARE("Comparar"),
    VAULT("Cofre")
}

data class HashAnalysis(
    val length: Int,
    val isHex: Boolean,
    val possibleAlgorithms: List<HashAlgorithm>,
    val bestGuess: HashAlgorithm?,
    val entropy: Double,
    val charset: String,
    val securityAssessment: String
)

enum class CompareMode {
    HASH_VS_HASH,
    TEXT_VS_HASH
}

class ZeetsuViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ZeetsuDatabase.getDatabase(application)
    private val repository = HashRepository(database.hashDao())
    private val decryptionEngine = DecryptionEngine(repository)

    // Current Tab
    private val _currentTab = MutableStateFlow(ZeetsuTab.DECRYPT)
    val currentTab: StateFlow<ZeetsuTab> = _currentTab.asStateFlow()

    // Notification / Toast message
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // --- DECRYPT STATE ---
    val decryptInput = MutableStateFlow("")
    val decryptSelectedAlgo = MutableStateFlow<HashAlgorithm?>(null) // null = Auto-detect
    private val _isDecrypting = MutableStateFlow(false)
    val isDecrypting: StateFlow<Boolean> = _isDecrypting.asStateFlow()
    private val _crackResult = MutableStateFlow<CrackResult?>(null)
    val crackResult: StateFlow<CrackResult?> = _crackResult.asStateFlow()
    private val _crackProgress = MutableStateFlow(0L)
    val crackProgress: StateFlow<Long> = _crackProgress.asStateFlow()

    // Multi-line batch decrypt
    val multiDecryptInput = MutableStateFlow("")
    private val _multiCrackResults = MutableStateFlow<List<CrackResult>>(emptyList())
    val multiCrackResults: StateFlow<List<CrackResult>> = _multiCrackResults.asStateFlow()

    // --- ENCRYPT STATE ---
    val encryptInput = MutableStateFlow("")
    val encryptSelectedAlgo = MutableStateFlow(HashAlgorithm.MD5)
    val encryptSalt = MutableStateFlow("")
    val encryptSaltPosition = MutableStateFlow(SaltPosition.NONE)
    val encryptSecretKey = MutableStateFlow("")
    val encryptUppercase = MutableStateFlow(false)
    private val _liveHash = MutableStateFlow("")
    val liveHash: StateFlow<String> = _liveHash.asStateFlow()

    // Overview of all algos at once
    private val _allHashesOverview = MutableStateFlow<Map<HashAlgorithm, String>>(emptyMap())
    val allHashesOverview: StateFlow<Map<HashAlgorithm, String>> = _allHashesOverview.asStateFlow()

    // Batch Encrypt
    val batchEncryptInput = MutableStateFlow("")
    private val _batchEncryptOutput = MutableStateFlow("")
    val batchEncryptOutput: StateFlow<String> = _batchEncryptOutput.asStateFlow()

    // --- IDENTIFIER STATE ---
    val identifyInput = MutableStateFlow("")
    private val _analysis = MutableStateFlow<HashAnalysis?>(null)
    val analysis: StateFlow<HashAnalysis?> = _analysis.asStateFlow()

    // --- COMPARE STATE ---
    val compareMode = MutableStateFlow(CompareMode.HASH_VS_HASH)
    val compareInputA = MutableStateFlow("")
    val compareInputB = MutableStateFlow("")
    val compareAlgo = MutableStateFlow(HashAlgorithm.MD5)
    private val _compareMatch = MutableStateFlow<Boolean?>(null)
    val compareMatch: StateFlow<Boolean?> = _compareMatch.asStateFlow()

    // --- VAULT & HISTORY ---
    val allRecords: StateFlow<List<HashEntity>> = repository.allRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customWords = repository.customWords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vaultSearchQuery = MutableStateFlow("")

    init {
        // Compute initial sample hash for 'admin' or empty
        recalculateLiveHash()
    }

    fun selectTab(tab: ZeetsuTab) {
        _currentTab.value = tab
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    // --- DECRYPT ACTIONS ---
    fun onDecryptInputChanged(newVal: String) {
        decryptInput.value = newVal
        _crackResult.value = null
    }

    fun startDecryption() {
        val input = decryptInput.value.trim()
        if (input.isEmpty()) {
            showToast("Por favor, digite ou cole um hash para decifrar.")
            return
        }

        viewModelScope.launch {
            _isDecrypting.value = true
            _crackProgress.value = 0L

            val result = decryptionEngine.decryptHash(
                hashInput = input,
                specifiedAlgo = decryptSelectedAlgo.value,
                onProgress = { count -> _crackProgress.value = count }
            )

            _crackResult.value = result
            _isDecrypting.value = false

            if (result.isSuccess && result.plainText != null) {
                // Save to Room vault automatically
                repository.saveRecord(
                    HashEntity(
                        plainText = result.plainText,
                        hashValue = input.lowercase(),
                        algorithm = result.algorithm.name,
                        source = "DECRYPTED"
                    )
                )
            }
        }
    }

    fun startBatchDecryption() {
        val lines = multiDecryptInput.value.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) {
            showToast("Insira ao menos um hash por linha.")
            return
        }

        viewModelScope.launch {
            _isDecrypting.value = true
            val results = mutableListOf<CrackResult>()
            for (line in lines) {
                val res = decryptionEngine.decryptHash(line, decryptSelectedAlgo.value)
                results.add(res)
                if (res.isSuccess && res.plainText != null) {
                    repository.saveRecord(
                        HashEntity(
                            plainText = res.plainText,
                            hashValue = line.lowercase(),
                            algorithm = res.algorithm.name,
                            source = "DECRYPTED"
                        )
                    )
                }
            }
            _multiCrackResults.value = results
            _isDecrypting.value = false
            showToast("Processamento de ${results.size} hashes concluído!")
        }
    }

    fun loadSampleHash(hash: String, algo: HashAlgorithm? = null) {
        decryptInput.value = hash
        decryptSelectedAlgo.value = algo
        _crackResult.value = null
    }

    // --- ENCRYPT ACTIONS ---
    fun onEncryptInputChanged(newVal: String) {
        encryptInput.value = newVal
        recalculateLiveHash()
    }

    fun onEncryptAlgoChanged(algo: HashAlgorithm) {
        encryptSelectedAlgo.value = algo
        recalculateLiveHash()
    }

    fun onSaltChanged(newSalt: String) {
        encryptSalt.value = newSalt
        recalculateLiveHash()
    }

    fun onSaltPositionChanged(pos: SaltPosition) {
        encryptSaltPosition.value = pos
        recalculateLiveHash()
    }

    fun onSecretKeyChanged(key: String) {
        encryptSecretKey.value = key
        recalculateLiveHash()
    }

    fun toggleUppercase() {
        encryptUppercase.value = !encryptUppercase.value
        recalculateLiveHash()
    }

    fun recalculateLiveHash() {
        val text = encryptInput.value
        val algo = encryptSelectedAlgo.value
        val salt = encryptSalt.value
        val pos = encryptSaltPosition.value
        val key = encryptSecretKey.value
        val upper = encryptUppercase.value

        if (text.isEmpty()) {
            _liveHash.value = ""
            _allHashesOverview.value = emptyMap()
            return
        }

        _liveHash.value = CryptoEngine.generateHash(
            input = text,
            algorithm = algo,
            salt = salt,
            saltPosition = pos,
            secretKey = key,
            uppercase = upper
        )

        // Generate overview of core algorithms simultaneously
        val overview = mutableMapOf<HashAlgorithm, String>()
        val coreAlgos = listOf(
            HashAlgorithm.MD5,
            HashAlgorithm.SHA_1,
            HashAlgorithm.SHA_256,
            HashAlgorithm.SHA_512,
            HashAlgorithm.NTLM,
            HashAlgorithm.CRC32,
            HashAlgorithm.BASE64
        )
        for (a in coreAlgos) {
            overview[a] = CryptoEngine.generateHash(
                input = text,
                algorithm = a,
                salt = salt,
                saltPosition = pos,
                secretKey = key,
                uppercase = upper
            )
        }
        _allHashesOverview.value = overview
    }

    fun saveCurrentHashToVault() {
        val text = encryptInput.value
        val hash = _liveHash.value
        val algo = encryptSelectedAlgo.value.name
        if (text.isEmpty() || hash.isEmpty()) {
            showToast("Nenhum hash gerado para salvar.")
            return
        }

        viewModelScope.launch {
            repository.saveRecord(
                HashEntity(
                    plainText = text,
                    hashValue = hash,
                    algorithm = algo,
                    salt = encryptSalt.value,
                    source = "GENERATED"
                )
            )
            showToast("Hash salvo no cofre Zeetsu!")
        }
    }

    fun executeBatchEncrypt() {
        val lines = batchEncryptInput.value.lines().filter { it.isNotEmpty() }
        if (lines.isEmpty()) {
            showToast("Insira ao menos um texto por linha.")
            return
        }

        val algo = encryptSelectedAlgo.value
        val upper = encryptUppercase.value
        val sb = StringBuilder()

        for (line in lines) {
            val h = CryptoEngine.generateHash(
                input = line,
                algorithm = algo,
                uppercase = upper
            )
            sb.append("$line : $h\n")
        }
        _batchEncryptOutput.value = sb.toString().trimEnd()
        showToast("${lines.size} textos criptografados com sucesso!")
    }

    // --- IDENTIFIER ACTIONS ---
    fun onIdentifyInputChanged(newVal: String) {
        identifyInput.value = newVal
        val trimmed = newVal.trim()
        if (trimmed.isEmpty()) {
            _analysis.value = null
            return
        }

        val len = trimmed.length
        val isHex = trimmed.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
        val possible = HashAlgorithm.detectPossibleAlgorithms(trimmed)
        val bestGuess = possible.firstOrNull()

        // Calculate Shannon entropy
        val freqMap = trimmed.groupingBy { it }.eachCount()
        var entropy = 0.0
        for (count in freqMap.values) {
            val p = count.toDouble() / len
            entropy -= p * (kotlin.math.ln(p) / kotlin.math.ln(2.0))
        }

        val charset = when {
            trimmed.all { it.isDigit() } -> "Numérico (0-9)"
            isHex -> "Hexadecimal (0-9, a-f)"
            trimmed.all { it in 'a'..'z' || it in 'A'..'Z' } -> "Alfabético"
            trimmed.all { it.isLetterOrDigit() } -> "Alfanumérico"
            else -> "Caracteres especiais / ASCII estendido"
        }

        val assessment = when (bestGuess?.rating) {
            SecurityRating.BROKEN -> "CRÍTICO: Este algoritmo possui vulnerabilidades graves e colisões conhecidas. Não deve ser usado para segurança nova."
            SecurityRating.LEGACY -> "OBSOLETO: Considerado fraco para padrões modernos pelo NIST. Substitua por SHA-256 ou SHA-512."
            SecurityRating.SECURE -> "SEGURO: Algoritmo robusto recomendado pelo NIST e em conformidade com as práticas atuais de segurança."
            SecurityRating.VERY_SECURE -> "MÁXIMA RESISTÊNCIA: Nível criptográfico altíssimo, resistente a ataques de força bruta massivos."
            SecurityRating.CHECKSUM -> "CHECKSUM: Feito para verificação de integridade de dados e arquivos, não oferece segurança criptográfica."
            SecurityRating.ENCODING -> "CODIFICAÇÃO: Fácil e totalmente reversível sem necessidade de chaves."
            null -> "Padrão de hash desconhecido ou string customizada."
        }

        _analysis.value = HashAnalysis(
            length = len,
            isHex = isHex,
            possibleAlgorithms = possible,
            bestGuess = bestGuess,
            entropy = (entropy * 100).toInt() / 100.0,
            charset = charset,
            securityAssessment = assessment
        )
    }

    // --- COMPARE ACTIONS ---
    fun onCompareInputAChanged(newVal: String) {
        compareInputA.value = newVal
        runComparison()
    }

    fun onCompareInputBChanged(newVal: String) {
        compareInputB.value = newVal
        runComparison()
    }

    fun setCompareMode(mode: CompareMode) {
        compareMode.value = mode
        runComparison()
    }

    fun setCompareAlgo(algo: HashAlgorithm) {
        compareAlgo.value = algo
        runComparison()
    }

    private fun runComparison() {
        val a = compareInputA.value.trim()
        val b = compareInputB.value.trim()

        if (a.isEmpty() || b.isEmpty()) {
            _compareMatch.value = null
            return
        }

        when (compareMode.value) {
            CompareMode.HASH_VS_HASH -> {
                _compareMatch.value = a.equals(b, ignoreCase = true)
            }
            CompareMode.TEXT_VS_HASH -> {
                val computed = CryptoEngine.generateHash(a, compareAlgo.value)
                _compareMatch.value = computed.equals(b, ignoreCase = true)
            }
        }
    }

    // --- VAULT ACTIONS ---
    fun deleteRecord(id: Long) {
        viewModelScope.launch {
            repository.deleteRecord(id)
            showToast("Registro removido do cofre.")
        }
    }

    fun toggleFavorite(record: HashEntity) {
        viewModelScope.launch {
            repository.updateRecord(record.copy(isFavorite = !record.isFavorite))
        }
    }

    fun clearVaultHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            showToast("Histórico do cofre limpo.")
        }
    }

    fun addCustomDictionaryWord(word: String) {
        viewModelScope.launch {
            if (word.isNotBlank()) {
                repository.addCustomWord(word)
                showToast("Palavra '$word' adicionada ao dicionário de decifração!")
            }
        }
    }

    fun deleteCustomWord(id: Long) {
        viewModelScope.launch {
            repository.deleteCustomWord(id)
            showToast("Palavra removida do dicionário.")
        }
    }
}
